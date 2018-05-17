package io.choerodon.manager.api.eventhandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.choerodon.manager.api.dto.RegisterInstancePayload;
import io.choerodon.manager.domain.service.IDocumentService;
import io.choerodon.manager.domain.service.IRouteService;
import io.choerodon.manager.domain.service.SwaggerRefreshService;

/**
 * eureka-instance消息队列的新消息监听处理
 *
 * @author zhipeng.zuo
 * @author wuguokai
 */
@Component
public class EurekaInstanceRegisteredListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EurekaInstanceRegisteredListener.class);
    private static final String STATUS_UP = "UP";
    private static final long QUERY_INTERVAL = (3 * 1000);
    private final ObjectMapper mapper = new ObjectMapper();
    @Value("${choerodon.swagger.skip.service}")
    private String[] skipService;

    private ConcurrentMap<String, RegisterInstancePayload> map = new ConcurrentHashMap<>();

    private ConcurrentMap<String, RegisterInstancePayload> routeMap = new ConcurrentHashMap<>();

    private ConcurrentMap<String, Integer> countMap = new ConcurrentHashMap();

    private ConcurrentMap<String, Integer> routeCountMap = new ConcurrentHashMap();

    private IDocumentService iDocumentService;

    private SwaggerRefreshService swaggerRefreshService;

    private IRouteService iRouteService;

    public EurekaInstanceRegisteredListener(IDocumentService iDocumentService,
                                            SwaggerRefreshService swaggerRefreshService,
                                            IRouteService iRouteService) {
        this.iDocumentService = iDocumentService;
        this.swaggerRefreshService = swaggerRefreshService;
        this.iRouteService = iRouteService;
    }

    /**
     * 监听eureka-instance消息队列的新消息处理
     *
     * @param record 消息信息
     * @throws Exception 异常
     */
    @KafkaListener(topics = "register-server")
    public void handle(ConsumerRecord<byte[], byte[]> record) throws IOException {
        LOGGER.info("******-->******");
        String message = new String(record.value());
        LOGGER.info("receive message from register-server, {}", message);
        RegisterInstancePayload registerInstancePayload = mapper.readValue(message, RegisterInstancePayload.class);
        boolean isSkipService =
                Arrays.stream(skipService).anyMatch(t -> t.equals(registerInstancePayload.getAppName()));
        LOGGER.info("%%% isSkipService {} status {}", isSkipService, registerInstancePayload.getStatus());
        if (!STATUS_UP.equals(registerInstancePayload.getStatus()) || isSkipService) {
            return;
        }
        LOGGER.info("a new instance is up , prepare to refresh swagger and parse permission {}", message);
        String key = registerInstancePayload.getAppName() + registerInstancePayload.getVersion();
        map.put(key, registerInstancePayload);
        countMap.put(key, 0);
        routeMap.put(key, registerInstancePayload);
        routeCountMap.put(key, 0);
    }

    /**
     * 定时执行在eureka上注册的新服务信息,刷新permission,存储到数据库
     */
    @Scheduled(fixedDelay = QUERY_INTERVAL)
    protected void execute() {
//        LOGGER.info("--> --> -->");
//        try {
//            LOGGER.info(mapper.writeValueAsString(map));
//            LOGGER.info(mapper.writeValueAsString(countMap));
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
        map.keySet().forEach(t -> {
            RegisterInstancePayload registerInstancePayload = map.get(t);
            try {
                String service = registerInstancePayload.getAppName();
                String version = registerInstancePayload.getVersion();
                int count = countMap.get(t);
                LOGGER.info("------------ parse permission -----------------");
                LOGGER.info("&&&&& service {}, version {}, count {}", service, version, count);
                //1.5s执行一次，如果执行200次无法拿到instance或fetch json failed，退出循环
                if (count >= 200) {
                    map.remove(t);
                    countMap.remove(t);
                    LOGGER.info("can not fetch swagger json for 5 min, so jump out of the loop, "
                                    + "please reboot the instance to trigger this schedule, the instance is {}",
                            mapper.writeValueAsString(registerInstancePayload));
                }
                countMap.put(t, ++count);
                String json = iDocumentService
                        .fetchSwaggerJson(service, version);
                if (StringUtils.isEmpty(json)) {
                    //每20次打印一次日志
                    if (count % 20 == 0) {
                        LOGGER.info("fetched swagger json data is empty, count {}, instance {}",
                                count, mapper.writeValueAsString(registerInstancePayload));
                    }
                } else {
                    swaggerRefreshService.updateOrInsertSwagger(registerInstancePayload, json);
                    swaggerRefreshService.parsePermission(registerInstancePayload, json);
                    map.remove(t, registerInstancePayload);
                    countMap.remove(t);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        });
    }

    /**
     * 定时执行在eureka上注册的新服务信息,刷新route,存储到数据库
     */
    @Scheduled(fixedDelay = QUERY_INTERVAL)
    protected void routeExecute() {
        routeMap.keySet().forEach(t -> {
            RegisterInstancePayload registerInstancePayload = routeMap.get(t);
            try {
                String service = registerInstancePayload.getAppName();
                String version = registerInstancePayload.getVersion();
                int count = routeCountMap.get(t);
                LOGGER.info("---------- refresh route ------------");
                LOGGER.info("&&&&& service {}, version {}, count {}", service, version, count);
                //1.5s执行一次，如果执行200次无法拿到instance或fetch json failed，退出循环
                if (count >= 200) {
                    routeMap.remove(t);
                    routeCountMap.remove(t);
                    LOGGER.info("can not fetch swagger json for 5 min, so jump out of the loop, "
                                    + "please reboot the instance to trigger this schedule, the instance is {}",
                            mapper.writeValueAsString(registerInstancePayload));
                }
                routeCountMap.put(t, ++count);
                String json = iDocumentService
                        .fetchSwaggerJson(service, version);
                if (StringUtils.isEmpty(json)) {
                    //每20次打印一次日志
                    if (count % 20 == 0) {
                        LOGGER.info("fetched swagger json data is empty, count {}, instance {}",
                                count, mapper.writeValueAsString(registerInstancePayload));
                    }
                } else {
                    if (iRouteService.queryRouteByService(service) == null) {
                        iRouteService.autoRefreshRoute(json);
                    } else {
                        routeMap.remove(t, registerInstancePayload);
                        routeCountMap.remove(t);
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        });
    }


}


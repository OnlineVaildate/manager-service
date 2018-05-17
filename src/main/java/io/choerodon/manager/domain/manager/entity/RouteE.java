package io.choerodon.manager.domain.manager.entity;

import java.util.List;

import org.apache.commons.collections.map.MultiKeyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.choerodon.manager.api.eventhandler.EurekaInstanceRegisteredListener;
import io.choerodon.manager.domain.repository.RouteRepository;
import io.choerodon.manager.infra.common.annotation.RouteNotifyRefresh;
import io.choerodon.manager.infra.common.utils.VersionUtil;

/**
 * 路由领域对象
 *
 * @author superleader8@gmail.com
 * @author wuguokai
 */
@Component
@Scope("prototype")
public class RouteE {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouteE.class);

    private Long id;

    private String name;

    private String path;

    private String serviceId;

    private String url;

    private Boolean stripPrefix;

    private Boolean retryable;

    private String sensitiveHeaders;

    private Boolean customSensitiveHeaders;

    private String helperService;

    private Long objectVersionNumber;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private DiscoveryClient discoveryClient;

    public MultiKeyMap getAllRunningInstances() {
        List<RouteE> routeEList = getAllRoute();
        List<String> serviceIds = discoveryClient.getServices();
        MultiKeyMap multiKeyMap = new MultiKeyMap();
        for (String serviceIdInList : serviceIds) {
            for (ServiceInstance instance : discoveryClient.getInstances(serviceIdInList)) {
                String version = instance.getMetadata().get(VersionUtil.METADATA_VERSION);
                if (StringUtils.isEmpty(version)) {
                    version = VersionUtil.NULL_VERSION;
                }
                if (multiKeyMap.get(serviceIdInList, version) == null) {
                    RouteE routeE = selectZuulRouteByServiceId(routeEList, serviceIdInList);
                    if (routeE == null) {
                        continue;
                    }
                    multiKeyMap.put(serviceIdInList, version, routeE);
                }
            }
        }
        return multiKeyMap;
    }

    private RouteE selectZuulRouteByServiceId(List<RouteE> routeEList, String serviceId) {
        for (RouteE routeE : routeEList) {
            if (routeE.getServiceId().equals(serviceId)) {
                return routeE;
            }
        }
        return null;
    }

    /**
     * 通过路由名称获取对象
     *
     * @return RouteE
     */
    public RouteE getRouteByName() {
        return routeRepository.queryRoute(this);
    }

    /**
     * 通过服务id获取路由对象
     *
     * @return routeE
     */
    public RouteE getRouteByServiceId() {
        return routeRepository.queryRoute(this);
    }

    /**
     * 添加一个路由
     *
     * @return RouteE
     */
    @RouteNotifyRefresh
    public RouteE addRoute() {
        return routeRepository.addRoute(this);
    }

    /**
     * 更新一个路由
     *
     * @return RouteE
     */
    @RouteNotifyRefresh
    public RouteE updateRoute() {
        return routeRepository.updateRoute(this);
    }

    /**
     * 删除一个对象
     *
     * @return boolean
     */
    @RouteNotifyRefresh
    public boolean deleteRoute() {
        return routeRepository.deleteRoute(this);
    }

    /**
     * 获取所有的路由
     *
     * @return list
     */
    public List<RouteE> getAllRoute() {
        return routeRepository.getAllRoute();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getStripPrefix() {
        return stripPrefix;
    }

    public void setStripPrefix(Boolean stripPrefix) {
        this.stripPrefix = stripPrefix;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public String getSensitiveHeaders() {
        return sensitiveHeaders;
    }

    public void setSensitiveHeaders(String sensitiveHeaders) {
        this.sensitiveHeaders = sensitiveHeaders;
    }

    public Boolean getCustomSensitiveHeaders() {
        return customSensitiveHeaders;
    }

    public void setCustomSensitiveHeaders(Boolean customSensitiveHeaders) {
        this.customSensitiveHeaders = customSensitiveHeaders;
    }

    public String getHelperService() {
        return helperService;
    }

    public void setHelperService(String helperService) {
        this.helperService = helperService;
    }

    public Long getObjectVersionNumber() {
        return objectVersionNumber;
    }

    public void setObjectVersionNumber(Long objectVersionNumber) {
        this.objectVersionNumber = objectVersionNumber;
    }

    @Override
    public String toString() {
        return "RouteE{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", path='" + path + '\''
                + ", serviceId='" + serviceId + '\''
                + ", url='" + url + '\''
                + ", stripPrefix=" + stripPrefix
                + ", retryable=" + retryable
                + ", sensitiveHeaders='" + sensitiveHeaders + '\''
                + ", customSensitiveHeaders=" + customSensitiveHeaders
                + ", helperService='" + helperService + '\''
                + ", objectVersionNumber=" + objectVersionNumber
                + ", routeRepository=" + routeRepository
                + ", discoveryClient=" + discoveryClient
                + '}';
    }
}

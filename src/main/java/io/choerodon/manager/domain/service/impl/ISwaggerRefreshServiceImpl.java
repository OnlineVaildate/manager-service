package io.choerodon.manager.domain.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import io.choerodon.core.exception.CommonException;
import io.choerodon.manager.api.dto.RegisterInstancePayload;
import io.choerodon.manager.domain.service.SwaggerRefreshService;
import io.choerodon.manager.domain.service.VersionStrategy;
import io.choerodon.manager.infra.dataobject.SwaggerDO;
import io.choerodon.manager.infra.mapper.SwaggerMapper;

/**
 * 实现类
 *
 * @author zhipeng.zuo
 * @author wuguokai
 */
@Service
public class ISwaggerRefreshServiceImpl implements SwaggerRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ISwaggerRefreshServiceImpl.class);
    private static final String SWAGGER_TOPIC_NAME = "manager-service";
    private final ObjectMapper mapper = new ObjectMapper();
    private SwaggerMapper swaggerMapper;

    private VersionStrategy versionStrategy;

    private KafkaTemplate kafkaTemplate;

    /**
     * 构造器
     */
    public ISwaggerRefreshServiceImpl(SwaggerMapper swaggerMapper,
                                      VersionStrategy versionStrategy,
                                      KafkaTemplate kafkaTemplate) {
        this.swaggerMapper = swaggerMapper;
        this.versionStrategy = versionStrategy;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void updateOrInsertSwagger(RegisterInstancePayload registerInstancePayload, String json) {
        SwaggerDO querySelf = new SwaggerDO();
        querySelf.setServiceVersion(registerInstancePayload.getVersion());
        querySelf.setServiceName(registerInstancePayload.getAppName());
        SwaggerDO self = swaggerMapper.selectOne(querySelf);
        if (self != null) {
            self.setValue(json);
            if (swaggerMapper.updateByPrimaryKey(self) != 1) {
                throw new CommonException("error.swagger.update");
            }
        } else {
            SwaggerDO inert = new SwaggerDO();
            inert.setServiceName(registerInstancePayload.getAppName());
            inert.setServiceVersion(registerInstancePayload.getVersion());
            inert.setValue(json);
            SwaggerDO queryDefault = new SwaggerDO();
            queryDefault.setServiceName(registerInstancePayload.getAppName());
            queryDefault.setDefault(true);
            SwaggerDO defaultVersion = swaggerMapper.selectOne(queryDefault);
            if (defaultVersion == null) {
                inert.setDefault(true);
            } else if (versionStrategy
                    .compareVersion(registerInstancePayload.getVersion(), defaultVersion.getServiceVersion()) > 0) {
                inert.setDefault(true);
                defaultVersion.setDefault(false);
                if (swaggerMapper.updateByPrimaryKeySelective(defaultVersion) != 1) {
                    throw new CommonException("error.swagger.update");
                }
            } else {
                inert.setDefault(false);
            }
            if (swaggerMapper.insert(inert) != 1) {
                throw new CommonException("error.swagger.insert");
            }
        }

    }

    @Override
    public void parsePermission(RegisterInstancePayload registerInstancePayload, String json) {
        registerInstancePayload.setApiData(json);
        try {
            String data = mapper.writeValueAsString(registerInstancePayload);
            kafkaTemplate.send(SWAGGER_TOPIC_NAME, data.getBytes());
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage());
        }
    }
}

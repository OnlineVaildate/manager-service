package io.choerodon.manager.domain.service.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.models.auth.OAuth2Definition;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import io.choerodon.manager.api.dto.RegisterInstancePayload;
import io.choerodon.manager.domain.manager.entity.RouteE;
import io.choerodon.manager.domain.service.IDocumentService;
import io.choerodon.manager.domain.service.IRouteService;
import io.choerodon.manager.domain.service.SwaggerRefreshService;
import io.choerodon.manager.infra.common.utils.VersionUtil;
import io.choerodon.manager.infra.dataobject.SwaggerDO;
import io.choerodon.manager.infra.mapper.SwaggerMapper;

/**
 * {@inheritDoc}
 *
 * @author xausky
 * @author wuguokai
 */
@org.springframework.stereotype.Service
public class IDocumentServiceImpl implements IDocumentService, IDocumentService.RefreshSwaggerListener {


    @Value("${spring.profiles.active:default}")
    private String profiles;
    private static final Logger LOGGER = LoggerFactory.getLogger(IDocumentServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String METADATA_CONTEXT = "CONTEXT";
    private static final String DEFAULT = "default";
    @Value("${choerodon.swagger.oauth-url:http://localhost:8080/iam/oauth/authorize}")
    private String oauthUrl;
    @Value("${choerodon.swagger.client:client}")
    private String client;
    @Value("${choerodon.gateway.domain:localhost}")
    private String gatewayDomain;
    private RestTemplate restTemplate = new RestTemplate();
    private SwaggerMapper swaggerMapper;
    private DiscoveryClient discoveryClient;
    private IRouteService iRouteService;
    private SwaggerRefreshService swaggerRefreshService;

    /**
     * 构造器
     */
    public IDocumentServiceImpl(SwaggerMapper swaggerMapper,
                                DiscoveryClient discoveryClient, IRouteService iRouteService,
                                SwaggerRefreshService swaggerRefreshService) {
        this.swaggerMapper = swaggerMapper;
        this.discoveryClient = discoveryClient;
        this.iRouteService = iRouteService;
        this.swaggerRefreshService = swaggerRefreshService;
    }

    @Override
    public ObjectNode getSwaggerJsonByIdAndVersion(String service, String version) throws IOException {
        String json = fetchSwaggerJsonByService(service, version);
        if (StringUtils.isEmpty(json)) {
            throw new RemoteAccessException("fetch swagger json failed");
        }
        ObjectNode node = (ObjectNode) MAPPER.readTree(json);
        List<Map<String, List<String>>> security = new LinkedList<>();
        Map<String, List<String>> clients = new TreeMap<>();
        clients.put(client, Collections.singletonList(DEFAULT));
        security.add(clients);
        OAuth2Definition definition = new OAuth2Definition();
        definition.setAuthorizationUrl(oauthUrl);
        definition.setType("oauth2");
        definition.setFlow("implicit");
        definition.setScopes(Collections.singletonMap(DEFAULT, "default scope"));
        LOGGER.info(definition.getScopes().toString());
        node.putPOJO("securityDefinitions", Collections.singletonMap(client, definition));
        Iterator<Map.Entry<String, JsonNode>> pathIterator = node.get("paths").fields();
        while (pathIterator.hasNext()) {
            Map.Entry<String, JsonNode> pathNode = pathIterator.next();
            Iterator<Map.Entry<String, JsonNode>> methodIterator = pathNode.getValue().fields();
            while (methodIterator.hasNext()) {
                Map.Entry<String, JsonNode> methodNode = methodIterator.next();
                ((ObjectNode) methodNode.getValue()).putPOJO("security", security);
            }
        }
        return node;
    }

    @Override
    public String fetchSwaggerJsonByService(String service, String version) {
        SwaggerDO query = new SwaggerDO();
        query.setServiceName(service);
        query.setServiceVersion(version);
        SwaggerDO data = swaggerMapper.selectOne(query);
        if (profiles.equals(DEFAULT) || data == null || StringUtils.isEmpty(data.getValue())) {
            return getJsonByNameAndVersion(service, version);
        } else {
            return data.getValue();
        }
    }

    @Override
    public String fetchSwaggerJson(String service, String version) {
        return getJsonByNameAndVersion(service, version);
    }

    private String getJsonByNameAndVersion(String service, String version) {
        List<ServiceInstance> instances = discoveryClient.getInstances(service);
        LOGGER.info("==============>begin fetch json=========>");
        for (ServiceInstance instance : instances) {
            String mdVersion = instance.getMetadata().get(VersionUtil.METADATA_VERSION);
            LOGGER.info("### service {}", service);
            LOGGER.info("### version {}", version);
            if (StringUtils.isEmpty(mdVersion)) {
                mdVersion = VersionUtil.NULL_VERSION;
            }
            LOGGER.info("@@@ mdService {}", service);
            LOGGER.info("@@@ mdVersion {}", mdVersion);
            LOGGER.info("------------------------------");
            if (version.equals(mdVersion)) {
                return fetch(instance);
            }
        }
        return null;
    }

    @Override
    public String getSwaggerJson(String name, String version) throws IOException {
        MultiKeyMap multiKeyMap = iRouteService.getAllRunningInstances();
        RouteE routeE = iRouteService
                .getRouteFromRunningInstancesMap(multiKeyMap, name, version);
        if (routeE == null) {
            return "";
        }
        String basePath = routeE.getPath().replace("/**", "");
        ObjectNode root = getSwaggerJsonByIdAndVersion(routeE.getServiceId(), version);
        root.put("basePath", basePath);
        root.put("host", gatewayDomain);
        LOGGER.debug("put basePath:{}, host:{}", basePath, root.get("host"));
        return MAPPER.writeValueAsString(root);
    }

    @Override
    public void manualRefresh(String serviceName, String version) {
        String json = fetchSwaggerJsonByService(serviceName, version);
        RegisterInstancePayload registerInstancePayload = new RegisterInstancePayload();
        registerInstancePayload.setAppName(serviceName);
        registerInstancePayload.setVersion(version);
        swaggerRefreshService.updateOrInsertSwagger(registerInstancePayload, json);
        swaggerRefreshService.parsePermission(registerInstancePayload, json);
    }

    private String fetch(ServiceInstance instance) {
        ResponseEntity<String> response;
        String contextPath = instance.getMetadata().get(METADATA_CONTEXT);
        if (contextPath == null) {
            contextPath = "";
        }
        LOGGER.info("service: {} metadata : {}", instance.getServiceId(), instance.getMetadata());
        if ("OAUTH-SERVER".equals(instance.getServiceId())) {
            contextPath = "/oauth";
        }
        try {
            response = restTemplate.getForEntity(
                    instance.getUri() + contextPath + "/v2/choerodon/api-docs",
                    String.class);
        } catch (RestClientException e) {
            throw new RemoteAccessException("fetch failed, instance:" + instance.getServiceId());
        }
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RemoteAccessException("fetch failed : " + response);
        }
        return response.getBody();
    }

    @Override
    public void refresh(String service, String json) {
        // Do nothing only for override
    }
}

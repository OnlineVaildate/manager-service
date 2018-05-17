package io.choerodon.manager.api.dto;

/**
 * 从消息队列拿到的服务启动下线信息对应的实体
 *
 * @author zhipeng.zuo
 * @date 2018/1/23
 */
public class RegisterInstancePayload {

    private String status;

    private String appName;

    private String id;

    private String version;

    private String uuid;

    private String apiData;

    public String getApiData() {
        return apiData;
    }

    public void setApiData(String apiData) {
        this.apiData = apiData;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

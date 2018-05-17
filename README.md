# manager-service

## 用途

#### 一、提供动态修改应用配置的服务

1. 提供接口修改服务配置
    * 直接通过服务名和配置项修改
    * 通过服务 -> 配置 -> 配置项修改配置
    * 通过配置文件文本形式修改配置
1. 提供接口修改网关路由
1. 提供接口修改灰度发布对应的用户组
1


## 使用

### 数据库创建

```bash
CREATE USER 'hapcloud'@'%' IDENTIFIED BY "handhand";
CREATE DATABASE manager_service DEFAULT CHARACTER SET utf8;
GRANT ALL PRIVILEGES ON manager_service.* TO hapcloud@'%';
FLUSH PRIVILEGES;
```

### 数据初始化

在项目根目录进入bash， 运行 `sh init-local-database.sh`，初始化数据。


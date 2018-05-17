#!/usr/bin/env bash
mkdir -p target
if [ ! -f target/choerodon-tool-config.jar ]
then
    curl http://nexus.saas.hand-china.com/content/repositories/rdcsnapshot/io/choerodon/choerodon-tool-config/1.0-SNAPSHOT/choerodon-tool-config-1.0-20180418.025442-18.jar -o target/choerodon-tool-config.jar
fi
java -Dspring.datasource.url="jdbc:mysql://192.168.12.156:3306/manager_service?useUnicode=true&characterEncoding=utf-8&useSSL=false" \
 -Dspring.datasource.username=root \
 -Dspring.datasource.password=handhand \
 -Dservice.name=manager-service \
 -Dservice.version=1.0-SNAPSHOT \
 -Dconfig.file=application-default.yml \
 -jar target/choerodon-tool-config.jar # 该处version可能会发生改动请根据下载的架包名更改
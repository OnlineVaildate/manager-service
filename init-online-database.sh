#!/usr/bin/env bash
mkdir -p target
if [ ! -f target/choerodon-tool-liquibase.jar ]
then
    curl http://nexus.saas.hand-china.com/content/repositories/rdcsnapshot/io/choerodon/choerodon-tool-liquibase/1.0-SNAPSHOT/choerodon-tool-liquibase-1.0-20180419.073252-19.jar -o target/choerodon-tool-liquibase.jar
fi
java -Dspring.datasource.url="jdbc:mysql://192.168.12.156:3306/manager_service?useUnicode=true&characterEncoding=utf-8&useSSL=false" \
 -Dspring.datasource.username=root \
 -Dspring.datasource.password=handhand \
 -Ddata.drop=false -Ddata.init=init \
 -Ddata.dir=src/main/resources \
 -jar target/choerodon-tool-liquibase.jar
package script.db

databaseChangeLog(logicalFilePath: 'script/db/mgmt_service_configonfig.groovy') {
    changeSet(author: 'superleader8@gmail.com', id: '2018-03-09-service-config') {
        createTable(tableName: 'mgmt_service_config') {
            column(name: 'id', type: 'BIGINT UNSIGNED', autoIncrement: true, remarks: '表ID，主键，供其他表做外键，unsigned bigint、单表时自增、步长为 1') {
                constraints(primaryKey: true)
            }
            column(name: 'service_version', type: 'VARCHAR(128)', remarks: '服务版本') {
                constraints(nullable: false)
            }
            column(name: 'is_default', type: "TINYINT(1)", defaultValue: '0', remarks: '是否为默认版本,0表示不是，1表示是') {
                constraints(nullable: false)
            }
            column(name: 'service_id', type: 'BIGINT UNSIGNED', remarks: '配置所属服务Id') {
                constraints(nullable: false)
            }
            column(name: 'description', type: "VARCHAR(128)", remarks: '配置描述') {
                constraints(nullable: false)
            }
            column(name: 'value', type: "LONGTEXT", remarks: '配置集合') {
                constraints(nullable: false)
            }
            column(name: 'public_time', type: 'DATETIME', remarks: '配置发布时间') {
                constraints(nullable: false)
            }

            column(name: "object_version_number", type: "BIGINT UNSIGNED", defaultValue: "1")
            column(name: "created_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "creation_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
            column(name: "last_updated_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "last_update_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
        }
    }

    changeSet(author: 'guokai.wu.work@gmail.com', id: '2018-04-19-service-config') {
        addColumn(tableName: 'mgmt_service_config') {
            column(name: "name", type: 'VARCHAR(64)', remarks: '配置名，对应前端的配置id', afterColumn: 'id') {
                constraints(nullable: false)
            }
        }
        addColumn(tableName: 'mgmt_service_config') {
            column(name: 'source', type: 'VARCHAR(64)', remarks: '配置来源', afterColumn: 'value')
        }
        renameColumn(tableName: 'mgmt_service_config', oldColumnName: "service_version", newColumnName: "config_version", columnDataType: 'VARCHAR(128)', remarks: '配置版本')
        dropColumn(tableName: "mgmt_service_config", columnName: "description")
    }
    changeSet(author: 'guokai.wu.work@gmail.com', id: '2018-04-21-service-config') {
        addUniqueConstraint(tableName: 'mgmt_service_config', columnNames: 'service_id,config_version')
    }
}
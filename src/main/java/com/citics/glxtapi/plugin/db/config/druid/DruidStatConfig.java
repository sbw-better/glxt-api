package com.citics.glxtapi.plugin.db.config.druid;

import lombok.Data;

/**
 * Druid监控配置
 */
@Data
public class DruidStatConfig {

    private Long slowSqlMillis;

    private Boolean logSlowSql;

    private Boolean mergeSql;

}
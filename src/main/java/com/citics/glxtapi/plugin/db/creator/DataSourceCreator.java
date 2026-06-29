package com.citics.glxtapi.plugin.db.creator;

import com.citics.glxtapi.plugin.db.config.DynamicDataSourceProperties;
import com.citics.glxtapi.plugin.db.domain.DataSourceProperty;
import com.citics.glxtapi.plugin.db.domain.ItemDataSource;
import com.citics.glxtapi.plugin.db.support.ScriptRunner;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

import static com.citics.glxtapi.plugin.db.support.DdConstants.*;

/**
 * 数据源创建器
 */
@Slf4j
@Setter
public class DataSourceCreator {

    /**
     * 是否存在druid
     */
    private static Boolean druidExists = false;
    /**
     * 是否存在hikari
     */
    private static Boolean hikariExists = false;

    static {
        try {
            Class.forName(DRUID_DATASOURCE);
            druidExists = true;
            log.debug("dynamic-datasource detect druid,Please Notice \n" +
                    "https://github.com/baomidou/dynamic-datasource-spring-boot-starter/wiki/Integration-With-Druid");
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName(HIKARI_DATASOURCE);
            hikariExists = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

    private BasicDataSourceCreator basicDataSourceCreator;
    private JndiDataSourceCreator jndiDataSourceCreator;
    private HikariDataSourceCreator hikariDataSourceCreator;
    private DruidDataSourceCreator druidDataSourceCreator;
    private DynamicDataSourceProperties properties;

    /**
     * 创建数据源
     *
     * @param dataSourceProperty 数据源信息
     * @return 数据源
     */
    public DataSource createDataSource(DataSourceProperty dataSourceProperty) {
        DataSource dataSource;
        //如果是jndi数据源
        String jndiName = dataSourceProperty.getJndiName();
        if (jndiName != null && !jndiName.isEmpty()) {
            dataSource = createJNDIDataSource(jndiName);
        } else {
            Class<? extends DataSource> type = dataSourceProperty.getType();
            if (type == null) {
                if (druidExists) {
                    dataSource = createDruidDataSource(dataSourceProperty);
                } else if (hikariExists) {
                    dataSource = createHikariDataSource(dataSourceProperty);
                } else {
                    dataSource = createBasicDataSource(dataSourceProperty);
                }
            } else if (DRUID_DATASOURCE.equals(type.getName())) {
                dataSource = createDruidDataSource(dataSourceProperty);
            } else if (HIKARI_DATASOURCE.equals(type.getName())) {
                dataSource = createHikariDataSource(dataSourceProperty);
            } else {
                dataSource = createBasicDataSource(dataSourceProperty);
            }
        }
        this.runScrip(dataSource, dataSourceProperty);
        return wrapDataSource(dataSource, dataSourceProperty);
    }

    private void runScrip(DataSource dataSource, DataSourceProperty dataSourceProperty) {
        String schema = dataSourceProperty.getSchema();
        String data = dataSourceProperty.getData();
        if (StringUtils.hasText(schema) || StringUtils.hasText(data)) {
            ScriptRunner scriptRunner = new ScriptRunner(dataSourceProperty.isContinueOnError(), dataSourceProperty.getSeparator());
            if (StringUtils.hasText(schema)) {
                scriptRunner.runScript(dataSource, schema);
            }
            if (StringUtils.hasText(data)) {
                scriptRunner.runScript(dataSource, data);
            }
        }
    }

    private DataSource wrapDataSource(DataSource dataSource, DataSourceProperty dataSourceProperty) {
        String name = dataSourceProperty.getPoolName();
        DataSource targetDataSource = dataSource;
        return new ItemDataSource(name, dataSource, targetDataSource);
    }

    /**
     * 创建基础数据源
     *
     * @param dataSourceProperty 数据源参数
     * @return 数据源
     */
    public DataSource createBasicDataSource(DataSourceProperty dataSourceProperty) {
        if (StringUtils.isEmpty(dataSourceProperty.getPublicKey())) {
            dataSourceProperty.setPublicKey(properties.getPublicKey());
        }
        return basicDataSourceCreator.createDataSource(dataSourceProperty);
    }

    /**
     * 创建JNDI数据源
     *
     * @param jndiName jndi数据源名称
     * @return 数据源
     */
    public DataSource createJNDIDataSource(String jndiName) {
        return jndiDataSourceCreator.createDataSource(jndiName);
    }

    /**
     * 创建Druid数据源
     *
     * @param dataSourceProperty 数据源参数
     * @return 数据源
     */
    public DataSource createDruidDataSource(DataSourceProperty dataSourceProperty) {
        if (StringUtils.isEmpty(dataSourceProperty.getPublicKey())) {
            dataSourceProperty.setPublicKey(properties.getPublicKey());
        }
        return druidDataSourceCreator.createDataSource(dataSourceProperty);
    }

    /**
     * 创建Hikari数据源
     *
     * @param dataSourceProperty 数据源参数
     * @return 数据源
     */
    public DataSource createHikariDataSource(DataSourceProperty dataSourceProperty) {
        if (StringUtils.isEmpty(dataSourceProperty.getPublicKey())) {
            dataSourceProperty.setPublicKey(properties.getPublicKey());
        }
        return hikariDataSourceCreator.createDataSource(dataSourceProperty);
    }
}

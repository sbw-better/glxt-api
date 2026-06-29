package com.citics.glxtapi.plugin.db.service.Impl;

import com.citics.glxtapi.plugin.db.DynamicRoutingDataSource;
import com.citics.glxtapi.plugin.db.creator.DataSourceCreator;
import com.citics.glxtapi.plugin.db.domain.DataSourceProperty;
import com.citics.glxtapi.plugin.db.service.DataSourceService;
import com.citics.glxtapi.plugin.db.support.DdConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

@Component
@Profile("!local")
@Slf4j
public class DataSourceServiceImpl implements DataSourceService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DataSourceCreator dataSourceCreator;

    @Override
    public Map<String, DataSource> addDataSource(DataSourceProperty dataSourceProperty) {
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        DataSource dataSource = dataSourceCreator.createDataSource(dataSourceProperty);
        ds.addDataSource(dataSourceProperty.getPoolName() + "_" + DdConstants.MASTER, dataSource);
        return ds.getCurrentDataSources();
    }

    @Override
    public Map<String, DataSource> deleteDataSource(String name) {
        name += "_" + DdConstants.MASTER;
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        ds.removeDataSource(name);
        return ds.getCurrentDataSources();
    }

    @Override
    public DataSource getDataSource(String key) {
        key += "_" + DdConstants.MASTER;
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        Map<String, DataSource> dataSourceMap = ds.getCurrentDataSources();
        if (dataSourceMap.containsKey(key)) {
            return dataSourceMap.get(key);
        }
        return null;
    }

    @Override
    public Map<String, DataSource> getDataSources() {
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        return ds.getCurrentDataSources();
    }

    @Override
    public boolean containsKey(String key) {
        key += "_" + DdConstants.MASTER;
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        Map<String, DataSource> dataSourceMap = ds.getCurrentDataSources();
        return dataSourceMap.containsKey(key);
    }

    @Override
    public DataSource getPrimaryDataSource() {
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        return ds.getDataSource("");
    }
}

package com.citics.glxtapi.web.config;

import com.citics.glxtapi.plugin.sql.DbModule;
import com.citics.glxtapi.web.service.TenantService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DbModuleConfig {

    @Bean
    public DbModule dbModule(JdbcTemplate jdbcTemplate, TenantService tenantService) {
        return new DbModule(jdbcTemplate, null, tenantService);
    }
}

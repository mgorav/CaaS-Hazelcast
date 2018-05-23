package com.ingenico.epayments.shared.jpa.eclipselink.autoconfigure;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;

import javax.sql.DataSource;

/**
 * This class configures data source for JPA/EclipseLink
 */
@Configuration
public class JpaDataSourceConfigurer {

    @Bean
    @ConfigurationProperties(prefix = "ingenico.epayments.shared.jpa.datasource")
    public DataSourceProperties jpaDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "jpaDataSource")
    public DataSource jpaDataSource() {
        return jpaDataSourceProperties().initializeDataSourceBuilder().build();
    }
}

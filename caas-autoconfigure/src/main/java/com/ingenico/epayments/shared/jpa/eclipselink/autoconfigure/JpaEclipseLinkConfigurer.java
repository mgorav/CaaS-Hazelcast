package com.ingenico.epayments.shared.jpa.eclipselink.autoconfigure;

import com.ingenico.epayments.shared.jpa.eclipselink.customizer.EclipseLinkCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.Entity;
import javax.sql.DataSource;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.springframework.util.StringUtils.collectionToCommaDelimitedString;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "ingenico.epayments.shared.jpa")
@Import({JpaDataSourceConfigurer.class, JpaTransactionManagerConfigurer.class})
public class JpaEclipseLinkConfigurer {

    private Map<String, String> properties;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("jpaDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(dataSource);


        JpaVendorAdapter vendorAdapter = new EclipseLinkJpaVendorAdapter();
        emfBean.setJpaVendorAdapter(vendorAdapter);

        Properties properties = new Properties();

        setEntityPackages(emfBean);


        this.properties.forEach((key, value) -> {
            properties.setProperty(key, value);

            setDefaultEclipseLinkJpaProperties(properties);

            enableEclipseLinkLoggingIfConfigured(properties, key, value);

            enableEclipseLinkProfierIfConfigured(properties, key, value);
        });


        emfBean.setJpaProperties(properties);

        return emfBean;

    }

    private void enableEclipseLinkProfierIfConfigured(Properties properties, String key, String value) {
        if (key.equals(" ingenico.epayments.jpa.eclipselink-performance.profiler")) {
            if (!properties.contains("eclipselink.session.customizer") && Boolean.valueOf(value)) {
                properties.setProperty("eclipselink.session.customizer", EclipseLinkCustomizer.class.getCanonicalName());

            }
        }
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    // ~~~ private methods

    private void enableEclipseLinkLoggingIfConfigured(Properties properties, String key, String value) {
        if (key.equals("ingenico.epayments.jpa.eclipselink-logging") && value != null && Boolean.valueOf(value)) {
            // enable logging

            if (!properties.contains("eclipselink.logging.timestamp")) {
                properties.setProperty("eclipselink.logging.timestamp", "true");
            }
            if (!properties.contains("eclipselink.logging.session")) {
                properties.setProperty("eclipselink.logging.session", "true");
            }
            if (!properties.contains("eclipselink.logging.connection")) {
                properties.setProperty("eclipselink.logging.connection", "true");
            }
            if (!properties.contains("eclipselink.logging.thread")) {
                properties.setProperty("eclipselink.logging.thread", "true");
            }
            if (!properties.contains("eclipselink.logging.level.transaction")) {
                properties.setProperty("eclipselink.logging.level.transaction", "ALL");
            }
            if (!properties.contains("eclipselink.logging.level.sql")) {
                properties.setProperty("eclipselink.logging.level.sql", "ALL");
            }
            if (!properties.contains("eclipselink.logging.level.event")) {
                properties.setProperty("eclipselink.logging.level.event", "ALL");
            }
            if (!properties.contains("eclipselink.logging.level.connection")) {
                properties.setProperty("eclipselink.logging.level.connection", "ALL");
            }
            if (!properties.contains("eclipselink.logging.level.query")) {
                properties.setProperty("eclipselink.logging.level.query", "ALL");
            }
            if (!properties.contains("eclipselink.logging.level.cache")) {
                properties.setProperty("eclipselink.logging.level.cache", "ALL");
            }
            if (!properties.contains("eclipselink.logging.level")) {
                properties.setProperty("eclipselink.logging.level", "ALL");
            }
            if (!properties.contains("eclipselink.logging.parameters")) {
                properties.setProperty("eclipselink.logging.parameters", "true");
            }
        }
    }

    private void setDefaultEclipseLinkJpaProperties(Properties properties) {
        if (!properties.contains("eclipselink.cache.shared.default")) {
            properties.setProperty("eclipselink.cache.shared.default", "true");
        }
        if (!properties.contains("eclipselink.jdbc.batch-writing")) {
            properties.setProperty("eclipselink.jdbc.batch-writing", "jdbc");
        }
        if (!properties.contains("eclipselink.jdbc.batch-writing.size")) {
            properties.setProperty("eclipselink.jdbc.batch-writing.size", "1000");
        }
        if (!properties.contains("eclipselink.jdbc.cache-statements")) {
            properties.setProperty("eclipselink.jdbc.cache-statements", "true");
        }
        if (!properties.contains("eclipselink.jdbc.cache-statements.size")) {
            properties.setProperty("eclipselink.jdbc.cache-statements.size", "1000");
        }
        if (!properties.contains("eclipselink.jdbc.cache-statements.size")) {
            properties.setProperty("eclipselink.jdbc.bind-parameterse", "true");
        }
        if (!properties.contains("eclipselink.weaving")) {
            properties.setProperty("eclipselink.weaving", "static");
        }
        if (!properties.contains("eclipselink.persistence-context.flush-mode")) {
            properties.setProperty("eclipselink.persistence-context.flush-mode", "COMMIT");
        }
        if (!properties.contains("eclipselink.ddl-generation")) {
            properties.setProperty("eclipselink.ddl-generation", "create-tables");
        }
    }

    private void setEntityPackages(LocalContainerEntityManagerFactoryBean emfBean) {
        EntityScanner scanner = new EntityScanner(applicationContext);
        try {
            Set<Class<?>> scannedEntityClasses = scanner.scan(Entity.class);
            if (!scannedEntityClasses.isEmpty()) {
                Set<String> fullQualifiedEntityPkgs = new HashSet<>(scannedEntityClasses.size());

                scannedEntityClasses.forEach(aClass -> {
                    if (!fullQualifiedEntityPkgs.contains(aClass.getCanonicalName())) {
                        fullQualifiedEntityPkgs.add(aClass.getPackage().getName());
                    }
                });

                if (!fullQualifiedEntityPkgs.isEmpty()) {
                    emfBean.setPackagesToScan(collectionToCommaDelimitedString(fullQualifiedEntityPkgs));
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}

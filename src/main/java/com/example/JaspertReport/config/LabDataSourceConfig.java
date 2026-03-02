package com.example.JaspertReport.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class LabDataSourceConfig {

	/**
	 * DataSource principal: base de datos "DB_NAME" (DB_HOST:3307).
	 * Las propiedades se cargan desde spring.datasource.lab.* en
	 * application.properties.
	 * Marcado como @Primary para que sea el datasource por defecto de Spring Boot.
	 */
	@Primary
	@Bean("labDataSource")
	@ConfigurationProperties(prefix = "spring.datasource.lab")
	public DataSource labDataSource() {
		return DataSourceBuilder.create().build();
	}

	/**
	 * JdbcTemplate dedicado a la base de datos de laboratorio.
	 * Al ser @Primary, es el que se inyecta por defecto cuando no se
	 * usa @Qualifier.
	 */
	@Primary
	@Bean("labJdbcTemplate")
	public JdbcTemplate labJdbcTemplate(@Qualifier("labDataSource") DataSource labDataSource) {
		return new JdbcTemplate(labDataSource);
	}
}

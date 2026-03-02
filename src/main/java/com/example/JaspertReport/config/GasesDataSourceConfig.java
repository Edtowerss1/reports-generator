package com.example.JaspertReport.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class GasesDataSourceConfig {

	/**
	 * DataSource secundario: base de datos "gases" (192.168.0.35:3307).
	 * Las propiedades se cargan desde spring.datasource.gases.* en
	 * application.properties.
	 */
	@Bean("gasesDataSource")
	@ConfigurationProperties(prefix = "spring.datasource.gases")
	public DataSource gasesDataSource() {
		return DataSourceBuilder.create().build();
	}

	/**
	 * JdbcTemplate dedicado a la base de datos "gases".
	 * Se puede inyectar usando @Qualifier("gasesJdbcTemplate").
	 */
	@Bean("gasesJdbcTemplate")
	public JdbcTemplate gasesJdbcTemplate(@Qualifier("gasesDataSource") DataSource gasesDataSource) {
		return new JdbcTemplate(gasesDataSource);
	}
}

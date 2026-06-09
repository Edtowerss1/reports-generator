package com.example.JasperReport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JasperReportApplication {

	public static void main(String[] args) {
		SpringApplication.run(JasperReportApplication.class, args);
	}

}

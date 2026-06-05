package com.example.JaspertReport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JaspertReportApplication {

	public static void main(String[] args) {
		SpringApplication.run(JaspertReportApplication.class, args);
	}

}

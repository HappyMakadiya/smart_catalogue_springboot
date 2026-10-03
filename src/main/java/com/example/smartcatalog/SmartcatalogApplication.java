package com.example.smartcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
// For @CreatedDate and @LastModifiedDate to function. If you omit this, the dates will remain null.
@EnableJpaAuditing
@EnableCaching
@EnableAsync
public class SmartcatalogApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartcatalogApplication.class, args);
	}

}


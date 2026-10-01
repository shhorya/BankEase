package com.hcl.bankease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "com.hcl.bankease.repository")
@SpringBootApplication
public class BankeaseBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankeaseBackendApplication.class, args);
	}

}

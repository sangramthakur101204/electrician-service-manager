package com.electrician.servicemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServicemanagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServicemanagerApplication.class, args);
	}

}

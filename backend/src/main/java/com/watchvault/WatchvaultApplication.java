package com.watchvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // registers @ConfigurationProperties records such as AuthProperties
public class WatchvaultApplication {

	public static void main(String[] args) {
		SpringApplication.run(WatchvaultApplication.class, args);
	}

}

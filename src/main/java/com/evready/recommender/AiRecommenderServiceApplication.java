package com.evready.recommender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiRecommenderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiRecommenderServiceApplication.class, args);
	}

}

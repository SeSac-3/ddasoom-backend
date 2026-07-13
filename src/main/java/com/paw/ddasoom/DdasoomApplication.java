package com.paw.ddasoom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class DdasoomApplication {

	public static void main(String[] args) {
		SpringApplication.run(DdasoomApplication.class, args);
	}

}

package com.space.munova;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class MunovaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MunovaApplication.class, args);
    }

}

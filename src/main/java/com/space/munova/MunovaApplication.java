package com.space.munova;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MunovaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MunovaApplication.class, args);
    }

}

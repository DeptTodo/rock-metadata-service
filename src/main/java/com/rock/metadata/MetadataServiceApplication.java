package com.rock.metadata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class MetadataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetadataServiceApplication.class, args);
    }
}

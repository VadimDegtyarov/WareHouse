package com.kis.authservice.configmodule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigModuleApplication.class, args);
    }

}

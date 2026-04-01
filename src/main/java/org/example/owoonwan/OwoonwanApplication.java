package org.example.owoonwan;

import org.example.owoonwan.auth.config.AuthTokenProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AuthTokenProperties.class)
public class OwoonwanApplication {

    public static void main(String[] args) {
        SpringApplication.run(OwoonwanApplication.class, args);
    }

}

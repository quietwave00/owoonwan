package org.example.owoonwan.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth.token")
public class AuthTokenProperties {

    private String secret = "owoonwan-local-token-secret-change-me";
    private long maxAgeDays = 3650;
}

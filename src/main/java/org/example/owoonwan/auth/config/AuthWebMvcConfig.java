package org.example.owoonwan.auth.config;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.interceptor.SessionAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AuthWebMvcConfig implements WebMvcConfigurer {

    private final SessionAuthInterceptor sessionAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionAuthInterceptor)
                .addPathPatterns("/auth/me", "/auth/logout", "/checkins/**", "/board/**", "/stats/**", "/titles/**", "/pledges/**");
    }
}

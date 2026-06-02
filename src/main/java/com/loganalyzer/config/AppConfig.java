package com.loganalyzer.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    // =====================================================
    // PASSWORD ENCODER
    // =====================================================
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    // =====================================================
    // OKHTTP CLIENT
    // =====================================================
    @Bean
    public OkHttpClient okHttpClient() {

        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .writeTimeout(java.time.Duration.ofSeconds(60))
                .build();
    }
}
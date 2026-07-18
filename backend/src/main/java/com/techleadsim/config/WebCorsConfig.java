package com.techleadsim.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    private static final String RENDER_ORIGIN_PATTERN = "https://*.onrender.com";

    private final String allowedOrigin;

    public WebCorsConfig(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigin, RENDER_ORIGIN_PATTERN)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}

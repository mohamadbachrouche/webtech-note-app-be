package de.htw.webtech;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Allow all endpoints
                .allowedOrigins(
                        "http://localhost:5173", // Your local frontend
                        "http://localhost:5177", // The other local port
                        "https://webtech-note-app-fe.onrender.com" // Your future deployed frontend
                )
                .allowedMethods("*"); // Allow all methods (GET, POST, etc.)
    }
}
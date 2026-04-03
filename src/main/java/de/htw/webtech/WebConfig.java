package de.htw.webtech;

import org.springframework.context.annotation.Configuration;

// CORS is now configured in SecurityConfig.corsConfigurationSource()
// so that it is processed by Spring Security before auth headers are checked.
@Configuration
public class WebConfig {
}

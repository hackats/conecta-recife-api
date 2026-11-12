package br.com.conectarecife.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final String[] origens;

    public CorsConfig(@Value("${app.cors.allowed-origins:}") String origens) {
        this.origens = Arrays.stream(origens.split(",")).map(String::strip)
                .filter(origem -> !origem.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origens.length == 0) return;
        registry.addMapping("/**").allowedOrigins(origens)
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("Content-Type", "Idempotency-Key")
                .exposedHeaders("Location", "Content-Disposition");
    }
}

package tda.app.app;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Allow static resources (safe for Spring Boot 3 / Spring Security 6)
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                // In case some assets/pages are served from the root of /static
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/*.html",
                    "/*.css",
                    "/*.js",
                    "/*.png",
                    "/*.jpg",
                    "/*.jpeg",
                    "/*.svg",
                    "/*.webp",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**"
                ).permitAll()

                // API
                .requestMatchers("/api/**").permitAll()

                // Everything else
                .anyRequest().permitAll()
            );

        return http.build();
    }
}

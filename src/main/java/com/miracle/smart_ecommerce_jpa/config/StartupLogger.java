package com.miracle.smart_ecommerce_jpa.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Stream;

@Component
public class StartupLogger implements ApplicationRunner {
    private final ConfigurableEnvironment env;

    public StartupLogger(ConfigurableEnvironment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("[StartupLogger] Listing property sources for spring.jackson keys and env vars containing 'JACKSON'...");

        for (PropertySource<?> ps : env.getPropertySources()) {
            if (ps instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> eps = (EnumerablePropertySource<?>) ps;
                String[] names = eps.getPropertyNames();
                Stream.of(names)
                        .filter(n -> n != null && n.startsWith("spring.jackson"))
                        .forEach(n -> System.out.println("[PropertySource:" + ps.getName() + "] " + n + "=" + env.getProperty(n)));
            }
        }

        System.out.println("[StartupLogger] Checking environment variables containing 'JACKSON' or 'WRITE'...");
        Map<String, String> map = System.getenv();
        map.forEach((k, v) -> {
            if (k.toUpperCase().contains("JACKSON") || k.toUpperCase().contains("WRITE")) {
                System.out.println("[ENV] " + k + "=" + v);
            }
        });

    }
}

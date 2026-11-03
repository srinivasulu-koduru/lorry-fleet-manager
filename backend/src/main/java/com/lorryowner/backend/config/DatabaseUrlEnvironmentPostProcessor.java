package com.lorryowner.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * EnvironmentPostProcessor that inspects DATABASE_URL / DB_URL supplied by cloud hosting
 * providers (Voroa, Heroku, Railway, Render, Supabase, Neon) and normalizes standard URI
 * formats (e.g., 'postgres://user:pass@host:5432/dbname' or 'postgresql://...') into
 * standard Spring Boot JDBC properties (spring.datasource.url = 'jdbc:postgresql://...').
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor.class);
    public static final String PROPERTY_SOURCE_NAME = "normalizedCloudDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String rawUrl = environment.getProperty("DATABASE_URL");
        if (!StringUtils.hasText(rawUrl)) {
            rawUrl = environment.getProperty("DB_URL");
        }

        if (!StringUtils.hasText(rawUrl)) {
            return;
        }

        rawUrl = rawUrl.trim();

        // If it starts with standard cloud URI prefix 'postgres://' or 'postgresql://' (without 'jdbc:')
        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                // Use http scheme momentarily to parse host, port, user-info, query via java.net.URI
                String pseudoUriString = rawUrl.replaceFirst("^postgres(ql)?://", "http://");
                URI uri = URI.create(pseudoUriString);

                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String dbName = (path != null && path.length() > 1) ? path.substring(1) : "";

                StringBuilder jdbcUrl = new StringBuilder();
                jdbcUrl.append("jdbc:postgresql://").append(host).append(":").append(port).append("/").append(dbName);

                if (StringUtils.hasText(uri.getQuery())) {
                    jdbcUrl.append("?").append(uri.getQuery());
                }

                Map<String, Object> properties = new HashMap<>();
                properties.put("spring.datasource.url", jdbcUrl.toString());
                properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

                String userInfo = uri.getUserInfo();
                if (StringUtils.hasText(userInfo)) {
                    String[] userPass = userInfo.split(":", 2);
                    String username = URLDecoder.decode(userPass[0], StandardCharsets.UTF_8);
                    properties.put("spring.datasource.username", username);

                    if (userPass.length > 1) {
                        String password = URLDecoder.decode(userPass[1], StandardCharsets.UTF_8);
                        properties.put("spring.datasource.password", password);
                    }
                }

                log.info("Normalized cloud DATABASE_URL into JDBC format: jdbc:postgresql://{}:{}/{}", host, port, dbName);
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));

            } catch (Exception e) {
                log.warn("Failed to parse cloud DATABASE_URL '{}'; falling back to default configuration. Error: {}", rawUrl, e.getMessage());
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

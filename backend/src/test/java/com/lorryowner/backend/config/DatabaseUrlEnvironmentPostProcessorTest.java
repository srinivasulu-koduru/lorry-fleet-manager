package com.lorryowner.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    @DisplayName("Normalizes postgres:// standard cloud URL format with user, password, and port")
    void testNormalizesPostgresUri() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "postgres://voroa_user:Secret%23Pass1@voroa-pg.db.cloud:5432/lorry_fleet_prod"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://voroa-pg.db.cloud:5432/lorry_fleet_prod");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("voroa_user");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("Secret#Pass1");
        assertThat(env.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
    }

    @Test
    @DisplayName("Normalizes postgresql:// URL with query parameters like sslmode=require")
    void testNormalizesPostgresqlUriWithQueryParams() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "postgresql://voroa_admin:my_password@pg-host:5432/fleet_db?sslmode=require"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://pg-host:5432/fleet_db?sslmode=require");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("voroa_admin");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("my_password");
    }

    @Test
    @DisplayName("Falls back to DB_URL if DATABASE_URL is not set")
    void testFallsBackToDbUrl() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DB_URL", "postgres://fallback_user:pass123@localhost:5432/fallback_db"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://localhost:5432/fallback_db");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("fallback_user");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("pass123");
    }

    @Test
    @DisplayName("Leaves standard JDBC URL unchanged")
    void testLeavesJdbcUrlUnchanged() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "jdbc:postgresql://localhost:5432/already_jdbc_db"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url")).isNull();
    }
}

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
    @DisplayName("Normalizes postgres:// standard cloud URL format with user, password, port, and enforces SSL/timeouts")
    void testNormalizesPostgresUri() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "postgres://voroa_user:Secret%23Pass1@voroa-pg.db.cloud:5432/lorry_fleet_prod"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://voroa-pg.db.cloud:5432/lorry_fleet_prod?sslmode=require&connectTimeout=15&socketTimeout=30");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("voroa_user");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("Secret#Pass1");
        assertThat(env.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
    }

    @Test
    @DisplayName("Normalizes postgresql:// URL with query parameters preserving explicit sslmode and adding timeouts")
    void testNormalizesPostgresqlUriWithQueryParams() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "postgresql://voroa_admin:my_password@pg-host:5432/fleet_db?sslmode=verify-full"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://pg-host:5432/fleet_db?sslmode=verify-full&connectTimeout=15&socketTimeout=30");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("voroa_admin");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("my_password");
    }

    @Test
    @DisplayName("Safely handles special characters in password including raw #, @, :, !, %")
    void testNormalizesPostgresUriWithSpecialCharactersInPassword() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "postgres://voroa_owner:P@ss#w:rd!%25123@db.voroa.internal:5432/bhp_logistics_db"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.voroa.internal:5432/bhp_logistics_db?sslmode=require&connectTimeout=15&socketTimeout=30");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("voroa_owner");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("P@ss#w:rd!%123");
    }

    @Test
    @DisplayName("Strips surrounding quotes often provided by cloud environment variables")
    void testNormalizesQuotedUri() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "\"postgres://quoted_user:quoted_pass@host:5432/quoted_db\""
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://host:5432/quoted_db?sslmode=require&connectTimeout=15&socketTimeout=30");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("quoted_user");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("quoted_pass");
    }

    @Test
    @DisplayName("Defaults port to 5432 when omitted in cloud URI")
    void testDefaultsPortWhenOmitted() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "postgres://user:pass@ep-host.voroa.app/default_port_db"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://ep-host.voroa.app:5432/default_port_db?sslmode=require&connectTimeout=15&socketTimeout=30");
    }

    @Test
    @DisplayName("Enhances existing jdbc:postgresql:// URL with missing sslmode and timeouts")
    void testEnhancesExistingJdbcUrl() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "jdbc:postgresql://localhost:5432/already_jdbc_db"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://localhost:5432/already_jdbc_db?sslmode=require&connectTimeout=15&socketTimeout=30");
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
                .isEqualTo("jdbc:postgresql://localhost:5432/fallback_db?sslmode=require&connectTimeout=15&socketTimeout=30");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("fallback_user");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("pass123");
    }
}

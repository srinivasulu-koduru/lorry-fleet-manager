package com.lorryowner.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    @DisplayName("Verifies that DatabaseUrlEnvironmentPostProcessor is registered in META-INF/spring.factories for Spring Boot 3")
    void testRegisteredInSpringFactories() {
        List<String> postProcessors = SpringFactoriesLoader.loadFactoryNames(
                EnvironmentPostProcessor.class,
                Thread.currentThread().getContextClassLoader()
        );
        assertThat(postProcessors)
                .contains("com.lorryowner.backend.config.DatabaseUrlEnvironmentPostProcessor");
    }

    @Test
    @DisplayName("Normalizes postgres:// standard cloud URL format with user, password, port, and enforces SSL/timeouts")
    void testNormalizesPostgresUri() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "postgres://voroa_user:Secret%23Pass1@voroa-pg.db.cloud:5432/lorry_fleet_prod"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://voroa-pg.db.cloud:5432/lorry_fleet_prod?sslmode=require&connectTimeout=15&socketTimeout=30&loginTimeout=15");
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
                .isEqualTo("jdbc:postgresql://pg-host:5432/fleet_db?sslmode=verify-full&connectTimeout=15&socketTimeout=30&loginTimeout=15");
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
                .isEqualTo("jdbc:postgresql://db.voroa.internal:5432/bhp_logistics_db?sslmode=require&connectTimeout=15&socketTimeout=30&loginTimeout=15");
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
                .isEqualTo("jdbc:postgresql://host:5432/quoted_db?sslmode=require&connectTimeout=15&socketTimeout=30&loginTimeout=15");
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
                .isEqualTo("jdbc:postgresql://ep-host.voroa.app:5432/default_port_db?sslmode=require&connectTimeout=15&socketTimeout=30&loginTimeout=15");
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
                .isEqualTo("jdbc:postgresql://localhost:5432/already_jdbc_db?sslmode=require&connectTimeout=15&socketTimeout=30&loginTimeout=15");
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
                .isEqualTo("jdbc:postgresql://localhost:5432/fallback_db?sslmode=require&connectTimeout=15&socketTimeout=30&loginTimeout=15");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("fallback_user");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("pass123");
    }

    @Test
    @DisplayName("Preserves existing loginTimeout when supplied by incoming URL without overwriting")
    void testPreservesExistingLoginTimeout() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "postgres://user:pass@db-host:20201/bhp_logistics_db?loginTimeout=25"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db-host:20201/bhp_logistics_db?loginTimeout=25&sslmode=require&connectTimeout=15&socketTimeout=30");
        assertThat(env.getProperty("spring.datasource.url"))
                .doesNotContain("loginTimeout=15");
    }

    @Test
    @DisplayName("Preserves custom SSL mode and timeout parameters when supplied by incoming URL")
    void testPreservesExistingSslAndTimeoutParameters() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "postgresql://user:pass@db-host:20201/bhp_logistics_db?sslmode=verify-full&connectTimeout=20&socketTimeout=45&loginTimeout=25"
        )));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db-host:20201/bhp_logistics_db?sslmode=verify-full&connectTimeout=20&socketTimeout=45&loginTimeout=25");
    }

    @Test
    @DisplayName("Does not duplicate parameters when all required parameters are already present")
    void testNoDuplicateParametersWhenAllPresent() {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "postgres://user:pass@db-host:20201/bhp_logistics_db?sslmode=require&connectTimeout=15&socketTimeout=30&loginTimeout=15"
        )));

        processor.postProcessEnvironment(env, application);

        String url = env.getProperty("spring.datasource.url");
        assertThat(url).isNotNull();
        // Count occurrences of each parameter
        assertThat(url.split("sslmode=").length - 1).isEqualTo(1);
        assertThat(url.split("connectTimeout=").length - 1).isEqualTo(1);
        assertThat(url.split("socketTimeout=").length - 1).isEqualTo(1);
        assertThat(url.split("loginTimeout=").length - 1).isEqualTo(1);
    }

    @Test
    @DisplayName("Outputs early diagnostic log to System.err with host, port, database, sslmode and timeouts without exposing credentials")
    void testDiagnosticOutputToSystemErr() {
        ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(errCapture));

            ConfigurableEnvironment env = new StandardEnvironment();
            env.getPropertySources().addLast(new MapPropertySource("testEnv", Map.of(
                    "DATABASE_URL", "postgres://secret_user:SuperSecretPassword123@db-4293d6e4e653.db.getvoroa.com:20201/bhp_logistics_db"
            )));

            processor.postProcessEnvironment(env, application);

            String errOutput = errCapture.toString(StandardCharsets.UTF_8);
            assertThat(errOutput)
                    .contains("PostgreSQL target: host='db-4293d6e4e653.db.getvoroa.com'")
                    .contains("port=20201")
                    .contains("database='bhp_logistics_db'")
                    .contains("sslmode='require'")
                    .contains("connectTimeout=15s")
                    .contains("socketTimeout=30s")
                    .contains("loginTimeout=15s")
                    .doesNotContain("secret_user")
                    .doesNotContain("SuperSecretPassword123")
                    .doesNotContain("postgres://");
        } finally {
            System.setErr(originalErr);
        }
    }
}

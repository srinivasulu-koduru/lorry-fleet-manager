package com.lorryowner.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * EnvironmentPostProcessor that inspects DATABASE_URL / DB_URL supplied by cloud hosting
 * providers (Voroa, Heroku, Railway, Render, Supabase, Neon) and safely normalizes standard URI
 * formats (e.g., 'postgres://user:pass@host:5432/dbname' or 'postgresql://...') into
 * standard Spring Boot JDBC properties (spring.datasource.url = 'jdbc:postgresql://...').
 *
 * Enforces production reliability:
 * 1. Safe parsing resilient to special characters in passwords (#, @, %, :, !, etc.) without URI.create() flaws.
 * 2. Enforces SSL mode (sslmode=require) for cloud-hosted PostgreSQL instances like Voroa.
 * 3. Enforces finite JDBC connectTimeout (15s), socketTimeout (30s), and loginTimeout (15s) to prevent infinite socketRead0 hangs.
 * 4. Strips surrounding quotes often injected by cloud environment dashboards.
 * 5. Masks credentials in all log statements to prevent credential leakage.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor.class);
    public static final String PROPERTY_SOURCE_NAME = "normalizedCloudDatabaseUrl";

    public static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 15;
    public static final int DEFAULT_SOCKET_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_LOGIN_TIMEOUT_SECONDS = 15;
    public static final String DEFAULT_SSL_MODE = "require";

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
        // Remove surrounding single or double quotes often added by cloud secret dashboards
        if ((rawUrl.startsWith("\"") && rawUrl.endsWith("\"")) || (rawUrl.startsWith("'") && rawUrl.endsWith("'"))) {
            rawUrl = rawUrl.substring(1, rawUrl.length() - 1).trim();
        }

        String lowerRaw = rawUrl.toLowerCase();

        // Case 1: Already a JDBC URL
        if (lowerRaw.startsWith("jdbc:postgresql://")) {
            String normalizedJdbcUrl = normalizeJdbcQueryParams(rawUrl);
            if (!normalizedJdbcUrl.equals(rawUrl)) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("spring.datasource.url", normalizedJdbcUrl);
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
                log.info("Enhanced existing JDBC DATABASE_URL with required SSL and timeout parameters.");
            }
            return;
        }

        // Case 2: Standard cloud URI format 'postgres://' or 'postgresql://'
        if (lowerRaw.startsWith("postgres://") || lowerRaw.startsWith("postgresql://")) {
            try {
                String withoutScheme = rawUrl.replaceFirst("(?i)^postgres(ql)?://", "");

                // Split query parameters from path/authority
                String query = null;
                int questionMarkIdx = withoutScheme.indexOf('?');
                String mainPart = withoutScheme;
                if (questionMarkIdx != -1) {
                    mainPart = withoutScheme.substring(0, questionMarkIdx);
                    query = withoutScheme.substring(questionMarkIdx + 1);
                }

                // Locate the last '@' before the database path delimiter '/' to reliably separate credentials from host.
                // This correctly handles passwords containing '@' or special characters.
                int slashIdx = mainPart.indexOf('/');
                int atIdx = (slashIdx != -1) ? mainPart.lastIndexOf('@', slashIdx) : mainPart.lastIndexOf('@');

                String userInfo = null;
                String hostAndPortAndDb = mainPart;
                if (atIdx != -1) {
                    userInfo = mainPart.substring(0, atIdx);
                    hostAndPortAndDb = mainPart.substring(atIdx + 1);
                }

                String username = null;
                String password = null;
                if (StringUtils.hasText(userInfo)) {
                    int colonIdx = userInfo.indexOf(':');
                    if (colonIdx != -1) {
                        username = safeDecode(userInfo.substring(0, colonIdx));
                        password = safeDecode(userInfo.substring(colonIdx + 1));
                    } else {
                        username = safeDecode(userInfo);
                    }
                }

                // Extract database name and host/port
                int pathSlashIdx = hostAndPortAndDb.indexOf('/');
                String hostAndPort = (pathSlashIdx != -1) ? hostAndPortAndDb.substring(0, pathSlashIdx) : hostAndPortAndDb;
                String dbName = (pathSlashIdx != -1 && pathSlashIdx + 1 < hostAndPortAndDb.length())
                        ? hostAndPortAndDb.substring(pathSlashIdx + 1) : "";

                String host = hostAndPort;
                int port = 5432;

                // Handle IPv6 bracket format [::1]:5432 or standard host:port
                if (hostAndPort.startsWith("[") && hostAndPort.contains("]")) {
                    int closeBracket = hostAndPort.indexOf(']');
                    host = hostAndPort.substring(1, closeBracket);
                    int colonAfter = hostAndPort.indexOf(':', closeBracket);
                    if (colonAfter != -1) {
                        try {
                            port = Integer.parseInt(hostAndPort.substring(colonAfter + 1));
                        } catch (NumberFormatException ignored) {}
                    }
                } else {
                    int colonIdx = hostAndPort.lastIndexOf(':');
                    if (colonIdx != -1) {
                        host = hostAndPort.substring(0, colonIdx);
                        try {
                            port = Integer.parseInt(hostAndPort.substring(colonIdx + 1));
                        } catch (NumberFormatException ignored) {}
                    }
                }

                StringBuilder baseJdbc = new StringBuilder();
                baseJdbc.append("jdbc:postgresql://").append(host).append(":").append(port).append("/").append(dbName);

                String finalJdbcUrl = normalizeJdbcQueryParams(baseJdbc.toString(), query);

                Map<String, Object> properties = new HashMap<>();
                properties.put("spring.datasource.url", finalJdbcUrl);
                properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

                if (username != null) {
                    properties.put("spring.datasource.username", username);
                }
                if (password != null) {
                    properties.put("spring.datasource.password", password);
                }

                log.info("Normalized cloud DATABASE_URL into JDBC format for host '{}:{}' on database '{}' (sslmode=require, connectTimeout={}s, socketTimeout={}s, loginTimeout={}s)",
                        host, port, dbName, DEFAULT_CONNECT_TIMEOUT_SECONDS, DEFAULT_SOCKET_TIMEOUT_SECONDS, DEFAULT_LOGIN_TIMEOUT_SECONDS);

                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));

            } catch (Exception e) {
                log.warn("Failed to parse cloud DATABASE_URL; falling back to default configuration. Error: {}", e.getMessage());
            }
        }
    }

    /**
     * Ensures that the JDBC URL includes sslmode=require, connectTimeout, socketTimeout, and loginTimeout
     * without overriding explicit query parameters configured by the user.
     */
    public static String normalizeJdbcQueryParams(String jdbcUrl) {
        int qIdx = jdbcUrl.indexOf('?');
        String base = (qIdx != -1) ? jdbcUrl.substring(0, qIdx) : jdbcUrl;
        String query = (qIdx != -1) ? jdbcUrl.substring(qIdx + 1) : null;
        return normalizeJdbcQueryParams(base, query);
    }

    public static String normalizeJdbcQueryParams(String baseJdbcUrl, String query) {
        StringBuilder queryBuilder = new StringBuilder();
        boolean hasSslMode = false;
        boolean hasConnectTimeout = false;
        boolean hasSocketTimeout = false;
        boolean hasLoginTimeout = false;

        if (StringUtils.hasText(query)) {
            String[] params = query.split("&");
            for (String param : params) {
                if (!StringUtils.hasText(param)) continue;
                String[] kv = param.split("=", 2);
                String key = kv[0].trim();
                if ("sslmode".equalsIgnoreCase(key)) hasSslMode = true;
                if ("connectTimeout".equalsIgnoreCase(key)) hasConnectTimeout = true;
                if ("socketTimeout".equalsIgnoreCase(key)) hasSocketTimeout = true;
                if ("loginTimeout".equalsIgnoreCase(key)) hasLoginTimeout = true;

                if (queryBuilder.length() > 0) queryBuilder.append("&");
                queryBuilder.append(param);
            }
        }

        if (!hasSslMode) {
            if (queryBuilder.length() > 0) queryBuilder.append("&");
            queryBuilder.append("sslmode=").append(DEFAULT_SSL_MODE);
        }
        if (!hasConnectTimeout) {
            if (queryBuilder.length() > 0) queryBuilder.append("&");
            queryBuilder.append("connectTimeout=").append(DEFAULT_CONNECT_TIMEOUT_SECONDS);
        }
        if (!hasSocketTimeout) {
            if (queryBuilder.length() > 0) queryBuilder.append("&");
            queryBuilder.append("socketTimeout=").append(DEFAULT_SOCKET_TIMEOUT_SECONDS);
        }
        if (!hasLoginTimeout) {
            if (queryBuilder.length() > 0) queryBuilder.append("&");
            queryBuilder.append("loginTimeout=").append(DEFAULT_LOGIN_TIMEOUT_SECONDS);
        }

        return baseJdbcUrl + "?" + queryBuilder;
    }

    /**
     * Safely decodes URL percent-encoding. If percent signs are raw characters rather than
     * valid hex escape pairs, returns the original string gracefully.
     */
    private static String safeDecode(String value) {
        if (value == null) return null;
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

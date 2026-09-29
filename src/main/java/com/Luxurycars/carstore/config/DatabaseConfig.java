package com.Luxurycars.carstore.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;

@Configuration
@Profile("!test")
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:}")
    private String configuredUrl;

    @Value("${spring.datasource.username:}")
    private String configuredUsername;

    @Value("${spring.datasource.password:}")
    private String configuredPassword;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Bean
    @Primary
    public DataSource dataSource() {
        // 1. Resolve raw URL from environment or configuration
        String rawUrl = getEnvFirst(
                "MYSQL_PRIVATE_URL",
                "MYSQL_URL",
                "DATABASE_PRIVATE_URL",
                "DATABASE_URL",
                "DB_URL",
                "SPRING_DATASOURCE_URL"
        );

        String jdbcUrl;
        String username = getEnvFirst("MYSQLUSER", "MYSQL_USER", "DB_USERNAME");
        String password = getEnvFirst("MYSQLPASSWORD", "MYSQL_PASSWORD", "DB_PASSWORD");

        if (rawUrl != null && !rawUrl.isBlank()) {
            rawUrl = rawUrl.trim();
            if (rawUrl.startsWith("mysql://") || rawUrl.startsWith("mysqls://")) {
                try {
                    URI uri = URI.create(rawUrl);
                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                    String path = uri.getPath();
                    String dbName = "railway";
                    if (path != null && path.length() > 1) {
                        String cleanPath = path.substring(1);
                        if (cleanPath.contains("?")) {
                            cleanPath = cleanPath.substring(0, cleanPath.indexOf("?"));
                        }
                        if (!cleanPath.isBlank()) {
                            dbName = cleanPath;
                        }
                    }

                    if (uri.getUserInfo() != null) {
                        String[] parts = uri.getUserInfo().split(":", 2);
                        if (parts.length > 0 && (username == null || username.isBlank())) {
                            username = parts[0];
                        }
                        if (parts.length > 1 && (password == null || password.isBlank())) {
                            password = parts[1];
                        }
                    }

                    jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&autoReconnect=true";
                } catch (Exception e) {
                    log.warn("Could not parse database URI '{}', falling back: {}", rawUrl, e.getMessage());
                    jdbcUrl = rawUrl.startsWith("jdbc:") ? rawUrl : "jdbc:" + rawUrl;
                }
            } else if (!rawUrl.startsWith("jdbc:")) {
                jdbcUrl = "jdbc:" + rawUrl;
            } else {
                jdbcUrl = rawUrl;
            }
        } else {
            // Check individual host, port, database vars
            String host = getEnvFirst("MYSQLHOST", "MYSQL_HOST", "DB_HOST");
            String port = getEnvFirst("MYSQLPORT", "MYSQL_PORT", "DB_PORT");
            String db = getEnvFirst("MYSQLDATABASE", "MYSQL_DATABASE", "DB_NAME");

            if (host != null && !host.isBlank()) {
                if (port == null || port.isBlank()) port = "3306";
                if (db == null || db.isBlank()) db = "railway";
                jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db
                        + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&autoReconnect=true";
            } else if (configuredUrl != null && !configuredUrl.isBlank()) {
                jdbcUrl = configuredUrl;
            } else {
                jdbcUrl = "jdbc:mysql://localhost:3306/railway?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&autoReconnect=true";
            }
        }

        if (username == null || username.isBlank()) {
            username = (configuredUsername != null && !configuredUsername.isBlank()) ? configuredUsername : "root";
        }
        if (password == null) {
            password = (configuredPassword != null) ? configuredPassword : "";
        }

        log.info("🔌 Initializing DataSource: url='{}', username='{}'", jdbcUrl, username);

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);

        HikariDataSource ds = new HikariDataSource(config);

        // Verify connection early to output helpful diagnostics if Railway MySQL is missing or sleeping
        try (Connection conn = ds.getConnection()) {
            log.info("✅ Database connection established successfully to {}", jdbcUrl);
        } catch (Exception e) {
            log.error("""
                    \n********************************************************************************
                    ❌ DATABASE CONNECTION FAILED: {}
                    URL: {}
                    Username: {}
                    
                    👉 If running on Railway:
                    1. Ensure your MySQL service in Railway is running.
                    2. In your backend service -> 'Variables' tab:
                       Add: MYSQL_URL = ${{MySQL.MYSQL_URL}}
                       or add reference variables: MYSQLHOST, MYSQLPORT, MYSQLUSER, MYSQLPASSWORD, MYSQLDATABASE.
                    ********************************************************************************
                    """, e.getMessage(), jdbcUrl, username);
            throw new IllegalStateException("Failed to connect to MySQL database at " + jdbcUrl + ": " + e.getMessage(), e);
        }

        return ds;
    }

    private String getEnvFirst(String... varNames) {
        for (String varName : varNames) {
            String val = System.getenv(varName);
            if (val != null && !val.isBlank()) {
                return val;
            }
            val = System.getProperty(varName);
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return null;
    }
}

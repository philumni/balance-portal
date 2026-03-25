package com.portal.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages the HikariCP connection pool for MariaDB.
 *
 * ENVIRONMENT VARIABLES (set before starting Tomcat):
 * ---------------------------------------------------
 *   DB_HOST      MariaDB host          (default: localhost)
 *   DB_PORT      MariaDB port          (default: 3306)
 *   DB_NAME      Database name         (default: balance_portal)
 *   DB_USERNAME  App DB user           (default: portaluser)
 *   DB_PASSWORD  App DB password       (required)
 *
 * WHY HIKARICP?
 * -------------
 * Opening a raw JDBC connection on every request creates a new TCP
 * handshake each time — expensive and slow under load.
 * HikariCP maintains a warm pool of ready connections and is the
 * fastest connection pool available for Java. Spring Boot uses it
 * as its default for exactly this reason.
 *
 * POOL SIZING
 * -----------
 * The formula from HikariCP's author:
 *   pool_size = (core_count * 2) + effective_spindle_count
 * For a dev machine with 4 cores and SSD: ~10 connections is fine.
 * For production: tune based on MariaDB's max_connections setting.
 */
public class ConnectionPool {

    private static volatile HikariDataSource dataSource;
    private static volatile boolean          available = false;

    private ConnectionPool() {}

    /**
     * Initializes the pool. Called once at application startup
     * by RepositoryFactory. Safe to call multiple times (idempotent).
     *
     * @return true if the pool connected successfully, false otherwise
     */
    public static synchronized boolean initialize() {
        if (dataSource != null) return available;

        String host     = env("DB_HOST",     "localhost");
        String port     = env("DB_PORT",     "3306");
        String dbName   = env("DB_NAME",     "balance_portal");
        String username = env("DB_USERNAME", "portaluser");
        String password = env("DB_PASSWORD", "");

        if (password.isBlank()) {
            System.err.println("[ConnectionPool] DB_PASSWORD env var is not set. " +
                               "Falling back to MockDataStore.");
            return false;
        }

        String jdbcUrl = String.format(
            "jdbc:mariadb://%s:%s/%s" +
            "?useUnicode=true&characterEncoding=UTF-8" +
            "&connectTimeout=5000" +
            "&socketTimeout=30000",
            host, port, dbName
        );

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.mariadb.jdbc.Driver");

        // Pool sizing
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300_000);          // 5 min
        config.setConnectionTimeout(5_000);      // 5 sec — fail fast
        config.setMaxLifetime(1_800_000);         // 30 min — recycle before MariaDB closes it

        // Validate connections before handing them out
        config.setConnectionTestQuery("SELECT 1");

        // Pool name — shows in logs and JMX
        config.setPoolName("BalancePortalPool");

        try {
            dataSource = new HikariDataSource(config);

            // Test that we can actually get a connection
            try (Connection c = dataSource.getConnection()) {
                System.out.println("[ConnectionPool] Connected to MariaDB at "
                        + host + ":" + port + "/" + dbName);
                available = true;
            }

        } catch (HikariPool.PoolInitializationException | SQLException e) {
            System.err.println("[ConnectionPool] Could not connect to MariaDB: "
                    + e.getMessage());
            System.err.println("[ConnectionPool] Falling back to MockDataStore.");
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
            available = false;
        }

        return available;
    }

    /**
     * Returns the DataSource. Throws if the pool was never initialized
     * or failed to connect.
     */
    public static DataSource getDataSource() {
        if (dataSource == null || !available) {
            throw new IllegalStateException(
                "Connection pool is not available. Check DB env vars.");
        }
        return dataSource;
    }

    /**
     * Returns a connection from the pool.
     * Always use try-with-resources so the connection is returned to the pool.
     *
     * Example:
     *   try (Connection conn = ConnectionPool.getConnection()) { ... }
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static boolean isAvailable() { return available; }

    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[ConnectionPool] Pool shut down.");
        }
    }

    private static String env(String key, String defaultVal) {
        String v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : defaultVal;
    }
}

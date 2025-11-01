package com.educollab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DatabaseConfig {
    
    @Value("${spring.datasource.url}")
    private String dataSourceUrl;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Bean
    public DataSource dataSource() {
        String jdbcUrl = dataSourceUrl;
        String dbUsername = username;
        String dbPassword = password;
        
        // Parse Supabase connection string format: postgresql://user:password@host:port/db
        if (jdbcUrl.startsWith("postgresql://")) {
            try {
                // Extract credentials from URL if present
                String urlWithoutProtocol = jdbcUrl.substring("postgresql://".length());
                int atIndex = urlWithoutProtocol.indexOf('@');
                
                if (atIndex > 0) {
                    // Credentials are in the URL
                    String credentials = urlWithoutProtocol.substring(0, atIndex);
                    String hostAndDb = urlWithoutProtocol.substring(atIndex + 1);
                    
                    int colonIndex = credentials.indexOf(':');
                    if (colonIndex > 0) {
                        dbUsername = credentials.substring(0, colonIndex);
                        dbPassword = credentials.substring(colonIndex + 1);
                        // URL decode password if needed
                        dbPassword = java.net.URLDecoder.decode(dbPassword, java.nio.charset.StandardCharsets.UTF_8);
                    }
                    
                    // Build JDBC URL without credentials
                    // Add parameters to fix prepared statement caching issues with Supabase
                    String params = hostAndDb.contains("?") ? "&" : "?";
                    params += "prepareThreshold=0&reWriteBatchedInserts=true";
                    jdbcUrl = "jdbc:postgresql://" + hostAndDb + params;
                    System.out.println("Parsed credentials from DATABASE_URL");
                } else {
                    // No credentials in URL, convert to JDBC format
                    // Add parameters to fix prepared statement caching issues with Supabase
                    String params = jdbcUrl.contains("?") ? "&" : "?";
                    params += "prepareThreshold=0&reWriteBatchedInserts=true";
                    jdbcUrl = "jdbc:" + jdbcUrl + params;
                }
            } catch (Exception e) {
                System.err.println("Error parsing DATABASE_URL: " + e.getMessage());
                // Fallback: just add jdbc: prefix
                // Add parameters to fix prepared statement caching issues with Supabase
                String params = jdbcUrl.contains("?") ? "&" : "?";
                params += "prepareThreshold=0&reWriteBatchedInserts=true";
                jdbcUrl = "jdbc:" + jdbcUrl + params;
            }
        }
        
        System.out.println("========================================");
        System.out.println("🔌 Database Configuration:");
        System.out.println("JDBC URL: " + jdbcUrl);
        System.out.println("Username: " + dbUsername);
        System.out.println("Password length: " + (dbPassword != null ? dbPassword.length() : 0));
        System.out.println("========================================");
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("org.postgresql.Driver");
        config.setConnectionTimeout(30000);
        
        // Reduced pool size for Supabase free tier (15 connections max)
        // Keep it conservative to avoid hitting limits
        config.setMaximumPoolSize(2); // Very conservative
        config.setMinimumIdle(1);
        config.setInitializationFailTimeout(-1);
        
        // Connection validation settings
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        config.setMaxLifetime(300000); // 5 minutes - shorter lifetime to release connections faster
        config.setIdleTimeout(120000); // 2 minutes - shorter idle timeout
        
        // Connection leak detection
        config.setLeakDetectionThreshold(60000); // 60 seconds
        
        // Disable autocommit so Hibernate can manage transactions
        config.setAutoCommit(false);
        
        // Connection pool name for debugging
        config.setPoolName("SupabaseHikariCP");
        
        HikariDataSource dataSource = new HikariDataSource(config);
        
        // Register shutdown hook to close connections properly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down database connection pool...");
            dataSource.close();
        }));
        
        return dataSource;
    }
}


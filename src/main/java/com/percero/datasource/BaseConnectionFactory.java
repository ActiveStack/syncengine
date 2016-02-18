package com.percero.datasource;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class BaseConnectionFactory implements IConnectionFactory {

    private static Logger logger = Logger.getLogger(BaseConnectionFactory.class);

    public static final String HIKARI_CONNECTION_POOL = "hikari";
    public static final String C3P0_CONNECTION_POOL = "c3p0";
    
    private String name;
    private String preferredConnectionPool = HIKARI_CONNECTION_POOL;
    private Integer acquireIncrement = 4;
    private Integer minPoolSize = 4;
    private Integer maxPoolSize = 52;
    private Integer maxIdleTime = 60 * 30; // 30 Minutes
    private String testQuery = "SELECT 1 FROM dual";
    private Integer fetchSize = 100;
    
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPreferredConnectionPool() {
		return preferredConnectionPool;
	}

	public void setPreferredConnectionPool(String preferredConnectionPool) {
		this.preferredConnectionPool = preferredConnectionPool;
	}

	public Integer getAcquireIncrement() {
		return acquireIncrement;
	}

	public void setAcquireIncrement(Integer acquireIncrement) {
		this.acquireIncrement = acquireIncrement;
	}

	public Integer getMinPoolSize() {
		return minPoolSize;
	}

	public void setMinPoolSize(Integer minPoolSize) {
		this.minPoolSize = minPoolSize;
	}

	public Integer getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(Integer maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public Integer getMaxIdleTime() {
		return maxIdleTime;
	}

	public void setMaxIdleTime(Integer maxIdleTime) {
		this.maxIdleTime = maxIdleTime;
	}

	public String getTestQuery() {
		return testQuery;
	}

	public void setTestQuery(String testQuery) {
		this.testQuery = testQuery;
	}

	public Integer getFetchSize() {
		// Default to 100;
		if (fetchSize == null || fetchSize <= 0) {
			return 100;
		}
		return fetchSize;
	}

	public void setFetchSize(Integer fetchSize) {
		this.fetchSize = fetchSize;
	}

    private String driverClassName;
    public void setDriverClassName(String val){
        this.driverClassName = val;
    }
    public String getDriverClassName(){
    	return driverClassName;
    }

    private String username;
    public void setUsername(String val){
        this.username = val;
    }
    public String getUsername(){
    	return username;
    }

    private String password;
    public void setPassword(String val){
        this.password = val;
    }
    public String getPassword(){
    	return password;
    }

    private String jdbcUrl;
    public void setJdbcUrl(String val){
        this.jdbcUrl = val;
    }
    public String getJdbcUrl(){
    	return jdbcUrl;
    }

    private DataSource ds;
    public DataSource getDataSource() {
    	return ds;
    }

    public void init() throws PropertyVetoException{
        try {
            if (C3P0_CONNECTION_POOL.equalsIgnoreCase(preferredConnectionPool)) {
                ComboPooledDataSource cpds = new ComboPooledDataSource();
                cpds.setDriverClass(driverClassName); // loads the jdbc driver
                cpds.setJdbcUrl(jdbcUrl);
                cpds.setUser(username);
                cpds.setPassword(password);

                // the settings below are optional -- c3p0 can work with
                // defaults
                if (minPoolSize != null) {
                  cpds.setMinPoolSize(minPoolSize);
                }
                if (acquireIncrement != null) {
                  cpds.setAcquireIncrement(acquireIncrement);
                }
                if (maxPoolSize != null) {
                  cpds.setMaxPoolSize(maxPoolSize);
                }
                if (maxIdleTime != null) {
                  cpds.setMaxIdleTime(maxIdleTime);
                  cpds.setIdleConnectionTestPeriod(maxIdleTime);
                }
                cpds.setNumHelperThreads(30);
                cpds.setTestConnectionOnCheckout(true);
                if (StringUtils.hasText(testQuery)) {
                  cpds.setPreferredTestQuery(testQuery);
                }

                ds = cpds;
              } else {
                // Default to Hikari Connection Pool.
                HikariConfig config = new HikariConfig();
                config.setDriverClassName(driverClassName);
                config.setRegisterMbeans(true);
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

                if (StringUtils.hasText(name)) {
                	config.setPoolName(name);
                }
                if (minPoolSize != null) {
                  config.setMinimumIdle(minPoolSize);
                }
                // if (acquireIncrement != null) {
                // config.setAcquireIncrement(acquireIncrement);
                // }
                if (maxPoolSize != null) {
                  config.setMaximumPoolSize(maxPoolSize);
                }
                if (maxIdleTime != null) {
                  config.setIdleTimeout(maxIdleTime * 1000); // Convert to
                                        // milliseconds
                }
                // config.setNumHelperThreads(30);
                // config.setTestConnectionOnCheckout(true);
                if (StringUtils.hasText(testQuery)) {
                  config.setConnectionTestQuery(testQuery);
                }

                ds = new HikariDataSource(config);
              }
        	
        }catch(PropertyVetoException pve){
            logger.error(pve.getMessage(), pve);
            throw pve;
        }
    }

    public Connection getConnection() throws SQLException{
        try{
        	if (ds == null) {
                init();
            }
            return getDataSource().getConnection();
        }
        catch(PropertyVetoException e){
            logger.error(e.getMessage(), e);
            throw new SQLException(e);
        }
        catch(SQLException e){
        	logger.error(e.getMessage(), e);
        	throw e;
        }
    }

}

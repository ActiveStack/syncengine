package com.percero.datasource.sql;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.percero.agents.sync.services.DAODataProvider;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.datasource.IConnectionFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * See http://www.mchange.com/projects/c3p0/ for configuration tuning.
 * 
 */
public class SqlConnectionFactory implements IConnectionFactory {

  private static Logger logger = Logger.getLogger(SqlConnectionFactory.class);

  private static final long MAX_CONNECT_TIME = 2500; // 2.5 Seconds.

  private static final String HIKARI_CONNECTION_POOL = "hikari";
  private static final String C3P0_CONNECTION_POOL = "c3p0";

  public SqlConnectionFactory() {

  }

  private String name;
  private String preferredConnectionPool = HIKARI_CONNECTION_POOL;
  private String driverClassName;
  private String username;
  private String password;
  private String jdbcUrl;
  private Integer acquireIncrement = 4;
  private Integer minPoolSize = 4;
  private Integer maxPoolSize = 52;
  private Integer maxIdleTime = 60 * 30; // 30 Minutes
  private String testQuery = null;
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

  public String getDriverClassName() {
    return driverClassName;
  }

  public void setDriverClassName(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public void setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
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

  // private ComboPooledDataSource cpds;
  private DataSource ds;

  private volatile boolean initialized = false;

  public synchronized void init() throws Exception {
    if (initialized) {
      return;
    }
    try {
      logger.info("Initializing connection factory: " + getName());

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
        if (!StringUtils.hasText(testQuery)) {
        	defaultTestQuery();
        }
        cpds.setPreferredTestQuery(testQuery);

        ds = cpds;
      } else {
        // Default to Hikari Connection Pool.
        HikariConfig config = new HikariConfig();
        // config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        config.setPoolName(name);
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
        if (!StringUtils.hasText(testQuery)) {
        	defaultTestQuery();
        }
        config.setConnectionTestQuery(testQuery);

        ds = new HikariDataSource(config);
      }

      BaseSqlDataConnectionRegistry.getInstance()
          .registerConnectionFactory(getName(), this);
      DataProviderManager.getInstance().setDefaultDataProvider(
          DAODataProvider.getInstance());

      initialized = true;
    } catch (Exception pve) {
      logger.error(pve.getMessage(), pve);
      throw pve;
    }
  }

/**
 * 
 */
private void defaultTestQuery() {
	if (driverClassName.toLowerCase().contains("mysql") || driverClassName.toLowerCase().contains("h2") || driverClassName.toLowerCase().contains("mssql") || driverClassName.toLowerCase().contains("postgre") || driverClassName.toLowerCase().contains("sqlite")) {
		testQuery = "SELECT 1";
	}
	else if (driverClassName.toLowerCase().contains("oracle")) {
		testQuery = "SELECT 1 from dual";
	}
	else if (driverClassName.toLowerCase().contains("hsql")) {
		testQuery = "SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS";
	}
	else if (driverClassName.toLowerCase().contains("db2")) {
		testQuery = "SELECT 1 FROM SYSIBM.SYSDUMMY1";
	}
	else if (driverClassName.toLowerCase().contains("infomix")) {
		testQuery = "select count(*) from systables";
	}
	else {
		testQuery = "SELECT 1";
	}
}

  /*
   * (non-Javadoc)
   * 
   * @see com.pulse.dataprovider.IConnectionFactory#getConnection()
   */
  @Override
  public Connection getConnection() throws SQLException {
    long timeStart = System.currentTimeMillis();
    try {
      if (!initialized) {
        try {
          init();
        } catch (Exception e) {
          logger.error("Error initializing SqlConnectionFactory: "
              + this.getName(), e);
        }
      }
      Connection result = ds.getConnection();
      // Connection result = cpds.getConnection();
      logger.debug("Database Connection Time: "
          + (System.currentTimeMillis() - timeStart) + "ms ["
          + this.getName() + ": " + this.getJdbcUrl() + "]");
      return result;
    } catch (SQLException e) {
      logger.error(e.getMessage(), e);
      throw e;
    } finally {
      long timeEnd = System.currentTimeMillis();
      long totalTime = timeEnd - timeStart;
      if (totalTime > MAX_CONNECT_TIME) {
        logger.warn("Long Database Connection: " + totalTime + "ms ["
            + this.getName() + ": " + this.getJdbcUrl() + "]");
      }
    }
  }

  // public static void main(String[] args) throws Exception{
  // UpdateTableConnectionFactory cf = new UpdateTableConnectionFactory();
  // cf.driverClassName = "com.mysql.jdbc.Driver";
  // cf.jdbcUrl = "jdbc:mysql://localhost/test";
  // cf.username = "root";
  // cf.password = "root";
  // cf.init();
  //
  // Connection c = cf.getConnection();
  // Statement stmt = c.createStatement();
  //
  //
  // ResultSet rs = stmt.executeQuery("SELECT * FROM Account");
  // while(rs.next()) {
  // logger.info("ID: " + rs.getInt("ID"));
  // logger.info("markedForRemoval: "+ rs.getDate("markedForRemoval"));
  // }
  // }
}

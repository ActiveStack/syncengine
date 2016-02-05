package com.percero.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariDataSource;

public class TestBaseConnectionFactory {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	BaseConnectionFactory bcf = null;
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		bcf = null;
	}

	@Test
	public void testInit() {
		BaseConnectionFactory bcf = new BaseConnectionFactory();
		bcf.setJdbcUrl("jdbc:h2:~/test");
		bcf.setDriverClassName("org.h2.Driver");
		bcf.setName("TESTS");
		bcf.setUsername("sa");
		bcf.setPassword("");
		
		String testQuery = "SELECT 1 FROM dual";
		bcf.setTestQuery(testQuery);
		bcf.setMaxIdleTime(1000);
		
		try {
			// Validate data source type.
			bcf.init();
			assertTrue(bcf.getDataSource() instanceof HikariDataSource);
			HikariDataSource hds = (HikariDataSource) bcf.getDataSource();
			assertEquals(bcf.getDriverClassName(), hds.getDriverClassName());
			assertEquals(bcf.getJdbcUrl(), hds.getJdbcUrl());
			assertEquals(bcf.getUsername(), hds.getUsername());
			assertEquals(bcf.getPassword(), hds.getPassword());
			assertEquals(bcf.getName(), hds.getPoolName());
			assertEquals(bcf.getMinPoolSize().intValue(), hds.getMinimumIdle());
			assertEquals(bcf.getMaxPoolSize().longValue(), hds.getMaximumPoolSize());
			assertEquals(bcf.getMaxIdleTime().longValue() * 1000, hds.getIdleTimeout());
			assertEquals(testQuery, hds.getConnectionTestQuery());
			
			assertEquals("true", hds.getDataSourceProperties().getProperty("cachePrepStmts"));
			assertEquals("250", hds.getDataSourceProperties().getProperty("prepStmtCacheSize"));
			assertEquals("2048", hds.getDataSourceProperties().getProperty("prepStmtCacheSqlLimit"));

			// C3PO.
			bcf.setPreferredConnectionPool(BaseConnectionFactory.C3P0_CONNECTION_POOL);
			bcf.init();
			assertTrue(bcf.getDataSource() instanceof ComboPooledDataSource);
			ComboPooledDataSource cpds = (ComboPooledDataSource) bcf.getDataSource();
			assertEquals(bcf.getDriverClassName(), cpds.getDriverClass());
			assertEquals(bcf.getJdbcUrl(), cpds.getJdbcUrl());
			assertEquals(bcf.getUsername(), cpds.getUser());
			assertEquals(bcf.getPassword(), cpds.getPassword());
			assertEquals(bcf.getMinPoolSize().intValue(), cpds.getMinPoolSize());
			assertEquals(bcf.getAcquireIncrement().intValue(), cpds.getAcquireIncrement());
			assertEquals(bcf.getMaxPoolSize().intValue(), cpds.getMaxPoolSize());
			assertEquals(bcf.getMaxIdleTime().intValue(), cpds.getMaxIdleTime());
			assertEquals(bcf.getMaxIdleTime().intValue(), cpds.getIdleConnectionTestPeriod());
			assertEquals(30, cpds.getNumHelperThreads());
			assertEquals(testQuery, cpds.getPreferredTestQuery());

			// Hikari.
			bcf.setPreferredConnectionPool(BaseConnectionFactory.HIKARI_CONNECTION_POOL);
			bcf.init();
			assertTrue(bcf.getDataSource() instanceof HikariDataSource);
		} catch (PropertyVetoException e) {
			// TODO Auto-generated catch block
			fail("Error with property: " + e.getMessage());
		}
	}

	@Test
	public void testGetConnection() {
		
		DataSource mockDataSource = Mockito.mock(DataSource.class);
		Connection connection = Mockito.mock(Connection.class);
		
		try {
			Mockito.when(mockDataSource.getConnection()).thenReturn(connection);
		} catch (SQLException e) {
			fail("Failed to Mock DataSource.getConnection(): " + e.getMessage());
		}
		
		bcf = Mockito.mock(BaseConnectionFactory.class);
		Mockito.when(bcf.getDataSource()).thenReturn(mockDataSource);
		
		try {
			Mockito.when(bcf.getConnection()).thenCallRealMethod();
			assertSame(connection, bcf.getConnection());
		} catch (SQLException e) {
			fail("Failed to get Connection from DataSource: " + e.getMessage());
		}
	}

	@Test
	public void testYmlImport() {
        try {
        	URL ymlUrl = TestBaseConnectionFactory.class.getClassLoader().getResource("baseconnectionfactories.yml");
        	if (ymlUrl == null) {
            	fail("No configuration file found: baseconnectionFactories.yml");
        	}
        	File configFile = new File(ymlUrl.getFile());
        	YamlReader reader = new YamlReader(new FileReader(configFile));

    		BaseConnectionFactory connectionFactory = reader.read(BaseConnectionFactory.class);
    		assertNotNull(connectionFactory);
			assertEquals("org.h2.Driver", connectionFactory.getDriverClassName());
			assertEquals("jdbc:h2:~/test", connectionFactory.getJdbcUrl());
			assertEquals("sa", connectionFactory.getUsername());
			assertEquals("pass", connectionFactory.getPassword());
			assertEquals("TEST", connectionFactory.getName());
			assertEquals(new Integer(5), connectionFactory.getMinPoolSize());
			assertEquals(new Integer(25), connectionFactory.getMaxPoolSize());
			assertEquals(new Integer(1000), connectionFactory.getMaxIdleTime());
			assertEquals("SELECT 1 FROM dual", connectionFactory.getTestQuery());
        }
        catch (FileNotFoundException e) {
        	fail("Error processing config file baseconnectionfactories.yml: " + e.getMessage());
		} catch (YamlException e) {
			fail("YAML Error processing config file baseconnectionfactories.yml: " + e.getMessage());
		}
	}
}

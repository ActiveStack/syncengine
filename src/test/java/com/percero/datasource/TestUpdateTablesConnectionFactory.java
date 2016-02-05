package com.percero.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.percero.agents.sync.jobs.UpdateTableConnectionFactory;

public class TestUpdateTablesConnectionFactory {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testYmlImport() {
        try {
        	URL ymlUrl = TestUpdateTablesConnectionFactory.class.getClassLoader().getResource("updatetablesconnectionfactories.yml");
        	if (ymlUrl == null) {
            	fail("No configuration file found: updatetablesconnectionfactories.yml");
        	}
        	File configFile = new File(ymlUrl.getFile());
        	YamlReader reader = new YamlReader(new FileReader(configFile));

        	// First UpdateTableConnectionFactory
    		UpdateTableConnectionFactory connectionFactory = reader.read(UpdateTableConnectionFactory.class);
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
			
			assertEquals(50, connectionFactory.getWeight());
			assertEquals("UPDATE_TABLE_LOCK_BATCH", connectionFactory.getStoredProcedureName());
			assertEquals("My Stored Proc", connectionFactory.getStoredProcedureDefinition());
			
			assertNotNull(connectionFactory.getTableNames());
			assertEquals(2, connectionFactory.getTableNames().length);
			assertEquals("update_table_1", connectionFactory.getTableNames()[0]);
			assertEquals("update_table_2", connectionFactory.getTableNames()[1]);

			// Second UpdateTableConnectionFactory
			connectionFactory = reader.read(UpdateTableConnectionFactory.class);
			assertNotNull(connectionFactory);
			assertEquals("org.h2.Driver", connectionFactory.getDriverClassName());
			assertEquals("jdbc:h2:~/test", connectionFactory.getJdbcUrl());
			assertEquals("sa", connectionFactory.getUsername());
			assertEquals("pass", connectionFactory.getPassword());
			
			assertEquals(55, connectionFactory.getWeight());
			assertEquals("UPDATE_TABLE_LOCK_BATCH", connectionFactory.getStoredProcedureName());
			assertEquals("My Stored Proc", connectionFactory.getStoredProcedureDefinition());
			
			assertNotNull(connectionFactory.getTableNames());
			assertEquals(1, connectionFactory.getTableNames().length);
			assertEquals("update_table_1", connectionFactory.getTableNames()[0]);
        }
        catch (FileNotFoundException e) {
        	fail("Error processing config file updatetablesconnectionfactories.yml: " + e.getMessage());
		} catch (YamlException e) {
			fail("YAML Error processing config file updatetablesconnectionfactories.yml: " + e.getMessage());
		}
	}
}

package com.percero.agents.auth.services;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests the Basic Auth Provider Factory class
 * 
 * @author Collin Brown
 *
 */
public class TestBasicAuthProviderFactory {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
    AuthProviderRegistry authProviderRegistry;
	DatabaseHelper authDatabaseHelper;
	BasicAuthProviderFactory factory;

	@Before
	public void setUp() throws Exception {
		
		authDatabaseHelper = Mockito.mock(DatabaseHelper.class);
		
		authProviderRegistry = Mockito.mock(AuthProviderRegistry.class);
		
		factory = new BasicAuthProviderFactory();
		factory.authDatabaseHelper = authDatabaseHelper;
		factory.authProviderRegistry = authProviderRegistry;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testInit() {
		// Basic Auth DISABLED - null
		factory.basicAuthEnabled = null;
		factory.init();
		Mockito.verify(authProviderRegistry, Mockito.never()).addProvider(Mockito.any(IAuthProvider.class));
		
		// Basic Auth DISABLED - false
		factory.basicAuthEnabled = false;
		factory.init();
		Mockito.verify(authProviderRegistry, Mockito.never()).addProvider(Mockito.any(IAuthProvider.class));
		
		// Basic Auth ENABLED
		factory.basicAuthEnabled = true;
		factory.init();
		Mockito.verify(authProviderRegistry, Mockito.atLeastOnce()).addProvider(Mockito.any(IAuthProvider.class));
		
	}

}

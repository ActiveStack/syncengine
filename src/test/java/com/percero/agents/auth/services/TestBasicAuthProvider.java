package com.percero.agents.auth.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.percero.agents.auth.vo.AuthProviderResponse;
import com.percero.agents.auth.vo.BasicAuthCredential;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.agents.sync.services.ISyncAgentService;

public class TestBasicAuthProvider {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	BasicAuthProvider authProvider;
	ISyncAgentService syncAgentService;
	AuthService2 authService2;
	DatabaseHelper authDatabaseHelper;

	@Before
	public void setUp() throws Exception {
		
		syncAgentService = Mockito.mock(ISyncAgentService.class);
		authService2 = Mockito.mock(AuthService2.class);
		authDatabaseHelper = Mockito.mock(DatabaseHelper.class);
		
		authProvider = new BasicAuthProvider(syncAgentService, authService2, authDatabaseHelper);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRegiser() {
		String credentialStr = "{\"username\":\"tester3\",\"password\":\"password\",\"metadata\":{\"personType\":\"Parent\",\"firstName\":\"Test3\",\"lastName\":\"Er\",\"email\":\"test3@er.com\",\"dob\":\"2000-01-01\"}}";

		// Test no ServiceUser
		try {
			Mockito.when(authDatabaseHelper.registerUser(Mockito.any(BasicAuthCredential.class), Mockito.anyString())).thenReturn(null);
		} catch (AuthException e) {
			// Do nothing.
		}
		AuthProviderResponse response = authProvider.register(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.FAILURE, response.authCode);
		
		// Test valid ServiceUser
		try {
			Mockito.when(authDatabaseHelper.registerUser(Mockito.any(BasicAuthCredential.class), Mockito.anyString())).thenReturn(new ServiceUser());
		} catch (AuthException e) {
			// Do nothing.
		}
		response = authProvider.register(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.SUCCESS, response.authCode);
		assertNotNull(response.serviceUser);
		assertEquals("Unexpected AuthProvider ID", BasicAuthProvider.ID, response.serviceUser.getAuthProviderID());
	}

	@Test
	public void testException_duplicateUserName() {
		String credentialStr = "{\"username\":\"tester3\",\"password\":\"password\",\"metadata\":{\"personType\":\"Parent\",\"firstName\":\"Test3\",\"lastName\":\"Er\",\"email\":\"test3@er.com\",\"dob\":\"2000-01-01\"}}";
		
		try {
			Mockito.when(authDatabaseHelper.registerUser(Mockito.any(BasicAuthCredential.class), Mockito.anyString())).thenThrow(new AuthException("", AuthException.DUPLICATE_USER_NAME));
		} catch (AuthException e) {
			// Do nothing.
		}
		AuthProviderResponse response = authProvider.register(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.DUPLICATE_USER_NAME, response.authCode);
	}
	
	@Test
	public void testException_dataError() {
		String credentialStr = "{\"username\":\"tester3\",\"password\":\"password\",\"metadata\":{\"personType\":\"Parent\",\"firstName\":\"Test3\",\"lastName\":\"Er\",\"email\":\"test3@er.com\",\"dob\":\"2000-01-01\"}}";
		
		try {
			Mockito.when(authDatabaseHelper.registerUser(Mockito.any(BasicAuthCredential.class), Mockito.anyString())).thenThrow(new AuthException("", AuthException.DATA_ERROR));
		} catch (AuthException e) {
			// Do nothing.
		}
		AuthProviderResponse response = authProvider.register(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.FAILURE, response.authCode);
	}
	
	@Test
	public void testException_invalidUserIdentifier() {
		String credentialStr = "{\"username\":\"tester3\",\"password\":\"password\",\"metadata\":{\"personType\":\"Parent\",\"firstName\":\"Test3\",\"lastName\":\"Er\",\"email\":\"test3@er.com\",\"dob\":\"2000-01-01\"}}";
		
		try {
			Mockito.when(authDatabaseHelper.registerUser(Mockito.any(BasicAuthCredential.class), Mockito.anyString())).thenThrow(new AuthException("", AuthException.INVALID_USER_IDENTIFIER));
		} catch (AuthException e) {
			// Do nothing.
		}
		AuthProviderResponse response = authProvider.register(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.FAILURE, response.authCode);
	}
	
	@Test
	public void testException_invalidUserPassword() {
		String credentialStr = "{\"username\":\"tester3\",\"password\":\"password\",\"metadata\":{\"personType\":\"Parent\",\"firstName\":\"Test3\",\"lastName\":\"Er\",\"email\":\"test3@er.com\",\"dob\":\"2000-01-01\"}}";
		
		try {
			Mockito.when(authDatabaseHelper.registerUser(Mockito.any(BasicAuthCredential.class), Mockito.anyString())).thenThrow(new AuthException("", AuthException.INVALID_USER_PASSWORD));
		} catch (AuthException e) {
			// Do nothing.
		}
		AuthProviderResponse response = authProvider.register(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.BAD_USER_PASS, response.authCode);
	}
	
}

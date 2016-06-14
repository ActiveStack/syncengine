package com.percero.agents.auth.services;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.percero.agents.auth.vo.AuthProviderResponse;
import com.percero.agents.auth.vo.BasicAuthCredential;
import com.percero.agents.auth.vo.ServiceUser;

/**
 * Tests the BasicAuthProvider
 * 
 * @author Collin Brown
 *
 */
public class TestBasicAuthProvider {

	private static final String VALID_CREDENTIAL_JSON = "{\"username\":\"tester3\",\"password\":\"password\",\"metadata\":{\"personType\":\"Parent\",\"firstName\":\"Test3\",\"lastName\":\"Er\",\"email\":\"test3@er.com\",\"dob\":\"2000-01-01\"}}";
	private static final String INVALID_CREDENTIAL_JSON = "\"username\" \"tester3\",\"password\" \"password\",";
	private static final String VALID_CREDENTIAL_STRING = "tester3:password";
	private static final String INVALID_CREDENTIAL_STRING = "tester3 password";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	BasicAuthProvider authProvider;
	DatabaseHelper authDatabaseHelper;

	@Before
	public void setUp() throws Exception {
		
		authDatabaseHelper = Mockito.mock(DatabaseHelper.class);
		
		authProvider = new BasicAuthProvider(authDatabaseHelper);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAuthenticate() {
		
		// Test no ServiceUser
		String credentialStr = VALID_CREDENTIAL_JSON;
		Mockito.when(authDatabaseHelper.getServiceUser(Mockito.any(BasicAuthCredential.class))).thenReturn(null);
		AuthProviderResponse response = authProvider.authenticate(credentialStr);
		assertNotNull(response);
		assertNull(response.serviceUser);
		assertEquals("Unexpected auth code", BasicAuthCode.BAD_USER_PASS, response.authCode);
		
		// Test invalid credential JSON
		credentialStr = INVALID_CREDENTIAL_JSON;
		response = authProvider.authenticate(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.BAD_USER_PASS, response.authCode);
		assertNull(response.serviceUser);
		
		// Test invalid credential string
		credentialStr = INVALID_CREDENTIAL_STRING;
		response = authProvider.authenticate(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.BAD_USER_PASS, response.authCode);
		assertNull(response.serviceUser);
		
		// Test valid ServiceUser - JSON
		credentialStr = VALID_CREDENTIAL_JSON;
		Mockito.when(authDatabaseHelper.getServiceUser(Mockito.any(BasicAuthCredential.class))).thenReturn(new ServiceUser());
		response = authProvider.authenticate(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.SUCCESS, response.authCode);
		assertNotNull(response.serviceUser);
		assertEquals("Unexpected AuthProvider ID", BasicAuthProvider.ID, response.serviceUser.getAuthProviderID());
		
		// Test valid ServiceUser - String
		credentialStr = VALID_CREDENTIAL_STRING;
		Mockito.when(authDatabaseHelper.getServiceUser(Mockito.any(BasicAuthCredential.class))).thenReturn(new ServiceUser());
		response = authProvider.authenticate(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.SUCCESS, response.authCode);
		assertNotNull(response.serviceUser);
		assertEquals("Unexpected AuthProvider ID", BasicAuthProvider.ID, response.serviceUser.getAuthProviderID());
	}
	
	@Test
	public void testRegiser() {
		String credentialStr = VALID_CREDENTIAL_JSON;

		// Test no ServiceUser
		try {
			Mockito.when(authDatabaseHelper.registerUser(Mockito.any(BasicAuthCredential.class), Mockito.anyString())).thenReturn(null);
		} catch (AuthException e) {
			// Do nothing.
		}
		AuthProviderResponse response = authProvider.register(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.FAILURE, response.authCode);
		
		// Test invalid credential JSON
		credentialStr = INVALID_CREDENTIAL_JSON;
		response = authProvider.register(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.BAD_USER_PASS, response.authCode);
		assertNull(response.serviceUser);
		
		// Test invalid credential string
		credentialStr = INVALID_CREDENTIAL_STRING;
		response = authProvider.register(credentialStr);
		assertNotNull(response);
		assertEquals("Unexpected auth code", BasicAuthCode.BAD_USER_PASS, response.authCode);
		assertNull(response.serviceUser);
		
		// Test valid ServiceUser
		credentialStr = VALID_CREDENTIAL_JSON;
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

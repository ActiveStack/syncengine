package com.percero.agents.auth.services;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.percero.agents.auth.vo.AuthProviderResponse;
import com.percero.agents.auth.vo.AuthenticationRequest;
import com.percero.agents.auth.vo.AuthenticationResponse;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.agents.auth.vo.UserAccount;
import com.percero.agents.auth.vo.UserToken;
import com.percero.framework.bl.IManifest;

public class TestAuthService2 {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	AuthService2 authService;

	@Before
	public void setUp() throws Exception {
		authService = Mockito.mock(AuthService2.class);
		authService.authProviderRegistry = Mockito.mock(AuthProviderRegistry.class);
		authService.manifest = Mockito.mock(IManifest.class);
		authService.sessionFactoryAuth = Mockito.mock(SessionFactory.class);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRegister_prePostHandlers() {
		AuthenticationRequest request = new AuthenticationRequest();
		
		Mockito.when(authService.register(Mockito.any(AuthenticationRequest.class))).thenCallRealMethod();
		
		// Test invalid AuthProvider
		Mockito.when(authService.authProviderRegistry.hasProvider(Mockito.anyString())).thenReturn(false);
		try {
			authService.register(request);
			fail("Did not catch invalid Auth Provider");
		} catch(IllegalArgumentException e) {
			// Do nothing.
		}
		
		// Test Pre/Post Register Handlers
		IAuthProvider authProvider = Mockito.mock(IAuthProvider.class);
		AuthProviderResponse apResponse = new AuthProviderResponse();
		apResponse.authCode = BasicAuthCode.SUCCESS;
		apResponse.serviceUser = new ServiceUser();
		Mockito.when(authProvider.register(Mockito.anyString())).thenReturn(apResponse);
		Mockito.when(authService.authProviderRegistry.getProvider(Mockito.anyString())).thenReturn(authProvider);

		TestPreRegisterHandler preRegisterHandler = new TestPreRegisterHandler();
		authService.preRegisterHandlers = new ArrayList<>();
		authService.preRegisterHandlers.add(preRegisterHandler);
		TestPostRegisterHandler postRegisterHandler = new TestPostRegisterHandler();
		authService.postRegisterHandlers = new ArrayList<>();
		authService.postRegisterHandlers.add(postRegisterHandler);
		
		Mockito.when(authService.authProviderRegistry.hasProvider(Mockito.anyString())).thenReturn(true);
		request.setCredential("CREDENTIAL");
		authService.register(request);
		assertEquals("PreRegisterHandler NOT called", 1, preRegisterHandler.myTimesCalled);
		assertEquals("PostRegisterHandler NOT called", 1, postRegisterHandler.myTimesCalled);
	}
	
	private class TestPreRegisterHandler implements IPreRegisterHandler {
		int myTimesCalled = 0;
		@Override
		public void run(AuthenticationRequest request) throws AuthException {
			myTimesCalled++;
		}
	}
	
	private class TestPostRegisterHandler implements IPostRegisterHandler {
		int myTimesCalled = 0;
		@Override
		public AuthenticationResponse run(AuthenticationRequest request, AuthenticationResponse registerResponse, ServiceUser serviceUser) throws AuthException {
			myTimesCalled++;
			return registerResponse;
		}
	}
	
	@Test
	public void testAuthenticate_prePostHandlers() {
		AuthenticationRequest request = new AuthenticationRequest();
		Mockito.when(authService.authenticate(Mockito.any(AuthenticationRequest.class))).thenCallRealMethod();
		
		// Test invalid AuthProvider
		Mockito.when(authService.authProviderRegistry.hasProvider(Mockito.anyString())).thenReturn(false);
		try {
			authService.authenticate(request);
			fail("Did not catch invalid Auth Provider");
		} catch(IllegalArgumentException e) {
			// Do nothing.
		}
		
		Mockito.when(authService.getOrCreateUserAccount(Mockito.any(ServiceUser.class), Mockito.any(IAuthProvider.class))).thenReturn(new UserAccount());
		Mockito.when(authService.loginUserAccount(Mockito.any(UserAccount.class), Mockito.anyString(), Mockito.anyString())).thenReturn(new UserToken());
		
		// Test Pre/Post Authenticate Handlers
		IAuthProvider authProvider = Mockito.mock(IAuthProvider.class);
		AuthProviderResponse apResponse = new AuthProviderResponse();
		apResponse.authCode = BasicAuthCode.SUCCESS;
		apResponse.serviceUser = new ServiceUser();
		Mockito.when(authProvider.authenticate(Mockito.anyString())).thenReturn(apResponse);
		Mockito.when(authService.authProviderRegistry.getProvider(Mockito.anyString())).thenReturn(authProvider);
		
		TestPreAuthenticateHandler preAuthenticateHandler = new TestPreAuthenticateHandler();
		authService.preAuthenticateHandlers = new ArrayList<>();
		authService.preAuthenticateHandlers.add(preAuthenticateHandler);
		TestPostAuthenticateHandler postAuthenticateHandler = new TestPostAuthenticateHandler();
		authService.postAuthenticateHandlers = new ArrayList<>();
		authService.postAuthenticateHandlers.add(postAuthenticateHandler);
		
		Mockito.when(authService.authProviderRegistry.hasProvider(Mockito.anyString())).thenReturn(true);
		request.setCredential("CREDENTIAL");
		authService.authenticate(request);
		assertEquals("PreAuthenticateHandler NOT called", 1, preAuthenticateHandler.myTimesCalled);
		assertEquals("PostAuthenticateHandler NOT called", 1, postAuthenticateHandler.myTimesCalled);
	}
	
	private class TestPreAuthenticateHandler implements IPreAuthenticateHandler {
		int myTimesCalled = 0;
		@Override
		public void run(AuthenticationRequest request) throws AuthException {
			myTimesCalled++;
		}
	}
	
	private class TestPostAuthenticateHandler implements IPostAuthenticateHandler {
		int myTimesCalled = 0;
		@Override
		public AuthenticationResponse run(AuthenticationRequest request, AuthenticationResponse authenticateResponse, ServiceUser serviceUser) throws AuthException {
			myTimesCalled++;
			return authenticateResponse;
		}
	}

}

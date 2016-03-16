package com.percero.agents.auth.services;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.percero.agents.auth.vo.AuthProvider;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.agents.auth.vo.UserAccount;

public class TestAuthService {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	AuthService authService;
	ServiceUser facebookServiceUser;
	ServiceUser googleServiceUser;
	ServiceUser githubServiceUser;
	ServiceUser linkedInServiceUser;

	@Before
	public void setUp() throws Exception {
		authService = Mockito.mock(AuthService.class);
		authService.anonAuthEnabled = false;

		// Setup Facebook Helper
		authService.facebookHelper = Mockito.mock(FacebookHelper.class);
		facebookServiceUser = new ServiceUser();
		facebookServiceUser.setAuthProviderID(AuthProvider.FACEBOOK.name());
		Mockito.when(authService.facebookHelper.getServiceUser(Mockito.anyString(), Mockito.anyString())).thenReturn(facebookServiceUser);
		
		// Setup Google Helper
		authService.googleHelper = Mockito.mock(GoogleHelper.class);
		googleServiceUser = new ServiceUser();
		googleServiceUser.setAuthProviderID(AuthProvider.GOOGLE.name());
		Mockito.when(authService.googleHelper.authenticateAccessToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(googleServiceUser);
		
		// Setup Github Helper
		authService.githubHelper = Mockito.mock(GitHubHelper.class);
		githubServiceUser = new ServiceUser();
		githubServiceUser.setAuthProviderID(AuthProvider.GITHUB.name());
		Mockito.when(authService.githubHelper.getServiceUser(Mockito.anyString(), Mockito.anyString())).thenReturn(githubServiceUser);
		
		// Setup LinkedIn Helper
		authService.linkedInHelper = Mockito.mock(LinkedInHelper.class);
		linkedInServiceUser = new ServiceUser();
		linkedInServiceUser.setAuthProviderID(AuthProvider.LINKEDIN.name());
		Mockito.when(authService.linkedInHelper.getServiceUser(Mockito.anyString(), Mockito.anyString())).thenReturn(linkedInServiceUser);

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetServiceProviderServiceUserUserAccountString() {
		Mockito.when(authService.getServiceProviderServiceUser(Mockito.any(UserAccount.class), Mockito.anyString())).thenCallRealMethod();
		Mockito.when(authService.getServiceProviderServiceUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();
		
		UserAccount userAccount = new UserAccount();
		userAccount.setAccessToken("ACCESS_TOKEN");
		userAccount.setAccountId("ACCOUNT_ID");
		userAccount.setRefreshToken("REFRESH_TOKEN");
		
		// authService.getServiceProviderServiceUser(userAccount,
		// AuthProvider.FACEBOOK.name()) is just a wrapper function, so as long
		// as it returns a ServiceUser it is valid.
		ServiceUser resultingServiceUser = authService.getServiceProviderServiceUser(userAccount, AuthProvider.FACEBOOK.name());
		assertNotNull(resultingServiceUser);
		assertSame(facebookServiceUser, resultingServiceUser);
		assertEquals(AuthProvider.FACEBOOK.name(), resultingServiceUser.getAuthProviderID());
	}
	
	@Test
	public void testGetServiceProviderServiceUserStringStringStringString() {
		Mockito.when(authService.getServiceProviderServiceUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();
		
		String accessToken = "ACCESS_TOKEN";
		String refreshToken = "REFRESH_TOKEN";
		String accountId = "ACCOUNT_ID";
		String authProviderID = "NONE";
		
		// Test finding Auth Provider by name
		
		// FACEBOOK
		authProviderID = "FacEBooK";	// Case should NOT matter.
		ServiceUser resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(facebookServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());

		authProviderID = AuthProvider.FACEBOOK.name();
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(facebookServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		authProviderID = AuthProvider.FACEBOOK.toString();
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(facebookServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		// GOOGLE
		authProviderID = "gOOgle";	// Case should NOT matter.
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(googleServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		authProviderID = AuthProvider.GOOGLE.name();
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(googleServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		authProviderID = AuthProvider.GOOGLE.toString();
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(googleServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		// GITHUB
		authProviderID = "gIThub";	// Case should NOT matter.
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(githubServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		authProviderID = AuthProvider.GITHUB.name();
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(githubServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		authProviderID = AuthProvider.GITHUB.toString();
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(githubServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		// LINKEDIN
		authProviderID = "lINKEDin";	// Case should NOT matter.
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(linkedInServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		authProviderID = AuthProvider.LINKEDIN.name();
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(linkedInServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		authProviderID = AuthProvider.LINKEDIN.toString();
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertSame(linkedInServiceUser, resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		// ANON
		authService.anonAuthEnabled = false;
		authService.anonAuthCode = null;
		authProviderID = "aNOn";	// Case should NOT matter.
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNull(resultingServiceUser);
		
		authService.anonAuthEnabled = true;
		authService.anonAuthCode = refreshToken;
		authProviderID = "aNOn";	// Case should NOT matter.
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		authProviderID = AuthProvider.ANON.name();
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		
		authProviderID = AuthProvider.ANON.toString();
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());

		authProviderID = AuthProvider.ANON.name();
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNotNull(resultingServiceUser);
		assertEquals(authProviderID, resultingServiceUser.getAuthProviderID());
		assertEquals(refreshToken, resultingServiceUser.getRefreshToken());
		
		// INVALID
		authService.anonAuthEnabled = true;
		authService.anonAuthCode = refreshToken;
		authProviderID = "InvALID";	// Case should NOT matter.
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNull(resultingServiceUser);

		authService.anonAuthEnabled = false;	// ANON should not impact INVALID auth provider ID.
		authService.anonAuthCode = null;
		authProviderID = "InvALID";	// Case should NOT matter.
		resultingServiceUser = authService.getServiceProviderServiceUser(accessToken, refreshToken, accountId, authProviderID);
		assertNull(resultingServiceUser);
	}

}

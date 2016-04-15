package com.percero.agents.auth.services;

import java.util.List;
import java.util.Set;

import com.percero.agents.auth.vo.OAuthResponse;
import com.percero.agents.auth.vo.OAuthToken;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.agents.auth.vo.UserAccount;


/**
 * The AuthService is responsible for authenticating Users.
 * 
 * @author Collin Brown
 *
 */
public interface IAuthService {

	/**
	 * Authenticates an OAuth Code.  Typical use case is client-side code produces an OAuth Code that the server then valdates.
	 * 
	 * @param authProviderID
	 * @param code
	 * @param clientId
	 * @param deviceId
	 * @param redirectUrl
	 * @param requestToken
	 * @return
	 */
	public OAuthResponse authenticateOAuthCode(String authProviderID, String code, String clientId, String deviceId, String redirectUrl, OAuthToken requestToken);

	/**
	 * In the case the OAuth Provider allows exchanging a user name/password combination for an Access Token, this method does just that.
	 * 
	 * @param authProviderID
	 * @param userName
	 * @param password
	 * @param scopes
	 * @param appUrl
	 * @param clientId
	 * @param deviceId
	 * @param requestToken
	 * @return
	 */
	public OAuthResponse authenticateBasicOAuth(String authProviderID, String userName, String password, String scopes, String appUrl, String clientId, String deviceId, OAuthToken requestToken);

	/**
	 * Authenticates an OAuth Access Token. Typically, OAuth providers issue an AccessToken (and RefreshToken) that can be used to authenticate
	 * a user for a pre-determined amount of time.
	 * 
	 * @param authProviderID
	 * @param accessToken
	 * @param refreshToken
	 * @param clientId
	 * @param deviceId
	 * @return
	 */
	public OAuthResponse authenticateOAuthAccessToken(String authProviderID, String accessToken, String refreshToken, String clientId, String deviceId);
	
	/**
	 * Checks to see if the aUserId, aToken, aClientId combination exists in the Auth Service data store.
	 * 
	 * @param regAppKey
	 * @param aUserId
	 * @param aToken
	 * @param aClientId
	 * @return
	 */
	public boolean validateUserByToken(String regAppKey, String aUserId, String aToken, String aClientId);
	
	/**
	 * Checks to see if the aUserId, aToken, anExistingClientId combination exists in the Auth Service data store.
	 * If the combo does exist, then updates the combo to use aClientId in place of anExistingClientId
	 * 
	 * @param regAppKey
	 * @param aUserId
	 * @param aToken
	 * @param aClientId
	 * @return
	 */
	public boolean validateUserByToken(String regAppKey, String aUserId, String aToken, String aClientId, Set<String> existingClientIds);

	/**
	 * Returns a List of ServiceUser objects for the corresponding user.  This is meant to include a ServiceUser object
	 * for every AuthProvider that the user has authenticated against.
	 * 
	 * @param aUserId
	 * @return
	 */
	public List<ServiceUser> getServiceUsers(String aUserId);
	
	/**
	 * Returns a Set of UserAccount objects for the specified user.  A UserAccount object should exist for every
	 * AuthProvider that the user has authenticated against.
	 * 
	 * @param aUserId
	 * @return
	 */
	public Set<UserAccount> getUserAccounts(String aUserId);

	/**
	 * Logs out a User as identified uniquely by UserID/ClientID. However, the bare minimum to identify 
	 * a user to logout is either a user id or a client id. Logging out a User (with no client ID) will 
	 * logout every client for that User, so use with caution.  Logging out a Client (with no User ID)
	 * should uniquely link back to the User, so this should behave in similar fashion to logging out with
	 * a User ID AND a Client ID.
	 * 
	 * @param aUserId
	 * @param aToken
	 * @param clientIds
	 * @return TRUE if user/client successfully logged out, FALSE if user/client unable to be logged out 
	 */
	public Boolean logoutUser(String aUserId, String aToken, Set<String> clientIds);
	public Boolean logoutUser(String aUserId, String aToken, String clientId);
}

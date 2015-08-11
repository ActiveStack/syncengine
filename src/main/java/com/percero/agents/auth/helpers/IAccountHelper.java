package com.percero.agents.auth.helpers;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import com.percero.agents.auth.services.IAuthService;
import com.percero.agents.auth.vo.ServiceUser;

public interface IAccountHelper {

	public Object validateUser(String regAppKey, String userId, IAuthService authService) throws Exception;

	/**
	 * Returns a List of user role names.  Only returns the Strings of the role names themselves, rather than
	 * complete UserRoleEI objects.
	 * 
	 * @param userId
	 * @return
	 * @throws Exception
	 */
	public List<String> getUserRoles(String userId) throws Exception;
	public void setupUserRoles(String userId, List<ServiceUser> serviceUserList) throws Exception;
	//public Principal doAuthentication(String regAppKey, String svcOAuthKey, String userName, String password, String clientId, String clientType);
	public Principal authenticateOAuth(String regAppKey, String svcOAuthKey, String userId, String userToken, String clientId, String clientType, String deviceId);
	public Principal authenticateOAuth(String regAppKey, String svcOAuthKey, String userId, String userToken, String clientId, String clientType, String deviceId, Set<String> existingClientIds);
}

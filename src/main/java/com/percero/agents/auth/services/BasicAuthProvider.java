package com.percero.agents.auth.services;

import org.apache.log4j.Logger;

import com.percero.agents.auth.vo.AuthCode;
import com.percero.agents.auth.vo.AuthProviderResponse;
import com.percero.agents.auth.vo.BasicAuthCredential;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.agents.sync.services.ISyncAgentService;

/**
 * Basic AuthProvider implementation for database username/password
 * authentication.
 */
public class BasicAuthProvider implements IAuthProvider {

    private static Logger logger = Logger.getLogger(BasicAuthProvider.class);
    public static final String ID = "activestack:basic";
    
	private static final long TIMEOUT_TIME = 2500;

    public String getID() {
        return ID;
    }

	private ISyncAgentService syncAgentService = null;
	private AuthService2 authService2 = null;
    private DatabaseHelper authDatabaseHelper = null;


    /**
     * @param jsonCredentialString - JSON String in `{"username":<USERNAME>, "password":<PASSWORD>}` format
     * @return
     */
    public AuthProviderResponse authenticate(String jsonCredentialString) {
        AuthProviderResponse response = new AuthProviderResponse();
    	ServiceUser serviceUser = null;
    	BasicAuthCredential cred = BasicAuthCredential.fromJsonString(jsonCredentialString);

        logger.debug("Autheticating user " + cred.getUsername());
        
        serviceUser = authDatabaseHelper.getServiceUser(cred);
        
        if (serviceUser == null) {
			logger.debug("AUTH FAILURE: " + response.authCode.getMessage());
            response.authCode = BasicAuthCode.BAD_USER_PASS;
        }
        else {
        	serviceUser.setAuthProviderID(getID());
//            UserAccount userAccount = authService2.getOrCreateUserAccount(serviceUser, this);
//            serviceUser.setAccessToken(userAccount.getAccessToken());
//            serviceUser.setRefreshToken(userAccount.getRefreshToken());
            response.serviceUser = serviceUser;
//            validateTeamLeader(teamLeader, serviceUser);
            response.authCode = AuthCode.SUCCESS;
        }

    	return response;
    }
    
    /**
     * @param jsonRegistrationString - JSON String in `{"username":<USERNAME>, "password":<PASSWORD>, "metadata":{<METADATA>}}` format
     * @return
     */
    public AuthProviderResponse register(String jsonRegistrationString) {
        AuthProviderResponse response = new AuthProviderResponse();
    	ServiceUser serviceUser = null;
    	BasicAuthCredential cred = BasicAuthCredential.fromJsonString(jsonRegistrationString);

        logger.debug("Autheticating user " + cred.getUsername());
        
        try {
			serviceUser = authDatabaseHelper.registerUser(cred, getID());
			
			if (serviceUser == null) {
				response.authCode = BasicAuthCode.FAILURE;
				logger.debug("AUTH FAILURE: " + response.authCode.getMessage());
			}
			else {
				serviceUser.setAuthProviderID(getID());
				response.serviceUser = serviceUser;
				response.authCode = AuthCode.SUCCESS;
			}
		} catch (AuthException e) {
			logger.error(e);
			// Based on the type of exception, return a specific result.
			if (AuthException.DUPLICATE_USER_NAME.equalsIgnoreCase(e.getDetail())) {
				response.authCode = BasicAuthCode.DUPLICATE_USER_NAME;
			}
			else if (AuthException.DATA_ERROR.equalsIgnoreCase(e.getDetail())) {
				response.authCode = BasicAuthCode.FAILURE;
			}
			else if (AuthException.INVALID_USER_IDENTIFIER.equalsIgnoreCase(e.getDetail())) {
				response.authCode = BasicAuthCode.FAILURE;
			}
			else if (AuthException.INVALID_USER_PASSWORD.equalsIgnoreCase(e.getDetail())) {
				response.authCode = BasicAuthCode.BAD_USER_PASS;
			}
		} catch(Exception e) {
			// Handle any other type of Exception here.
			logger.error(e);
			response.authCode = BasicAuthCode.FAILURE;
		}

    	return response;
    }

    /**
     * @param hostPortAndContext - e.g. https://some_host:5400/auth
     * @param objectMapper
     */
	public BasicAuthProvider(ISyncAgentService syncAgentService,
			AuthService2 authService2, DatabaseHelper authDatabaseHelper) {
        this.syncAgentService  = syncAgentService;
        this.authService2 = authService2;
        this.authDatabaseHelper = authDatabaseHelper;
        
    }
}

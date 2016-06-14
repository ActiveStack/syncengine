package com.percero.agents.auth.services;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.percero.agents.auth.vo.AuthCode;
import com.percero.agents.auth.vo.AuthProviderResponse;
import com.percero.agents.auth.vo.BasicAuthCredential;
import com.percero.agents.auth.vo.ServiceUser;

/**
 * Basic AuthProvider implementation for database username/password
 * authentication.
 * 
 * @author Collin Brown
 */
public class BasicAuthProvider implements IAuthProvider {

    private static Logger logger = Logger.getLogger(BasicAuthProvider.class);

    public static final String ID = "activestack:basic";	// The unique ID of this auth provider.
    
    public String getID() {
        return ID;
    }

    private DatabaseHelper authDatabaseHelper = null;


    /**
     * @param jsonCredentialString - JSON String in `{"username":<USERNAME>, "password":<PASSWORD>, "metadata":{<METADATA>}}` format
     * @return
     */
    public AuthProviderResponse authenticate(String jsonCredentialString) {
        AuthProviderResponse response = new AuthProviderResponse();
    	ServiceUser serviceUser = null;
    	BasicAuthCredential cred = null;
    	// Attempt to de-serialize the credential as JSON first, then as ":" delimited string.
    	try {
    		cred = BasicAuthCredential.fromJsonString(jsonCredentialString);
    	} catch(Exception e) {
    		// Do nothing.
    	}
    	if (cred == null) {
    		cred = BasicAuthCredential.fromString(jsonCredentialString);
    	}
    	
    	// If cred is empty, it means that we didn't find any valid/parseable credentials.
    	if (!StringUtils.hasText(cred.getUsername())) {
        	response.authCode = BasicAuthCode.BAD_USER_PASS;
			logger.debug("AUTH FAILURE unable to parse credentials: " + response.authCode.getMessage());
			return response;
    	}

        logger.debug("Autheticating user " + cred.getUsername());
        
        serviceUser = authDatabaseHelper.getServiceUser(cred);
        
        if (serviceUser == null) {
        	response.authCode = BasicAuthCode.BAD_USER_PASS;
			logger.debug("AUTH FAILURE for user " + cred.getUsername() + ": " + response.authCode.getMessage());
        }
        else {
        	serviceUser.setAuthProviderID(getID());
            response.serviceUser = serviceUser;
            response.authCode = AuthCode.SUCCESS;
			logger.debug("AUTH SUCCESS for user " + cred.getUsername());
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
    	BasicAuthCredential cred = null;
    	// Attempt to de-serialize the credential as JSON first, then as ":" delimited string.
    	try {
    		cred = BasicAuthCredential.fromJsonString(jsonRegistrationString);
    	} catch(Exception e) {
    		// Do nothing.
    	}
    	if (cred == null) {
    		cred = BasicAuthCredential.fromString(jsonRegistrationString);
    	}

    	// If cred is empty, it means that we didn't find any valid/parseable credentials.
    	if (!StringUtils.hasText(cred.getUsername())) {
        	response.authCode = BasicAuthCode.BAD_USER_PASS;
			logger.debug("REGISTER FAILURE unable to parse credentials: " + response.authCode.getMessage());
			return response;
    	}

    	logger.debug("Registering user " + cred.getUsername());
        
        try {
			serviceUser = authDatabaseHelper.registerUser(cred, getID());
			
			if (serviceUser == null) {
				response.authCode = BasicAuthCode.FAILURE;
				logger.debug("REGISTER FAILURE for user " + cred.getUsername() + ": " + response.authCode.getMessage());
			}
			else {
				serviceUser.setAuthProviderID(getID());
				response.serviceUser = serviceUser;
				response.authCode = AuthCode.SUCCESS;
				logger.debug("REGISTER SUCCESS for user " + cred.getUsername());
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

	public BasicAuthProvider(DatabaseHelper authDatabaseHelper) {
        this.authDatabaseHelper = authDatabaseHelper;
    }
}

package com.percero.agents.auth.principal;

import java.security.Principal;

public class PrincipalUser implements Principal {

	private String userId;
	private String name;
	private String token;
	private String clientType;
	private String clientId;
	private String[] groups;
	
	public PrincipalUser(String theUserId, String theName, String theToken, String theClientType, String theClientId, String[] theGroups) {
		userId = theUserId;
		name = theName;
		token = theToken;
		clientType = theClientType;
		clientId = theClientId;
		groups = theGroups;
	}


	public boolean hasRole(String role) {
		for (int i = 0; i < groups.length; i++) {
			if (groups[i].equals(role))
				return true;
		}
		return false;
	}
	
	public String[] getGroups() {
		return groups;
	}
	public void setGroups(String[] value) {
		groups = value;
	}

	
	public String getUserId() {
		return userId;
	}
	
	public String getName() {
		return name;
	}
	
	public String getToken() {
		return token;
	}
	
	public String getClientType() {
		return clientType;
	}
	
	public String getClientId() {
		return clientId;
	}

}

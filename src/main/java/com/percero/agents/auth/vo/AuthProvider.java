/**
 * 
 */
package com.percero.agents.auth.vo;

/**
 * @author Jonathan Samples<jonnysamps@gmail.com>
 *
 */
public enum AuthProvider {
	GOOGLE, FACEBOOK, LINKEDIN, ANON, DATABASE, GITHUB;
	
	public static AuthProvider getAuthProvider(String value) {
		if ("google".equalsIgnoreCase(value)) {
			return GOOGLE;
		}
		else if ("facebook".equalsIgnoreCase(value)) {
			return FACEBOOK;
		}
		else if ("linkedin".equalsIgnoreCase(value)) {
			return LINKEDIN;
		}
		else if ("anon".equalsIgnoreCase(value)) {
			return ANON;
		}
		else if ("database".equalsIgnoreCase(value)) {
			return DATABASE;
		}
		else if ("github".equalsIgnoreCase(value)) {
			return GITHUB;
		}
		else
			return null;
	}
}

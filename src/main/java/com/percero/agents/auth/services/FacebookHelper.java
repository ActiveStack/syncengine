package com.percero.agents.auth.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.GitHubTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.json.Json;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.JsonToken;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.percero.agents.auth.vo.ServiceIdentifier;
import com.percero.agents.auth.vo.ServiceUser;
import com.restfb.types.FacebookType;

@Component
public class FacebookHelper implements IAuthHelper {

	private static Logger log = Logger.getLogger(FacebookHelper.class);

	public static final String SERVICE_PROVIDER_NAME = "facebook";
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	
	@Autowired @Value("$pf{oauth.facebook.clientId}")
	private String clientId = "154825077913326";
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	@Autowired @Value("$pf{oauth.facebook.clientSecret}")
	private String clientSecret = "9362cc26df81205ae61bb324c2102c22";
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	
	// Facebook does not provide role names, so they are always invalid.
	private Boolean useRoleNames = false;
	
	@PostConstruct
	public void init(){
		log.debug("FacebookHelper init");
		log.debug("Facebook OAuth:clientId: "+clientId);
		log.debug("Facebook OAuth:clientSecret: "+clientSecret);
	}
		
	public ServiceUser authenticateOAuthCode(String code, String redirectUri) throws Exception {
		try {
			// Sample code from: https://cwiki.apache.org/confluence/display/OLTU/OAuth+2.0+Client+Quickstart
			OAuthClientRequest request = OAuthClientRequest
	                .tokenProvider(OAuthProviderType.FACEBOOK)
	                .setGrantType(GrantType.AUTHORIZATION_CODE)
	                .setClientId(clientId)
	                .setClientSecret(clientSecret)
	                .setRedirectURI(redirectUri)
	                .setCode(code)
	                .buildQueryMessage();
			
			// Create OAuth client that uses custom http client under the hood.
			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
			
			// Facebook is not fully compatible with OAuth 2.0 draft 10, access token response is
            // application/x-www-form-urlencoded, not json encoded so we use dedicated response class for that
            // Custom response classes are an easy way to deal with oauth providers that introduce modifications to
            // OAuth 2.0 specification
			GitHubTokenResponse oAuthResponse = oAuthClient.accessToken(request, GitHubTokenResponse.class);
			
			String accessToken = oAuthResponse.getAccessToken();
			String refreshToken = oAuthResponse.getRefreshToken();
			
			ServiceUser serviceUser = getServiceUser(accessToken, refreshToken);
			
			return serviceUser;
		} catch(Exception e) {
			log.error("Unable to get authenticate oauth code", e);
			return null;
		}
	}
	
	public ServiceUser getServiceUser(String accessToken, String refreshToken) {
		
		ServiceUser result = null;

		OAuthClientRequest bearerClientRequest;
		try {
			bearerClientRequest = new OAuthBearerClientRequest("https://graph.facebook.com/me")
					.setAccessToken(accessToken).buildQueryMessage();

			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
			OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);		
			
			final String ID = "id";	// "123456"
			final String NAME = "name";
			final String FIRST_NAME = "first_name";
			final String LAST_NAME = "last_name";
			final String LINK = "link";	// "https:\/\/www.facebook.com\/user.name"
			final String GENDER = "gender";	// "male"
			final String EMAIL = "email";	// "user\u0040mail.com"
			final String TIMEZONE = "timezone";	// -8
			final String LOCALE = "locale";	// "en_US"
			final String VERIFIED = "verified";	// true
			final String UPDATED_TIME = "updated_time";	// "2014-01-03T15:04:28+0000"
			final String USERNAME = "username";	// "user.name"
	
			if (resourceResponse != null) {
				result = new ServiceUser();
				result.setAreRoleNamesAccurate(useRoleNames);
				result.setAccessToken(accessToken);
				result.setRefreshToken(refreshToken);
				
				JsonParser parser = null;
				try {
					parser = JSON_FACTORY.createJsonParser(resourceResponse.getBody());
					JsonToken nextToken = null;
					while ((nextToken = parser.nextToken()) != null) {
						String fieldName = parser.getCurrentName();
						
						if (ID.equalsIgnoreCase(fieldName)) {
							if (nextToken == JsonToken.VALUE_STRING) {
								String text = parser.getText();
								result.setId(text);
							}
						}
						else if (NAME.equalsIgnoreCase(fieldName)) {
							if (nextToken == JsonToken.VALUE_STRING) {
								String text = parser.getText();
								result.setName(text);
							}
						}
						else if (FIRST_NAME.equalsIgnoreCase(fieldName)) {
							if (nextToken == JsonToken.VALUE_STRING) {
								String text = parser.getText();
								result.setFirstName(text);
							}
						}
						else if (LAST_NAME.equalsIgnoreCase(fieldName)) {
							if (nextToken == JsonToken.VALUE_STRING) {
								String text = parser.getText();
								result.setLastName(text);
							}
						}
						else if (LINK.equalsIgnoreCase(fieldName)) {
							if (nextToken == JsonToken.VALUE_STRING) {
								String text = parser.getText();
								result.setLink(text);
							}
						}
						else if (GENDER.equalsIgnoreCase(fieldName)) {
							// Do nothing.
						}
						else if (EMAIL.equalsIgnoreCase(fieldName)) {
							if (nextToken == JsonToken.VALUE_STRING) {
								String text = parser.getText();
								result.getEmails().add(text);
								result.getIdentifiers().add(new ServiceIdentifier(ServiceIdentifier.EMAIL, text));
							}
						}
						else if (LOCALE.equalsIgnoreCase(fieldName)) {
							if (nextToken == JsonToken.VALUE_STRING) {
								String text = parser.getText();
								result.setLocale(text);
							}
						}
						else if (USERNAME.equalsIgnoreCase(fieldName)) {
							if (nextToken == JsonToken.VALUE_STRING) {
								String text = parser.getText();
								result.setLogin(text);
							}
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error("Unable to parse JSON response from Facebook getServiceUser", e);
					return null;
				} finally {
					if (parser != null) {
						try {
							parser.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
	//			if (!StringUtils.isBlank(user.getEmail()) ) {
	//				List<String> emails = new ArrayList<String>();
	//				emails.add(user.getEmail());
	//				result.setEmails(emails);
	//			}
	//			
	//			result.setFirstName(user.getFirstName());
	//			result.setLastName(user.getLastName());
	//			result.setId(user.getId());
	//			result.setName(user.getName());
	//			result.setLink(user.getLink());
	//			result.setLocale(user.getLocale());
			} else {
				log.error("Unable to get user from Facebook.");
			}
		} catch (OAuthSystemException e) {
			e.printStackTrace();
			log.error("Unable to get Facebook User", e);
			return null;
		} catch (OAuthProblemException e) {
			e.printStackTrace();
			log.error("Unable to get Facebook User", e);
			return null;
		}
		
		return result;
	}
}

package com.percero.agents.auth.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.LinkedInApi;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.percero.agents.auth.vo.OAuthToken;
import com.percero.agents.auth.vo.ServiceIdentifier;
import com.percero.agents.auth.vo.ServiceUser;

@Component
public class LinkedInHelper {

	private static Logger log = Logger.getLogger(LinkedInHelper.class);

	public static final String SERVICE_PROVIDER_NAME = "linkedin";
	
	private static final String ACCESS_TOKEN_URL = "https://www.linkedin.com/uas/oauth2/accessToken";


	@Autowired @Value("$pf{oauth.linkedin.clientId}")
	private String clientId = "";
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	@Autowired @Value("$pf{oauth.linkedin.clientSecret}")
	private String clientSecret = "";
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	
	// LinkedIn doesn't provide role names, so they are marked as invalid.
	private Boolean useRoleNames = false;
	
	@PostConstruct
	public void init(){
		log.debug("LinkedInHelper init");
		log.debug("LinkedIn OAuth:clientId: "+clientId);
		log.debug("LinkedIn OAuth:clientSecret: "+clientSecret);
	}

	public OAuthToken getRequestToken(String clientId, String clientSecret) {
		OAuthService service = new ServiceBuilder().provider(LinkedInApi.class).apiKey(clientId).apiSecret(clientSecret).build();
		Token requestToken = service.getRequestToken();
		OAuthToken result = new OAuthToken();
		result.setToken(requestToken.getToken());
		result.setTokenSecret(requestToken.getSecret());
		return result;
	}

	public OAuthToken getAccessToken(String clientId, String clientSecret, String authorizationCode, OAuthToken requestToken){
		OAuthToken token = null;

		try {
			OAuthService service = new ServiceBuilder().provider(LinkedInApi.class).apiKey(clientId).apiSecret(clientSecret).build();
			Verifier verifier = new Verifier(authorizationCode);
			Token reqToken = new Token(requestToken.getToken(), requestToken.getTokenSecret());
			Token accessToken = service.getAccessToken(reqToken, verifier);
			token = new OAuthToken();
			token.setToken(accessToken.getToken());
			token.setTokenSecret(accessToken.getSecret());
		} catch (Exception e) {
			log.error("Error getting AccessToken", e);
		}

		return token;
	}
	

	public ServiceUser authenticateOAuthCode(String code, String redirectUri) throws Exception {
		try {
			StringBuilder urlParamsBuilder = new StringBuilder(ACCESS_TOKEN_URL);
			urlParamsBuilder.append("?grant_type=authorization_code&code=");
			urlParamsBuilder.append(URLEncoder.encode(code));
			urlParamsBuilder.append("&client_id=");
			urlParamsBuilder.append(clientId);
			urlParamsBuilder.append("&client_secret=");
			urlParamsBuilder.append(clientSecret);
			urlParamsBuilder.append("&redirect_uri=");
			urlParamsBuilder.append(URLEncoder.encode(redirectUri));
			String urlParameters = urlParamsBuilder.toString();
			
			URL obj = new URL(urlParameters);
			HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestMethod("POST");

			int responseCode = con.getResponseCode();
			log.debug("\nSending LinkedInd 'POST' request to URL : " + urlParameters);
			log.debug("Response Code : " + responseCode);
	 
			// Read the stream and convert to JSON.
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));			
			JsonParser parser = new JsonParser();
			JsonElement jsonRefreshTokenInfo = parser.parse(in);
			JsonObject jsonGroupObject = jsonRefreshTokenInfo.getAsJsonObject();
			JsonElement jsonAccessTokenElement = jsonGroupObject.get("access_token");
			// NOT USED - JsonElement jsonExpiresInElement = jsonGroupObject.get("expires_in");
			
			String accessToken = jsonAccessTokenElement.getAsString();
			// NOT USED - Long expiresIn = jsonExpiresInElement.getAsLong();
			String refreshToken = "";	// Not provided by LinkedIn

		    ServiceUser serviceUser = getServiceUser(accessToken, refreshToken);
	 
			return serviceUser;
		} catch(Exception e) {
			log.error("Unable to get authenticate oauth code", e);
			return null;
		}
	}
	
	public ServiceUser getServiceUser(String accessToken, String refreshToken){
		ServiceUser result = null;
		
		try {
			String url = "https://api.linkedin.com/v1/people/~:(first-name,last-name,formatted-name,id,main-address,email-address,im-accounts,twitter-accounts,site-standard-profile-request,location,picture-url)";
			url += "?format=json&oauth2_access_token=" + accessToken;
			
			URL obj = new URL(url);
			HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			
			JsonParser parser = new JsonParser();
			JsonElement jsonUserInfo = parser.parse(in);
			if (jsonUserInfo != null && (jsonUserInfo instanceof JsonObject)) {
				result = new ServiceUser();
				result.setAreRoleNamesAccurate(useRoleNames);

				// This is set here in the case we had to refresh this access token.
				result.setAccessToken(accessToken);

				result.setId(getJsonObjectStringValue(
						jsonUserInfo.getAsJsonObject(), "id"));

				result.setFirstName(getJsonObjectStringValue(
						jsonUserInfo.getAsJsonObject(), "firstName"));
				result.setLastName(getJsonObjectStringValue(
						jsonUserInfo.getAsJsonObject(), "lastName"));
				result.setName(getJsonObjectStringValue(
						jsonUserInfo.getAsJsonObject(), "formattedName"));
				
				result.setAvatarUrl(getJsonObjectStringValue(
						jsonUserInfo.getAsJsonObject(), "pictureUrl"));
				
				List<ServiceIdentifier> identifiers = new ArrayList<ServiceIdentifier>();
				identifiers.add(new ServiceIdentifier(ServiceIdentifier.LINKED_ID, result.getId()));
				result.setIdentifiers(identifiers);
				
				List<String> emails = new ArrayList<String>();
				String email = getJsonObjectStringValue(
						jsonUserInfo.getAsJsonObject(), "emailAddress");
				emails.add(email);
				identifiers.add(new ServiceIdentifier(ServiceIdentifier.EMAIL, email));
				result.setEmails(emails);
				result.setLink(getJsonObjectStringValue(
						jsonUserInfo.getAsJsonObject().getAsJsonObject("siteStandardProfileRequest"), "url"));
				result.setLocale(getJsonObjectStringValue(
						jsonUserInfo.getAsJsonObject().getAsJsonObject("location"), "name"));
			} else {
				log.debug("Unable to get Google Profile Information. User information was not populated");
			}
		} catch (Exception e) {
			log.error("Error getting ServiceUser", e);
		}
		
		return result;
	}
	
	public String getJsonObjectStringValue(JsonObject jsonObject,
			String fieldName) {
		if (jsonObject.has(fieldName))
			return jsonObject.get(fieldName).getAsString();
		else
			return "";
	}
}

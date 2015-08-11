package com.percero.agents.auth.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.Application;
import org.eclipse.egit.github.core.Authorization;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GsonUtils;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.GitHubService;
import org.eclipse.egit.github.core.service.OAuthService;
import org.eclipse.egit.github.core.service.OrganizationService;
import org.eclipse.egit.github.core.service.UserService;
import org.eclipse.egit.github.core.util.EncodingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.api.client.http.HttpResponseException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.percero.agents.auth.vo.ServiceIdentifier;
import com.percero.agents.auth.vo.ServiceRole;
import com.percero.agents.auth.vo.ServiceUser;

@Component
public class GitHubHelper implements IAuthHelper {
	
	@Autowired @Value("$pf{oauth.github.clientId}")
	private String clientId;
	
	@Autowired @Value("$pf{oauth.github.clientSecret}")
	private String clientSecret;
	
	@Autowired @Value("$pf{oauth.github.webCallbackUrl}")
	private String webRedirectUrl;
	
	private Boolean useRoleNames = false;
	@PostConstruct
	public void init(){
		log.debug("GitHubHelper init");
	}

	private static Logger log = Logger.getLogger(GitHubHelper.class);

	public static final String SERVICE_PROVIDER_NAME = "google";
    public static final String GROUP_ID = "groupId";
    public static final String GROUP_NAME = "groupName";
    public static final String MEMBER_TYPE = "memberType";
    public static final String MEMBER_ID = "memberId";
    public static final String USER = "User";
    public static final String GROUP = "Group";

	// This value should be used when doing OAuth with an installed client (non-browser)
	public static final String INSTALLED_CALLBACK_URL = "urn:ietf:wg:oauth:2.0:oob";
	

	public String getInstalledAccessTokenResponse(String authorizationCode) {
		return getAccessTokenResponse(authorizationCode, INSTALLED_CALLBACK_URL);
	}
	
	public String getWebAccessTokenResponse(String authorizationCode){
		return getAccessTokenResponse(authorizationCode, webRedirectUrl);
	}
	
	/**
	 * Uses the OAuth code passed up from the client to request an access and refresh token
	 * from Google oAuth provider.
	 * 
	 * @param authorizationCode - String passed up from Client
	 * @param redirectUrl - String registered with Google
	 * @return
	 */
	public String getAccessTokenResponse(String authorizationCode, String redirectUrl){
		String accessToken = null;

		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost("https://github.com/login/oauth/access_token");
			
			List<NameValuePair> list = new ArrayList<NameValuePair>(3);

			 list.add(new BasicNameValuePair("client_id",clientId));
			 list.add(new BasicNameValuePair("client_secret",clientSecret));
			 list.add(new BasicNameValuePair("code",authorizationCode));
			 if (StringUtils.hasText(redirectUrl)) {
				 list.add(new BasicNameValuePair("redirect_uri", redirectUrl));
			 }

			 httpPost.setEntity(new UrlEncodedFormEntity(list));

			 org.apache.http.HttpResponse r = httpClient.execute(httpPost);
			 StringWriter writer = new StringWriter();
			 InputStream is = r.getEntity().getContent();
			 String encoding = null;
			 if (r.getEntity().getContentEncoding() != null) {
				 encoding = r.getEntity().getContentEncoding().getValue();
				 IOUtils.copy(is, writer, encoding);
			 }
			 else {
				 IOUtils.copy(is, writer);
			 }
			 String theString = writer.toString();
			 
			 // Look for access_token param.
			 String[] paramsAndValues = theString.split("&");
			 for(int i=0; i<paramsAndValues.length; i++) {
				 String[] param = paramsAndValues[0].split("=");
				 if (param.length == 2) {
					 if (param[0].equalsIgnoreCase("access_token")) {
						 // Found the AccessToken
						 accessToken = param[1];
						 break;
					 }
				 }
			 }

		} catch (HttpResponseException e) {
			log.error("Error getting AccessTokenResponse", e);
			try {
				log.error(e.getStatusMessage());
			} catch (Exception e1) {
				log.error("Error parsing HttpResponseException", e1);
			}
		} catch (IOException e) {
			log.error("Error getting AccessTokenResponse", e);
		}

		return accessToken;
	}
	
	protected static final String HEADER_ACCEPT = "Accept"; //$NON-NLS-1$
	protected static final String HEADER_AUTHORIZATION = "Authorization"; //$NON-NLS-1$
	protected static final String HEADER_USER_AGENT = "User-Agent"; //$NON-NLS-1$
	protected static final String USER_AGENT = "GitHubJava/2.1.3"; //$NON-NLS-1$
	public String getBasicAccessTokenResponse(String userName, String password, String scopes, String appUrl){
		String accessToken = null;
		
		try {
			List<String> lstScopes = new ArrayList<String>();
			for(String nextScope : scopes.split(",")) {
				lstScopes.add(nextScope);
			}

			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost("https://github.com/authorizations");
			
			List<NameValuePair> list = new ArrayList<NameValuePair>(2);
			String credentials = "Basic " //$NON-NLS-1$
					+ EncodingUtils.toBase64(userName + ':' + password);
			//httpPost.setHeader(HEADER_AUTHORIZATION, credentials);
			//httpPost.setHeader(HEADER_USER_AGENT, USER_AGENT);
			//httpPost.setHeader(HEADER_ACCEPT, "application/vnd.github.beta+json");
			//list.add(new BasicNameValuePair(HEADER_AUTHORIZATION, credentials));
			//list.add(new BasicNameValuePair(HEADER_USER_AGENT, USER_AGENT));
			//list.add(new BasicNameValuePair(HEADER_ACCEPT, "application/vnd.github.beta+json"));
			list.add(new BasicNameValuePair("client_id",clientId));
			list.add(new BasicNameValuePair("client_secret",clientSecret));
			//list.add(new BasicNameValuePair("scopes",scopes));
			//list.add(new BasicNameValuePair("code",authorizationCode));
			//list.add(new BasicNameValuePair("redirect_uri", redirectUrl));
			
			httpPost.setEntity(new UrlEncodedFormEntity(list));
			
			org.apache.http.HttpResponse r = httpClient.execute(httpPost);
			StringWriter writer = new StringWriter();
			InputStream is = r.getEntity().getContent();
			String encoding = null;
			if (r.getEntity().getContentEncoding() != null) {
				encoding = r.getEntity().getContentEncoding().getValue();
				IOUtils.copy(is, writer, encoding);
			}
			else {
				IOUtils.copy(is, writer);
			}
			String theString = writer.toString();

			/*
			if (credentials != null)
				request.setRequestProperty(HEADER_AUTHORIZATION, credentials);
			request.setRequestProperty(HEADER_USER_AGENT, userAgent);
			request.setRequestProperty(HEADER_ACCEPT,
					"application/vnd.github.beta+json"); //$NON-NLS-1$
			return request;*/
			
			GitHubClient client = new GitHubClient();
			client.setCredentials(userName, password);
			org.eclipse.egit.github.core.service.OAuthService oauthService = new OAuthService(client);
			List<Authorization> lstAuths = oauthService.getAuthorizations();
			
			Authorization someAuth = new Authorization();
			someAuth.setScopes(lstScopes);
			
			Gson gson = GsonUtils.getGson();
			String jsonAuth = gson.toJson(someAuth);

			someAuth = oauthService.createAuthorization(someAuth);
			
			Iterator<Authorization> itrAuths = lstAuths.iterator();
			Authorization thisAuth = null;
			while (itrAuths.hasNext()) {
				Authorization nextAuth = itrAuths.next();
				nextAuth = oauthService.getAuthorization(nextAuth.getId());
				Application nextApp = nextAuth.getApp();
				if (nextApp.getUrl().equalsIgnoreCase(appUrl)) {
					thisAuth = nextAuth;
					break;
				}
			}
			
			if (thisAuth == null) {
				thisAuth = new Authorization();
				Application app = new Application();
				app.setUrl(appUrl);
				thisAuth.setApp(app);
				thisAuth = oauthService.createAuthorization(thisAuth);
				thisAuth = oauthService.addScopes(thisAuth.getId(), lstScopes);
			}
			else {
				// Make sure all required scopes are in place.
				List<String> existingScopes = thisAuth.getScopes();
				Iterator<String> itrExistingScopes = existingScopes.iterator();
				Iterator<String> itrScopes = null;
				List<String> scopesToRemove = new ArrayList<String>();
				List<String> scopesToAdd = new ArrayList<String>();
				while (itrExistingScopes.hasNext()) {
					String nextExistingScope = itrExistingScopes.next();
					Boolean scopeIsRequired = false;
					itrScopes = lstScopes.iterator();
					while (itrScopes.hasNext()) {
						if (itrScopes.next().equals(nextExistingScope)) {
							scopeIsRequired = true;
							break;
						}
					}
					
					if (!scopeIsRequired) {
						scopesToRemove.add(nextExistingScope);
					}
				}
				
				itrScopes = lstScopes.iterator();
				while (itrScopes.hasNext()) {
					String nextScope = itrScopes.next();
					Boolean scopeExists = false;
					itrExistingScopes = existingScopes.iterator();
					while (itrExistingScopes.hasNext()) {
						String nextExistingScope = itrExistingScopes.next();
						if (nextExistingScope.equals(nextScope)) {
							scopeExists = true;
							break;
						}
					}
					
					if (!scopeExists) {
						scopesToAdd.add(nextScope);
					}
				}
				
				if (scopesToRemove.size() > 0) {
					oauthService.removeScopes(thisAuth.getId(), scopesToRemove);
				}
				if (scopesToAdd.size() > 0) {
					oauthService.addScopes(thisAuth.getId(), scopesToAdd);
				}
				accessToken = thisAuth.getToken();
			}
			
			return accessToken;
			/**
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost("https://github.com/authorizations");
			
			List<NameValuePair> list = new ArrayList<NameValuePair>(3);
			
			list.add(new BasicNameValuePair("client_id",clientId));
			list.add(new BasicNameValuePair("client_secret",clientSecret));
			list.add(new BasicNameValuePair("scopes",scopes));
			//list.add(new BasicNameValuePair("code",authorizationCode));
			//list.add(new BasicNameValuePair("redirect_uri", redirectUrl));
			
			httpPost.setEntity(new UrlEncodedFormEntity(list));
			
			org.apache.http.HttpResponse r = httpClient.execute(httpPost);
			StringWriter writer = new StringWriter();
			InputStream is = r.getEntity().getContent();
			String encoding = null;
			if (r.getEntity().getContentEncoding() != null) {
				encoding = r.getEntity().getContentEncoding().getValue();
				IOUtils.copy(is, writer, encoding);
			}
			else {
				IOUtils.copy(is, writer);
			}
			String theString = writer.toString();
			
			// Look for access_token param.
			String[] paramsAndValues = theString.split("&");
			for(int i=0; i<paramsAndValues.length; i++) {
				String[] param = paramsAndValues[0].split("=");
				if (param.length == 2) {
					if (param[0].equalsIgnoreCase("access_token")) {
						// Found the AccessToken
						accessToken = param[1];
						break;
					}
				}
			}*/
			
		} catch (HttpResponseException e) {
			log.error("Error getting AccessTokenResponse", e);
			try {
				log.error(e.getStatusMessage());
			} catch (Exception e1) {
				log.error("Error parsing HttpResponseException", e1);
			}
		} catch (IOException e) {
			log.error("Error getting AccessTokenResponse", e);
		}
		
		return accessToken;
	}
	
	public List<String> getUserRoleNames(String login)
			throws Exception{
		List<String> result = new ArrayList<String>();
		
		Collection<ServiceRole> userSvcRoles = getUserRoles(login);
		Iterator<ServiceRole> itrUserSvcRoles = userSvcRoles.iterator();
		while(itrUserSvcRoles.hasNext()) {
			ServiceRole nextUserSvcRole = itrUserSvcRoles.next();
			if (!result.contains(nextUserSvcRole.getName()))
				result.add(nextUserSvcRole.getName());
		}
		
		return result;
	}
	
	public Collection<ServiceRole> getUserRoles(String login)
			throws Exception{
		return getUserRoles(login, false);
	}
	public Collection<ServiceRole> getUserRoles(String login, Boolean useRecursion)
			throws Exception{
		Collection<ServiceRole> userServiceRoles = new HashSet<ServiceRole>();
		
		Collection<String> retrievedMemberNames = new HashSet<String>();
		Collection<String> memberNames = new HashSet<String>();
		memberNames.add(login);

		/**
		// The 2-LO authorization section
		GoogleOAuthDomainWideDelegation googleOAuthDomainWideDelegation = new GoogleOAuthDomainWideDelegation();
		
		while(!memberNames.isEmpty()) {
			String nextMember = (String) memberNames.toArray()[0];
			memberNames.remove(nextMember);
			retrievedMemberNames.add(nextMember);
			GenericUrl requestUrl = new GenericUrl(RETRIEVE_GROUPS_URL.replace("{domain}", domain) + "&member=" + nextMember);
			googleOAuthDomainWideDelegation.requestorId = admin;
			googleOAuthDomainWideDelegation.parameters = getOauthHmacParameters();
			
			try {
				NetHttpTransport transport = new NetHttpTransport();
				HttpRequestFactory httpRequestFactory = transport.createRequestFactory(googleOAuthDomainWideDelegation);
				HttpRequest request = httpRequestFactory.buildGetRequest(requestUrl);
				HttpResponse response = request.execute();
				JsonParser parser = new JsonParser();
				JsonElement jsonUserGroupInfo = parser.parse(new InputStreamReader(
						response.getContent()));
				
				JsonObject jsonGroupObject = jsonUserGroupInfo.getAsJsonObject();
				JsonElement jsonFeedElement = jsonGroupObject.get("feed");
				if (jsonFeedElement.getAsJsonObject().has("entry")) {
					JsonArray jsonEntryArray = jsonFeedElement.getAsJsonObject().get("entry").getAsJsonArray();
					Iterator<JsonElement> itr = jsonEntryArray.iterator();
					
					while(itr.hasNext()) {
						try {
							JsonElement nextJsonGroupEntry = itr.next();
							JsonObject nextJsonGroupObject = nextJsonGroupEntry.getAsJsonObject();
							JsonArray jsonAppsPropertyArray = nextJsonGroupObject.get("apps$property").getAsJsonArray();
							Iterator<JsonElement> appsPropsItr = jsonAppsPropertyArray.iterator();
							
							ServiceRole nextRole = new ServiceRole();
							while(appsPropsItr.hasNext()) {
								try {
									JsonObject nextAppsPropertyObject = appsPropsItr.next().getAsJsonObject();
									String nextAppsPropName = nextAppsPropertyObject.get("name").getAsString();
									if (GROUP_NAME.equalsIgnoreCase(nextAppsPropName)) {
										// Found the Group Name.
										String groupName = nextAppsPropertyObject.get("value").getAsString();
										nextRole.setName(groupName);
										
										if (useRecursion && !retrievedMemberNames.contains(groupName)) {
											// If useRecursion, this will explicitly check each group's groups until all groups the user
											//	is a member of (whether directly or indirectly) have been queried.
											memberNames.add(groupName);
										}
										
										if (nextRole.getId() != null && nextRole.getName() != null) {
											userServiceRoles.add(nextRole);
											break;
										}
									}
									else if (GROUP_ID.equalsIgnoreCase(nextAppsPropName)) {
										// Found the Group Name.
										String groupId = nextAppsPropertyObject.get("value").getAsString();
										nextRole.setId(groupId);
										
										if (nextRole.getId() != null && nextRole.getName() != null) {
											userServiceRoles.add(nextRole);
											break;
										}
									}
								} catch(Exception e) {
									e.printStackTrace();
								}
							}

						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			} catch(IOException ioe) {
				ioe.printStackTrace();
				log.warn("Unable to get user " + login + " Google role names", ioe);
				throw ioe;
			}
		}
		*/
		
		return userServiceRoles;
	}
	
	public Collection<ServiceRole> getRoles()
			throws IOException {
		Collection<ServiceRole> result = new HashSet<ServiceRole>();

		/**
		// The 2-LO authorization section
		GoogleOAuthDomainWideDelegation googleOAuthDomainWideDelegation = new GoogleOAuthDomainWideDelegation();
		GenericUrl requestUrl = new GenericUrl(RETRIEVE_GROUPS_URL.replace("{domain}", domain));
		googleOAuthDomainWideDelegation.requestorId = admin;
		googleOAuthDomainWideDelegation.parameters = getOauthHmacParameters();
		
		try {
			NetHttpTransport transport = new NetHttpTransport();
			HttpRequestFactory httpRequestFactory = transport.createRequestFactory(googleOAuthDomainWideDelegation);
			HttpRequest request = httpRequestFactory.buildGetRequest(requestUrl);
			HttpResponse response = request.execute();
			JsonParser parser = new JsonParser();
			JsonElement jsonUserGroupInfo = parser.parse(new InputStreamReader(
					response.getContent()));
			
			JsonObject jsonGroupObject = jsonUserGroupInfo.getAsJsonObject();
			JsonElement jsonFeedElement = jsonGroupObject.get("feed");
			if (jsonFeedElement.getAsJsonObject().has("entry")) {
				JsonArray jsonEntryArray = jsonFeedElement.getAsJsonObject().get("entry").getAsJsonArray();
				Iterator<JsonElement> itr = jsonEntryArray.iterator();
		
				while(itr.hasNext()) {
					try {
						JsonElement nextJsonGroupEntry = itr.next();
						JsonObject nextJsonGroupObject = nextJsonGroupEntry.getAsJsonObject();
						JsonArray jsonAppsPropertyArray = nextJsonGroupObject.get("apps$property").getAsJsonArray();
						Iterator<JsonElement> appsPropsItr = jsonAppsPropertyArray.iterator();
						ServiceRole nextRole = new ServiceRole();
						while(appsPropsItr.hasNext()) {
							try {
								JsonObject nextAppsPropertyObject = appsPropsItr.next().getAsJsonObject();
								String nextAppsPropName = nextAppsPropertyObject.get("name").getAsString();
								if (GROUP_NAME.equalsIgnoreCase(nextAppsPropName)) {
									// Found the Group Name.
									nextRole.setName(nextAppsPropertyObject.get("value").getAsString());
									
									if (nextRole.getId() != null && nextRole.getName() != null)
										result.add(nextRole);
								}
								else if (GROUP_ID.equalsIgnoreCase(nextAppsPropName)) {
									// Found the Group Name.
									nextRole.setId(nextAppsPropertyObject.get("value").getAsString());
									
									if (nextRole.getId() != null && nextRole.getName() != null)
										result.add(nextRole);
								}
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
			log.warn("Unable to get groups for domain " + domain, ioe);
			throw ioe;
		}
		*/
		
		return result;
	}
	
	public ServiceUser getServiceUser(String accessToken,
			String refreshToken) {
		return getServiceUser(accessToken, refreshToken, true);
	}
	
	private ServiceUser getServiceUser(String accessToken,
			String refreshToken, Boolean refreshAccessTokenOnFault) {
		ServiceUser result = null;

		try {
		
			GitHubClient client = new GitHubClient();
			client.setOAuth2Token(accessToken);
			UserService userService = new UserService(client);
			User user = userService.getUser();
			
			
			if (user != null) {
				
				result = new ServiceUser();
				result.setAreRoleNamesAccurate(useRoleNames);
				result.setId(user.getId() + "");
				result.getIdentifiers().add(new ServiceIdentifier(ServiceIdentifier.GITHUB, user.getId() + ""));
				result.setAccessToken(accessToken);
				result.setLink(user.getUrl());
				result.setLocale(user.getLocation());
				result.setLogin(user.getLogin());
				if (user.getAvatarUrl() != null)
					result.setAvatarUrl(user.getAvatarUrl());
				if (user.getGravatarId() != null)
					result.setGravatarId(user.getGravatarId());
				
				List<String> emails = userService.getEmails();
				if (user.getEmail() != null && !emails.contains(user.getEmail()))
					emails.add(user.getEmail());
				result.setEmails(emails);
				
				Iterator<String> itrEmails = emails.iterator();
				while (itrEmails.hasNext()) {
					result.getIdentifiers().add(new ServiceIdentifier(ServiceIdentifier.EMAIL, itrEmails.next()));
				}
				
				result.setName(user.getName());
			}
			
		} catch (RequestException re) {
			log.error("Error getting GitHub Profile Information:\n" + re.getMessage() + "\n" + re.formatErrors(), re);
		} catch (Exception e) {
			log.error("Error getting GitHub Profile Information", e);
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

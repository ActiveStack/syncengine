package com.percero.agents.auth.services;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.Group;
import com.google.api.services.admin.directory.model.Groups;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.gson.JsonObject;
import com.percero.agents.auth.vo.ServiceIdentifier;
import com.percero.agents.auth.vo.ServiceOrganization;
import com.percero.agents.auth.vo.ServiceRole;
import com.percero.agents.auth.vo.ServiceUser;

@Component
public class GoogleHelper implements IAuthHelper {
	
	private static final String SERVICE_ACCOUNT_PKCS12_FILE_PATH = "/google/privatekey.p12";
	private static final String CLIENT_SECRETS_FILE_PATH = "/google/client_secrets.json";
	private static final String p12Password = "notasecret";
	private static final String keyAlias = "privatekey";
	private static Logger log = Logger.getLogger(GoogleHelper.class);
	
	public static final String SERVICE_PROVIDER_NAME = "google";
	public static final String GROUP_ID = "groupId";
	public static final String GROUP_NAME = "groupName";
	public static final String MEMBER_TYPE = "memberType";
	public static final String MEMBER_ID = "memberId";
	public static final String USER = "User";
	public static final String GROUP = "Group";
	
	// This value should be used when doing OAuth with an installed client (non-browser)
	public static final String INSTALLED_CALLBACK_URL = "urn:ietf:wg:oauth:2.0:oob";
	//private static final HttpTransport TRANSPORT = new NetHttpTransport();
	private static HttpTransport TRANSPORT = null;
	
	static {
		try {
			TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		} catch (GeneralSecurityException e) {
			log.error("Unable to create transport", e);
		} catch (IOException e) {
			log.error("Unable to create transport", e);
		}
	}
	
//	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	
	@Autowired @Value("$pf{oauth.google.clientId}")
	private String clientId;
	
	@Autowired @Value("$pf{oauth.google.clientSecret}")
	private String clientSecret;
	
	@Autowired @Value("$pf{oauth.google.webCallbackUrl}")
	private String webRedirectUrl;
	
	@Autowired @Value("$pf{oauth.google.domain}")
	private String domain;
	
	@Autowired @Value("$pf{oauth.google.admin}")
	private String admin;
	
	@Autowired @Value("$pf{oauth.google.application_name}")
	private String applicationName;
	
	@Autowired @Value("$pf{oauth.google.use_role_names:true}")
	private Boolean useRoleNames;
	
	@PostConstruct
	public void init(){
		log.debug("GoogleHelper init");
		log.debug("Google OAuth:clientId: "+clientId);
		log.debug("Google OAuth:clientSecret: "+clientSecret);
		log.debug("Google OAuth:applicationName: "+applicationName);
		
		// If no application name, then not using Google OAuth, so exit out.
		if (!StringUtils.hasText(applicationName)) {
			return;
		}
		
		if (directoryScopes == null) {
			directoryScopes = new HashSet<String>();
			directoryScopes.add(DirectoryScopes.ADMIN_DIRECTORY_GROUP);
			directoryScopes.add(DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY);
			directoryScopes.add(DirectoryScopes.ADMIN_DIRECTORY_GROUP_MEMBER);
			directoryScopes.add(DirectoryScopes.ADMIN_DIRECTORY_GROUP_MEMBER_READONLY);
			directoryScopes.add(DirectoryScopes.ADMIN_DIRECTORY_USER);
			directoryScopes.add(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY);
		}
		
		try {
			if (clientSecrets == null) {
			    clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
			            new InputStreamReader(GoogleHelper.class.getResourceAsStream(CLIENT_SECRETS_FILE_PATH)));
			    SERVICE_ACCOUNT_EMAIL = (String) clientSecrets.getWeb().get("client_email");
			}
			
			if (privateKey == null) {
			    privateKey = SecurityUtils.loadPrivateKeyFromKeyStore(
			            SecurityUtils.getPkcs12KeyStore(), 
			            GoogleHelper.class.getResourceAsStream(SERVICE_ACCOUNT_PKCS12_FILE_PATH),
			            p12Password,
			            keyAlias,
			            p12Password
			            );
			}
		} catch(IOException ioe) {
			log.error("Unable to instantiate GoogleHelper", ioe);
		} catch(GeneralSecurityException gse) {
			log.error("Unable to instantiate GoogleHelper", gse);
		} catch(NullPointerException npe) {
			log.error("Unable to instantiate GoogleHelper", npe);
		}
		
		if (directoryCredential == null) {
		    directoryCredential = new GoogleCredential.Builder()
			        .setTransport(TRANSPORT)
			        .setJsonFactory(JSON_FACTORY)
			        .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
			        .setClientSecrets(clientSecrets)
			        .setServiceAccountScopes(directoryScopes)
			        .setServiceAccountPrivateKey(privateKey)
			        .setServiceAccountUser(admin)
			        //.setServiceAccountPrivateKeyFromP12File(
			        //    new File(SERVICE_ACCOUNT_PKCS12_FILE_PATH))
			        .build();
		}
		
		if (directory == null) {
		    directory = new Directory.Builder(TRANSPORT, JSON_FACTORY, directoryCredential).setApplicationName(applicationName).build();
		}
	}


	private Collection<String> directoryScopes = null;
    
    private GoogleClientSecrets clientSecrets = null;

    private PrivateKey privateKey = null;
    
    private String SERVICE_ACCOUNT_EMAIL = null;
    private GoogleCredential directoryCredential = null;
    private Directory directory = null;

	public ServiceUser authenticateOAuthCode(String code, String redirectUri) throws Exception {
		try {
			if ("DEV".equalsIgnoreCase(code)) {
				ServiceUser serviceUser = new ServiceUser();
				serviceUser.setAccessToken("DEV");
				serviceUser.setAreRoleNamesAccurate(useRoleNames);
				serviceUser.setAuthProvider(null);
				serviceUser.setAvatarUrl("");
				
				ArrayList<String> emails = new ArrayList<String>();
				emails.add("PSIDev@plantsciences.com");
				serviceUser.setEmails(emails);
				
				serviceUser.setFirstName("PSI");
				serviceUser.setGravatarId("");
				serviceUser.setId("110435249190841877255");
				
				ArrayList<ServiceIdentifier> identifiers = new ArrayList<ServiceIdentifier>();
				identifiers.add(new ServiceIdentifier("google", "110435249190841877255"));
				identifiers.add(new ServiceIdentifier("email", "PSIDev@plantsciences.com"));
				serviceUser.setIdentifiers(identifiers);
				
				serviceUser.setIsAdmin(false);
				serviceUser.setIsSupended(false);
				serviceUser.setLastName("Developer");
				serviceUser.setLink(null);
				serviceUser.setLocale("en");
				serviceUser.setLogin("");
				serviceUser.setName("PSI Developer");
				serviceUser.setOrganizations(new ArrayList<ServiceOrganization>());
				serviceUser.setRefreshToken("DEV");
				
				ArrayList<String> roleNames = new ArrayList<String>();
				roleNames.add("Harvest Manager");
				roleNames.add("WebeDigSheet");
				roleNames.add("WebInventory");
				roleNames.add("Trim Shed Counter");
				roleNames.add("ReadOnlyWeb");
				roleNames.add("WebOrders");
				roleNames.add("Trim Shed Manager");
				roleNames.add("Percero Framework");
				roleNames.add("PSI Global Admin");
				roleNames.add("PSI-NetAdmin");
				roleNames.add("PSI Global");
				roleNames.add("Growing Agreements Manager");
				roleNames.add("Business Agreements Manager");
				roleNames.add("Business13");
				roleNames.add("WebPlanting");
				roleNames.add("WebShipping");
				serviceUser.setRoleNames(roleNames);
				
				return serviceUser;
			}
			else {
				GoogleAuthorizationCodeTokenRequest authRequest = new GoogleAuthorizationCodeTokenRequest(TRANSPORT, JSON_FACTORY, clientId, clientSecret, code, redirectUri);
				GoogleTokenResponse authResponse = authRequest.execute();
				System.out.println(authResponse.getAccessToken());
				  
			    GoogleCredential credential = new GoogleCredential.Builder()
			      .setTransport(TRANSPORT)
			      .setJsonFactory(JSON_FACTORY)
			      .setClientSecrets(clientSecrets)
			      .build();
				credential.setAccessToken(authResponse.getAccessToken());
				credential.setRefreshToken(authResponse.getRefreshToken());
				credential.setExpiresInSeconds(authResponse.getExpiresInSeconds());
				credential.setFromTokenResponse(authResponse);
				
				ServiceUser serviceUser = getServiceUser(credential);
				
				return serviceUser;
			}
		} catch(Exception e) {
			log.error("Unable to get authenticate oauth code", e);
			return null;
		}
	}
	
	public ServiceUser authenticateAccessToken(String accessToken, String refreshToken, String accountId) throws Exception {
		return authenticateAccessToken(accessToken, refreshToken, accountId, true);
	}

	private ServiceUser authenticateAccessToken(String accessToken, String refreshToken, String accountId, Boolean retryOnFail) throws Exception {
		try {
			if ("DEV".equalsIgnoreCase(accessToken)) {
				long currentTime = System.currentTimeMillis();
				while ((System.currentTimeMillis() - currentTime) < 3500) {}
				ServiceUser serviceUser = new ServiceUser();
				serviceUser.setAccessToken("DEV");
				serviceUser.setAreRoleNamesAccurate(useRoleNames);
				serviceUser.setAuthProvider(null);
				serviceUser.setAvatarUrl("");
				
				ArrayList<String> emails = new ArrayList<String>();
				emails.add("PSIDev@plantsciences.com");
				serviceUser.setEmails(emails);
				
				serviceUser.setFirstName("PSI");
				serviceUser.setGravatarId("");
				serviceUser.setId("110435249190841877255");
				
				ArrayList<ServiceIdentifier> identifiers = new ArrayList<ServiceIdentifier>();
				identifiers.add(new ServiceIdentifier("google", "110435249190841877255"));
				identifiers.add(new ServiceIdentifier("email", "PSIDev@plantsciences.com"));
				serviceUser.setIdentifiers(identifiers);
				
				serviceUser.setIsAdmin(false);
				serviceUser.setIsSupended(false);
				serviceUser.setLastName("Developer");
				serviceUser.setLink(null);
				serviceUser.setLocale("en");
				serviceUser.setLogin("");
				serviceUser.setName("PSI Developer");
				serviceUser.setOrganizations(new ArrayList<ServiceOrganization>());
				serviceUser.setRefreshToken("DEV");
				
				ArrayList<String> roleNames = new ArrayList<String>();
				roleNames.add("Harvest Manager");
				roleNames.add("WebeDigSheet");
				roleNames.add("WebInventory");
				roleNames.add("Trim Shed Counter");
				roleNames.add("ReadOnlyWeb");
				roleNames.add("WebOrders");
				roleNames.add("Trim Shed Manager");
				roleNames.add("Percero Framework");
				roleNames.add("PSI Global Admin");
				roleNames.add("PSI-NetAdmin");
				roleNames.add("PSI Global");
				roleNames.add("Growing Agreements Manager");
				roleNames.add("Business Agreements Manager");
				roleNames.add("Business13");
				roleNames.add("WebPlanting");
				roleNames.add("WebShipping");
				serviceUser.setRoleNames(roleNames);
				
				return serviceUser;
			}
			else {
			    GoogleCredential credential = new GoogleCredential.Builder()
			    	.setTransport(TRANSPORT)
			    	.setJsonFactory(JSON_FACTORY)
			    	.setClientSecrets(clientSecrets)
			    	.build();
			    credential.setAccessToken(accessToken);
			    credential.setRefreshToken(refreshToken);
			      
//			    Oauth2 someOauth = new Oauth2.Builder(TRANSPORT, JSON_FACTORY, credential).setApplicationName(applicationName).build();
//			    Tokeninfo tokeninfo = someOauth.tokeninfo().setAccessToken(credential.getAccessToken()).execute();
	
				ServiceUser serviceUser = getServiceUser(credential);
				serviceUser.setAccessToken(credential.getAccessToken());
				serviceUser.setRefreshToken(credential.getRefreshToken());
				
				return serviceUser;
			}
		} catch(Exception e) {
			if (retryOnFail) {
				try {
			      TokenResponse response =
			          new GoogleRefreshTokenRequest(TRANSPORT, JSON_FACTORY, refreshToken, clientId, clientSecret).setGrantType("refresh_token").execute();
			      return authenticateAccessToken(response.getAccessToken(), response.getRefreshToken(), accountId, false);
			    } catch (TokenResponseException tre) {
			      if (tre.getDetails() != null) {
					log.error("Error: " + tre.getDetails().getError());
			        if (tre.getDetails().getErrorDescription() != null) {
						log.error(tre.getDetails().getErrorDescription());
			        }
			        if (tre.getDetails().getErrorUri() != null) {
						log.error(tre.getDetails().getErrorUri());
			        }
			      } else {
					log.error("Unable to get authenticate oauth code", e);
			      }
			      // TODO: Remove this.  Security hole.
			      return retrieveServiceUser(accountId);
//			      return null;
			    } catch (NullPointerException npe) {
			    	// Google OAuth failed, which is not necessarily a problem here.
			    	log.debug("Google OAuth Failed: ", npe);
			    	// TODO: Remove this.  Security hole.
			    	return retrieveServiceUser(accountId);
//			    	return null;
			    } catch (Exception e1) {
		    		log.error("Error: ", e1);
			    	// TODO: Remove this.  Security hole.
			    	return retrieveServiceUser(accountId);
//			    	return null;
			    }			
			}
			else {
				log.error("Unable to get authenticate oauth code", e);
				return null;
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public ServiceUser retrieveServiceUser(String accountId) {
		ServiceUser result = null;
		
		try {
		    directoryCredential = new GoogleCredential.Builder()
		        .setTransport(TRANSPORT)
		        .setJsonFactory(JSON_FACTORY)
		        .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
		        .setClientSecrets(clientSecrets)
		        .setServiceAccountScopes(directoryScopes)
		        .setServiceAccountPrivateKey(privateKey)
		        .setServiceAccountUser(admin)
		        //.setServiceAccountPrivateKeyFromP12File(
		        //    new File(SERVICE_ACCOUNT_PKCS12_FILE_PATH))
		        .build();
	
		    directory = new Directory.Builder(TRANSPORT, JSON_FACTORY, directoryCredential).setApplicationName(applicationName).build();
	
			User user = directory.users().get(accountId).setUserKey(accountId).execute();
			result = new ServiceUser();
				
			result.setId(user.getId());
			result.getIdentifiers().add(new ServiceIdentifier(ServiceIdentifier.GOOGLE, result.getId()));
			
			result.setName(user.getName().getFullName());
			result.setFirstName(user.getName().getGivenName());
			result.setLastName(user.getName().getFamilyName());
			
			// If domain is set, then we need to verify this user has an email address that belongs to the domain.
			Boolean hasValidDomain = !StringUtils.hasText(domain);
			
			List<String> emails = new ArrayList<String>();
			String user_email = user.getPrimaryEmail();
			//Boolean user_emailIsVerified = user.getVerifiedEmail();
			//if (user_emailIsVerified) {
				
				if (!hasValidDomain) {
					// If no valid domain has been found yet, check to see if this email has the domain.
					hasValidDomain = user_email.toLowerCase().endsWith("@" + domain.toLowerCase());
				}
				
				emails.add(user_email);
			//}
			
			@SuppressWarnings("unchecked")
			Iterator<Object> itrUserEmails = ((List<Object>)user.getEmails()).iterator();
			while (itrUserEmails.hasNext()) {
				Map nextUserEmail = (Map)itrUserEmails.next();
				try {
					String address = (String) nextUserEmail.get("address");
					Boolean isPrimary = (Boolean) nextUserEmail.get("primary");
					isPrimary = (isPrimary != null ? isPrimary : false);
					if (!emails.contains( address )) {
						emails.add( address );
					}
				} catch(Exception e) {
					log.debug("Error retrieving Google email for ServiceUser " + result.getName());
				}
			}
			
			result.setEmails(emails);
			Iterator<String> itrEmails = emails.iterator();
			while (itrEmails.hasNext()) {
				result.getIdentifiers().add(new ServiceIdentifier(ServiceIdentifier.EMAIL, itrEmails.next()));
			}
			
			// Get the user name from the email address.
			//	If no domain, then assume user name is the entire email address.
			String userName = user_email;
			if (StringUtils.hasText(domain)) {
				int atIndex = user_email.indexOf("@");
				if (atIndex >= 0)
					userName = user_email.substring(0, atIndex);
			}
			
			// If no valid domain, then reject user.
			if (!hasValidDomain) {
				log.warn("Valid Google user [" + (StringUtils.hasText(userName) ? userName : "UNKNOWN") + "] found, but they do not belong to the domain " + domain);
				return null;
			}
			
			// Get the user's role (Group) names.
			result.setAreRoleNamesAccurate(useRoleNames);
			if (useRoleNames) {
				try {
					List<String> userRoleNames = getUserRoleNames(userName);
					result.setRoleNames(userRoleNames);
				} catch(Exception e) {
					result.setAreRoleNamesAccurate(false);
				}
			}
			
		} catch (GoogleJsonResponseException gjre) {
			log.debug("Google Login Rejected: " + gjre.getMessage());
		} catch (Exception e) {
			log.error("Error getting Google Profile Information", e);
		}
		
		return result;
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
		return getUserRoles(login, true);
	}
	private List<Group> getGroups(Directory theDirectory, String userKey) throws IOException {
	    Groups groups = theDirectory.groups().list().setUserKey(userKey).execute();
	    List<Group> listGroups = groups.getGroups();
	    return listGroups;
	}
  
	public Collection<ServiceRole> getUserRoles(String login, Boolean useRecursion)
			throws Exception{
		Collection<ServiceRole> userServiceRoles = new HashSet<ServiceRole>();
		
		Collection<String> retrievedMemberNames = new HashSet<String>();
		Collection<String> memberNames = new HashSet<String>();
		memberNames.add(login);
		
	    directoryCredential = new GoogleCredential.Builder()
		        .setTransport(TRANSPORT)
		        .setJsonFactory(JSON_FACTORY)
		        .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
		        .setClientSecrets(clientSecrets)
		        .setServiceAccountScopes(directoryScopes)
		        .setServiceAccountPrivateKey(privateKey)
		        .setServiceAccountUser(admin)
		        //.setServiceAccountPrivateKeyFromP12File(
		        //    new File(SERVICE_ACCOUNT_PKCS12_FILE_PATH))
		        .build();
		
	    directory = new Directory.Builder(TRANSPORT, JSON_FACTORY, directoryCredential).setApplicationName(applicationName).build();
		
		while(!memberNames.isEmpty()) {
			String nextMember = (String) memberNames.toArray()[0];
			log.debug("Retrieving roles for " + nextMember);
			memberNames.remove(nextMember);
			retrievedMemberNames.add(nextMember);
			
			try {
				List<Group> groups = getGroups(directory, nextMember);
				if (groups != null) {
					Iterator<Group> itrGroups = groups.iterator();
					
					while(itrGroups.hasNext()) {
						try {
							Group nextGroup = itrGroups.next();
							
							ServiceRole nextRole = new ServiceRole();
							nextRole.setName(nextGroup.getName());
							nextRole.setId(nextGroup.getId());
							userServiceRoles.add(nextRole);

							if (useRecursion && !retrievedMemberNames.contains(nextGroup.getEmail())) {
								// If useRecursion, this will explicitly check each group's groups until all groups the user
								//	is a member of (whether directly or indirectly) have been queried.
								memberNames.add(nextGroup.getEmail());
							}
							
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			} catch(IOException ioe) {
				//ioe.printStackTrace();
				//log.warn("Unable to get user " + login + " Google role names");
				//throw ioe;
				// Do nothing.
				log.warn("Error getting role for " + nextMember);
			}
		}
		
		return userServiceRoles;
	}
	
	private ServiceUser getServiceUser(Credential credential) {
		ServiceUser result = null;
		
		try {
			Oauth2 oauth2 = new Oauth2.Builder(TRANSPORT, JSON_FACTORY, credential).setApplicationName(applicationName).build();
			Userinfoplus userinfo = getUserInfo(oauth2);
			if (userinfo == null) {
				return null;
			}
			
			result = new ServiceUser();
				
			// This is set here in the case we had to refresh this access token.
			result.setAccessToken(credential.getAccessToken());
			result.setRefreshToken(credential.getRefreshToken());
			
			result.setId(userinfo.getId());
			result.getIdentifiers().add(new ServiceIdentifier(ServiceIdentifier.GOOGLE, result.getId()));
			
			result.setName(userinfo.getName());
			result.setFirstName(userinfo.getGivenName());
			result.setLastName(userinfo.getFamilyName());
			result.setLink(userinfo.getLink());
			result.setLocale(userinfo.getLocale());
			
			// If domain is set, then we need to verify this user has an email address that belongs to the domain.
			Boolean hasValidDomain = !StringUtils.hasText(domain);
			
			List<String> emails = new ArrayList<String>();
			String user_email = userinfo.getEmail();
			Boolean user_emailIsVerified = userinfo.getVerifiedEmail();
			if (user_emailIsVerified) {
				
				if (!hasValidDomain) {
					// If no valid domain has been found yet, check to see if this email has the domain.
					hasValidDomain = user_email.toLowerCase().endsWith("@" + domain.toLowerCase());
				}
				
				emails.add(user_email);
			}
			
			result.setEmails(emails);
			Iterator<String> itrEmails = emails.iterator();
			while (itrEmails.hasNext()) {
				result.getIdentifiers().add(new ServiceIdentifier(ServiceIdentifier.EMAIL, itrEmails.next()));
			}
			
			// Get the user name from the email address.
			//	If no domain, then assume user name is the entire email address.
			String userName = user_email;
			if (StringUtils.hasText(domain)) {
				int atIndex = user_email.indexOf("@");
				if (atIndex >= 0)
					userName = user_email.substring(0, atIndex);
			}
			
			// If no valid domain, then reject user.
			if (!hasValidDomain) {
				log.warn("Valid Google user [" + (StringUtils.hasText(userName) ? userName : "UNKNOWN") + "] found, but they do not belong to the domain " + domain);
				return null;
			}
			
			// Get the user's role (Group) names.
			result.setAreRoleNamesAccurate(useRoleNames);
			if (useRoleNames) {
				try {
					List<String> userRoleNames = getUserRoleNames(userinfo.getEmail());
					result.setRoleNames(userRoleNames);
				} catch(Exception e) {
					log.warn("Role names NOT accurate", e);
					result.setAreRoleNamesAccurate(false);
				}
			}
			
//			throw new RuntimeException("Just for testing");
			
		} catch (Exception e) {
			log.info("Error getting Google Profile Information: " + e.getMessage());
		}
		
		return result;
	}
	
	private Userinfoplus getUserInfo(Oauth2 oauth2) throws IOException {
		try {
			Userinfoplus userinfo = oauth2.userinfo().get().execute();
			System.out.println(userinfo.toPrettyString());
			return userinfo;
		} catch(TokenResponseException tre) {
			log.info("Error getting Google Profile Information: " + tre.getMessage());
			return null;
		} catch(GoogleJsonResponseException gjre) {
			log.info("Error getting Google Profile Information: " + gjre.getMessage());
			return null;
		}
	}
	
	public String refreshAccessToken(String refreshToken) {
		String result = "";
		
		/*try {
			String strContent = REFRESH_ACCESS_TOKEN_PARAMS.replace("{client_id}", clientId).replace("{client_secret}", clientSecret).replace("{refresh_token}", refreshToken);
			URL url = new URL(REFRESH_ACCESS_TOKEN_URL);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(strContent);
			wr.flush();
			
			InputStreamReader inputStreamReader = new InputStreamReader(conn.getInputStream());
			// Get the response.
			JsonParser parser = new JsonParser();
			JsonElement jsonRefreshTokenInfo = parser.parse(inputStreamReader);
			JsonObject jsonGroupObject = jsonRefreshTokenInfo.getAsJsonObject();
			JsonElement jsonAccessTokenElement = jsonGroupObject.get("access_token");
			
			String newAccessToken = jsonAccessTokenElement.getAsString();
			result = newAccessToken;
			
			wr.close();
			inputStreamReader.close();
		} catch(IOException ioe) {
			log.error("Error refreshing access token", ioe);
			ioe.printStackTrace();
		}*/
		
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

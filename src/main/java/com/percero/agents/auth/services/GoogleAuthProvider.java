package com.percero.agents.auth.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
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
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.percero.agents.auth.vo.*;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.common.OAuth;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.*;

/**
 * Created by jonnysamps on 8/19/15.
 */
@Component
public class GoogleAuthProvider implements IAuthProvider{

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

    private static final String ID = "google";
    public String getID() {
        return ID;
    }

    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    @Autowired
    @Value("$pf{oauth.google.clientId}")
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

    @Autowired
    ObjectMapper om;

    private Collection<String> directoryScopes = null;

    private GoogleClientSecrets clientSecrets = null;

    private PrivateKey privateKey = null;

    private String SERVICE_ACCOUNT_EMAIL = null;
    private GoogleCredential directoryCredential = null;
    private Directory directory = null;


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

    public ServiceUser authenticate(String credential) {
        try {
            OAuthCredential decodedCredential = om.readValue(credential, OAuthCredential.class);

            GoogleAuthorizationCodeTokenRequest authRequest = new GoogleAuthorizationCodeTokenRequest(TRANSPORT, JSON_FACTORY, clientId, clientSecret, decodedCredential.getCode(), decodedCredential.getRedirectUrl());
            GoogleTokenResponse authResponse = authRequest.execute();
            System.out.println(authResponse.getAccessToken());

            GoogleCredential googleCredential = new GoogleCredential.Builder()
                    .setTransport(TRANSPORT)
                    .setJsonFactory(JSON_FACTORY)
                    .setClientSecrets(clientSecrets)
                    .build();
            googleCredential.setAccessToken(authResponse.getAccessToken());
            googleCredential.setRefreshToken(authResponse.getRefreshToken());
            googleCredential.setExpiresInSeconds(authResponse.getExpiresInSeconds());
            googleCredential.setFromTokenResponse(authResponse);

            ServiceUser serviceUser = getServiceUser(googleCredential);

            return serviceUser;
        } catch(Exception e) {
            log.error("Unable to get authenticate oauth code", e);
            return null;
        }
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

    private List<Group> getGroups(Directory theDirectory, String userKey) throws IOException {
        Groups groups = theDirectory.groups().list().setUserKey(userKey).execute();
        List<Group> listGroups = groups.getGroups();
        return listGroups;
    }

}

package com.percero.agents.auth.services;

import org.apache.log4j.Logger;

import com.percero.agents.auth.services.AuthService2;
import com.percero.agents.auth.services.DatabaseHelper;
import com.percero.agents.auth.services.IAuthProvider;
import com.percero.agents.auth.vo.AuthCode;
import com.percero.agents.auth.vo.AuthProviderResponse;
import com.percero.agents.auth.vo.BasicAuthCredential;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.agents.auth.vo.UserAccount;
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
//    private TeamLeaderDAO teamLeaderDAO;
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
    
    public AuthProviderResponse register(String jsonRegistrationString) {
        AuthProviderResponse response = new AuthProviderResponse();
    	ServiceUser serviceUser = null;
    	BasicAuthCredential cred = BasicAuthCredential.fromJsonString(jsonRegistrationString);

        logger.debug("Autheticating user " + cred.getUsername());
        
        serviceUser = authDatabaseHelper.registerUser(cred, getID());
        
        if (serviceUser == null) {
        	response.authCode = BasicAuthCode.BAD_USER_PASS;
			logger.debug("AUTH FAILURE: " + response.authCode.getMessage());
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
    
//	/**
//	 * @param teamLeader
//	 * @throws SyncException
//	 */
//	private void validateTeamLeader(TeamLeader teamLeader, ServiceUser serviceUser) throws SyncException {
//		// Make sure a valid PulseUser exists for this TeamLeader.
//		
//		// First make sure we have a valid user.
//        serviceUser.setAuthProviderID(getID());
//        UserAccount userAccount = authService2.getOrCreateUserAccount(serviceUser, this);
////        authService2.ensureAnchorUserExists(serviceUser, userAccount.getUser());
//
//		IDataProvider pulseUserDataProvider = DataProviderManager.getInstance().getDataProviderByName(PulseUser.class.getCanonicalName());
//		PulseUser thePulseUser = null;
//
//		PulseUser pulseUserExample = new PulseUser();
//		pulseUserExample.setTeamLeader(teamLeader);
//		List<IPerceroObject> listPulseUsers = pulseUserDataProvider.findByExample(pulseUserExample, null, null, false);
//		if (listPulseUsers != null && !listPulseUsers.isEmpty()) {
//			if (listPulseUsers.size() > 1) {
//				logger.error("[PulseHttpAuthProvider] " + listPulseUsers.size() + " PulseUsers found for TeamLeader " + teamLeader.getID());
//			}
//			thePulseUser = (PulseUser) listPulseUsers.get(0);
//			
//			if (!userAccount.getUser().getID().equalsIgnoreCase(thePulseUser.getUserId())) {
//				// Need to update this PulseUser with the correct user id.
//				thePulseUser.setUserId(userAccount.getUser().getID());
//				syncAgentService.systemPutObject(thePulseUser, null, null, null, true);
//			}
//		}
//		else {
//			// No valid PulseUser, so create one.
//			logger.debug("[PulseHttpAuthProvider] NO PulseUser found for TeamLeader " + teamLeader.getID() + ", creating new PulseUser");
//			PulseUser newPulseUser = new PulseUser();
//			newPulseUser.setID(UUID.randomUUID().toString());
//			newPulseUser.setTeamLeader(teamLeader);
//			newPulseUser.setEmployeeId(teamLeader.getEmployeeId());
//			newPulseUser.setFirstName(teamLeader.getFirstName());
//			newPulseUser.setLastName(teamLeader.getLastName());
//			newPulseUser.setUserId(userAccount.getUser().getID());
//			thePulseUser = syncAgentService.systemCreateObject(newPulseUser, null);
//			logger.debug("[PulseHttpAuthProvider] PulseUser created");
//		}
//		
//		// Now make sure there is a valid Email for this PulseUser.
//		IDataProvider emailDataProvider = DataProviderManager.getInstance().getDataProviderByName(Email.class.getCanonicalName());
//		Email emailExample = new Email();
//		emailExample.setPulseUser(thePulseUser);
//		List<IPerceroObject> listEmails = emailDataProvider.findByExample(emailExample, null, null, false);
//		if (listEmails != null && !listEmails.isEmpty()) {
//			if (listEmails.size() > 1) {
//				logger.error("[PulseHttpAuthProvider] " + listEmails.size() + " Emails found for PulseUser " + thePulseUser.getID());
//			}
//		}
//		else {
//			// No valid PulseUser, so create one.
//			logger.debug("[PulseHttpAuthProvider] NO Email found for PulseUser " + thePulseUser.getID() + ", creating new Email");
//			Email newEmail = new Email();
//			newEmail.setID(UUID.randomUUID().toString());
//			newEmail.setPulseUser(thePulseUser);
//			newEmail.setEmailAddress(teamLeader.getEmailAddress());
//			syncAgentService.systemCreateObject(newEmail, null);
//			logger.debug("[PulseHttpAuthProvider] Email created");
//		}
//	}

//    private ObjectMapper objectMapper;

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

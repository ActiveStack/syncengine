package com.percero.agents.auth.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.auth.helpers.IAccountHelper;
import com.percero.agents.auth.hibernate.AuthHibernateUtils;
import com.percero.agents.auth.vo.AuthProvider;
import com.percero.agents.auth.vo.OAuthResponse;
import com.percero.agents.auth.vo.OAuthToken;
import com.percero.agents.auth.vo.ServiceIdentifier;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.agents.auth.vo.SvcAppRole;
import com.percero.agents.auth.vo.User;
import com.percero.agents.auth.vo.UserAccount;
import com.percero.agents.auth.vo.UserIdentifier;
import com.percero.agents.auth.vo.UserToken;
import com.percero.agents.sync.access.IAccessManager;

/**
 * The AuthService is responsible for managing authentication of users within the Percero framework. The AuthService
 * maintains its own separate database that generically references users.  Implementations of IAuthHelper's provide
 * integration to 3rd party auth providers/services (Google, Facebook, LinkedIn, etc.).  Custom IAuthHelper's can
 * also be provided for custom auth integrations (database, LDAP, etc.).
 * 
 * @author Collin Brown
 *
 */
@Component
public class AuthService implements IAuthService {

	// TODO: Better manage Hibernate Sessions (opening and closing).

	private static Logger log = Logger.getLogger(AuthService.class);

	@Autowired
	SessionFactory sessionFactoryAuth;

	@Autowired
	DatabaseHelper databaseHelper;

	@Autowired
	IAccessManager accessManager;

	@Autowired @Value("$pf{anonAuth.enabled:false}")
	Boolean anonAuthEnabled = false;
	@Autowired @Value("$pf{anonAuth.code:ANON}")
	String anonAuthCode = "ANON";
	@Autowired @Value("$pf{anonAuth.roleNames:}")
	String anonAuthRoleNames = "";

	@Autowired
	GoogleHelper googleHelper;

	@Autowired
	FacebookHelper facebookHelper;
	
	@Autowired
	LinkedInHelper linkedInHelper;
	
	@Autowired
	GitHubHelper githubHelper;

	@Autowired
	IAccountHelper accountHelper;

	@Autowired @Value("$pf{auth.maxUserTokenCleanupCount:500}")
	private Integer maxUserTokenCleanupCount = 500;
	
	public void setSessionFactoryAuth(SessionFactory value) {
		sessionFactoryAuth = value;
	}

	public AuthService() {
	}


	/* (non-Javadoc)
	 * @see com.com.percero.agents.auth.services.IAuthService#authenticateOAuthCode(com.com.percero.agents.auth.vo.AuthProvider, java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.com.percero.agents.auth.vo.OAuthToken)
	 */
	public OAuthResponse authenticateOAuthCode(String authProviderID, String code, String clientId, String deviceId, String redirectUrl, OAuthToken requestToken){

		Object accessTokenResult = getServiceProviderAccessToken(authProviderID, code, redirectUrl, requestToken);
		if (accessTokenResult == null)
			return null;

		if (accessTokenResult instanceof ServiceUser) {
			ServiceUser serviceUser = (ServiceUser) accessTokenResult;
			serviceUser.setAuthProviderID(authProviderID);
			return setupServiceUser(authProviderID, serviceUser, clientId, deviceId);
		}
		else if (accessTokenResult instanceof OAuthToken){
			OAuthToken token = (OAuthToken) accessTokenResult;
			return authenticateOAuthAccessToken(authProviderID, token.getToken(), token.getTokenSecret(), clientId, deviceId, false);
		}
		else {
			log.error("Invalid access token result in authenticateOAuthCode");
			return null;
		}
	}

	protected Object getServiceProviderAccessToken(String authProviderID, String code, String redirectUrl, OAuthToken requestToken) {
		OAuthToken token = null;

		try {
			/*if (authProviderID.equals(AuthProvider.LINKEDIN)) {
				//				LinkedInHelper linkedInHelper = new LinkedInHelper();
				//				token = linkedInHelper.getAccessToken(svcOAuth.getAppKey(), svcOAuthSecret.getAppToken(), code, requestToken);
			}
			//			else if (svcOAuth.getServiceApplication().getServiceProvider().getName().equalsIgnoreCase(FacebookHelper.SERVICE_PROVIDER_NAME)) {
			//				//oauthToken = FacebookHelper.getRequestToken(svcOauth.getAppKey(), svcOauthSecret.getAppToken());
			//				throw new IllegalArgumentException("Facebook OAuth Not supported");
			//			}
			else */
			if (authProviderID.equals(AuthProvider.GOOGLE.toString())) {
				ServiceUser serviceUser = googleHelper.authenticateOAuthCode(code, redirectUrl);
				return serviceUser;
			}
			else if (authProviderID.equals(AuthProvider.LINKEDIN.toString())) {
				ServiceUser serviceUser = linkedInHelper.authenticateOAuthCode(code, redirectUrl);
				return serviceUser;
			}
			else if (authProviderID.equals(AuthProvider.FACEBOOK.toString())) {
				ServiceUser serviceUser = facebookHelper.authenticateOAuthCode(code, redirectUrl);
				return serviceUser;
			}
			else if (authProviderID.equals(AuthProvider.GITHUB.toString())) {
				String accessToken = githubHelper.getAccessTokenResponse(code, redirectUrl);
				token = new OAuthToken();
				token.setToken(accessToken);
			}
			else if(authProviderID.equals(AuthProvider.ANON.toString())){
				token = new OAuthToken();
				token.setToken("ANON");
				token.setTokenSecret("ANON");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return token;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.auth.services.IAuthService#authenticateBasicOAuth(com.com.percero.agents.auth.vo.AuthProvider, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.com.percero.agents.auth.vo.OAuthToken)
	 */
	public OAuthResponse authenticateBasicOAuth(String authProviderID, String userName, String password, String scopes, String appUrl, String clientId, String deviceId, OAuthToken requestToken){

		OAuthToken token = getServiceProviderAccessTokenViaBasicAuth(authProviderID, userName, password, scopes, appUrl, requestToken);
		if (token == null)
			return null;

		return authenticateOAuthAccessToken(authProviderID, token.getToken(), token.getTokenSecret(), clientId, deviceId, false);
	}

	protected OAuthToken getServiceProviderAccessTokenViaBasicAuth(String authProviderID, String userName, String password, String scopes, String appUrl, OAuthToken requestToken) {
		OAuthToken token = null;

		try {
			if (authProviderID.equals(AuthProvider.GITHUB)) {
				String accessToken = githubHelper.getBasicAccessTokenResponse(userName, password, scopes, appUrl);
				token = new OAuthToken();
				token.setToken(accessToken);
			}
			else if(authProviderID.equals(AuthProvider.ANON)){
				token = new OAuthToken();
				token.setToken("ANON");
				token.setTokenSecret("ANON");
			}
			else {
				throw new IllegalArgumentException(authProviderID + " OAuth Not supported");

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return token;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.auth.services.IAuthService#authenticateOAuthAccessToken(com.com.percero.agents.auth.vo.AuthProvider, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public OAuthResponse authenticateOAuthAccessToken(String authProviderID, String accessToken, String refreshToken, String clientId, String deviceId) {
		return authenticateOAuthAccessToken(authProviderID, accessToken, refreshToken, clientId, deviceId, true);
	}
	
	public OAuthResponse authenticateOAuthAccessToken(String authProviderID, String accessToken, String refreshToken, String clientId, String deviceId, Boolean canOverideAccessToken) {
		/**
		 * The client may not actually have the refresh token.  If not then we need to find it, because without a valid
		 * refresh token then any "Auto-login" will fail after the token expires... which can cause client connections
		 * to go stale without them knowing it.
		 * 
		 * In the case where multiple devices are connected as the same user a device may have the wrong accessToken.  So use the 
		 * good one.
		 * 
		 * We only want to do this when we are doing a "TRUE" auth by access token (not by OAuth code, which delegates to this function).
		 * That is why we include canOverideAccessToken.
		 */
		if(canOverideAccessToken && (refreshToken == null || refreshToken.isEmpty())){
			Session session = sessionFactoryAuth.getCurrentSession();
			session.beginTransaction();

			User user = (User) 
			(session.createQuery("select distinct(ut.user) from UserToken ut where clientId=:clientId OR deviceId=:deviceId")
					.setParameter("clientId", clientId)
					.setParameter("deviceId", deviceId)
					).uniqueResult();
			if(user != null){
				for(Object ob : user.getUserAccounts()){
					UserAccount ua = (UserAccount)ob;
					if(ua.getAuthProviderID().equals(authProviderID)){
						refreshToken = ua.getRefreshToken();
						accessToken = ua.getAccessToken();
						break;
					}
				}
			}

		}

		ServiceUser serviceUser = getServiceProviderServiceUser(accessToken, refreshToken, "", authProviderID);
		if (serviceUser == null)
			return null;
		else
			return setupServiceUser(authProviderID, serviceUser, clientId, deviceId);
	}

	public OAuthResponse setupServiceUser(String authProviderID, ServiceUser serviceUser, String clientId, String deviceId) {
		
		OAuthResponse result = null;
		UserToken userToken = null;
		
		/**
		 * The client may not actually have the refresh token.  If not then we need to find it, because without a valid
		 * refresh token then any "Auto-login" will fail after the token expires... which can cause client connections
		 * to go stale without them knowing it.
		 * 
		 * In the case where multiple devices are connected as the same user a device may have the wrong accessToken.  So use the 
		 * good one.
		 */
		if(!StringUtils.hasText(serviceUser.getRefreshToken()) && StringUtils.hasText(clientId)){
			Session session = sessionFactoryAuth.getCurrentSession();
			session.beginTransaction();
			
			User user = (User) 
					(session.createQuery("select distinct(ut.user) from UserToken ut where clientId=:clientId OR deviceId=:deviceId")
							.setParameter("clientId", clientId)
							.setParameter("deviceId", deviceId)
							).uniqueResult();
			if(user != null){
				for(Object ob : user.getUserAccounts()){
					UserAccount ua = (UserAccount)ob;
					if(ua.getAuthProviderID().equals(authProviderID)){
						serviceUser.setRefreshToken(ua.getRefreshToken());
						serviceUser.setAccessToken(ua.getAccessToken());
						break;
					}
				}
			}
			
		}
		
		if (serviceUser != null && serviceUser.getId() != null && serviceUser.getId().length() > 0) {
			// The access token may have been updated for this ServiceUser.
			
			String accountId = serviceUser.getId();
			if (accountId != null && accountId.trim().length() > 0) {
				
				// TODO: We need to know the ServiceProvider and ServiceApplication here.
				//	Without setting the queryUserAccount.ServiceProvider, updateUserAccountToken will fail if the UserAccount does not already exist. 
				UserAccount queryUserAccount = new UserAccount();
				queryUserAccount.setAccountId(accountId);
				queryUserAccount.setAccessToken(serviceUser.getAccessToken());
				queryUserAccount.setRefreshToken(serviceUser.getRefreshToken());
				queryUserAccount.setAuthProviderID(serviceUser.getAuthProviderID());
				
				UserAccount theFoundUserAccount = updateUserAccountToken(queryUserAccount, true, serviceUser);
				
				if (theFoundUserAccount != null) {
					try {
						// Validate the user in the project database (by
						// checking the IUserAnchor class)
						// In the case that the UserAnchor object already exists
						// in the project database but is NOT linked to the
						// User, we need to link it here.
						Object validateUserResult = accountHelper.validateUser(null, theFoundUserAccount.getUser().getID(), this);
						System.out.println(validateUserResult);
					} catch (Exception e) {
						log.warn("Error validating user", e);
					}
					
					// Now check Service Application Roles.
					Boolean foundMatchingRole = validateUserRoles(serviceUser, theFoundUserAccount.getUser().getID());
					
					if (foundMatchingRole) {
						userToken = loginUserAccount(theFoundUserAccount, clientId, deviceId);
						result = new OAuthResponse();
						result.userToken = userToken;
						result.accessToken = serviceUser.getAccessToken();
						result.refreshToken = serviceUser.getRefreshToken();
					} else {
						log.warn("Unable to validate user " + serviceUser.getName() + ". No valid role found.");
					}
				} else {
					log.debug("Unable to find user account for ServiceUser " + serviceUser.getName());
				}
			}
		}
		
		return result;
	}
	
	// TODO: Need to check the Service Application only, NOT ALL Service Applications for this Service Provider.
	protected Boolean validateUserRoles(ServiceUser serviceUser, String userId) {
		// Get list of role names required for this Auth authProviderID
		List<SvcAppRole> authProviderRequiredSvcRoles = getSvcAppRoles(serviceUser.getAuthProviderID());
		
		// If the auth authProviderID requires no valid roles, then we have "foundMatchingRole"
		Boolean foundMatchingRole = (authProviderRequiredSvcRoles == null || authProviderRequiredSvcRoles.size() == 0);

		// Only check the ServiceUser roles list if role names for the ServiceUser are valid.
		if (serviceUser.getAreRoleNamesAccurate() && !foundMatchingRole) {
			Iterator<SvcAppRole> itrSvcRoles = authProviderRequiredSvcRoles.iterator();

			// Check to see if the ServiceUser has at least one role that is in the required auth authProviderID roles list.
			while(!foundMatchingRole && itrSvcRoles.hasNext()) {
				SvcAppRole nextSvcAppRole = itrSvcRoles.next();
				for(String nextRole : serviceUser.getRoleNames()) {
					if (nextRole.matches(nextSvcAppRole.getValue())) {
						foundMatchingRole = true;
						break;
					}
				}
			}
		}

		// Now check existing auth roles for the user. Not all Auth Providers authProviderID roles.
		if (!foundMatchingRole) {
			Iterator<SvcAppRole> itrAuthProviderRequiredSvcRoles = authProviderRequiredSvcRoles.iterator();

			try {
				// This is a list of existing roles for this user.
				List<String> userRoles = accountHelper.getUserRoles(userId);
				
				while(!foundMatchingRole && itrAuthProviderRequiredSvcRoles.hasNext()) {
					SvcAppRole nextAuthProviderRequiredSvcAppRole = itrAuthProviderRequiredSvcRoles.next();

					Iterator<String> itrUserRoles = userRoles.iterator();
					while (itrUserRoles.hasNext()) {
						String nextRole = itrUserRoles.next();
						if (nextRole.matches(nextAuthProviderRequiredSvcAppRole.getValue())) {
							foundMatchingRole = true;
							break;
						}
					}
					
					if (!foundMatchingRole) {
						log.debug("Unable to find matching role " + nextAuthProviderRequiredSvcAppRole.getValue() + " for user " + userId);
					}
				}
			} catch(Exception e) {
				log.error("Unable to get UserRoles for user " + userId, e);
			}
		}

		return foundMatchingRole;
	}


	/**
	 * Retrieve the list of valid role names for the specified auth authProviderID (or NONE)
	 * for which a user must have have at least one to access the system.
	 * 
	 * @param authProviderID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<SvcAppRole> getSvcAppRoles(String authProviderID) {
		Session s = null;
		List<SvcAppRole> result = null;

		try {
			s = sessionFactoryAuth.openSession();
			Query query = s.createQuery("FROM SvcAppRole sar WHERE sar.authProvider = :authProvider OR sar.authProvider = NULL");
			query.setString("authProvider", authProviderID.toString());

			result = (List<SvcAppRole>) query.list();
			result = (List<SvcAppRole>) AuthHibernateUtils.cleanObject(result);
		} catch (Exception e) {
			log.error("Unable to getSvcAppRoles", e);
		} finally {
			if (s != null)
				s.close();
		}

		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private UserAccount updateUserAccountToken(UserAccount theQueryObject, Boolean createIfNotExist, ServiceUser serviceUser) {
		UserAccount theFoundUserAccount = null;
		Session s = null;
		try {
			s = sessionFactoryAuth.openSession();
			Criteria criteria = s.createCriteria(UserAccount.class)
					.add(Restrictions.eq("accountId", theQueryObject.getAccountId()))
					.add(Restrictions.eq("authProviderID", theQueryObject.getAuthProviderID()));
			List result = criteria.list();
			List userAccounts = (List) AuthHibernateUtils.cleanObject(result);

			// It is possible that this Service Provider (or this use case) does not have an AccessToken, so set one here.
			if (theQueryObject.getAccessToken() == null || theQueryObject.getAccessToken().length() == 0)
				theQueryObject.setAccessToken(getRandomId());

			if (userAccounts != null && !userAccounts.isEmpty()) {
				// Found a valid UserAccount.
				List userAccountList = (List) userAccounts;
				theFoundUserAccount = (UserAccount) userAccountList.get(0);
				// Check the AccessToken.
				if (!theQueryObject.getAccessToken().equals(theFoundUserAccount.getAccessToken()) ) {
					theFoundUserAccount.setAccessToken(theQueryObject.getAccessToken());
					if(theQueryObject.getRefreshToken() != null && !theQueryObject.getRefreshToken().isEmpty())
						theFoundUserAccount.setRefreshToken(theQueryObject.getRefreshToken());
					Transaction tx = s.beginTransaction();
					tx.begin();
					theFoundUserAccount = (UserAccount) s
							.merge(theFoundUserAccount);
					tx.commit();
				}
			} else if (createIfNotExist) {
				User theUser = null;

				// Attempt to find this user by finding a matching UserIdentifier.
				if (serviceUser.getIdentifiers() != null && serviceUser.getIdentifiers().size() > 0) {
					String strFindUserIdentifier = "SELECT ui.user FROM UserIdentifier ui WHERE";
					int counter = 0;
					for(ServiceIdentifier nextServiceIdentifier : serviceUser.getIdentifiers()) {
						if (counter > 0)
							strFindUserIdentifier += " OR ";
						strFindUserIdentifier += " ui.type='" + nextServiceIdentifier.getParadigm() + "' AND ui.userIdentifier='" + nextServiceIdentifier.getValue() + "'";
						counter++;
					}
					strFindUserIdentifier += "";
					Query q = s.createQuery(strFindUserIdentifier);
					List<User> userList = (List<User>) q.list();
					if (userList.size() > 0) {
						theUser = userList.get(0);
					}
				}

				Transaction tx = s.beginTransaction();
				tx.begin();
				Date currentDate = new Date();

				if (theUser == null) {
					theUser = new User();
					theUser.setID(UUID.randomUUID().toString());
					theUser.setDateCreated(currentDate);
					theUser.setDateModified(currentDate);
					s.save(theUser);
				}

				theFoundUserAccount = new UserAccount();

				theFoundUserAccount.setAuthProviderID(serviceUser.getAuthProviderID());
				theFoundUserAccount.setUser(theUser);
				theFoundUserAccount.setDateCreated(currentDate);
				theFoundUserAccount.setDateModified(currentDate);
				theFoundUserAccount.setAccessToken(theQueryObject
						.getAccessToken());
				theFoundUserAccount.setRefreshToken(theQueryObject
						.getRefreshToken());
				theFoundUserAccount.setAccountId(theQueryObject
						.getAccountId());

				s.save(theFoundUserAccount);
				tx.commit();

				s.close();
				s = sessionFactoryAuth.openSession();

				theFoundUserAccount = (UserAccount) s.get(UserAccount.class,
						theFoundUserAccount.getID());
			}

			if (theFoundUserAccount != null) {
				theFoundUserAccount = (UserAccount) AuthHibernateUtils
						.cleanObject(theFoundUserAccount);

				// Now enter in the UserIdentifiers for this User.
				if (serviceUser.getIdentifiers() != null && serviceUser.getIdentifiers().size() > 0) {
					Transaction tx = s.beginTransaction();
					Query q = null;
					for(ServiceIdentifier nextServiceIdentifier : serviceUser.getIdentifiers()) {
						q = s.createQuery("FROM UserIdentifier ui WHERE ui.userIdentifier=:uid AND ui.type=:paradigm");
						q.setString("uid", nextServiceIdentifier.getValue());
						q.setString("paradigm", nextServiceIdentifier.getParadigm());

						List<UserIdentifier> userIdenditifierList = (List<UserIdentifier>) q.list();

						if (userIdenditifierList.size() == 0) {
							try {
								UserIdentifier userIdentifier = new UserIdentifier();
								userIdentifier.setType(nextServiceIdentifier.getParadigm());
								userIdentifier.setUser(theFoundUserAccount.getUser());
								userIdentifier.setUserIdentifier(nextServiceIdentifier.getValue());
								s.saveOrUpdate(userIdentifier);
							} catch(Exception e) {
								log.warn("Unable to save UserIdentifier for " + serviceUser.getName(), e);
							}
						}
					}
					tx.commit();
				}
			}
		} catch (Exception e) {
			log.error("Unable to run authenticate UserAccount", e);
		} finally {
			if (s != null)
				s.close();
		}
		return theFoundUserAccount;
	}

	@SuppressWarnings("rawtypes")
	private UserToken loginUserAccount(UserAccount theUserAccount, String clientId, String deviceId) {
		Session s = null;
		try {
			if (theUserAccount != null) {
				UserToken theUserToken = null;
				if (theUserAccount != null) {
					Date currentDate = new Date();
					s = sessionFactoryAuth.openSession();
					Criteria criteria = s.createCriteria(UserToken.class).add(Restrictions.eq("clientId", clientId))
							.add(Restrictions.eq("user.ID", theUserAccount.getUser().getID()));
					List result = criteria.list();
					List userTokenResult = (List) AuthHibernateUtils.cleanObject(result);

					if ( !userTokenResult.isEmpty() ) {
						theUserToken = (UserToken) userTokenResult.get(0);
					}

					Transaction tx = s.beginTransaction();
					tx.begin();

					if (theUserToken == null) {
						if (StringUtils.hasText(deviceId)) {
							// Need to delete all of UserTokens for this User/Device.
							log.debug("Deleting ALL UserToken's for User " + theUserAccount.getUser().getID() + ", Device " + deviceId);
							String deleteUserTokenSql = "DELETE FROM UserToken WHERE deviceId=:deviceId AND user=:user";
							Query deleteQuery = s.createQuery(deleteUserTokenSql);
							deleteQuery.setString("deviceId", deviceId);
							deleteQuery.setEntity("user", theUserAccount.getUser());
							deleteQuery.executeUpdate();
						}

						theUserToken = new UserToken();
						theUserToken.setUser(theUserAccount.getUser());
						theUserToken.setClientId(clientId);
						theUserToken.setDeviceId(deviceId);
						theUserToken.setDateCreated(currentDate);
						theUserToken.setDateModified(currentDate);
						theUserToken.setToken(getRandomId());
						theUserToken.setLastLogin(currentDate);
						s.save(theUserToken);

					} else {
						theUserToken.setToken(getRandomId());
						theUserToken.setLastLogin(currentDate);
						//s.merge(theUserToken);
						s.saveOrUpdate(theUserToken);
					}

					tx.commit();
				}

				return theUserToken;

			} else {
				return null;
			}
		} catch (LockAcquisitionException lae) {
			log.error("Unable to run authenticate UserAccount", lae);
			/**
			 * TODO: Fix this!
2014-09-26 14:45:55,176 [SimpleAsyncTaskExecutor-5] WARN  org.hibernate.util.JDBCExceptionReporter - SQL Error: 1213, SQLState: 40001
2014-09-26 14:45:55,177 [SimpleAsyncTaskExecutor-5] ERROR org.hibernate.util.JDBCExceptionReporter - Deadlock found when trying to get lock; try restarting transaction
2014-09-26 14:45:55,180 [SimpleAsyncTaskExecutor-5] ERROR org.hibernate.event.def.AbstractFlushingEventListener - Could not synchronize database state with session
org.hibernate.exception.LockAcquisitionException: Could not execute JDBC batch update
at org.hibernate.exception.SQLStateConverter.convert(SQLStateConverter.java:82)
at org.hibernate.exception.JDBCExceptionHelper.convert(JDBCExceptionHelper.java:43)
at org.hibernate.jdbc.AbstractBatcher.executeBatch(AbstractBatcher.java:253)
at org.hibernate.engine.ActionQueue.executeActions(ActionQueue.java:237)
at org.hibernate.engine.ActionQueue.executeActions(ActionQueue.java:141)
at org.hibernate.event.def.AbstractFlushingEventListener.performExecutions(AbstractFlushingEventListener.java:298)
at org.hibernate.event.def.DefaultFlushEventListener.onFlush(DefaultFlushEventListener.java:27)
at org.hibernate.impl.SessionImpl.flush(SessionImpl.java:1000)
at org.hibernate.impl.SessionImpl.managedFlush(SessionImpl.java:338)
at org.hibernate.transaction.JDBCTransaction.commit(JDBCTransaction.java:106)
at com.com.percero.agents.auth.services.AuthService.loginUserAccount(AuthService.java:644)
				 */
		} catch (Exception e) {
			log.error("Unable to run authenticate UserAccount", e);
		} finally {
			if (s != null)
				s.close();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.auth.services.IAuthService#logoutUser(java.lang.String, java.lang.String, java.lang.String)
	 */
	public Boolean logoutUser(String aUserId, String aToken, String aClientId) {
		Set<String> clientIds = new HashSet<String>(1);
		clientIds.add(aClientId);
		return logoutUser(aUserId, aToken, clientIds);
	}

	public Boolean logoutUser(String aUserId, String aToken, Set<String> clientIds) {
		Boolean result = false;
		boolean validUser = StringUtils.hasText(aUserId);
		boolean validClient = clientIds != null && !clientIds.isEmpty();
		boolean validToken = StringUtils.hasText(aToken);
		
		// If neither a valid user or a valid client, then no one to logout.
		if (!validUser && !validClient && !validToken) {
			log.warn("Invalid user/client/token on AuthService.logoutUser");
			return false;
		}

		String deleteUserTokenSql = "DELETE FROM UserToken WHERE ";
		
		// Match EITHER the ClientID OR the Token
		if (validClient && validToken) {
			log.debug("Logging out Client(s): " + StringUtils.collectionToCommaDelimitedString(clientIds) + " / Token: " + aToken);
			deleteUserTokenSql += " (clientId IN (" + StringUtils.collectionToDelimitedString(clientIds, ",", "\"", "\"") + ") OR token=:token) ";
		}
		else if (validToken) {
			log.debug("Logging out Token: " + aToken);
			deleteUserTokenSql += " token=:token ";
		}
		else if (validClient) {
			log.debug("Logging out Client(s): " + StringUtils.collectionToCommaDelimitedString(clientIds));
			deleteUserTokenSql += " clientId IN (" + StringUtils.collectionToDelimitedString(clientIds, ",", "\"", "\"") + ") ";
		}
		else if (validUser) {
			// This will log out ALL of the User's devices, logging them out completely.
			log.warn("Logging out ALL User " + aUserId + " devices!");
			deleteUserTokenSql += " user_ID=:user_ID ";
		}

		Session s = null;
		try {
			s = sessionFactoryAuth.openSession();

			Query deleteQuery = s.createSQLQuery(deleteUserTokenSql);
			
			if (validClient && validToken) {
				deleteQuery.setString("token", aToken);
			}
			else if (validToken) {
				deleteQuery.setString("token", aToken);
			}
			else if (validClient) {
				// Do nothing.
			}
			else if (validUser) {
				deleteQuery.setString("user_ID", aUserId);
			}
			
			deleteQuery.executeUpdate();
		} catch (StaleStateException e) {
			// Most likely this failed because the userToken has already been deleted from the database.
			log.debug("Unable to delete UserToken due to StaleStateException: " + e.getMessage());
			result = false;
		} catch (Exception e) {
			log.error("Unable to delete UserToken", e);
			result = false;
		}
		
		if (s != null && s.isOpen()) {
			s.close();
		}

		return result;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.auth.services.IAuthService#validateUserByToken(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	// TODO: This function should also validate that the user is valid against the ServiceProvider's API.
	public boolean validateUserByToken(String regAppKey, String aUserId, String aToken, String aClientId) {
    	log.debug("[AuthService] Validating user " + aUserId + " by token " + aToken + ", client " + aClientId + " NO existing clients");

		boolean result = false;

		if (/*StringUtils.hasText(regAppKey) && */StringUtils.hasText(aUserId) && StringUtils.hasText(aToken)) {
			Session s = null;
			try {
				s = sessionFactoryAuth.openSession();
				String strQuery = "SELECT COUNT(ut.ID) FROM UserToken ut WHERE ut.user.ID = :userId AND ut.token = :token AND ut.clientId = :clientId";
				Query query = s.createQuery(strQuery);
				query.setString("userId", aUserId);
				query.setString("token", aToken);
				query.setString("clientId", aClientId);

				Long uniqueResultCount = (Long) query.uniqueResult();
				if (uniqueResultCount != null && uniqueResultCount > 0) {
					if (uniqueResultCount > 1) {
				    	log.error("[AuthService] " + uniqueResultCount + " UserTokens found for  user " + aUserId + ", token " + aToken + ", client " + aClientId);
					}
					result = true;
				}
				else
					log.warn("[AuthService] Invalid User in validateUserByToken: User " + aUserId + ", Token " + aToken + ", Client " + aClientId);
			} catch (Exception e) {
				log.error("[AuthService] Unable to validateUserByToken", e);
				result = false;
			} finally {
				if (s != null)
					s.close();
			}
		} else {
			log.warn("Invalid User in validateUserByToken");
		}

		return result;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.auth.services.IAuthService#validateUserByToken(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public boolean validateUserByToken(String regAppKey, String aUserId, String aToken, String aClientId, Set<String> existingClientIds) {
    	log.debug("[AuthService] Validating user " + aUserId + " by token " + aToken + ", client " + aClientId + " " + (existingClientIds != null && !existingClientIds.isEmpty() ? existingClientIds.size() + " existing clients" : " 0 existing clients"));

		boolean result = false;
		
		if (/*StringUtils.hasText(regAppKey) && */StringUtils.hasText(aUserId) && StringUtils.hasText(aToken)) {
			Session s = null;
			try {
				s = sessionFactoryAuth.openSession();
				String strQuery = "SELECT COUNT(ut.ID) FROM UserToken ut WHERE ut.user.ID = :userId AND ut.token = :token AND ut.clientId IN (:existingClientIds)";
				Query query = s.createQuery(strQuery);
				query.setString("userId", aUserId);
				query.setString("token", aToken);
				query.setParameterList("existingClientIds", existingClientIds);
				
				Long uniqueResultCount = (Long) query.uniqueResult();
				if (uniqueResultCount != null && uniqueResultCount > 0) {
					// Now update the UserToken with the new ClientId
					strQuery = "UPDATE UserToken ut SET ut.clientId=:newClientId WHERE ut.user.ID = :userId AND ut.token = :token AND ut.clientId IN (:existingClientIds)";
					query = s.createQuery(strQuery);
					query.setString("userId", aUserId);
					query.setString("token", aToken);
					query.setString("newClientId", aClientId);
					query.setParameterList("existingClientIds", existingClientIds);
					int updateResult = query.executeUpdate();
					if (updateResult > 0) {
						result = true;
					}
					else {
						log.warn("[AuthService] Unable to update UserToken in validateUserByToken: User " + aUserId + ", Token " + aToken + ", Client " + aClientId);
					}
				}
				else {
					log.warn("[AuthService] Invalid User in validateUserByToken: User " + aUserId + ", Token " + aToken + ", Client " + aClientId);
				}
			} catch (Exception e) {
				log.error("[AuthService] Unable to validateUserByToken", e);
				result = false;
			} finally {
				if (s != null)
					s.close();
			}
		} else {
			log.warn("[AuthService] Invalid User in validateUserByToken");
		}
		
		return result;
	}
	

	/* (non-Javadoc)
	 * @see com.com.percero.agents.auth.services.IAuthService#getServiceUsers(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<ServiceUser> getServiceUsers(String aUserId) {
		List<ServiceUser> result = new ArrayList<ServiceUser>();

		Session s = null;
		try {
			s = sessionFactoryAuth.openSession();

			User user = new User();
			user.setID(aUserId);
			String userAccountQueryString = "SELECT ua FROM UserAccount ua WHERE user=:user";
			Query userAccountQuery = s.createQuery(userAccountQueryString);
			userAccountQuery.setEntity("user", user);

			List<UserAccount> userAccounts = userAccountQuery.list();

			// Get list of all ServiceApplicationOAuth's for this ServiceProvider.
			for (UserAccount nextUserAccount : userAccounts) {

				ServiceUser nextServiceUser = getServiceProviderServiceUser(nextUserAccount, nextUserAccount.getAuthProviderID());
				if(nextServiceUser != null) {
					// Found a valid Service User, add to list and break.
					result.add(nextServiceUser);
					break;
				}
			}

		} catch (Exception e) {
			log.error("Unable to get ServiceUsers", e);
		} finally {
			if (s != null)
				s.close();
		}

		return result;
	}


	/* (non-Javadoc)
	 * @see com.com.percero.agents.auth.services.IAuthService#getUserAccounts(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public Set<UserAccount> getUserAccounts(String aUserId) {
		Set<UserAccount> result = new HashSet<UserAccount>();

		Session s = null;
		try {
			s = sessionFactoryAuth.openSession();

			User user = new User();
			user.setID(aUserId);
			String userAccountQueryString = "SELECT ua FROM UserAccount ua WHERE user=:user";
			Query userAccountQuery = s.createQuery(userAccountQueryString);
			userAccountQuery.setEntity("user", user);

			result.addAll(userAccountQuery.list());

		} catch (Exception e) {
			log.error("Unable to get UserAccounts", e);
		} finally {
			if (s != null)
				s.close();
		}

		return result;
	}

	/**
	 * Attempts to get the ServiceUser from the ServiceProvider associated with this UserAccount.
	 * 
	 * @param aUserAccount
	 * @return
	 */
	public ServiceUser getServiceProviderServiceUser(UserAccount aUserAccount, String authProviderID) {
		return getServiceProviderServiceUser(aUserAccount.getAccessToken(), aUserAccount.getRefreshToken(), aUserAccount.getAccountId(), authProviderID);
	}

	/**
	 * Attempts to get the ServiceUser from the ServiceProvider associated with this accessToken.
	 * 
	 * @param accessToken
	 * @param refreshToken
	 * @param accountId
	 * @param authProviderID
	 * @return
	 */
	public ServiceUser getServiceProviderServiceUser(String accessToken, String refreshToken, String accountId, String authProviderID) {
		ServiceUser serviceUser = null;

		try {
			if (AuthProvider.FACEBOOK.name().equalsIgnoreCase(authProviderID)) {
				ServiceUser fbServiceUser = facebookHelper.getServiceUser(
						accessToken,
						accountId);
				serviceUser = fbServiceUser;
			}
			else if (AuthProvider.GOOGLE.name().equalsIgnoreCase(authProviderID)) {
				ServiceUser glServiceUser = googleHelper.authenticateAccessToken(accessToken, refreshToken, accountId);
				//ServiceUser glServiceUser = googleHelper.retrieveServiceUser(accountId);
				serviceUser = glServiceUser;
			}
			else if (AuthProvider.GITHUB.name().equalsIgnoreCase(authProviderID)) {
				ServiceUser glServiceUser = githubHelper.getServiceUser(accessToken, refreshToken);
				serviceUser = glServiceUser;
			}
			else if (AuthProvider.LINKEDIN.name().equalsIgnoreCase(authProviderID)) {
				ServiceUser liServiceUser = linkedInHelper.getServiceUser(accessToken, refreshToken);
				serviceUser = liServiceUser;
			}
			else if(AuthProvider.ANON.name().equalsIgnoreCase(authProviderID)){
				// We only allow anonymous if anonAuthEnabled is TRUE AND the anonAuthCode matches the refresh token.
				if (anonAuthEnabled && StringUtils.hasText(anonAuthCode)) {
					if (refreshToken != null && refreshToken.equals(anonAuthCode)) {
						serviceUser = new ServiceUser();
						serviceUser.setFirstName("ANON");
						serviceUser.setLastName("ANON");
						serviceUser.setId("ANON");
						serviceUser.setAuthProviderID(AuthProvider.ANON.toString());
						serviceUser.setRefreshToken(anonAuthCode);

						List<String> roles = new ArrayList<String>();
						String[] roleNames = anonAuthRoleNames != null ? anonAuthRoleNames.split(",") : new String[0];
						for(int i = 0; i < roleNames.length; i++) {
							if (roleNames[i] != null && !roleNames[i].isEmpty())
								roles.add(roleNames[i]);
						}
						serviceUser.setRoleNames(roles);
						serviceUser.setAreRoleNamesAccurate(true);
					}
				}
			}
			else {
				log.warn("ServiceProvider not yet supported: " + authProviderID);
			}
		} catch (Exception e) {
			log.error("Error getting Service User from Auth Provider", e);
		}

		if (serviceUser != null) {
			// In case the client has used a different casing of the Auth
			// Provider ID, we want to return the exact same casing.
			//	Ex: LinkedIn vs. linkedIn
			serviceUser.setAuthProviderID(authProviderID);
		}

		return serviceUser;
	}

	
	
	/**********************************************
	 * HELPER METHODS
	 **********************************************/
	private static String getRandomId() {
		UUID randomId = UUID.randomUUID();
		return randomId.toString();
	}
	
	
	/**********************************************
	 * CLEANUP
	 **********************************************/
	static final String DELETE_USER_TOKENS_COLLECTION_SQL = "DELETE FROM UserToken WHERE clientId IN (:clientIds)";

	private Set<String> previousInvalidClientIds = new HashSet<String>();
	
//	@SuppressWarnings("unchecked")
	/**
	 * This method checks for any rogue/ghost UserTokens and removes them.
	 */
//	@Scheduled(fixedRate=30000)	// 30 Seconds
	@Scheduled(fixedDelay=300000, initialDelay=120000)	// 5 Minutes, 2 Minutes
	private void cleanupUserTokens() {
//		Session s = null;
		try {
//			s = sessionFactoryAuth.openSession();

			int firstResultCounter = 0;
			int maxResults = 30;

			List<String> clientIdsToDelete = new LinkedList<String>();
			Map<String, String> clientDevicesMap = retrieveListOfClientDevicesFromUserTokens(maxResults, firstResultCounter);
			
//			String userTokenQueryString = "SELECT DISTINCT(ut.clientId), ut.deviceId FROM UserToken ut ORDER BY ut.clientId";
//			Query userTokenQuery = s.createQuery(userTokenQueryString);
//			userTokenQuery.setMaxResults(maxResults);
//			userTokenQuery.setFirstResult(firstResultCounter);
//
//			// Gather up all clientIds to remove, then delete at the end.
//			List<String> clientIdsToDelete = new LinkedList<String>();
//			List<Object[]> userTokenClientIds = userTokenQuery.list();
//			Map<String, String> clientDevicesMap = new HashMap<String, String>(userTokenClientIds.size());
//			if (userTokenClientIds != null) {
//				Iterator<Object[]> itrUserTokenClientIds = userTokenClientIds.iterator();
//				while (itrUserTokenClientIds.hasNext()) {
//					Object[] nextClientDevice = itrUserTokenClientIds.next();
//					if (nextClientDevice != null && nextClientDevice.length >= 2) {
//						clientDevicesMap.put((String)nextClientDevice[0], (String)nextClientDevice[1]);
//					}
//				}
//			}
//			
			while (clientDevicesMap != null && !clientDevicesMap.isEmpty()) {
				Set<String> validClients = accessManager.validateClientsIncludeFromDeviceHistory(clientDevicesMap);
				
				clientDevicesMap.keySet().removeAll(validClients);
				clientIdsToDelete.addAll(clientDevicesMap.keySet());
				
				// If countToDelete is greater than Max User Token Cleanup Count, execute delete and start again.
				if (!clientIdsToDelete.isEmpty()) {
					log.warn("Cleaning up " + clientIdsToDelete.size() + " client UserTokens");
					
					// Logout ALL of these Clients.
					Iterator<String> itrClientIdsToDelete = clientIdsToDelete.iterator();
					while (itrClientIdsToDelete.hasNext()) {
						String clientId = itrClientIdsToDelete.next();
						
						// Want to give each client a bit of time to login, so
						// pend the client Id to remove the first time, then
						// actually remove it the second time.
						if (previousInvalidClientIds.contains(clientId)) {
							accessManager.logoutClient(clientId, true);	// Force the deletion, since this client is no longer valid.
							previousInvalidClientIds.remove(clientId);
						}
						else {
							previousInvalidClientIds.add(clientId);
						}
					}
					
					clientIdsToDelete.clear();
// 					firstResultCounter = 0;
				}
//				else {
					firstResultCounter += maxResults;
//				}

				clientDevicesMap = retrieveListOfClientDevicesFromUserTokens(maxResults, firstResultCounter);
//				userTokenQuery.setFirstResult(firstResultCounter);
//				userTokenClientIds = userTokenQuery.list();
//				if (userTokenClientIds != null) {
//					clientDevicesMap = new HashMap<String, String>(userTokenClientIds.size());
//					Iterator<Object[]> itrUserTokenClientIds = userTokenClientIds.iterator();
//					if (userTokenClientIds != null) {
//						while (itrUserTokenClientIds.hasNext()) {
//							Object[] nextClientDevice = itrUserTokenClientIds.next();
//							if (nextClientDevice != null && nextClientDevice.length >= 2) {
//								clientDevicesMap.put((String)nextClientDevice[0], (String)nextClientDevice[1]);
//							}
//						}
//					}
//				}
//				else {
//					clientDevicesMap = null;
//				}
			}

//			if (clientIdsToDelete.size() > 0) {
//				log.warn("Cleaning up " + clientIdsToDelete.size() + " client UserTokens");
//				
//				// Logout ALL of these Clients.
//				Iterator<String> itrClientIdsToDelete = clientIdsToDelete.iterator();
//				while (itrClientIdsToDelete.hasNext()) {
//					String clientId = itrClientIdsToDelete.next();
//					accessManager.logoutClient(clientId, true);	// Force the deletion, since this client is no longer valid.
//				}
////				log.warn("Deleting " + clientIdsToDelete.size() + " client UserTokens");
////				log.debug( "Deleting Client IDs: " + StringUtils.arrayToCommaDelimitedString(clientIdsToDelete.toArray()) );
////				Transaction tx = s.beginTransaction();
////				tx.begin();
////				Query deleteQuery = s.createQuery(DELETE_USER_TOKENS_COLLECTION_SQL);
////				deleteQuery.setParameterList("clientIds", clientIdsToDelete);
////				deleteQuery.executeUpdate();
////				tx.commit();
//
//				clientIdsToDelete.clear();
//			}
		} catch (Exception e) {
			log.error("Unable to get cleanup UserTokens", e);
//		} finally {
//			if (s != null)
//				s.close();
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, String> retrieveListOfClientDevicesFromUserTokens(int maxResults, int firstResultCounter) {
		Map<String, String> result = null;

		Session s = null;
		try {
			s = sessionFactoryAuth.openSession();
			String userTokenQueryString = "SELECT DISTINCT(ut.clientId), ut.deviceId FROM UserToken ut ORDER BY ut.clientId";
			Query userTokenQuery = s.createQuery(userTokenQueryString);
			userTokenQuery.setMaxResults(maxResults);
			userTokenQuery.setFirstResult(firstResultCounter);
	
			// Gather up all clientIds to remove, then delete at the end.
			List<Object[]> userTokenClientIds = userTokenQuery.list();
			result = new HashMap<String, String>(userTokenClientIds.size());

			if (userTokenClientIds != null) {
				Iterator<Object[]> itrUserTokenClientIds = userTokenClientIds.iterator();
				while (itrUserTokenClientIds.hasNext()) {
					Object[] nextClientDevice = itrUserTokenClientIds.next();
					if (nextClientDevice != null && nextClientDevice.length >= 2) {
						result.put((String)nextClientDevice[0], (String)nextClientDevice[1]);
					}
				}
			}
		} catch (Exception e) {
			log.error("Unable to retrieve list of Client Devices from UserTokens", e);
		} finally {
			if (s != null)
				s.close();
		}
		
		return result;
	}
}

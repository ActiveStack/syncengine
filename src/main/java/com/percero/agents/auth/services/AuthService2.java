package com.percero.agents.auth.services;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.auth.helpers.RoleHelper;
import com.percero.agents.auth.helpers.UserAnchorHelper;
import com.percero.agents.auth.helpers.UserIdentifierHelper;
import com.percero.agents.auth.hibernate.AssociationExample;
import com.percero.agents.auth.hibernate.AuthHibernateUtils;
import com.percero.agents.auth.hibernate.BaseDataObjectPropertySelector;
import com.percero.agents.auth.vo.AuthProviderResponse;
import com.percero.agents.auth.vo.AuthenticationRequest;
import com.percero.agents.auth.vo.AuthenticationResponse;
import com.percero.agents.auth.vo.IUserAnchor;
import com.percero.agents.auth.vo.IUserIdentifier;
import com.percero.agents.auth.vo.IUserRole;
import com.percero.agents.auth.vo.ReauthenticationRequest;
import com.percero.agents.auth.vo.ServiceIdentifier;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.agents.auth.vo.User;
import com.percero.agents.auth.vo.UserAccount;
import com.percero.agents.auth.vo.UserIdentifier;
import com.percero.agents.auth.vo.UserToken;
import com.percero.agents.sync.exceptions.SyncDataException;
import com.percero.agents.sync.metadata.EntityImplementation;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.PropertyImplementation;
import com.percero.agents.sync.metadata.PropertyImplementationParam;
import com.percero.agents.sync.metadata.RelationshipImplementation;
import com.percero.agents.sync.services.ISyncAgentService;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.framework.bl.IManifest;
import com.percero.framework.bl.ManifestHelper;
import com.percero.framework.vo.IPerceroObject;

/**
 * This class handles AuthenticationRequest type authentication
 */
@Component
public class AuthService2 {

    private static Logger logger = Logger.getLogger(AuthService2.class);

    @Autowired
    AuthProviderRegistry authProviderRegistry;

    @Autowired
    SessionFactory sessionFactoryAuth;

    @Autowired
    ISyncAgentService syncAgentService;

    @Autowired
    IManifest manifest;
    
    
    /**
     * Pre Register Handlers are handlers that are called BEFORE `register`.
     * They allow for any custom hooks/logic that may need to be run before
     * register is called. If an AuthException is thrown, the register request
     * is cancelled.
     * 
     * @param preRegisterHandler
     */
    protected List<IPreRegisterHandler> preRegisterHandlers = new ArrayList<IPreRegisterHandler>();
    public void addPreRegisterHandler(IPreRegisterHandler preRegisterHandler) {
    	preRegisterHandlers.add(preRegisterHandler);
    }
    
    /**
     * Post Register Handlers are handlers that are called AFTER `register`.
     * They allow for any custom hooks/logic that may need to be run after
     * register is called.
     * 
     * @param postRegisterHandler
     */
    protected List<IPostRegisterHandler> postRegisterHandlers = new ArrayList<IPostRegisterHandler>();
    public void addPostRegisterHandler(IPostRegisterHandler postRegisterHandler) {
    	postRegisterHandlers.add(postRegisterHandler);
    }

    /**
     * Pre Authenticate Handlers are handlers that are called BEFORE `authenticate`.
     * They allow for any custom hooks/logic that may need to be run before
     * authenticate is called. If an AuthException is thrown, the authenticate request
     * is cancelled.
     * 
     * @param preAuthenticateHandler
     */
    protected List<IPreAuthenticateHandler> preAuthenticateHandlers = new ArrayList<IPreAuthenticateHandler>();
    public void addPreAuthenticateHandler(IPreAuthenticateHandler preAuthenticateHandler) {
    	preAuthenticateHandlers.add(preAuthenticateHandler);
    }
    
    /**
     * Post Authenticate Handlers are handlers that are called AFTER `authenticate`.
     * They allow for any custom hooks/logic that may need to be run after
     * authenticate is called.
     * 
     * @param postAuthenticateHandler
     */
    protected List<IPostAuthenticateHandler> postAuthenticateHandlers = new ArrayList<IPostAuthenticateHandler>();
    public void addPostAuthenticateHandler(IPostAuthenticateHandler postAuthenticateHandler) {
    	postAuthenticateHandlers.add(postAuthenticateHandler);
    }
    
    
    /**
     * Registers a new user with the selected AuthProvider (if supported by the AuthProvider).
     * @param request
     * @return
     * @throws IllegalArgumentException
     */
    public AuthenticationResponse register(AuthenticationRequest request) throws IllegalArgumentException{
    	if(!authProviderRegistry.hasProvider(request.getAuthProvider()))
    		throw new IllegalArgumentException(request.getAuthProvider()+" auth provider not found");
    	
    	AuthenticationResponse response = new AuthenticationResponse();
    	
    	// Before we register, we call any PreRegisterHandlers
    	if (preRegisterHandlers != null && !preRegisterHandlers.isEmpty()) {
    		for(IPreRegisterHandler preRegisterHandler : preRegisterHandlers) {
    			try {
    				preRegisterHandler.run(request);
    			} catch(AuthException ae) {
    				// Request should be cancelled.
    				response.setStatusCode(BasicAuthCode.FAILURE.getCode());
    				return response;
    			}
    		}
    	}
    	
    	IAuthProvider provider = authProviderRegistry.getProvider(request.getAuthProvider());
    	
    	AuthProviderResponse apResponse = provider.register(request.getCredential());
    	ServiceUser serviceUser = apResponse.serviceUser;
    	response.setStatusCode(apResponse.authCode.getCode());
    	response.setMessage(apResponse.authCode.getMessage());
    	
    	if(serviceUser != null) {
    		// Registration successful
    		logger.debug(provider.getID() + " Registration success");
    		serviceUser.setAuthProviderID(provider.getID()); // Set the provider ID just in case the provider didn't
    	}
    	else {
    		// Registration failed
    		logger.warn("REGISTRATION FAILED: (" + provider.getID() + "): Unable to retrieve valid Service User");
    		logger.warn("              ERROR: ("+response.getStatusCode()+") "+response.getMessage());
    	}
    	
    	// Before we send back the response, we call any PostRegisterHandlers
    	if (postRegisterHandlers != null && !postRegisterHandlers.isEmpty()) {
    		for(IPostRegisterHandler postRegisterHandler : postRegisterHandlers) {
    			try {
    				response = postRegisterHandler.run(request, response, serviceUser);
    			} catch(AuthException ae) {
    				// Since we are "post" register, we are not going to undo the register.
    			}
    		}
    	}
    	
    	return response;
    }
    
    public AuthenticationResponse authenticate(AuthenticationRequest request) throws IllegalArgumentException{
        if(!authProviderRegistry.hasProvider(request.getAuthProvider()))
            throw new IllegalArgumentException(request.getAuthProvider()+" auth provider not found");

        AuthenticationResponse response = new AuthenticationResponse();

    	// Before we authenticate, we call any PreAuthenticateHandlers
    	if (preAuthenticateHandlers != null && !preAuthenticateHandlers.isEmpty()) {
    		for(IPreAuthenticateHandler preAuthenticateHandler : preAuthenticateHandlers) {
    			try {
    				preAuthenticateHandler.run(request);
    			} catch(AuthException ae) {
    				// Authenticate should be cancelled.
    				response.setStatusCode(BasicAuthCode.FAILURE.getCode());
    				return response;
    			}
    		}
    	}
    	
        IAuthProvider provider = authProviderRegistry.getProvider(request.getAuthProvider());

        AuthProviderResponse apResponse = provider.authenticate(request.getCredential());
        ServiceUser serviceUser = apResponse.serviceUser;
        response.setStatusCode(apResponse.authCode.getCode());
        response.setMessage(apResponse.authCode.getMessage());

        // Login successful
        if(serviceUser != null) {
            logger.debug(provider.getID() + " Authentication success");
            serviceUser.setAuthProviderID(provider.getID()); // Set the provider ID just in case the provider didn't
            UserAccount userAccount = getOrCreateUserAccount(serviceUser, provider);
            ensureAnchorUserExists(serviceUser, userAccount.getUser());
            UserToken userToken = loginUserAccount(userAccount, request.getClientId(), request.getDeviceId());
            userToken = (UserToken) AuthHibernateUtils.cleanObject(userToken);
            response.setResult(userToken);
        }
        else {
        	logger.warn("LOGIN FAILED: (" + provider.getID() + "): Unable to retrieve valid Service User");
            logger.warn("       ERROR: ("+response.getStatusCode()+") "+response.getMessage());
        }
    	
    	// Before we send back the authenticate, we call any PostAuthenticateHandlers
    	if (postAuthenticateHandlers != null && !postAuthenticateHandlers.isEmpty()) {
    		for(IPostAuthenticateHandler postAuthenticateHandler : postAuthenticateHandlers) {
    			try {
    				response = postAuthenticateHandler.run(request, response, serviceUser);
    			} catch(AuthException ae) {
    				// Since we are "post" authenticate, we are not going to undo the authenticate.
    			}
    		}
    	}

        return response;
    }

    /**
     * Allows the user to re-login with the token stored on their device from a previous login
     * @param request
     * @return
     */
    public AuthenticationResponse reauthenticate(ReauthenticationRequest request){
        AuthenticationResponse response = new AuthenticationResponse();

        Session session = sessionFactoryAuth.getCurrentSession();
        UserToken userToken = (UserToken) session.createCriteria(UserToken.class)
                .add(Restrictions.eq("token", request.getToken()))
                .uniqueResult();

        // TODO: add expiration to the token
        if(userToken != null){
            userToken.setLastLogin(new Date());
            userToken.setClientId(request.getClientId());
            session.save(userToken);
            userToken = (UserToken) AuthHibernateUtils.cleanObject(userToken);
            response.setResult(userToken);
        }

        return response;
    }

    /**
     * Find a UserAccount from a ServiceUser
     * @param serviceUser
     * @return
     */
    private UserAccount findUserAccount(ServiceUser serviceUser) throws SyncDataException {
        UserAccount theFoundUserAccount = null;
        UserAccount theQueryObject = new UserAccount();
        theQueryObject.setAccountId(serviceUser.getId());
        theQueryObject.setAuthProviderID(serviceUser.getAuthProviderID());

        List<String> excludeProperties = new ArrayList<String>();
        excludeProperties.add("accessToken");
        excludeProperties.add("refreshToken");
        excludeProperties.add("isAdmin");
        excludeProperties.add("isSuspended");
        List userAccounts = findByExample(theQueryObject,
                excludeProperties);

        if ((userAccounts instanceof List)) {
        	List listUserAccounts = (List) userAccounts;
    		if (listUserAccounts.size() == 1) {
	            // Found a valid UserAccount.
	            theFoundUserAccount = (UserAccount) listUserAccounts.get(0);
    		}
    		else if (listUserAccounts.size() > 1) {
    			throw new SyncDataException(listUserAccounts.size() + " UserAccounts found for serviceUser " + serviceUser.getId(), 1001);
    		}
        }

        return theFoundUserAccount;
    }

    /**
     * Find a User from a ServiceUser
     * @param serviceUser
     * @return
     */
    @SuppressWarnings("unchecked")
	private User findUser(ServiceUser serviceUser) throws SyncDataException {
        Session s = sessionFactoryAuth.openSession();

        User theUser = null;

        // Attempt to find this user by finding a matching UserIdentifier.
        if (serviceUser.getIdentifiers() != null && serviceUser.getIdentifiers().size() > 0) {
            String strFindUserIdentifier = "SELECT u.ID, u.dateCreated, u.dateModified, u.firstName, u.lastName FROM UserIdentifier ui, User u WHERE";
            int counter = 0;
            for(ServiceIdentifier nextServiceIdentifier : serviceUser.getIdentifiers()) {
                if (counter > 0) {
                    strFindUserIdentifier += " OR ";
                }
                else {
                	strFindUserIdentifier += "(";
                }
                strFindUserIdentifier += " ui.type='" + nextServiceIdentifier.getParadigm() + "' AND ui.userIdentifier='" + nextServiceIdentifier.getValue() + "'";
                counter++;
            }
            if (counter > 0) {
            	strFindUserIdentifier += ") ";
            }
            strFindUserIdentifier += " AND u.ID=ui.user_ID GROUP BY u.ID";

            Query q = s.createSQLQuery(strFindUserIdentifier);
            List queryResult = null;
            try {
            	queryResult = q.list();
            } catch(Exception e) {
            	throw new SyncDataException(e);
            }
            if (queryResult.size() == 1) {
            	Object[] firstResult = (Object[]) queryResult.get(0);
            	theUser = new User();
            	theUser.setID((String) firstResult[0]);
            	theUser.setDateCreated(firstResult[1] != null ? (Date) firstResult[1] : null);
            	theUser.setDateModified(firstResult[2] != null ? (Date) firstResult[2] : null);
            	theUser.setFirstName(firstResult[3] != null ? (String) firstResult[3] : null);
            	theUser.setLastName(firstResult[4] != null ? (String) firstResult[4] : null);
            }
            else if (queryResult.size() > 1) {
            	throw new SyncDataException(queryResult.size() + " UserIdentifiers found for serviceUser " + serviceUser.getId(), 1001);
            }
        }

        return theUser;
    }

    /**
     * Does the work of synchronizing the ServiceUser (from 3rd party) with the UserAccounts
     * that ActiveStack knows about.
     * @param serviceUser
     * @returns UserAccount
     */
    @SuppressWarnings("unchecked")
	public UserAccount getOrCreateUserAccount(ServiceUser serviceUser, IAuthProvider provider){

        UserAccount theFoundUserAccount = null;
        Session s = null;
        try {
            theFoundUserAccount = findUserAccount(serviceUser);

            if(theFoundUserAccount == null) {
                s = sessionFactoryAuth.openSession();

                User theUser = findUser(serviceUser);

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
                theFoundUserAccount.setAccessToken(serviceUser.getAccessToken());
                theFoundUserAccount.setRefreshToken(serviceUser.getRefreshToken());
                theFoundUserAccount.setUser(theUser);
                theFoundUserAccount.setDateCreated(currentDate);
                theFoundUserAccount.setDateModified(currentDate);
                theFoundUserAccount.setAccountId(serviceUser.getId());

                s.save(theFoundUserAccount);
                tx.commit();

                s.close();
                s = sessionFactoryAuth.openSession();

                theFoundUserAccount = (UserAccount) s.get(UserAccount.class,
                        theFoundUserAccount.getID());
            }

            theFoundUserAccount = (UserAccount) AuthHibernateUtils
                    .cleanObject(theFoundUserAccount);

            // Now enter in the UserIdentifiers for this User.
            if (serviceUser.getIdentifiers() != null && serviceUser.getIdentifiers().size() > 0) {
                if (s == null)
                    s = sessionFactoryAuth.openSession();
                Transaction tx = s.beginTransaction();
                Query q;
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
                            logger.warn("Unable to save UserIdentifier for " + serviceUser.getName(), e);
                        }
                    }
                }
                tx.commit();
            }
        } catch (Exception e) {
            logger.error("Unable to run getOrCreateUserAccount", e);
        } finally {
            if (s != null)
                s.close();
        }
        return theFoundUserAccount;
    }

    @SuppressWarnings("rawtypes")
    protected UserToken loginUserAccount(UserAccount theUserAccount, String clientId, String deviceId) {
        Session s = null;
        UserToken theUserToken = null;
        try {
            Date currentDate = new Date();
            UserToken queryUserToken = new UserToken();
            queryUserToken.setUser(theUserAccount.getUser());
            queryUserToken.setClientId(clientId);
            List userTokenResult = findByExample(queryUserToken, null);

            if ( !userTokenResult.isEmpty() ) {
                theUserToken = (UserToken) userTokenResult.get(0);
            }

            if (s == null)
                s = sessionFactoryAuth.openSession();
            Transaction tx = s.beginTransaction();
            tx.begin();

            if (theUserToken == null) {
                if (StringUtils.hasText(deviceId)) {
                    // Need to delete all of UserTokens for this User/Device.
                    logger.debug("Deleting ALL UserToken's for User " + theUserAccount.getUser().getID() + ", Device " + deviceId);
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

        } catch (LockAcquisitionException lae) {
            logger.error("Unable to run authenticate UserAccount", lae);
        } catch (Exception e) {
            logger.error("Unable to run authenticate UserAccount", e);
        } finally {
            if (s != null)
                s.close();
        }

        return theUserToken;
    }

    @SuppressWarnings("rawtypes")
    protected List findByExample(Object theQueryObject,
                                 List<String> excludeProperties) {
        Session s = null;
        try {
            s = sessionFactoryAuth.openSession();
            Criteria criteria = s.createCriteria(theQueryObject.getClass());
            AssociationExample example = AssociationExample
                    .create(theQueryObject);
            BaseDataObjectPropertySelector propertySelector = new BaseDataObjectPropertySelector(
                    excludeProperties);
            example.setPropertySelector(propertySelector);
            criteria.add(example);

            List result = criteria.list();
            return (List) AuthHibernateUtils.cleanObject(result);
        } catch (Exception e) {
            logger.error("Unable to findByExample", e);
        } finally {
            if (s != null)
                s.close();
        }
        return null;
    }

    private static String getRandomId() {
        UUID randomId = UUID.randomUUID();
        return randomId.toString();
    }

    public void ensureAnchorUserExists(ServiceUser serviceUser, User user){
        IUserAnchor result = UserAnchorHelper.getUserAnchor(user.getID());

        if (result == null)
            addOrUpdateUserAnchorFromServiceUserList(serviceUser, user, null);
        else {
            handleUserAnchorFound(serviceUser, user, result);
        }
    }

    private EntityImplementation getUserAnchorEntityImplementation(){
        ManifestHelper.setManifest(manifest);
        EntityImplementation userAnchorEI = null;
        List<EntityImplementation> userAnchorMappedClasses = MappedClass.findRootEntityImplementation(IUserAnchor.class);
        if (userAnchorMappedClasses.size() > 0) {
            userAnchorEI = userAnchorMappedClasses.get(0);
        }
        else{
            throw new ServiceConfigurationError("UserAnchor implementation not found");
        }

        return userAnchorEI;
    }

    private EntityImplementation getUserRoleEntityImplementation(){
        ManifestHelper.setManifest(manifest);
        EntityImplementation userRoleEI = null;
        List<EntityImplementation> userRoleMappedClasses = MappedClass.findRootEntityImplementation(IUserRole.class);
        if (userRoleMappedClasses.size() > 0) {
            userRoleEI = userRoleMappedClasses.get(0);
        }
        else{
            throw new ServiceConfigurationError("UserAnchor implementation not found");
        }

        return userRoleEI;
    }



    protected void handleUserAnchorFound(ServiceUser serviceUser, User user, IUserAnchor userAnchor) {
        if(!(userAnchor instanceof IPerceroObject))
            throw new ServiceConfigurationError("IUserAnchor not a IPerceroObject");

        try {
            // Attempt to get updated information from ServiceProvider.
            EntityImplementation userAnchorEI = getUserAnchorEntityImplementation();

            PropertyImplementation firstNamePropImpl = userAnchorEI.findPropertyImplementationByName(IUserAnchor.FIRST_NAME_FIELD);
            PropertyImplementation lastNamePropImpl = userAnchorEI.findPropertyImplementationByName(IUserAnchor.LAST_NAME_FIELD);
            //MappedField firstNameField = userAnchorEI.mappedClass.getExternalizeFieldByName("firstName");
            //MappedField lastNameField = userAnchorappedClass.getExternalizeFieldByName("lastName");
            String firstName = "";
            String lastName = "";

            if (firstNamePropImpl != null)
                firstName = (String) firstNamePropImpl.mappedField.getGetter().invoke(userAnchor);
            if (lastNamePropImpl != null)
                lastName = (String) lastNamePropImpl.mappedField.getGetter().invoke(userAnchor);

            boolean userAnchorUpdated = false;
            if (StringUtils.hasText(serviceUser.getFirstName()) && firstNamePropImpl != null) {
                if (serviceUser.getFirstName() != null && (firstName == null || !firstName.equals(serviceUser.getFirstName()))) {
                    firstName = serviceUser.getFirstName();
                    firstNamePropImpl.mappedField.getSetter().invoke(userAnchor, firstName);
                    userAnchorUpdated = true;
                }
            }
            if (StringUtils.hasText(serviceUser.getLastName()) && lastNamePropImpl != null) {
                if (serviceUser.getLastName() != null && (lastName == null || !lastName.equals(serviceUser.getLastName()))) {
                    lastName = serviceUser.getLastName();
                    lastNamePropImpl.mappedField.getSetter().invoke(userAnchor, lastName);
                    userAnchorUpdated = true;
                }
            }

            if (userAnchorUpdated) {
                syncAgentService.systemPutObject((IPerceroObject) userAnchor, null, new Date(), null, true);
            }
        } catch(Exception e) {
            logger.warn("Unable to get ServiceUser information for First/Last Name", e);
        }

        addOrUpdateUserAnchorFromServiceUserList(serviceUser, user, userAnchor);
    }

    protected IUserAnchor addOrUpdateUserAnchorFromServiceUserList(ServiceUser serviceUser, User user, IUserAnchor result) {
        try {
            EntityImplementation eiUserAnchor = getUserAnchorEntityImplementation();

            List<EntityImplementation> userIdentifierEntityImplementations = MappedClass.findRootEntityImplementation(IUserIdentifier.class);

            if (userIdentifierEntityImplementations.size() > 0) {
                List<IUserIdentifier> identifiersToSave = new ArrayList<IUserIdentifier>();

                Iterator<EntityImplementation> itrUserIdentifierEntityImplementations = userIdentifierEntityImplementations.iterator();
                while (itrUserIdentifierEntityImplementations.hasNext()) {
                    EntityImplementation userIdentifierEntityImplementation = itrUserIdentifierEntityImplementations.next();

                    PropertyImplementation userIdentifierPropImpl = userIdentifierEntityImplementation.findPropertyImplementationByName(IUserIdentifier.USER_IDENTIFIER_FIELD_NAME);
                    RelationshipImplementation userAnchorRelImpl = userIdentifierEntityImplementation.findRelationshipImplementationBySourceVarName(IUserIdentifier.USER_ANCHOR_FIELD_NAME);

                    for (ServiceIdentifier nextIdentifier : serviceUser.getIdentifiers()) {
                        try {
                            // Make sure this Identifier is in the same paradigm.
                            String paradigm = null;
                            Iterator<PropertyImplementationParam> itrParams = userIdentifierPropImpl.params.iterator();
                            while (itrParams.hasNext()) {
                                PropertyImplementationParam nextParam = itrParams.next();
                                if (nextParam.name.equalsIgnoreCase(IUserIdentifier.PARADIGM_PARAM_NAME)) {
                                    paradigm = nextParam.value;
                                    break;
                                }
                            }
                            if (nextIdentifier.getParadigm() == null || !nextIdentifier.getParadigm().equalsIgnoreCase(paradigm)) {
                                continue;
                            }

                            // Look for this existing identifier.
                            IUserIdentifier foundIdentifier = UserIdentifierHelper.getUserIdentifierForUserAndValue(userIdentifierEntityImplementation, result, nextIdentifier.getValue());


                            if (foundIdentifier == null) {
                                // Identifier.identifierValue will be set later, once that is determined.
                                IUserIdentifier newIdentifier = (IUserIdentifier) userIdentifierEntityImplementation.mappedClass.clazz.newInstance();
										/*
										IUserIdentifierA userIdentifierA = null;
										Class nextClass = newIdentifier.getClass();
										while(nextClass != null) {
											userIdentifierA = (IUserIdentifierA) nextClass.getAnnotation(IUserIdentifierA.class);
											if (userIdentifierA != null)
												break;
											else
												nextClass = nextClass.getSuperclass();
										}
										IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
										MappedClass mappedClass = mcm.getMappedClassByClassName(userIdentifierEntityImplementation.mappedClass.clazz.getCanonicalName());
										MappedField mappedField = mappedClass.getExternalizeFieldByName(userIdentifierA.userIdentifierFieldName());
										mappedField.getSetter().invoke(newIdentifier, nextIdentifier.getValue());
										*/
                                userIdentifierPropImpl.mappedField.getSetter().invoke(newIdentifier, nextIdentifier.getValue());
                                identifiersToSave.add(newIdentifier);
                            }
                        } catch(NonUniqueResultException nure) {
                            logger.warn("Non-unique User Identifier: " + nextIdentifier, nure);
                        }
                    }

                }

                // Need to create a new IUserAnchor
                if (result == null) {
                    result = (IUserAnchor) eiUserAnchor.mappedClass.clazz.newInstance();
                    ((IPerceroObject) result).setID(UUID.randomUUID().toString());
                    result.setUserId(user.getID());

                    // Find field that has firstName and lastName PropertyImplementations.
                    PropertyImplementation firstNamePropImpl = eiUserAnchor.findPropertyImplementationByName(IUserAnchor.FIRST_NAME_FIELD);
                    PropertyImplementation lastNamePropImpl = eiUserAnchor.findPropertyImplementationByName(IUserAnchor.LAST_NAME_FIELD);

                    if (firstNamePropImpl != null)
                        firstNamePropImpl.mappedField.getSetter().invoke(result, serviceUser.getFirstName());
                    if (lastNamePropImpl != null)
                        lastNamePropImpl.mappedField.getSetter().invoke(result, serviceUser.getLastName());


                    syncAgentService.systemCreateObject((IPerceroObject) result, null);

                }
                else if (result.getUserId() == null || result.getUserId().isEmpty()) {
                    result.setUserId(user.getID());

                    syncAgentService.systemPutObject((IPerceroObject) result, null, null, null, true);
                }

                for (IUserIdentifier nextUserIdentifier : identifiersToSave) {
                    // Set Email.person now that we have a firm handle on that Person object.
                    Boolean isNewObject = false;
                    if (((IPerceroObject)nextUserIdentifier).getID() == null || ((IPerceroObject)nextUserIdentifier).getID().isEmpty()) {
                        isNewObject = true;
                        ((IPerceroObject)nextUserIdentifier).setID(UUID.randomUUID().toString());
                    }

                    IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
                    MappedClass mc = mcm.getMappedClassByClassName(nextUserIdentifier.getClass().getName());
                    EntityImplementation entityImpl = mc.entityImplementations.get(IUserIdentifier.class);
                    if (entityImpl != null) {
                        RelationshipImplementation relImpl = entityImpl.findRelationshipImplementationBySourceVarName(IUserIdentifier.USER_ANCHOR_FIELD_NAME);
                        if (relImpl != null) {
                            relImpl.sourceMappedField.getSetter().invoke(nextUserIdentifier, result);
                            if (isNewObject) {
                                syncAgentService.systemCreateObject((IPerceroObject) nextUserIdentifier, null);
                            }
                            else {
                                syncAgentService.systemPutObject((IPerceroObject) nextUserIdentifier, null, null, null, true);
                            }
                        }
                        else {
                            logger.warn("No UserAnchor found for IUserIdentifier class " + nextUserIdentifier.getClass().getCanonicalName());
                        }
                    }
                }

                if (result != null) {
                    setupUserRoles(user, serviceUser);
                    return result;
                }
            }

        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    @SuppressWarnings({ "unchecked" })
    public void setupUserRoles(User user, ServiceUser serviceUser) throws Exception {
//        Session s = appSessionFactory.openSession();

        try {
            EntityImplementation userRoleEI = getUserRoleEntityImplementation();

            RelationshipImplementation userAnchorRelImpl = RoleHelper.getUserAnchorRI();

            IUserAnchor userAnchor = UserAnchorHelper.getUserAnchor(user.getID());

            List<IUserRole> userRoles = RoleHelper.getUserRoles(userAnchor);
            List<IUserRole> updatedUserRoles = new ArrayList<IUserRole>();

            // First, remove all roles that a Person has that are not in the serviceUserList.
            for(IUserRole nextUserRole : userRoles) {
                Boolean serviceUserRoleExists = false;
                Boolean isInaccurateList = false;
                if (!serviceUser.getAreRoleNamesAccurate()) {
                    logger.debug("Ignoring role names from " + serviceUser.getAuthProviderID().toString());
                    isInaccurateList = true;
                }
                else if (serviceUser.getRoleNames().contains(nextUserRole.getRoleName())) {
                    serviceUserRoleExists = true;
                }


                if (!isInaccurateList && !serviceUserRoleExists) {
                    logger.warn("Deleting role " + nextUserRole.getRoleName() + " for " + user.getID());
                    syncAgentService.systemDeleteObject(BaseDataObject.toClassIdPair(nextUserRole), null, true);
                } else
                    updatedUserRoles.add(nextUserRole);
            }

            // Second, add all roles that are not on the Person.
            if (serviceUser.getAreRoleNamesAccurate()) {
                // Role names from the service are accurate, so we can match them up here.
                for(String nextServiceRoleName : serviceUser.getRoleNames()) {
                    Boolean personRoleExists = false;
                    for(IUserRole nextUserRole : updatedUserRoles) {
                        if (nextUserRole.getRoleName().equalsIgnoreCase(nextServiceRoleName)) {
                            personRoleExists = true;
                            break;
                        }
                    }

                    if (!personRoleExists) {
                        IUserRole nextPersonRole = (IUserRole) userRoleEI.mappedClass.clazz.newInstance();
                        userAnchorRelImpl.sourceMappedField.getSetter().invoke(nextPersonRole, userAnchor);

                        nextPersonRole.setRoleName(nextServiceRoleName);
                        nextPersonRole.setID(UUID.randomUUID().toString());

                        syncAgentService.systemCreateObject(nextPersonRole, null);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Unable to Get Person Roles", e);
        }
    }
}

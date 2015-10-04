package com.percero.agents.auth.helpers;

import com.percero.agents.auth.principal.PrincipalUser;
import com.percero.agents.auth.services.IAuthService;
import com.percero.agents.auth.vo.*;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.dao.DAORegistry;
import com.percero.agents.sync.dao.IDataAccessObject;
import com.percero.agents.sync.helpers.PostCreateHelper;
import com.percero.agents.sync.helpers.PostDeleteHelper;
import com.percero.agents.sync.helpers.PostPutHelper;
import com.percero.agents.sync.hibernate.SyncHibernateUtils;
import com.percero.agents.sync.metadata.*;
import com.percero.agents.sync.services.IDataProviderManager;
import com.percero.agents.sync.services.ISyncAgentService;
import com.percero.agents.sync.vo.Client;
import com.percero.framework.bl.IManifest;
import com.percero.framework.bl.ManifestHelper;
import com.percero.framework.vo.IPerceroObject;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.*;

@Component
public class AccountHelper implements IAccountHelper {

    private static final Logger log = Logger.getLogger(AccountHelper.class);

    public AccountHelper() {
    }

    @Autowired
    protected IManifest manifest = null;
    public void setManifest(IManifest value) {
        manifest = value;
    }

    @Autowired
    protected IAuthService authService;
    public void setAuthService(IAuthService value) {
        authService = value;
    }

    @Autowired
    protected IAccessManager accessManager;
    public void setAccessManager(IAccessManager value) {
        accessManager = value;
    }

    @Autowired
    protected SessionFactory appSessionFactory;
    public void setAppSessionFactory(SessionFactory value) {
        appSessionFactory = value;
    }

    @Autowired
    protected ISyncAgentService syncAgentService;
    public void setSyncAgentService(ISyncAgentService value) {
        syncAgentService = value;
    }

    @Autowired
    protected PostPutHelper postPutHelper;
    public void setPostPutHelper(PostPutHelper value) {
        postPutHelper = value;
    }

    @Autowired
    protected IDataProviderManager dataProviderManager;
    public void setDataProviderManager(IDataProviderManager value) {
        dataProviderManager = value;
    }

    @Autowired
    protected PostCreateHelper postCreateHelper;
    public void setPostCreateHelper(PostCreateHelper value) {
        postCreateHelper = value;
    }
    @Autowired
    protected PostDeleteHelper postDeleteHelper;
    public void setPostDeleteHelper(PostDeleteHelper value) {
        postDeleteHelper = value;
    }

    public Principal authenticateOAuth(String regAppKey, String svcOAuthKey, String userId, String userToken, String clientId, String clientType, String deviceId) {
        return authenticateOAuth(regAppKey, svcOAuthKey, userId, userToken, clientId, clientType, deviceId, null);
    }

    public Principal authenticateOAuth(String regAppKey, String svcOAuthKey, String userId, String userToken, String clientId, String clientType, String deviceId, Set<String> existingClientIds) {
        if (!StringUtils.hasText(clientType)) {
            clientType = Client.NON_PERSISTENT_TYPE;
        }

        // Only worry about existing client if it is set and NOT equal to the "new" client Id.
        Boolean isExistingClient = ( existingClientIds != null && !existingClientIds.isEmpty() );
        boolean validated = false;
        try {
            if (isExistingClient) {
                validated = authService.validateUserByToken(regAppKey, userId, userToken, clientId, existingClientIds);
            } else {
                validated = authService.validateUserByToken(regAppKey, userId, userToken, clientId);
            }
            log.debug("ValidateUserByToken Result: " + userId +  " / " + userToken + " / " + clientId + " = " + (validated ? "true" : "false"));
        } catch (Exception e) {
            log.error("Error validating User by Token", e);
            e.printStackTrace();
            validated = false;
        }

        if (validated) {
            try {
                // Make sure a valid Person exists for this User.
                IUserAnchor userAnchor = validateUser(regAppKey, userId, authService);

                if (userAnchor == null) {
                    // Something went wrong here.
                    throw new Error("Invalid UserAnchor object.");
                }

                List<String> roleList = getUserRoles(userId);

                String[] groups = (String[]) roleList.toArray(new String[0]);
                PrincipalUser theUser = new PrincipalUser(userId, userId, userToken, clientType, clientId, groups);

                Boolean foundValidClient = false;
                if (StringUtils.hasText(deviceId))
                {
                    if (isExistingClient) {
                        Iterator<String> itrExistingClientIds = existingClientIds.iterator();
                        while (itrExistingClientIds.hasNext()) {
                            String existingClientId = itrExistingClientIds.next();
                            if (!existingClientId.equals(clientId)) {
                                // Need to move the existing client to the new client.
                                log.debug("Renaming client " + existingClientId + " to " + clientId);
                                accessManager.renameClient(existingClientId, clientId);
                            }
                        }
                    }
                }

                if (!foundValidClient)
                    foundValidClient = accessManager.findClientByClientIdUserId(clientId, userId);

                if (!foundValidClient) {
                    // Unable to find a valid client, so need to create one.
                    accessManager.createClient(clientId, userId, clientType, deviceId);
                }

                return theUser;
            } catch(Exception e) {
                log.error("Error getting Roles", e);
                e.printStackTrace();
            }
        }

        log.debug("authenticateOAuth: Returning null result");
        return null;
    }

    public IUserAnchor validateUser(String regAppKey, String userId, IAuthService authService) throws HibernateException {
        IUserAnchor result = null;
        Session s = appSessionFactory.openSession();
        try {
            ManifestHelper.setManifest(manifest);
            //Class userAnchorClass = ManifestHelper.findImplementingClass(IUserAnchor.class);
            EntityImplementation userAnchorEI = null;
            List<EntityImplementation> userAnchorMappedClasses = MappedClass.findEntityImplementation(IUserAnchor.class);
            if (userAnchorMappedClasses.size() > 0) {
                userAnchorEI = userAnchorMappedClasses.get(0);
            }

            if (userAnchorEI != null) {
                //IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
                MappedClass mc = userAnchorEI.mappedClass;
                if (mc != null) {
                    IUserAnchor foundUserAnchor = null;
                    try {
                        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(mc.className);
                        Class userAnchorClass = Class.forName(mc.className);
                        IUserAnchor example = (IUserAnchor) userAnchorClass.newInstance();
                        example.setUserId(userId);
                        List<IPerceroObject> list = dao.findByExample((IPerceroObject)example, null, null, false);
                        if(list.size() > 0)
                            foundUserAnchor = (IUserAnchor) list.get(0);
//                        String userAnchorQueryString = "SELECT ua FROM " + mc.tableName + " ua WHERE ua.userId=:userId";
//                        Query userAnchorQuery = s.createQuery(userAnchorQueryString);
//                        userAnchorQuery.setString("userId", userId);
//                        IUserAnchor foundUserAnchor = (IUserAnchor) userAnchorQuery.uniqueResult();
                    }catch(Exception e){}

                    if (foundUserAnchor == null) {
                        result = handleUserAnchorNotFound(regAppKey, userId, authService);
                    } else {
                        handleUserAnchorFound(regAppKey, userId, authService, foundUserAnchor);
                        result = foundUserAnchor;
                    }
                }
            }
        } catch (NonUniqueResultException nre) {
            log.error("More than one UserAnchor objects found for userId " + userId, nre);
        } finally {
            if (s != null && s.isOpen())
                s.close();
        }
        return result;
    }

    protected IUserAnchor handleUserAnchorNotFound(String regAppKey, String userId, IAuthService authService) {
        // Get this person's email addresses from the AuthManager.
        List<ServiceUser> serviceUserList = authService.getServiceUsers(userId);
        IUserAnchor result = addOrUpdateUserAnchorFromServiceUserList(userId, serviceUserList, null);
        //setupUserRoles(userId, serviceUserList);

        return result;
    }

    protected void handleUserAnchorFound(String regAppKey, String userId, IAuthService authService, IUserAnchor userAnchor) {
        // Get this person's email addresses from the AuthManager.
        List<ServiceUser> serviceUserList = authService.getServiceUsers(userId);

        if (userAnchor != null && (userAnchor instanceof IPerceroObject)) {
            try {
                // Attempt to get updated information from ServiceProvider.
                EntityImplementation userAnchorEI = null;
                List<EntityImplementation> userAnchorMappedClasses = MappedClass.findEntityImplementation(IUserAnchor.class);
                if (userAnchorMappedClasses.size() > 0) {
                    userAnchorEI = userAnchorMappedClasses.get(0);
                }

                PropertyImplementation firstNamePropImpl = userAnchorEI.findPropertyImplementationByName(IUserAnchor.FIRST_NAME_FIELD);
                PropertyImplementation lastNamePropImpl = userAnchorEI.findPropertyImplementationByName(IUserAnchor.LAST_NAME_FIELD);
                //MappedField firstNameField = userAnchorEI.mappedClass.getExternalizeFieldByName("firstName");
                //MappedField lastNameField = userAnchorappedClass.getExternalizeFieldByName("lastName");
                String firstName = "";
                String lastName = "";

                if (firstNamePropImpl != null || lastNamePropImpl != null) {
                    if (firstNamePropImpl != null)
                        firstName = (String) firstNamePropImpl.mappedField.getGetter().invoke(userAnchor);
                    if (lastNamePropImpl != null)
                        lastName = (String) lastNamePropImpl.mappedField.getGetter().invoke(userAnchor);

                    boolean userAnchorUpdated = false;
                    for (ServiceUser nextServiceUser : serviceUserList) {
                        if (StringUtils.hasText(nextServiceUser.getFirstName()) && firstNamePropImpl != null) {
                            if (firstName == null || !firstName.equals(nextServiceUser.getFirstName())) {
                                firstName = nextServiceUser.getFirstName();
                                firstNamePropImpl.mappedField.getSetter().invoke(userAnchor, firstName);
                                userAnchorUpdated = true;
                            }
                        }
                        if (StringUtils.hasText(nextServiceUser.getLastName()) && lastNamePropImpl != null) {
                            if (lastName == null || !lastName.equals(nextServiceUser.getLastName())) {
                                lastName = nextServiceUser.getLastName();
                                lastNamePropImpl.mappedField.getSetter().invoke(userAnchor, lastName);
                                userAnchorUpdated = true;
                            }
                        }
                    }

                    if (userAnchorUpdated) {
                        syncAgentService.systemPutObject((IPerceroObject) userAnchor, null, new Date(), null, true);
                    }
                }
            } catch(Exception e) {
                log.warn("Unable to get ServiceUser information for First/Last Name", e);
            }
        }

        //setupUserRoles(userId, serviceUserList);
        addOrUpdateUserAnchorFromServiceUserList(userId, serviceUserList, userAnchor);
    }

//	public Object validateServiceUser(String regAppKey, String svcOauthKey, ServiceUser theServiceUser, IAuthService theAuthService) throws Exception {
//		try {
//			// Attempt to get the ServiceUser from the AuthService.
//			ServiceProvider theServiceProvider = new ServiceProvider();
//			theServiceProvider.setID(theServiceUser.getServiceProviderId());
//			UserAccount theUserAccount = new UserAccount();
//			theUserAccount.setAccountId(theServiceUser.getId());
//			theUserAccount.setServiceProvider(theServiceProvider);
//			
//			Object authenticateUserAccountResult = theAuthService.authenticateUserAccount(regAppKey, svcOauthKey, theUserAccount, "", null, true);
//			
//			if (authenticateUserAccountResult instanceof UserToken) {
//				// User either found or created (don't care which at this point).
//				UserToken theUserToken = (UserToken) authenticateUserAccountResult;
//				return validateUser(regAppKey, theUserToken.getUser().getID(), theAuthService);
//			}
//		} catch (Exception e) {
//			log.error("Unable to Validate Service User", e);
//			return Boolean.FALSE;
//		}
//		
//		return Boolean.FALSE;
//	}

    /* (non-Javadoc)
     * @see com.com.percero.agents.auth.helpers.IAccountHelper#getUserRoles(java.lang.String)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<String> getUserRoles(String userId) throws Exception {

        ManifestHelper.setManifest(manifest);
        IUserAnchor userAnchor = UserAnchorHelper.getUserAnchor(userId);
        List<String> result = RoleHelper.getUserRoleNames(userAnchor);

        return result;
    }

    /**
     @SuppressWarnings({ "rawtypes", "unchecked" })
     protected IUserRoleA getUserRoleAnnotation(Class userRoleClass) {
     IUserRoleA userRoleAnnotation = null;
     Class nextClazz = userRoleClass;
     while(userRoleAnnotation == null && nextClazz != null) {
     userRoleAnnotation = (IUserRoleA) nextClazz.getAnnotation(IUserRoleA.class);
     if (userRoleAnnotation == null)
     nextClazz = nextClazz.getSuperclass();
     }
     return userRoleAnnotation;
     }

     @SuppressWarnings({ "rawtypes", "unchecked" })
     protected IUserIdentifierA getUserIdentifierAnnotation(Class userIdentifierClass) {
     IUserIdentifierA userIdentifierAnnotation = null;
     Class nextClazz = userIdentifierClass;
     while(userIdentifierAnnotation == null && nextClazz != null) {
     userIdentifierAnnotation = (IUserIdentifierA) nextClazz.getAnnotation(IUserIdentifierA.class);
     if (userIdentifierAnnotation == null)
     nextClazz = nextClazz.getSuperclass();
     }
     return userIdentifierAnnotation;
     }

     @SuppressWarnings({ "rawtypes", "unchecked" })
     protected IUserAnchorA getUserAnchorAnnotation(Class userAnchorClass) {
     IUserAnchorA userAnchorAnnotation = null;
     Class nextClazz = userAnchorClass;
     while(userAnchorAnnotation == null && nextClazz != null) {
     userAnchorAnnotation = (IUserAnchorA) nextClazz.getAnnotation(IUserAnchorA.class);
     if (userAnchorAnnotation == null)
     nextClazz = nextClazz.getSuperclass();
     }
     return userAnchorAnnotation;
     }

     @SuppressWarnings({ "rawtypes" })
     protected Field getUserAnchorField(Class entityInterfaceClass, String fieldName) {
     Class nextClazz = entityInterfaceClass;
     while(nextClazz != null) {
     List<Field> fields = SyncHibernateUtils.getClassFields(nextClazz);
     Iterator<Field> itrFields = fields.iterator();
     while (itrFields.hasNext()) {
     Field nextField = itrFields.next();
     PropertyInterfaces nextEntityInterfacePropertiesA = nextField.getAnnotation(PropertyInterfaces.class);

     if (nextEntityInterfacePropertiesA != null && nextEntityInterfacePropertiesA.entityInterfaceClass() == entityInterfaceClass) {
     PropertyInterface[] propInterfaces = nextEntityInterfacePropertiesA.propertyInterfaces();
     for(PropertyInterface nextPropInterface : propInterfaces) {
     if (nextPropInterface.propertyName() != null && nextPropInterface.propertyName().equals(fieldName)) {
     // We have found the appropriate field.
     return nextField;
     }
     }
     }
     }
     nextClazz = nextClazz.getSuperclass();
     }

     return null;
     }

     @SuppressWarnings({ "rawtypes" })
     protected PropertyInterface getEntityPropertyInterfaceAnnotation(Class entityInterfaceClass, String fieldName) {
     Class nextClazz = entityInterfaceClass;
     while(nextClazz != null) {
     List<Field> fields = SyncHibernateUtils.getClassFields(nextClazz);
     Iterator<Field> itrFields = fields.iterator();
     while (itrFields.hasNext()) {
     Field nextField = itrFields.next();
     PropertyInterfaces nextEntityInterfacePropertiesA = nextField.getAnnotation(PropertyInterfaces.class);

     if (nextEntityInterfacePropertiesA != null && nextEntityInterfacePropertiesA.entityInterfaceClass() == entityInterfaceClass) {
     PropertyInterface[] propInterfaces = nextEntityInterfacePropertiesA.propertyInterfaces();
     for(PropertyInterface nextPropInterface : propInterfaces) {
     if (nextPropInterface.propertyName() != null && nextPropInterface.propertyName().equals(fieldName)) {
     // We have found the appropriate field.
     return nextPropInterface;
     }
     }
     }
     }
     nextClazz = nextClazz.getSuperclass();
     }

     return null;
     }*/

    protected IUserAnchor addOrUpdateUserAnchorFromServiceUserList(String userId, List<ServiceUser> serviceUserList, IUserAnchor result) {
        Session s = null;
        try {
            ManifestHelper.setManifest(manifest);
            //Set<Class> userIdentifierClasses = ManifestHelper.findImplementingClasses(IUserIdentifier.class);
            //Class userAnchorClass = ManifestHelper.findImplementingClass(IUserAnchor.class);
            EntityImplementation eiUserAnchor = null;
            List<EntityImplementation> userAnchorEntityImplementations = MappedClass.findEntityImplementation(IUserAnchor.class);
            if (userAnchorEntityImplementations.size() > 0) {
                eiUserAnchor = userAnchorEntityImplementations.get(0);
            }
            List<EntityImplementation> userIdentifierEntityImplementations = MappedClass.findEntityImplementation(IUserIdentifier.class);

            if (userIdentifierEntityImplementations.size() > 0) {
                List<IUserIdentifier> identifiersToSave = new ArrayList<IUserIdentifier>();

                Iterator<EntityImplementation> itrUserIdentifierEntityImplementations = userIdentifierEntityImplementations.iterator();
                while (itrUserIdentifierEntityImplementations.hasNext()) {
                    EntityImplementation userIdentifierEntityImplementation = itrUserIdentifierEntityImplementations.next();

                    //MappedField userAnchorMappedField = userIdentifierEntityImplementation.getMappedFieldByName(userIdentifierAnnotation.userAnchorFieldName());
                    PropertyImplementation userIdentifierPropImpl = userIdentifierEntityImplementation.findPropertyImplementationByName(IUserIdentifier.USER_IDENTIFIER_FIELD_NAME);
                    RelationshipImplementation userAnchorRelImpl = userIdentifierEntityImplementation.findRelationshipImplementationBySourceVarName(IUserIdentifier.USER_ANCHOR_FIELD_NAME);
                    //IUserIdentifierA userIdentifierAnnotation = getUserIdentifierAnnotation(userIdentifierEntityImplementation);
                    if (userAnchorRelImpl != null) {

                        if (s == null) {
                            s = appSessionFactory.openSession();
                        }

                        // Get this userAnchor's identifier(s).
                        String userIdentifierQueryString = "SELECT ui FROM " + userIdentifierEntityImplementation.mappedClass.tableName
                                + " ui WHERE ui." + userIdentifierPropImpl.mappedField.getField().getName() + "=:value AND (ui." +
                                userAnchorRelImpl.sourceMappedField.getField().getName() + " IS NULL OR ui." + userAnchorRelImpl.sourceMappedField.getField().getName() +
                                " IN (SELECT ua FROM " + eiUserAnchor.mappedClass.tableName + " ua WHERE (ua.userId=null OR ua.userId='' OR ua.userId=:userId)))";
                        for (ServiceUser nextServiceUser : serviceUserList) {
                            for (ServiceIdentifier nextIdentifier : nextServiceUser.getIdentifiers()) {
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
                                    Query userIdentifierQuery = s.createQuery(userIdentifierQueryString);
                                    userIdentifierQuery.setString("userId", userId);
                                    userIdentifierQuery.setString("value", nextIdentifier.getValue());
                                    IUserIdentifier foundIdentifier = (IUserIdentifier) userIdentifierQuery.uniqueResult();

                                    if (foundIdentifier != null) {
                                        // If the email does not have a Person, then associate email with this Person.
                                        foundIdentifier = (IUserIdentifier) SyncHibernateUtils.cleanObject(foundIdentifier, s);
                                        IUserAnchor existingUserAnchor = (IUserAnchor) SyncHibernateUtils.cleanObject(userAnchorRelImpl.sourceMappedField.getGetter().invoke(foundIdentifier), s);
                                        if (existingUserAnchor == null) {
                                            if (result != null) {
                                                userAnchorRelImpl.sourceMappedField.getSetter().invoke(foundIdentifier, result);
                                                syncAgentService.systemPutObject((IPerceroObject) foundIdentifier, null, null, null, true);
                                            }
                                            else {
                                                identifiersToSave.add(foundIdentifier);
                                            }
                                        } else if (result == null) {
                                            result = existingUserAnchor;
                                        }
                                    } else { // Identifier object NOT found, need to add.
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
                                    log.warn("Non-unique User Identifier: " + nextIdentifier, nure);
                                }
                            }
                        }
                    }
                }

                if (s != null && s.isOpen()) {
                    s.close();
                }

                // Need to create a new IUserAnchor
                if (result == null) {
                    result = (IUserAnchor) eiUserAnchor.mappedClass.clazz.newInstance();
                    ((IPerceroObject) result).setID(UUID.randomUUID().toString());
                    result.setUserId(userId);
                    if (serviceUserList.size() > 0) {
                        /**
                         IUserAnchorA userAnchorAnnotation = getUserAnchorAnnotation(userAnchorClass);

                         if (userAnchorAnnotation.firstNameFieldName() != null && !userAnchorAnnotation.firstNameFieldName().isEmpty())
                         firstNameMappedField = mcUserAnchor.getMappedFieldByName(userAnchorAnnotation.firstNameFieldName());
                         MappedField lastNameMappedField = null;
                         if (userAnchorAnnotation.lastNameFieldName() != null && !userAnchorAnnotation.lastNameFieldName().isEmpty())
                         lastNameMappedField = mcUserAnchor.getMappedFieldByName(userAnchorAnnotation.lastNameFieldName());
                         */

                        // Find field that has firstName and lastName PropertyImplementations.
                        PropertyImplementation firstNamePropImpl = eiUserAnchor.findPropertyImplementationByName(IUserAnchor.FIRST_NAME_FIELD);
                        PropertyImplementation lastNamePropImpl = eiUserAnchor.findPropertyImplementationByName(IUserAnchor.LAST_NAME_FIELD);

                        ServiceUser firstServiceUser = serviceUserList.get(0);

                        if (firstNamePropImpl != null)
                            firstNamePropImpl.mappedField.getSetter().invoke(result, firstServiceUser.getFirstName());
                        if (lastNamePropImpl != null)
                            lastNamePropImpl.mappedField.getSetter().invoke(result, firstServiceUser.getLastName());
                    }

                    syncAgentService.systemCreateObject((IPerceroObject) result, null);

                }
                else if (result.getUserId() == null || result.getUserId().isEmpty()) {
                    result.setUserId(userId);

                    syncAgentService.systemPutObject((IPerceroObject) result, null, null, null, true);
                }

                for (IUserIdentifier nextUserIdentifier : identifiersToSave) {
                    // Set Email.person now that we have a firm handle on that Person object.
                    Boolean isNewObject = false;
                    if (((IPerceroObject)nextUserIdentifier).getID() == null || ((IPerceroObject)nextUserIdentifier).getID().isEmpty()) {
                        isNewObject = true;
                        ((IPerceroObject)nextUserIdentifier).setID(UUID.randomUUID().toString());
                    }

					/*
					IUserIdentifierA userIdentifierAnnotation = getUserIdentifierAnnotation(nextUserIdentifier.getClass());
					MappedClass mc = mcm.getMappedClassByClassName(nextUserIdentifier.getClass().getName());
					MappedField userAnchorMappedField = mc.getMappedFieldByName(userIdentifierAnnotation.userAnchorFieldName());
					userAnchorMappedField.getSetter().invoke(nextUserIdentifier, result);
					
					if (isNewObject) {
						syncAgentService.systemCreateObject((IPerceroObject) nextUserIdentifier, null);
					}
					else {
						syncAgentService.systemPutObject((IPerceroObject) nextUserIdentifier, null, null, null, true);
					}
					*/

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
                            log.warn("No UserAnchor found for IUserIdentifier class " + nextUserIdentifier.getClass().getCanonicalName());
                        }
                    }
                }

                if (result != null) {
                    setupUserRoles(userId, serviceUserList);
                    return result;
                }
            }

        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (s != null && s.isOpen())
                s.close();
        }

        return null;
    }

    @SuppressWarnings({ "unchecked" })
    public void setupUserRoles(String userId, List<ServiceUser> serviceUserList) throws Exception {

        try {
            ManifestHelper.setManifest(manifest);
            EntityImplementation userRoleEI = RoleHelper.getUserRoleEntityImplementation();
            RelationshipImplementation userAnchorRelImpl = userRoleEI.findRelationshipImplementationBySourceVarName(IUserRole.USER_ANCHOR_FIELD_NAME);

            IUserAnchor userAnchor = UserAnchorHelper.getUserAnchor(userId);
            List<IUserRole> userRoles = RoleHelper.getUserRoles(userAnchor);
            List<IUserRole> updatedUserRoles = new ArrayList<IUserRole>();

            // First, remove all roles that a Person has that are not in the serviceUserList.
            for(IUserRole nextUserRole : userRoles) {
                Boolean serviceUserRoleExists = false;
                Boolean isInaccurateList = false;
                for (ServiceUser nextServiceUser : serviceUserList) {
                    if (!nextServiceUser.getAreRoleNamesAccurate()) {
                        log.debug("Ignoring role names from " + nextServiceUser.getAuthProviderID().toString());
                        isInaccurateList = true;
                        break;
                    }
                    else {
                        if (nextServiceUser.getRoleNames().contains(nextUserRole.getRoleName())) {
                            serviceUserRoleExists = true;
                            break;
                        }
                    }
                }

                if (!isInaccurateList && !serviceUserRoleExists) {
                    log.warn("Deleting role " + nextUserRole.getRoleName() + " for " + userId);
                    syncAgentService.systemDeleteObject(nextUserRole, null, true, new HashSet<IPerceroObject>());
                } else
                    updatedUserRoles.add(nextUserRole);
            }

            // Second, add all roles that are not on the Person.
            // Get MappedFields to set.
            //MappedField userAnchorMappedField = userRoleMappedClass.getMappedFieldByName(userRoleAnnotation.userAnchorFieldName());
            //MappedField userRoleDateCreatedField = userRoleMappedClass.getMappedFieldByName("dateCreated");
            //MappedField userRoleDateModifiedField = userRoleMappedClass.getMappedFieldByName("dateModified");

            for (ServiceUser nextServiceUser : serviceUserList) {
                if (nextServiceUser.getAreRoleNamesAccurate()) {
                    // Role names from the service are accurate, so we can match them up here.
                    for(String nextServiceRoleName : nextServiceUser.getRoleNames()) {
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
            }
        } catch (Exception e) {
            log.error("Unable to Get Person Roles", e);
        }
    }
}

package com.percero.agents.auth.services;

import com.percero.agents.auth.hibernate.AssociationExample;
import com.percero.agents.auth.hibernate.AuthHibernateUtils;
import com.percero.agents.auth.hibernate.BaseDataObjectPropertySelector;
import com.percero.agents.auth.vo.*;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * This class handles AuthenticationRequest type authentication
 */
@Component
public class AuthService2 {

    private static Logger logger = Logger.getLogger(AuthService2.class);

    @Autowired
    private AuthProviderRegistry authProviderRegistry;

    @Autowired
    SessionFactory sessionFactoryAuth;

    public AuthenticationResponse authenticate(AuthenticationRequest request) throws IllegalArgumentException{
        if(!authProviderRegistry.hasProvider(request.getAuthProvider()))
            throw new IllegalArgumentException(request.getAuthProvider()+" auth provider not found");

        AuthenticationResponse response = new AuthenticationResponse();

        IAuthProvider provider = authProviderRegistry.getProvider(request.getAuthProvider());

        ServiceUser serviceUser = provider.authenticate(request.getCredential());

        // Login successful
        if(serviceUser != null) {
            logger.info(provider.getID()+" Authentication success");
            serviceUser.setAuthProviderID(provider.getID()); // Set the provider ID just in case the provider didn't
            UserAccount userAccount = getOrCreateUserAccount(serviceUser, provider, request);

            UserToken userToken = loginUserAccount(userAccount, request.getClientId(), request.getDeviceId());
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
    private UserAccount findUserAccount(ServiceUser serviceUser){
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

        if ((userAccounts instanceof List)
                && ((List) userAccounts).size() > 0) {
            // Found a valid UserAccount.
            List userAccountList = (List) userAccounts;
            theFoundUserAccount = (UserAccount) userAccountList.get(0);
        }

        return theFoundUserAccount;
    }

    /**
     * Find a User from a ServiceUser
     * @param serviceUser
     * @return
     */
    private User findUser(ServiceUser serviceUser){
        Session s = sessionFactoryAuth.openSession();

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

            Query q = s.createQuery(strFindUserIdentifier);
            List<User> userList = (List<User>) q.list();
            if (userList.size() > 0) {
                theUser = userList.get(0);
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
    private UserAccount getOrCreateUserAccount(ServiceUser serviceUser, IAuthProvider provider, AuthenticationRequest request){

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
    private UserToken loginUserAccount(UserAccount theUserAccount, String clientId, String deviceId) {
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
}

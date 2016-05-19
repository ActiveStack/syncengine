package com.percero.agents.auth.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.gson.JsonObject;
import com.percero.agents.auth.hibernate.AuthHibernateUtils;
import com.percero.agents.auth.vo.BasicAuthCredential;
import com.percero.agents.auth.vo.ServiceIdentifier;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.agents.auth.vo.User;
import com.percero.agents.auth.vo.UserIdentifier;
import com.percero.agents.auth.vo.UserPassword;

@Component
public class DatabaseHelper {

	@Autowired
	SessionFactory sessionFactoryAuth;

	private static Logger log = Logger.getLogger(DatabaseHelper.class);

	public List<String> getUserRoleNames(String login, String consumerKey, String consumerSecret, String admin, String domain)
			throws Exception{
		List<String> result = new ArrayList<String>();

		return result;
	}
	
	public ServiceUser getServiceUser(BasicAuthCredential credential) {
		if (credential == null) {
			return null;
		}
		return getServiceUser(credential.getUsername(), credential.getPassword());
	}
	@SuppressWarnings("unchecked")
	public ServiceUser getServiceUser(String userName, String password) {
		ServiceUser result = null;

		Session s = null;
		try {
			s = sessionFactoryAuth.openSession();
			Query q = s.createQuery("FROM UserPassword up WHERE up.userIdentifier.userIdentifier=:userName AND up.password=PASSWORD(:password)");
			q.setString("userName", userName);
			q.setString("password", password);
			UserPassword userPassword = (UserPassword) AuthHibernateUtils.cleanObject(q.uniqueResult());
			UserIdentifier userIdentifier = null;
			if (userPassword != null)
				userIdentifier = (UserIdentifier) AuthHibernateUtils.cleanObject(userPassword.getUserIdentifier());
			
			if (userIdentifier != null) {
				result = new ServiceUser();
				result.setId(userIdentifier.getUserIdentifier());
				result.setIdentifiers(new ArrayList<ServiceIdentifier>());
				
				ServiceIdentifier userServiceIdentifier = new ServiceIdentifier(ServiceIdentifier.ACTIVESTACK_USERID, userIdentifier.getUser().getID());
				result.getIdentifiers().add(userServiceIdentifier);
				
				// Now retrieve all the EMAIL types for this User (if any exist).
				q = s.createQuery("FROM UserIdentifier ui WHERE ui.user.ID=:userId");
				q.setString("userId", userIdentifier.getUser().getID());
				List<UserIdentifier> userIdentifiers = (List<UserIdentifier>) AuthHibernateUtils.cleanObject(q.list());
				result.setEmails(new ArrayList<String>());

				if (userIdentifiers != null && !userIdentifiers.isEmpty()) {
					for(UserIdentifier nextIdentifier : userIdentifiers) {
						if ("email".equalsIgnoreCase(nextIdentifier.getType())) {
							result.getEmails().add(nextIdentifier.getUserIdentifier());
						}

						ServiceIdentifier serviceIdentifier = new ServiceIdentifier(nextIdentifier.getType(), nextIdentifier.getUserIdentifier());
						result.getIdentifiers().add(serviceIdentifier);
					}
				}
				
				// Now get the User details.
				q = s.createQuery("FROM User u WHERE u.ID=:userId");
				q.setString("userId", userIdentifier.getUser().getID());
				User user = (User) AuthHibernateUtils.cleanObject(q.uniqueResult());
				if (user != null) {
					result.setFirstName(user.getFirstName());
					result.setLastName(user.getLastName());
				}

				result.setAccessToken(UUID.randomUUID().toString());
//				result.setAuthProviderID(AuthProvider.DATABASE.toString());
				result.setAreRoleNamesAccurate(false);
			}
			
		} catch(Exception e) {
			log.error("Unable to getServiceUser", e);
		} finally {
			if (s != null)
				s.close();
		}

		return result;
	}
	
	public ServiceUser registerUser(BasicAuthCredential credential, String paradigm) throws AuthException {
		// If included in the metadata, we want to pull in the first and last name.
		String firstName = credential.retrieveMetadataString(BasicAuthCredential.FIRST_NAME);
		String lastName = credential.retrieveMetadataString(BasicAuthCredential.LAST_NAME);
		String email = credential.retrieveMetadataString(BasicAuthCredential.EMAIL);
		return registerUser(credential.getUsername(), credential.getPassword(), paradigm, firstName, lastName, email);
	}

	@SuppressWarnings("unchecked")
	public ServiceUser registerUser(String userName, String password, String paradigm, String firstName,
			String lastName, String email) throws AuthException {
		if (!StringUtils.hasText(userName) || !StringUtils.hasText(password) || !StringUtils.hasText(paradigm)) {
			// Invalid input.
			throw new AuthException("Missing required data", AuthException.INVALID_DATA);
		}
		
		Session s = null;
		try {
			s = sessionFactoryAuth.openSession();
			
			// First make sure this User does NOT already exist.
			Query q = s.createQuery("FROM UserPassword up WHERE up.userIdentifier.userIdentifier=:userName");
			q.setString("userName", userName);
			List<UserPassword> userPasswords = (List<UserPassword>) AuthHibernateUtils.cleanObject(q.list());
			
			if (userPasswords != null && !userPasswords.isEmpty()) {
				// User name exists, so unable to register User.
				throw new AuthException("User name already exists", AuthException.DUPLICATE_USER_NAME);
			}

			// Now check to see if the UserIdentifier exists.
			q = s.createQuery("FROM UserIdentifier ui WHERE ui.userIdentifier=:userName AND ui.type=:paradigm");
			q.setString("userName", userName);
			q.setString("paradigm", paradigm);

			User user = null;
			UserIdentifier userIdentifier = (UserIdentifier) AuthHibernateUtils.cleanObject(q.uniqueResult());
			if (userIdentifier == null) {
				// Need to create the UserIdentifier and User.
				Transaction tx = s.beginTransaction();
				user = new User();
				user.setID(UUID.randomUUID().toString());
				user.setFirstName(firstName);
				user.setLastName(lastName);
				user.setDateCreated(new Date());
				s.save(user);
				user = (User) s.get(User.class, user.getID());
				
				userIdentifier = new UserIdentifier();
				userIdentifier.setID(UUID.randomUUID().toString());
				userIdentifier.setUser(user);
				userIdentifier.setType(paradigm);
				userIdentifier.setUserIdentifier(userName);
				
				s.save(userIdentifier);
				
				// If an email was included, we can also create a UserIdentifier for email.
				if (StringUtils.hasText(email)) {
					userIdentifier = new UserIdentifier();
					userIdentifier.setID(UUID.randomUUID().toString());
					userIdentifier.setUser(user);
					userIdentifier.setType(ServiceIdentifier.EMAIL);
					userIdentifier.setUserIdentifier(email);
					s.save(userIdentifier);
				}
				
				tx.commit();
				
				// Make sure we have successfully created this UserIdentifier by re-running the query.
				userIdentifier = (UserIdentifier) AuthHibernateUtils.cleanObject(q.uniqueResult());
			}
			
			// If UserIdentifier is still null, then we have a problem.
			if (userIdentifier == null) {
				throw new AuthException("Invalid User Identifier", AuthException.INVALID_USER_IDENTIFIER);
			}
			
			// Now we can create the UserPassword.
			q = s.createSQLQuery("INSERT INTO UserPassword (`ID`, `password`, `userIdentifier_ID`) VALUES ('" + UUID.randomUUID().toString() + "',PASSWORD('" + password + "'),'" + userIdentifier.getID() + "')");
			int updateResult = q.executeUpdate();
			
			if (updateResult == 1) {
				// We have successfully registered the user name.
				return getServiceUser(userName, password);
			}
			else {
				// Something went wrong
				throw new AuthException("Invalid User Password", AuthException.INVALID_USER_PASSWORD);
			}
		} finally {
			if (s != null) {
				s.close();
			}
		}
	}
	
	public String getJsonObjectStringValue(JsonObject jsonObject,
			String fieldName) {
		if (jsonObject.has(fieldName))
			return jsonObject.get(fieldName).getAsString();
		else
			return "";
	}
}

package com.percero.agents.auth.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.gson.JsonObject;
import com.percero.agents.auth.hibernate.AuthHibernateUtils;
import com.percero.agents.auth.vo.AuthProvider;
import com.percero.agents.auth.vo.ServiceUser;
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
	
	public ServiceUser getServiceUser(String userName, String password) {
		ServiceUser result = null;

		Session s = null;
		try {
			s = sessionFactoryAuth.openSession();
			Query q = s.createQuery("FROM UserPassword up WHERE up.userIdentifier.userIdentifier=:userName AND up.password=:password");
			q.setString("userName", userName);
			q.setString("password", password);
			UserPassword userPassword = (UserPassword) AuthHibernateUtils.cleanObject(q.uniqueResult());
			UserIdentifier userIdentifier = null;
			if (userPassword != null)
				userIdentifier = (UserIdentifier) AuthHibernateUtils.cleanObject(userPassword.getUserIdentifier());
			
			if (userIdentifier != null) {
				result = new ServiceUser();
				result.setId(userIdentifier.getUserIdentifier());
				if (StringUtils.hasText(userIdentifier.getUserIdentifier())) {
					result.setEmails(new ArrayList<String>());
					result.getEmails().add(userIdentifier.getUserIdentifier());
				}
				result.setAccessToken(UUID.randomUUID().toString());
				result.setAuthProviderID(AuthProvider.DATABASE.toString());
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
	
	public String getJsonObjectStringValue(JsonObject jsonObject,
			String fieldName) {
		if (jsonObject.has(fieldName))
			return jsonObject.get(fieldName).getAsString();
		else
			return "";
	}
}

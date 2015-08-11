package com.percero.agents.auth.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionManager implements ISessionManager {

	private static Logger log = Logger.getLogger(AuthSessionManager.class);

	@Autowired
	SessionFactory sessionFactoryAuth;
	public void setSessionFactoryAuth(SessionFactory value) {
		if (value != null)
			log.debug("Setting sessionFactoryAuth to " + value.toString());
		else
			log.debug("Setting sessionFactoryAuth to NULL");
		sessionFactoryAuth = value;
	}
	public SessionFactory getSessionFactoryAuth() {
		return sessionFactoryAuth;
	}
	
	public Session getSession() throws HibernateException {
		return sessionFactoryAuth.openSession();
	}
	
	public Session getCurrentSession() {
		return sessionFactoryAuth.getCurrentSession();
	}
	
}
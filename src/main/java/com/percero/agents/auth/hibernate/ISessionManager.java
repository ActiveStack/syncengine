package com.percero.agents.auth.hibernate;

import org.hibernate.Session;

public interface ISessionManager {

	public Session getSession() throws Exception;
	public Session getCurrentSession() throws Exception;
	
}
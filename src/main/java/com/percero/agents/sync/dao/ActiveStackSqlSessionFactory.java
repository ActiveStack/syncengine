package com.percero.agents.sync.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.exceptions.SyncDataException;

@Component
public class ActiveStackSqlSessionFactory {
	
	private static ActiveStackSqlSessionFactory instance = null;
	public static ActiveStackSqlSessionFactory getInstance() throws SyncDataException {
		if (instance != null) {
			return instance;
		}
		
		throw new SyncDataException("No Session Factory", -1001);
	}

	public ActiveStackSqlSessionFactory() {
		ActiveStackSqlSessionFactory.instance = this;
	}

	@Autowired
	@Qualifier(value="appSessionFactory")
	SessionFactory appSessionFactory;
	public void setAppSessionFactory(SessionFactory value) {
		appSessionFactory = value;
	}

	public IActiveStackSqlSession retrieveOpenSession(String dataSource) {
		ActiveStackHibernateSqlSession session = new ActiveStackHibernateSqlSession(appSessionFactory.openSession());
		return session;
	}
}

package com.percero.agents.sync.dao;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.dialect.MySQLDialect;

public class ActiveStackMySQLDialect extends MySQLDialect {

	public ActiveStackMySQLDialect() {
		super();
		
		registerHibernateType(Types.LONGVARCHAR, Hibernate.TEXT.getName());
	}

}

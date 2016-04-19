package com.percero.agents.sync.dao;

import java.sql.Types;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.type.TextType;

public class ActiveStackMySQLDialect extends MySQLDialect {

	public ActiveStackMySQLDialect() {
		super();
		
		registerHibernateType(Types.LONGVARCHAR, TextType.INSTANCE.getName());
	}

}

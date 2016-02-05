package com.percero.datasource;

import java.sql.Connection;
import java.sql.SQLException;

public interface IConnectionFactory {

	String getName();
	void setName(String name);
	Connection getConnection() throws SQLException;
	
	Integer getFetchSize();
}

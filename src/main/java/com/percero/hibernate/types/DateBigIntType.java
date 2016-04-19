package com.percero.hibernate.types;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.hibernate.type.BigIntegerType;

public class DateBigIntType extends BigIntegerType {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5880508702368203232L;

	public Object get(ResultSet rs, String name)
			throws SQLException {
		long value = rs.getLong(name);
		if (value > 0)
			return new Date(value);
		else
			return null;
	}
	
	public void set(PreparedStatement stmt, Object value, int index)
		throws SQLException {
		if (value == null)
			stmt.setLong(index, 0);
		else
			stmt.setLong(index, ((Date) value).getTime());
	}
}

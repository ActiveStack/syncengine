package com.percero.util;

public class DateUtils {

	public static java.sql.Date utilDateToSqlDate(java.util.Date utilDate) {
		if (utilDate != null) {
			return new java.sql.Date(utilDate.getTime());
		}
		else {
			return null;
		}
	}

}

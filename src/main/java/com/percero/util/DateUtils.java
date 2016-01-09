package com.percero.util;

import java.sql.Timestamp;
import java.util.Date;

public class DateUtils {

	public static java.sql.Date utilDateToSqlDate(java.util.Date utilDate) {
		if (utilDate != null) {
			return new java.sql.Date(utilDate.getTime());
		}
		else {
			return null;
		}
	}
	
	public static Date utilDateFromSqlTimestamp(Timestamp timestamp) {
		if (timestamp != null) {
			return new Date(timestamp.getTime());
		}
		return null;
	}
    
    public static Timestamp sqlTimestampFromUtilDate(java.util.Date utilDate){
        if(utilDate!=null){
            return new Timestamp(utilDate.getTime());
        }
        return null;
    }

}

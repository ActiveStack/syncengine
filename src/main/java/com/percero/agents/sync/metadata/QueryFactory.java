package com.percero.agents.sync.metadata;

import com.percero.framework.metadata.IMappedQuery;

public class QueryFactory {
	public static IMappedQuery createQuery(String query){
		IMappedQuery result = null;
		if (query.indexOf("jpql:") >= 0) {
			result = new JpqlQuery();
			result.setQuery(query);
		}
		else if (query.indexOf("sql:") >= 0) {
			result = new SqlQuery(query.substring(query.indexOf("sql:")+4));
		}
		else {
			// Unsupported Query type
			return null;
		}
		
		return result;
	}
}

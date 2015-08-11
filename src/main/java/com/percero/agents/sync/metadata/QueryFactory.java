package com.percero.agents.sync.metadata;

import com.percero.framework.metadata.IMappedQuery;

public class QueryFactory {
	public static IMappedQuery createQuery(String query){
		IMappedQuery result = null;
		if (query.indexOf("jpql:") >= 0)
			result = new JpqlQuery();
		else
			result = new MappedQuery();
		
		result.setQuery(query);
		return result;
	}
}

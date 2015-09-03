package com.percero.agents.sync.metadata;

import org.hibernate.Query;
import org.hibernate.impl.SessionImpl;

public class JpqlQuery extends MappedQuery {

	@Override
	public void setQuery(String value) {
		int index = value.indexOf("jpql:");
		if (index >= 0)
			value = value.substring(index + 5);
		super.setQuery(value);
	}

	@Override
	public Query createQuery(Object theObject, String userId, Object[] params,
			SessionImpl s) throws Exception {
		Query theQuery = s.createQuery(getQuery());
		setQueryParameters(theQuery, theObject, userId, params, s);
		
		return theQuery;
	}
}

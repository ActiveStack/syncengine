package com.percero.framework.metadata;

import org.hibernate.Query;
import org.hibernate.impl.SessionImpl;

public interface IMappedQuery {

	public String getQuery();
	public void setQuery(String value);

	public String getQueryName();
	public void setQueryName(String value);
	
	public boolean getUseId();
	
	public void setQueryParameters(Query theQuery, Object theObject, String userId) throws Exception;
	public void setQueryParameters(Query theQuery, Object theObject, String userId, Object[] params, SessionImpl s) throws Exception;
}

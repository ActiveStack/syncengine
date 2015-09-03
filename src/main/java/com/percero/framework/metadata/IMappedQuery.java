package com.percero.framework.metadata;


public interface IMappedQuery {

	public String getQuery();
	public void setQuery(String value);

	public String getQueryName();
	public void setQueryName(String value);
	
	public boolean getUseId();
	
	public String setQueryParameters(String theQuery, Object theObject, String userId) throws Exception;
	public String setQueryParameters(String theQuery, Object theObject, String userId, Object[] params) throws Exception;
}

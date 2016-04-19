package com.percero.agents.sync.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.engine.query.spi.ParamLocationRecognizer;
import org.springframework.util.StringUtils;

import com.percero.framework.metadata.IMappedQuery;
import com.percero.framework.vo.IPerceroObject;

public class MappedQuery implements IMappedQuery {

	private static final Logger log = Logger.getLogger(MappedQuery.class);
	
	private String query;
	private String queryName;
	
	public String getQuery() {
		return query;
	}
	public void setQuery(String value) {
		query = value;
	}
	
	public String getQueryName() {
		return queryName;
	}
	public void setQueryName(String value) {
		queryName = value;
	}
	
	private boolean queryParameterNamesSet = false;
	private boolean useUserId = false;
	private boolean useId = false;
	private boolean useIds = false;
	
	@SuppressWarnings("rawtypes")
	protected void initialize() {
		if (!queryParameterNamesSet && StringUtils.hasText(query))
		{
			try {
				List<String> paramNames = new ArrayList<String>();
				ParamLocationRecognizer recognizer = ParamLocationRecognizer.parseLocations(query);
				Iterator itr = recognizer.getNamedParameterDescriptionMap().entrySet().iterator();
				while( itr.hasNext() ) {
					final Map.Entry entry = ( Map.Entry ) itr.next();
					String nextNamedParameter = (String)entry.getKey();
					paramNames.add(nextNamedParameter);

					if (nextNamedParameter.equalsIgnoreCase("userId"))
						useUserId = true;
					else if (nextNamedParameter.equalsIgnoreCase("id"))
						useId = true;
					else if (nextNamedParameter.equalsIgnoreCase("ids"))
						useIds = true;
				}
				queryParameterNamesSet = true;
				
			} catch(Exception e) {
				// Do nothing.
			}
		}
	}
	
	public boolean getUseId() {
		initialize();
		return this.useId;
	}

	public boolean getUseIds() {
		initialize();
		return this.useIds;
	}
	
	public String setQueryParameters(String theQuery, Object theObject, String userId) throws Exception {
		return setQueryParameters(theQuery, theObject, userId, null);
	}
	
	@SuppressWarnings("rawtypes")
	public String setQueryParameters(String theQuery, Object theObject, String userId, Object[] params) throws Exception {
		if (!queryParameterNamesSet) {
			useUserId = theQuery.contains(":userId");
			useId = theQuery.contains(":id");
			useIds = theQuery.contains(":ids");
			queryParameterNamesSet = true;
		}
		
		if (useId) {
			if (theObject instanceof String) {
				try {
					theQuery = theQuery.replaceAll(":id", "\"" + (String)theObject + "\"");
				} catch(Exception e) {
					log.warn("Unable to set ID for MappedQuery from String");
				}
			}
			else if (theObject instanceof IPerceroObject) {
				try {
					theQuery = theQuery.replaceAll(":id", "\"" + ((IPerceroObject)theObject).getID() + "\"");
				} catch(Exception e) {
					log.warn("Unable to set ID for MappedQuery from IPerceroObject");
				}
			}
			else if (theObject instanceof Object[])
			{
				for(Object nextParam : (Object[]) theObject) {
					if (nextParam instanceof String) {
						try {
							String[] nextParamSplit = ((String) nextParam).split(":");
							if (nextParamSplit.length == 2) {
								if (nextParamSplit[0].equalsIgnoreCase("id")) {
									theQuery = theQuery.replaceAll(":id", "\"" + nextParamSplit[1] + "\"");
									break;
								}
							}
						} catch(Exception e) {
							log.warn("Unable to set ID for MappedQuery from String");
						}
					}
					else if (nextParam instanceof IPerceroObject) {
						try {
							theQuery = theQuery.replaceAll(":id", "\"" + ((IPerceroObject)nextParam).getID() + "\"");
						} catch(Exception e) {
							log.warn("Unable to set ID for MappedQuery from IPerceroObject");
						}
					}
				}
			}
		}
		if (useIds) {
			if (theObject instanceof String) {
				try {
					theQuery = theQuery.replaceAll(":ids", "'" + (String)theObject + "'");
				} catch(Exception e) {
					log.warn("Unable to set IDs for MappedQuery from String");
				}
			}
			else if (theObject instanceof IPerceroObject) {
				try {
					theQuery = theQuery.replaceAll(":ids", "'" + ((IPerceroObject)theObject).getID() + "'");
				} catch(Exception e) {
					log.warn("Unable to set IDs for MappedQuery from IPerceroObject");
				}
			}
			else if (theObject instanceof Object[]) {
				Object[] theObjectArray = (Object[]) theObject;
				
				String idsListString = null;

				for(Object nextParam : theObjectArray) {
					if (nextParam instanceof String) {
						try {
							String[] nextParamSplit = ((String) nextParam).split(":");
							if (nextParamSplit.length == 2) {
								if (nextParamSplit[0].equalsIgnoreCase("id")) {
									if (idsListString != null) {
										idsListString += ",";
									}
									else {
										idsListString = "";
									}
									idsListString += nextParamSplit[1];
									break;
								}
							}
						} catch(Exception e) {
							log.warn("Unable to set IDs for MappedQuery from String");
						}
					}
					else if (nextParam instanceof IPerceroObject) {
						try {
							if (idsListString != null) {
								idsListString += ",";
							}
							else {
								idsListString = "";
							}
							idsListString += ((IPerceroObject)nextParam).getID();
						} catch(Exception e) {
							log.warn("Unable to set IDs for MappedQuery from IPerceroObject");
						}
					}
				}
				
				theQuery = theQuery.replaceAll(":ids", idsListString);
			}
			else if (theObject instanceof Collection) {
				Collection theObjectCollection = (Collection) theObject;
				
				String idsListString = null;
				
				for(Object nextParam : theObjectCollection) {
					if (nextParam instanceof String) {
						try {
							String[] nextParamSplit = ((String) nextParam).split(":");
							if (nextParamSplit.length == 2) {
								if (nextParamSplit[0].equalsIgnoreCase("id")) {
									if (idsListString != null) {
										idsListString += ",";
									}
									else {
										idsListString = "";
									}
									idsListString += nextParamSplit[1];
									break;
								}
							}
						} catch(Exception e) {
							log.warn("Unable to set IDs for MappedQuery from String");
						}
					}
					else if (nextParam instanceof IPerceroObject) {
						try {
							if (idsListString != null) {
								idsListString += ",";
							}
							else {
								idsListString = "";
							}
							idsListString += ((IPerceroObject)nextParam).getID();
						} catch(Exception e) {
							log.warn("Unable to set IDs for MappedQuery from IPerceroObject");
						}
					}
				}
				
				theQuery = theQuery.replaceAll(":ids", idsListString);
			}
		}
		if (useUserId) {
			try {
				theQuery = theQuery.replaceAll(":userId", "\"" + userId + "\"");
			} catch(Exception e) {
				log.warn("Unable to set UserID for MappedQuery");
			}
		}
		
		return theQuery;
	}
}

package com.percero.agents.sync.metadata;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.engine.query.NamedParameterDescriptor;
import org.hibernate.engine.query.ParamLocationRecognizer;
import org.hibernate.engine.query.ParameterMetadata;
import org.hibernate.type.TimestampType;
import org.hibernate.type.Type;
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
		if (!queryParameterNamesSet)
		{
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
			else if (theObject instanceof Object[])
			{
				Object[] theObjectArray = (Object[]) theObject;
				
				Collection<String> idsList = new HashSet<String>(theObjectArray.length);

				for(Object nextParam : theObjectArray) {
					if (nextParam instanceof String) {
						try {
							String[] nextParamSplit = ((String) nextParam).split(":");
							if (nextParamSplit.length == 2) {
								if (nextParamSplit[0].equalsIgnoreCase("id")) {
									idsList.add(nextParamSplit[1]);
									break;
								}
							}
						} catch(Exception e) {
							log.warn("Unable to set IDs for MappedQuery from String");
						}
					}
					else if (nextParam instanceof IPerceroObject) {
						try {
							idsList.add(((IPerceroObject)nextParam).getID());
						} catch(Exception e) {
							log.warn("Unable to set IDs for MappedQuery from IPerceroObject");
						}
					}
				}
				
				theQuery.setParameterList("ids", idsList);
			}
			else if (theObject instanceof Collection)
			{
				Collection theObjectCollection = (Collection) theObject;
				
				Collection<String> idsList = new HashSet<String>(theObjectCollection.size());
				
				for(Object nextParam : theObjectCollection) {
					if (nextParam instanceof String) {
						try {
							String[] nextParamSplit = ((String) nextParam).split(":");
							if (nextParamSplit.length == 2) {
								if (nextParamSplit[0].equalsIgnoreCase("id")) {
									idsList.add(nextParamSplit[1]);
									break;
								}
							}
						} catch(Exception e) {
							log.warn("Unable to set IDs for MappedQuery from String");
						}
					}
					else if (nextParam instanceof IPerceroObject) {
						try {
							idsList.add(((IPerceroObject)nextParam).getID());
						} catch(Exception e) {
							log.warn("Unable to set IDs for MappedQuery from IPerceroObject");
						}
					}
				}
				
				theQuery.setParameterList("ids", idsList);
			}
		}
		if (useUserId) {
			try {
				theQuery = theQuery.replaceAll(":userId", "\"" + userId + "\"");
			} catch(Exception e) {
				log.warn("Unable to set UserID for MappedQuery");
			}
		}
		
		// Get Parameter MetaData.
		if (params != null && params.length > 0) {
//			if (parameterMetadata == null) {
//				try {
//					parameterMetadata = ((SessionFactoryImpl)s.getSessionFactory()).getQueryPlanCache().getHQLQueryPlan(theQuery.getQueryString(), false, (s).getEnabledFilters()).getParameterMetadata();
//				} catch(Exception e) {
//					log.warn("Unable to get ParameterMetaData from Query:\n" + theQuery.getQueryString(), e);
//				}
//			}
			
			for(Object nextParam : params) {
				if (nextParam instanceof Map) {
					Map nextMapParam = (Map) nextParam;
					Iterator itr = nextMapParam.entrySet().iterator();
					while(itr.hasNext()) {
						Boolean paramSet = false;
						try {
							Map.Entry pairs = (Map.Entry) itr.next();
							Object key = pairs.getKey();
							Object value = pairs.getValue();
							
							if (key instanceof String) {
								String strKey = (String) key;
								if (parameterMetadata != null) {
									NamedParameterDescriptor npd = parameterMetadata.getNamedParameterDescriptor(strKey);
									if (npd != null) {
										Type expectedType = npd.getExpectedType();
										
										if (expectedType instanceof TimestampType) {
											Date dateValue = new Date((Long)value);
											theQuery = theQuery.replaceAll(":" + strKey, "'" + dateValue + "'");
										} else {
											theQuery = theQuery.replaceAll(":" + (String) key, "\"" + value + "\"");
										}
										paramSet = true;
									}
								}

								// Last ditch effort to set this parameter.
								if (!paramSet)
									theQuery = theQuery.replaceAll(":" + (String) key, "\"" + value + "\"");
							}
						} catch(Exception e) {
							log.warn("Unable to apply parameter to filter", e);
						}
					}
				} else if (nextParam instanceof String) {
					String nextStringParam = (String) nextParam;
					try {
						String[] paramSplit = nextStringParam.split(":");
						String key = paramSplit[0];
						String value = paramSplit[1];
						theQuery = theQuery.replaceAll(":" + key, "\"" + value + "\"");
					} catch(Exception e) {
						log.warn("Unable to apply parameter to filter", e);
					}
				}
			}
		}
		
		return theQuery;
	}
}

package com.percero.agents.sync.metadata;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.engine.query.NamedParameterDescriptor;
import org.hibernate.engine.query.ParamLocationRecognizer;
import org.hibernate.engine.query.ParameterMetadata;
import org.hibernate.impl.QueryImpl;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.impl.SessionImpl;
import org.hibernate.type.TimestampType;
import org.hibernate.type.Type;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.services.SyncAgentService;
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
	private ParameterMetadata parameterMetadata = null;
	
	@SuppressWarnings("rawtypes")
	public boolean getUseId() {
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
				}
				queryParameterNamesSet = true;
				
			} catch(Exception e) {
				// Do nothing.
			}
		}
		
		return this.useId;
	}

	public void setQueryParameters(Query theQuery, Object theObject, String userId) throws Exception {
		setQueryParameters(theQuery, theObject, userId, null, null);
	}
	
	@SuppressWarnings("rawtypes")
	public void setQueryParameters(Query theQuery, Object theObject, String userId, Object[] params, SessionImpl s) throws Exception {
		if (!queryParameterNamesSet)
		{
			for(String nextNamedParameter : theQuery.getNamedParameters()) {
				if (nextNamedParameter.equalsIgnoreCase("userId"))
					useUserId = true;
				else if (nextNamedParameter.equalsIgnoreCase("id"))
					useId = true;
			}
			queryParameterNamesSet = true;
		}
		
		if (useId) {
			if (theObject instanceof String) {
				try {
					theQuery.setString("id", (String)theObject);
				} catch(Exception e) {
					log.warn("Unable to set ID for MappedQuery from String");
				}
			}
			else if (theObject instanceof IPerceroObject) {
				try {
					theQuery.setString("id", ((IPerceroObject)theObject).getID());
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
									theQuery.setString("id", nextParamSplit[1]);
									break;
								}
							}
						} catch(Exception e) {
							log.warn("Unable to set ID for MappedQuery from String");
						}
					}
					else if (nextParam instanceof IPerceroObject) {
						try {
							theQuery.setString("id", ((IPerceroObject)nextParam).getID());
						} catch(Exception e) {
							log.warn("Unable to set ID for MappedQuery from IPerceroObject");
						}
					}
				}
			}
		}
		if (useUserId) {
			try {
				theQuery.setString("userId", userId);
			} catch(Exception e) {
				log.warn("Unable to set UserID for MappedQuery");
			}
		}
		
		// Get Parameter MetaData.
		if (params != null && params.length > 0) {
			if (parameterMetadata == null) {
				try {
					parameterMetadata = ((SessionFactoryImpl)s.getSessionFactory()).getQueryPlanCache().getHQLQueryPlan(theQuery.getQueryString(), false, (s).getEnabledFilters()).getParameterMetadata();
				} catch(Exception e) {
					log.warn("Unable to get ParameterMetaData from Query:\n" + theQuery.getQueryString(), e);
				}
			}
			
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
											theQuery.setDate(strKey, dateValue);
										} else {
											theQuery.setParameter((String) key, value);
										}
										paramSet = true;
									}
								}

								// Last ditch effort to set this parameter.
								if (!paramSet)
									theQuery.setParameter((String) key, value);
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
						theQuery.setParameter(key, value);
					} catch(Exception e) {
						log.warn("Unable to apply parameter to filter", e);
					}
				}
			}
		}
	}
}

package com.percero.agents.sync.metadata;

import org.apache.log4j.Logger;

public class SqlQuery extends MappedQuery {
	
	private static final Logger log = Logger.getLogger(SqlQuery.class);
	
	public SqlQuery(String query){
		super();
		super.setQuery(query);
	}

	public SqlQuery(){
		super();
	}

//	ParameterMetadata parameterMetadata = null;
	
//	public Query createQuery(Object theObject, String userId, Object[] params,
//			SessionImpl s) throws Exception {
//		String queryString = getQuery();
//		queryString = setQueryParameters(queryString, theObject, userId, params);
//		queryString = setParameterMetadata(queryString, theObject, userId, params, s);
//		Query theQuery = s.createQuery(queryString);
//		
//		return theQuery;
//	}

//	private String setParameterMetadata(String theQuery, Object theObject, String userId, Object[] params, SessionImpl s) {
//		// Get Parameter MetaData.
//		if (params != null && params.length > 0) {
//			if (parameterMetadata == null) {
//				try {
//					NativeSQLQuerySpecification querySpec = null;
//					parameterMetadata = ((SessionFactoryImpl)s.getSessionFactory()).getQueryPlanCache().getNativeSQLQueryPlan(querySpec).getParameterMetadata();
//				} catch(Exception e) {
//					log.warn("Unable to get ParameterMetaData from Query:\n" + theQuery, e);
//				}
//			}
//			
//			for(Object nextParam : params) {
//				if (nextParam instanceof Map) {
//					Map nextMapParam = (Map) nextParam;
//					Iterator itr = nextMapParam.entrySet().iterator();
//					while(itr.hasNext()) {
//						Boolean paramSet = false;
//						try {
//							Map.Entry pairs = (Map.Entry) itr.next();
//							Object key = pairs.getKey();
//							Object value = pairs.getValue();
//							
//							if (key instanceof String) {
//								String strKey = (String) key;
//								if (parameterMetadata != null) {
//									NamedParameterDescriptor npd = parameterMetadata.getNamedParameterDescriptor(strKey);
//									if (npd != null) {
//										Type expectedType = npd.getExpectedType();
//										
//										if (expectedType instanceof TimestampType) {
//											Date dateValue = new Date((Long)value);
//											theQuery = theQuery.replaceAll(":" + strKey, "'" + dateValue + "'");
//										} else {
//											theQuery = theQuery.replaceAll(":" + (String) key, "\"" + value + "\"");
//										}
//										paramSet = true;
//									}
//								}
//
//								// Last ditch effort to set this parameter.
//								if (!paramSet)
//									theQuery = theQuery.replaceAll(":" + (String) key, "\"" + value + "\"");
//							}
//						} catch(Exception e) {
//							log.warn("Unable to apply parameter to filter", e);
//						}
//					}
//				} else if (nextParam instanceof String) {
//					String nextStringParam = (String) nextParam;
//					try {
//						String[] paramSplit = nextStringParam.split(":");
//						String key = paramSplit[0];
//						String value = paramSplit[1];
//						theQuery = theQuery.replaceAll(":" + key, "\"" + value + "\"");
//					} catch(Exception e) {
//						log.warn("Unable to apply parameter to filter", e);
//					}
//				}
//			}
//		}
//		
//		return theQuery;
//	}
}

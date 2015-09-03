package com.percero.agents.sync.metadata;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.engine.query.ParameterMetadata;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.impl.SessionImpl;

public class SqlQuery extends MappedQuery {
	
	private static final Logger log = Logger.getLogger(SqlQuery.class);
	
	public SqlQuery(String query){
		super();
		super.setQuery(query);
	}

	public SqlQuery(){
		super();
	}
	
	@Override
	protected ParameterMetadata fetchParameterMetadataFromQuery(Query theQuery, SessionImpl s) {
		ParameterMetadata parameterMetadata = null;

		try {
			// TODO: This needs to be NativeSQL specific
//			parameterMetadata = ((SessionFactoryImpl)s.getSessionFactory()).getQueryPlanCache().getNativeSQLQueryPlan(theQuery.getQueryString(), false, s.getEnabledFilters()).getParameterMetadata();

			parameterMetadata = ((SessionFactoryImpl)s.getSessionFactory()).getQueryPlanCache().getHQLQueryPlan(theQuery.getQueryString(), false, s.getEnabledFilters()).getParameterMetadata();
		} catch(Exception e) {
			log.warn("Unable to get ParameterMetaData from Query:\n" + theQuery.getQueryString(), e);
		}
		
		return parameterMetadata;
	}

	@Override
	public Query createQuery(Object theObject, String userId, Object[] params,
			SessionImpl s) throws Exception {
		Query theQuery = s.createSQLQuery(getQuery());
		setQueryParameters(theQuery, theObject, userId, params, s);
		
		return theQuery;
	}
}

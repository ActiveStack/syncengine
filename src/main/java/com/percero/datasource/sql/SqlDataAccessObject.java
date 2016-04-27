package com.percero.datasource.sql;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.connectors.ConnectorException;
import com.percero.agents.sync.dao.IDataAccessObject;
import com.percero.agents.sync.exceptions.SyncDataException;
import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.services.IDataProviderManager;
import com.percero.agents.sync.services.ISyncAgentService;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.ClassIDPairs;
import com.percero.datasource.IConnectionFactory;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;

public abstract class SqlDataAccessObject<T extends IPerceroObject> implements IDataAccessObject<T> {

	static final Logger log = Logger.getLogger(SqlDataAccessObject.class);

	public static long LONG_RUNNING_QUERY_TIME = 2000;
	public static int QUERY_TIMEOUT = 10;

	public SqlDataAccessObject() {
		super();
		
		// We want to use the UTC time zone
		TimeZone timeZone = TimeZone.getTimeZone("UTC");
		TimeZone.setDefault(timeZone);
	}
	
	@Autowired
	ISyncAgentService syncAgentService;

	@Autowired
	protected IDataProviderManager dataProviderManager;

	protected IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
	
	BaseSqlDataConnectionRegistry connectionRegistry;
	public BaseSqlDataConnectionRegistry getConnectionRegistry() {
		if (connectionRegistry == null) {
			connectionRegistry = BaseSqlDataConnectionRegistry.getInstance();
		}
		return connectionRegistry;
	}

	protected String getConnectionFactoryName() {
		return null;
	}
	
//	protected String getSqlView() {
//		return "";
//	}
//
//    protected String getSelectFromStatementTableName(boolean shellOnly) {
//    	return "";
//    }
//    
//    protected String getInnerJoinStatement() {
//    	return "";
//    }
//
//    protected String getWhereIdEqualsClause(String equalTo, boolean isFirst) {
//    	return "";
//    }
//    
//    protected String getWhereClause(boolean shellOnly) {
//    	return "";
//    }
//    
//    protected String getWhereInClause(boolean shellOnly) {
//    	return "";
//    }
//    
//    protected String getIdColumnName() {
//		return "ID";
//	}
//
	protected String getSelectStarSQL() {
		return null;
	}

	protected String getSelectShellOnlySQL() {
		return null;
	}

	protected String getSelect(Boolean shellOnly) {
		if (shellOnly) {
			return getSelectShellOnlySQL();
		}
		else {
			return getSelectStarSQL();
		}
	}

	protected String getSelectInStarSQL() {
		return null;
	}

	protected String getSelectInShellOnlySQL() {
		return null;
	}

	protected String getSelectIn(Boolean shellOnly) {
		if (shellOnly) {
			return getSelectInShellOnlySQL();
		}
		else {
			return getSelectInStarSQL();
		}
	}

	protected String getSelectByRelationshipStarSQL(String joinColumnName) throws SyncDataException {
		return null;
	}

	protected String getSelectByRelationshipShellOnlySQL(String joinColumnName) {
		return null;
	}

	protected String getSelectByRelationship(String joinColumnName, Boolean shellOnly) throws SyncDataException {
		if (shellOnly) {
			return getSelectByRelationshipShellOnlySQL(joinColumnName);
		}
		else {
			return getSelectByRelationshipStarSQL(joinColumnName);
		}
	}

	protected String getSelectAllSql(Boolean shellOnly) throws SyncException {
		if (shellOnly) {
			return getSelectAllShellOnlySQL();
		}
		else {
			return getSelectAllStarSQL();
		}
	}

	protected String getFindByExampleSelectSql(Boolean shellOnly) {
		if (shellOnly) {
			return getFindByExampleSelectShellOnlySQL();
		}
		else {
			return getFindByExampleSelectAllStarSQL();
		}
	}

	protected String getFindByExampleSelectShellOnlySQL() {
		return null;
	}

	protected String getFindByExampleSelectAllStarSQL() {
		return null;
	}

	protected String getSelectAllStarSQL() throws SyncException {
		return null;
	}

	protected String getSelectAllStarWithLimitAndOffsetSQL() {
		return null;
	}

	protected String getSelectAllShellOnlySQL() throws SyncException {
		return null;
	}

	protected String getSelectAllShellOnlyWithLimitAndOffsetSQL() {
		return null;
	}

	protected String getCountAllSQL() {
		return null;
	}

	public T retrieveObject(ClassIDPair classIdPair, String userId,
			Boolean shellOnly) throws SyncException {
		T result = null;
		
		if (!StringUtils.hasText(classIdPair.getID())) {
			// Empty or NULL ID.
			return result;
		}

		String sql = getSelect(shellOnly);
		List<T> results = executeSelectById(sql, classIdPair.getID(), shellOnly);
		if (results != null && !results.isEmpty()) {
			result = results.get(0);

			if (result instanceof  BaseDataObject){
				((BaseDataObject)result).setDataSource(BaseDataObject.DATA_SOURCE_DATA_STORE);
			}
		}

		return result;
	}

	public List<T> retrieveObjects(ClassIDPairs classIdPairs,
			String userId, Boolean shellOnly) throws SyncException {
		// We are just selecting all the columns for this object, which map to Properties and Source Relationship ID's.
		// The order of these in the SELECT doesn't matter, it just needs to match the same order that we are retrieving them
		// below when we fill in the actual object.
		List<T> results = new ArrayList<T>();

		String questionMarkString = "";
		for(int i=0; i<classIdPairs.getIds().size(); i++) {
			if (i > 0) {
				questionMarkString += ",";
			}
			questionMarkString += "?";
		}

		String sql = getSelectIn(shellOnly);
		sql = sql.replace("?", questionMarkString);
        log.debug("running retrieveObjects query: \n"+sql);

		long timeStart = System.currentTimeMillis();

        Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			IConnectionFactory connectionFactory = getConnectionRegistry().getConnectionFactory(getConnectionFactoryName());
			conn = connectionFactory.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setFetchSize(connectionFactory.getFetchSize());
			pstmt.setQueryTimeout(QUERY_TIMEOUT);

			int counter = 1;	// PreparedStatement index starts at 1.
			Iterator<String> itrIds = classIdPairs.getIds().iterator();
			while (itrIds.hasNext()) {
				String nextId = itrIds.next();
				pstmt.setString(counter, nextId);
				counter++;
			}

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				T nextResult = extractObjectFromResultSet(rs, shellOnly, null);
				if (nextResult instanceof  BaseDataObject){
					((BaseDataObject)nextResult).setDataSource(BaseDataObject.DATA_SOURCE_DATA_STORE);
				}
				results.add(nextResult);
			}
		} catch(Exception e) {
			log.error("Unable to retrieveObjects\n" + sql, e);
			throw new SyncDataException(e);
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				log.error("Error closing database statement/connection", e);
			}
		}

		long timeEnd = System.currentTimeMillis();
		long totalTime = timeEnd - timeStart;
		if (totalTime > LONG_RUNNING_QUERY_TIME) {
			String idsStr = "";
			Iterator<String> itrIds = classIdPairs.getIds().iterator();
			int counter = 0;
			while (itrIds.hasNext()) {
				String nextId = itrIds.next();
				if (counter > 0) {
					idsStr += ",";
				}
				idsStr += nextId;
				counter++;
			}
			log.warn("LONG RUNNING QUERY: " + totalTime + "ms\n" + sql + "\n     Ids: " + idsStr);
		}

		return results;
	}

	public List<T> retrieveAllByRelationship(MappedField mappedField, ClassIDPair targetClassIdPair, Boolean shellOnly, String userId) throws SyncException {

		String sql = getSelectByRelationship(mappedField.getJoinColumnName(), shellOnly);

		List<T> results = executeSelectById(sql, targetClassIdPair.getID(), shellOnly);
		return results;
	}

	public List<T> findByExample(T theQueryObject,
			List<String> excludeProperties, String userId, Boolean shellOnly) throws SyncException {
		return null;
	}

	/**
	 * @param selectQueryString
	 * @return
	 * @throws SyncDataException
	 */
	protected List<T> executeSelect(String selectQueryString, Boolean shellOnly)
			throws SyncDataException {
		List<T> results = new ArrayList<T>();
		log.debug("running executeSelect query: \n"+selectQueryString);

		long timeStart = System.currentTimeMillis();

		// Open the database session.
		Connection conn = null;
		Statement stmt = null;
		try {
			IConnectionFactory connectionFactory = getConnectionRegistry().getConnectionFactory(getConnectionFactoryName());
			conn = connectionFactory.getConnection();
			stmt = conn.createStatement();
			stmt.setFetchSize(connectionFactory.getFetchSize());
			stmt.setQueryTimeout(QUERY_TIMEOUT);

	        ResultSet rs = stmt.executeQuery(selectQueryString);
	        while (rs.next()) {
	        	T nextResult = extractObjectFromResultSet(rs, shellOnly, null);
				if (nextResult instanceof  BaseDataObject){
					((BaseDataObject)nextResult).setDataSource(BaseDataObject.DATA_SOURCE_DATA_STORE);
				}
    			results.add(nextResult);
	        }
		} catch(Exception e) {
			log.error("Unable to retrieveObjects\n" + selectQueryString, e);
			throw new SyncDataException(e);
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				log.error("Error closing database statement/connection", e);
			}
		}

		long timeEnd = System.currentTimeMillis();
		long totalTime = timeEnd - timeStart;
		if (totalTime > LONG_RUNNING_QUERY_TIME) {
			log.warn("LONG RUNNING QUERY: " + totalTime + "ms\n" + selectQueryString);
		}

		return results;
	}

	protected List<T> executeSelectById(String selectQueryString, String id, Boolean shellOnly)
			throws SyncDataException {
		List<T> results = new ArrayList<T>();
		
		if (!StringUtils.hasText(id) || "null".equalsIgnoreCase(id)) {
			// Make sure we have a valid ID.
			log.debug("SelectById: Skipping NULL or empty Id");
			return results;
		}
		
		log.debug("running selectById query: \n"+selectQueryString+"\nID: "+id);

		long timeStart = System.currentTimeMillis();

		// Open the database session.
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			IConnectionFactory connectionFactory = getConnectionRegistry().getConnectionFactory(getConnectionFactoryName());
			conn = connectionFactory.getConnection();
			pstmt = conn.prepareStatement(selectQueryString);
			pstmt.setFetchSize(connectionFactory.getFetchSize());
			pstmt.setQueryTimeout(QUERY_TIMEOUT);
			pstmt.setString(1, id);

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				T nextResult = extractObjectFromResultSet(rs, shellOnly, null);
				if (nextResult instanceof  BaseDataObject){
					((BaseDataObject)nextResult).setDataSource(BaseDataObject.DATA_SOURCE_DATA_STORE);
				}
				results.add(nextResult);
			}
		} catch(Exception e) {
			log.error("Unable to retrieveObject:\n" + selectQueryString + "\n     ID: " + id, e);
			throw new SyncDataException(e);
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				log.error("Error closing database statement/connection", e);
			}
		}

		long timeEnd = System.currentTimeMillis();
		long totalTime = timeEnd - timeStart;
		if (totalTime > LONG_RUNNING_QUERY_TIME) {
			log.warn("LONG RUNNING QUERY: " + totalTime + "ms\n" + selectQueryString+"\n     ID: "+id);
		}

		return results;
	}

	protected List executeSelectWithParams(String selectQueryString, Object[] paramValues, Boolean shellOnly)
			throws SyncDataException {
		List<T> results = new ArrayList<T>();
		log.debug("running executeSelectWithParams query: \n"+selectQueryString+"\nparams: "+paramValues);

		long timeStart = System.currentTimeMillis();

		// Open the database session.
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			IConnectionFactory connectionFactory = getConnectionRegistry().getConnectionFactory(getConnectionFactoryName());
			conn = connectionFactory.getConnection();
			pstmt = conn.prepareStatement(selectQueryString);
			pstmt.setFetchSize(connectionFactory.getFetchSize());
			pstmt.setQueryTimeout(QUERY_TIMEOUT);

			for(int i=0; i<paramValues.length; i++) {
				pstmt.setObject(i+1, paramValues[i]);
			}

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				T nextResult = extractObjectFromResultSet(rs, shellOnly, null);
				if (nextResult instanceof  BaseDataObject){
					((BaseDataObject)nextResult).setDataSource(BaseDataObject.DATA_SOURCE_DATA_STORE);
				}
				results.add(nextResult);
			}
		} catch(Exception e) {
			log.error("Unable to retrieveObjects\n" + selectQueryString, e);
			throw new SyncDataException(e);
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				log.error("Error closing database statement/connection", e);
			}
		}

		long timeEnd = System.currentTimeMillis();
		long totalTime = timeEnd - timeStart;
		if (totalTime > LONG_RUNNING_QUERY_TIME) {
			String paramsString = "";
			for(Object nextParamValue : paramValues) {
				if (nextParamValue instanceof String) {
					paramsString += "\n     String: `" + (String) nextParamValue + "`";
				}
				else if (nextParamValue instanceof Integer) {
					paramsString += "\n     Integer: " + ((Integer) nextParamValue).toString();
				}
				else if (nextParamValue instanceof Double) {
					paramsString += "\n     Double: " + ((Double) nextParamValue).toString();
				}
				else if (nextParamValue instanceof Date) {
					paramsString += "\n     Date: " + ((Date) nextParamValue).toString();
				}
				else {
					paramsString += "\n     UNKNOWN: " + nextParamValue.toString();
				}
			}
			log.warn("LONG RUNNING QUERY: " + totalTime + "ms\n" + selectQueryString + paramsString);
		}

		return results;
	}

	protected T extractObjectFromResultSet(ResultSet rs, Boolean shellOnly, T perceroObject) throws SQLException {
		throw new SQLException("Method must be overridden");
	}

	public PerceroList<T> getAll(Integer pageNumber, Integer pageSize, Boolean returnTotal, String userId, Boolean shellOnly) throws Exception {

		boolean useLimit = pageNumber != null && pageSize != null && pageSize > 0;
		String sql = null;
		if (useLimit) {
			if (shellOnly) {
				sql = getSelectAllShellOnlyWithLimitAndOffsetSQL();
			}
			else {
				sql = getSelectAllStarWithLimitAndOffsetSQL();
			}
		}
		else {
			sql = getSelectAllSql(shellOnly);
		}
		List<T> objects = new ArrayList<T>();

		long timeStart = System.currentTimeMillis();

		// Open the database session.
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			log.debug("running getAll query: \n"+sql);

			IConnectionFactory connectionFactory = getConnectionRegistry().getConnectionFactory(getConnectionFactoryName());
			conn = connectionFactory.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setFetchSize(connectionFactory.getFetchSize());
			pstmt.setQueryTimeout(QUERY_TIMEOUT);

			if (useLimit) {
				pstmt.setInt(1, pageSize);
				pstmt.setInt(2, pageNumber * pageSize);
			}

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				T nextResult = extractObjectFromResultSet(rs, shellOnly, null);
				if (nextResult instanceof  BaseDataObject){
					((BaseDataObject)nextResult).setDataSource(BaseDataObject.DATA_SOURCE_DATA_STORE);
				}
				objects.add(nextResult);
			}
		} catch(Exception e) {
			log.error("Unable to retrieveObjects\n" + sql, e);
			throw new SyncDataException(e);
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				log.error("Error closing database statement/connection", e);
			}
		}

		long timeEnd = System.currentTimeMillis();
		long totalTime = timeEnd - timeStart;
		if (totalTime > LONG_RUNNING_QUERY_TIME) {
			log.warn("LONG RUNNING QUERY: " + totalTime + "ms\n" + sql + 
					(useLimit ? "\n     LIMIT: " + pageSize.toString() + "\n     PAGE: " + pageNumber.toString() : ""));
		}

		PerceroList<T> results = new PerceroList<T>(objects);
		results.setPageNumber(pageNumber);
		results.setPageSize(pageSize);

		if (returnTotal != null && returnTotal) {
			Integer total = countAll(userId);
			results.setTotalLength(total);
		}

		return results;
	}

	public Integer countAll(String userId) throws SyncException {

		String sql = getCountAllSQL();
        log.debug("running countAll query: \n"+sql);

		long timeStart = System.currentTimeMillis();

        // Open the database session.
		Connection conn = null;
		Statement stmt = null;
		Integer result = null;
		try {
			IConnectionFactory connectionFactory = getConnectionRegistry().getConnectionFactory(getConnectionFactoryName());
			conn = connectionFactory.getConnection();
			stmt = conn.createStatement();
			stmt.setQueryTimeout(QUERY_TIMEOUT);

	        ResultSet rs = stmt.executeQuery(sql);
	        if (rs.next()) {
	        	result = rs.getInt(1);

	        }
		} catch(Exception e) {
			log.error("Unable to executeSelectCount\n" + sql, e);
			throw new SyncDataException(e);
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				log.error("Error closing database statement/connection", e);
			}
		}

		long timeEnd = System.currentTimeMillis();
		long totalTime = timeEnd - timeStart;
		if (totalTime > LONG_RUNNING_QUERY_TIME) {
			log.warn("LONG RUNNING QUERY: " + totalTime + "ms\n" + sql);
		}

		return result;
	}

	protected int executeUpdate(String sqlStatement) throws SyncDataException {

		long timeStart = System.currentTimeMillis();

		Connection conn = null;
		Statement stmt = null;
		int result = 0;
		try {
			IConnectionFactory connectionFactory = getConnectionRegistry().getConnectionFactory(getConnectionFactoryName());
			conn = connectionFactory.getConnection();
			stmt = conn.createStatement();
			stmt.setQueryTimeout(QUERY_TIMEOUT);

	        result = stmt.executeUpdate(sqlStatement);

		} catch(Exception e) {
			log.error("Unable to executeUpdate\n" + sqlStatement, e);
			throw new SyncDataException(e);
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				log.error("Error closing database statement/connection", e);
			}
		}

		long timeEnd = System.currentTimeMillis();
		long totalTime = timeEnd - timeStart;
		if (totalTime > LONG_RUNNING_QUERY_TIME) {
			log.warn("LONG RUNNING QUERY: " + totalTime + "ms\n" + sqlStatement);
		}

		return result;
	}

	@Override
	public Boolean hasCreateAccess(ClassIDPair classIdPair, String userId) {
		// Defaults to true
		return true;
	}

	@Override
	public Boolean hasReadAccess(ClassIDPair classIdPair, String userId) {
		// Defaults to true
		return true;
	}

	@Override
	public Boolean hasUpdateAccess(ClassIDPair classIdPair, String userId) {
		// Defaults to true
		return true;
	}

	@Override
	public Boolean hasDeleteAccess(ClassIDPair classIdPair, String userId) {
		// Defaults to true
		return true;
	}

	public List<Object> runQuery(String selectQueryString, Object[] paramValues, String userId) throws SyncException {
		if (!StringUtils.hasText(selectQueryString)) {
			return new ArrayList<Object>(0);
		}
		
		List<Object> results = new ArrayList<Object>();
		log.debug("running executeSelectWithParams query: \n"+selectQueryString+"\nparams: "+paramValues);

		long timeStart = System.currentTimeMillis();

		// Open the database session.
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			IConnectionFactory connectionFactory = getConnectionRegistry().getConnectionFactory(getConnectionFactoryName());
			conn = connectionFactory.getConnection();
			pstmt = conn.prepareStatement(selectQueryString);
			pstmt.setFetchSize(connectionFactory.getFetchSize());
			pstmt.setQueryTimeout(QUERY_TIMEOUT);

			if (paramValues != null) {
				for(int i=0; i<paramValues.length; i++) {
					pstmt.setObject(i+1, paramValues[i]);
				}
			}

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				Object[] nextResult = new Object[rs.getMetaData().getColumnCount()];
				for(int i=0; i<rs.getMetaData().getColumnCount(); i++) {
					nextResult[i] = rs.getObject(i+1);
				}
				results.add(nextResult);
			}
		} catch(Exception e) {
			log.error("Unable to retrieveObjects\n" + selectQueryString, e);
			throw new SyncDataException(e);
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				log.error("Error closing database statement/connection", e);
			}
		}

		long timeEnd = System.currentTimeMillis();
		long totalTime = timeEnd - timeStart;
		if (totalTime > LONG_RUNNING_QUERY_TIME) {
			String paramsString = "";
			if (paramValues != null) {
				for(Object nextParamValue : paramValues) {
					if (nextParamValue instanceof String) {
						paramsString += "\n     String: `" + (String) nextParamValue + "`";
					}
					else if (nextParamValue instanceof Integer) {
						paramsString += "\n     Integer: " + ((Integer) nextParamValue).toString();
					}
					else if (nextParamValue instanceof Double) {
						paramsString += "\n     Double: " + ((Double) nextParamValue).toString();
					}
					else if (nextParamValue instanceof Date) {
						paramsString += "\n     Date: " + ((Date) nextParamValue).toString();
					}
					else {
						paramsString += "\n     UNKNOWN: " + nextParamValue.toString();
					}
				}
			}
			log.warn("LONG RUNNING QUERY: " + totalTime + "ms\n" + selectQueryString + paramsString);
		}

		return results;
	}

	// TODO: Fill this out
	public 	T cleanObjectForUser(T perceroObject,
			String userId) {
		return perceroObject;
	}

	protected String getUpdateCallableStatementSql() {
		return "";
	}
    protected String getUpdateCallableStatementParams() {
        return "?";
    }
	protected String getInsertCallableStatementSql() {
		return "";
	}
	protected String getInsertCallableStatementParams() {
		return "?";
	}
	protected String getDeleteCallableStatementSql() {
		return "";
	}

	protected void setCallableStatementInsertParams(T perceroObject, CallableStatement pstmt) throws SQLException {

	}

	protected void setCallableStatementUpdateParams(T perceroObject, CallableStatement pstmt) throws SQLException  {

	}

	protected int runStoredProcedure(T perceroObject, String userId, String storedProcedureCallSql) throws ConnectorException {
		int result = 0;
		IConnectionFactory connectionFactory = getConnectionRegistry().getConnectionFactory(getConnectionFactoryName());
		try (Connection conn = connectionFactory.getConnection(); Statement statement = conn.createStatement()) {
			CallableStatement cstmt = conn.prepareCall(storedProcedureCallSql);
			setBaseStatementInsertParams(perceroObject, userId, cstmt);
			result = cstmt.executeUpdate();
		} catch (SQLException e) {
			try (Connection conn = connectionFactory.getConnection(); Statement statement = conn.createStatement()) {
				CallableStatement cstmt = conn.prepareCall(storedProcedureCallSql);
				setBaseStatementInsertParams(perceroObject, userId, cstmt);
			} catch(SQLException e1) {
				e1.printStackTrace();
			}
			log.error(e.getMessage(), e);
			throw new ConnectorException(e);
		}
		
		return result;
	}
	

	@Override
	public T createObject(T perceroObject, String userId)
			throws SyncException {
		if ( !hasCreateAccess(BaseDataObject.toClassIdPair(perceroObject), userId) ) {
			return null;
		}
		
		long timeStart = System.currentTimeMillis();
		
		int result = runInsertStoredProcedure(perceroObject, userId);
		
		long timeEnd = System.currentTimeMillis();
		long totalTime = timeEnd - timeStart;
		if (totalTime > LONG_RUNNING_QUERY_TIME) {
			log.warn("LONG RUNNING QUERY: " + totalTime + "ms\n" + "Insert ThresholdExceededNotification");
		}
		
		if (result > 0) {
			return retrieveObject(BaseDataObject.toClassIdPair(perceroObject), userId, false);
		}
		else {
			return null;
		}
	}

	protected int runInsertStoredProcedure(T perceroObject, String userId) throws ConnectorException {
		int result = runStoredProcedure(perceroObject, userId, getInsertCallableStatementSql());
		return result;
	}
	
	protected void setBaseStatementInsertParams(T perceroObject, String userId, PreparedStatement pstmt) throws SQLException {
		// Do nothing
	}
	
	public T updateObject(T perceroObject,
			Map<ClassIDPair, Collection<MappedField>> changedFields, String userId)
			throws SyncException {
		if (!hasUpdateAccess(BaseDataObject.toClassIdPair(perceroObject), userId)) {
			return null;
		}
		
		long timeStart = System.currentTimeMillis();
		
		int updateResult = runUpdateStoredProcedure(perceroObject, userId);
		
		long timeEnd = System.currentTimeMillis();
		long totalTime = timeEnd - timeStart;
		if (totalTime > LONG_RUNNING_QUERY_TIME) {
			log.warn("LONG RUNNING QUERY: " + totalTime + "ms\n" + "Insert ThresholdExceededNotification");
		}

		T result = null;
		if (updateResult > 0) {
			result = retrieveObject(BaseDataObject.toClassIdPair(perceroObject), userId, false);
		}
		
		return result;
	}

	protected int runUpdateStoredProcedure(T perceroObject, String userId) throws ConnectorException {
		int result = runStoredProcedure(perceroObject, userId, getUpdateCallableStatementSql());
		return result;
	}
	
	public Boolean deleteObject(ClassIDPair classIdPair, String userId)
			throws SyncException {
		if ( !hasDeleteAccess(classIdPair, userId) ) {
			return false;
		}
		
		long timeStart = System.currentTimeMillis();
		
		int updateResult = runDeleteStoredProcedure(classIdPair, userId);
		
		long timeEnd = System.currentTimeMillis();
		long totalTime = timeEnd - timeStart;
		if (totalTime > LONG_RUNNING_QUERY_TIME) {
			log.warn("LONG RUNNING QUERY: " + totalTime + "ms\n" + "Insert ThresholdExceededNotification");
		}
		
		return (updateResult > 0);
	}

	protected int runDeleteStoredProcedure(ClassIDPair classIdPair, String userId) throws ConnectorException {
		int result = 0;
		IConnectionFactory connectionFactory = getConnectionRegistry().getConnectionFactory(getConnectionFactoryName());
		try (Connection conn = connectionFactory.getConnection(); Statement statement = conn.createStatement()) {
			CallableStatement cstmt = conn.prepareCall(getDeleteCallableStatementSql());
			cstmt.setString(1, classIdPair.getID());
			cstmt.setString(2, userId);
			result = cstmt.executeUpdate();
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			throw new ConnectorException(e);
		}

		return result;
	}

	public void initializeStoredProcedures() throws ConnectorException {
		// Do nothing.
	}
	
}
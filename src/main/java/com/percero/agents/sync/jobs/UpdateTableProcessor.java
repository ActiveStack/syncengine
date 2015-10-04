package com.percero.agents.sync.jobs;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.cache.CacheManager;
import com.percero.agents.sync.helpers.PostDeleteHelper;
import com.percero.agents.sync.helpers.PostPutHelper;
import com.percero.agents.sync.metadata.*;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.agents.sync.services.IDataProvider;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.bl.IManifest;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.util.StringUtils;

import javax.persistence.Table;
import java.sql.*;
import java.util.*;

/**
 * Responsible for querying an update table and processing the rows.
 * Created by Jonathan Samples<jonnysamps@gmail.com> on 8/31/15.
 */
public class UpdateTableProcessor {

    protected static Logger logger = Logger.getLogger(UpdateTableProcessor.class);
    public static final int INFINITE_ROWS = -1;

    protected static final int EXPIRATION_TIME = 1000*60*30; // 30 minutes
    protected String tableName;
    protected UpdateTableConnectionFactory connectionFactory;
    protected PostDeleteHelper postDeleteHelper;
    protected PostPutHelper postPutHelper;
    protected IManifest manifest;
    protected CacheManager cacheManager;
    protected DataProviderManager dataProviderManager;
    protected IAccessManager accessManager;
    protected int maxRowsToProcess = INFINITE_ROWS; // No max

    public UpdateTableProcessor(String tableName,
                                UpdateTableConnectionFactory connectionFactory,
                                IManifest manifest,
                                PostDeleteHelper postDeleteHelper,
                                PostPutHelper postPutHelper,
                                CacheManager cacheManager,
                                DataProviderManager dataProviderManager,
                                IAccessManager accessManager)
    {
        this.tableName          = tableName;
        this.connectionFactory  = connectionFactory;
        this.postDeleteHelper   = postDeleteHelper;
        this.postPutHelper      = postPutHelper;
        this.manifest           = manifest;
        this.cacheManager       = cacheManager;
        this.dataProviderManager= dataProviderManager;
        this.accessManager      = accessManager;
        this.maxRowsToProcess   = connectionFactory.getWeight();
    }


    /**
     * Update table schema looks like this
     *
     * `ID` int(11) NOT NULL AUTO_INCREMENT,
     * `tableName` varchar(255) DEFAULT NULL,
     * `row_ID` varchar(255) DEFAULT NULL,
     * `type` enum('INSERT','UPDATE','DELETE') NOT NULL DEFAULT 'UPDATE',
     * `lock_ID` int(11) DEFAULT NULL,
     * `lock_Date` datetime DEFAULT NULL,
     * `time_stamp` timestamp DEFAULT CURRENT_TIMESTAMP
     *
     * @return
     */
    public ProcessorResult process(){
        ProcessorResult result = new ProcessorResult();

        int numRowsProcessed = 0;
        while(numRowsProcessed < maxRowsToProcess || maxRowsToProcess == INFINITE_ROWS) {
            UpdateTableRow row = getRow();
            if(row == null) break;

            try {
                if (processRow(row)) {
                    result.addResult(row.getType().toString());
                    deleteRow(row);
                } else {
                    result.addResult(row.getType().toString(), false, "");
                }
            }catch(Exception e){
                logger.warn("Failed to process update: "+ e.getMessage(), e);
                result.addResult(row.getType().toString(), false, e.getMessage());
            }
            numRowsProcessed++;
        }

        return result;
    }

    protected boolean processRow(UpdateTableRow row) throws Exception{
        boolean result = true;
        logger.debug("UpdateTableProcessor: processRow");
        if(row.getRowId() != null)
            switch (row.getType()){
                case DELETE:
                    result = processDeleteSingle(row);
                    break;
                case UPDATE:
                    result = processUpdateSingle(row);
                    break;
                case INSERT:
                    result = processInsertSingle(row);
                    break;
                default: // Don't know how to process
                    result = true;
                    break;
            }
        else
            switch (row.getType()){
                case DELETE:
                    result = processDeleteTable(row);
                    break;
                case UPDATE:
                    result = processUpdateTable(row);
                    break;
                case INSERT:
                    result = processInsertTable(row);
                    break;
                default: // Don't know how to process
                    result = true;
                    break;
            }

        return result;
    }

    /**
     * Process a single record update
     * @param row
     * @return
     */
    @SuppressWarnings("rawtypes")
    protected boolean processUpdateSingle(UpdateTableRow row) throws Exception{
        Class clazz = getClassForTableName(row.getTableName());

        // If we found the class then we care about this row, otherwise return true and the row will be deleted
        if(clazz != null) {
            String className = clazz.getCanonicalName();

            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            MappedClass mappedClass = mcm.getMappedClassByClassName(className);
            IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);

            ClassIDPair pair = new ClassIDPair(row.getRowId(), className);
            handleUpdateClassIdPair(dataProvider, pair);

            // We need to update ALL referencing objects in the case that a
            // relationship was updated. Since we don't have the OLD object, we have
            // no way of telling what may have changed.
            updateReferences(className);
        }

        return true;
    }

    /**
     * Process a whole table with updates
     * @param row
     * @return
     */
    @SuppressWarnings("rawtypes")
	protected boolean processUpdateTable(UpdateTableRow row) throws Exception{
    	Class clazz = getClassForTableName(row.getTableName());

        if(clazz != null) {
            String className = clazz.getCanonicalName();

            // If there are any clients that have asked for all objects in a class then we have to push everything
            if (accessManager.getNumClientsInterestedInWholeClass(className) > 0) {

                IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
                MappedClass mappedClass = mcm.getMappedClassByClassName(className);
                IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);

                Integer pageNumber = 0;
                Integer pageSize = 25;
                Integer total = -1;

                while (total < 0 || pageNumber * pageSize <= total) {
                    PerceroList<IPerceroObject> objectsToUpdate = dataProvider.getAllByName(className, pageNumber, pageSize, true, null);
                    pageNumber++;
                    total = objectsToUpdate.getTotalLength();
                    if (total <= 0) {
                        break;
                    }

                    Set<String> objectIds = new HashSet<String>(objectsToUpdate.size());
                    Iterator<IPerceroObject> itrObjectsToUpdate = objectsToUpdate.iterator();
                    while (itrObjectsToUpdate.hasNext()) {
                        IPerceroObject nextObjectToUpdate = itrObjectsToUpdate.next();
                        objectIds.add(nextObjectToUpdate.getID());
                    }

                    processUpdates(className, objectIds);
                }
            } else {
                processUpdates(className, accessManager.getClassAccessJournalIDs(className));
            }

            updateReferences(className);
        }

        return true;
    }

    /**
     * Takes a class and list of ids that need to be pushed out
     * @param className
     * @param Ids
     * @throws Exception
     */
    protected void processUpdates(String className, Collection<String> Ids) throws Exception{
        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mappedClass = mcm.getMappedClassByClassName(className);
        IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);

        for(String ID : Ids) {
            ClassIDPair pair = new ClassIDPair(ID, className);
            handleUpdateClassIdPair(dataProvider, pair);
        }
    }

    protected void handleUpdateClassIdPair(IDataProvider dataProvider, ClassIDPair pair) throws Exception {
    	IPerceroObject object = dataProvider.findById(pair, null, true);

    	if (object != null) {
    		cacheManager.updateCachedObject(object, null);
    		postPutHelper.postPutObject(pair, null, null, true, null);
    	}
    }

    /**
     * process a single record insert
     * @param row
     * @return
     */
    @SuppressWarnings("rawtypes")
	protected boolean processInsertSingle(UpdateTableRow row) throws Exception{
        Class clazz = getClassForTableName(row.getTableName());

        // If clazz is not null then we care about this row, otherwise throw the row away
        if(clazz != null) {
            String className = clazz.getCanonicalName();

            // We do not use PostCreateHelper here because we are going to do all
            // that extra work for the whole class in updateReferences.
            postPutHelper.postPutObject(new ClassIDPair(row.getRowId(), className), null, null, true, null);

            updateReferences(className);
        }

        return true;
    }

    /**
     * Process a whole table with inserts
     * @param row
     * @return
     */
    @SuppressWarnings("rawtypes")
	protected boolean processInsertTable(UpdateTableRow row) throws Exception {
        Class clazz = getClassForTableName(row.getTableName());

        if(clazz != null) {
            String className = clazz.getCanonicalName();

            // if any client needs all of this class then the only choice we have is to push everything
            if (accessManager.getNumClientsInterestedInWholeClass(className) > 0 /* || true */) {
                Set<ClassIDPair> allClassIdPairs = getAllClassIdPairsForTable(row.getTableName());
                for (ClassIDPair classIdPair : allClassIdPairs) {
                    // We do not use PostCreateHelper here because we are going to
                    // do all that extra work for the whole class in
                    // updateReferences.
                    postPutHelper.postPutObject(classIdPair, null, null, true, null);
                }
            }

            updateReferences(className);
        }

        return true;
    }

    /**
     * Process a single record delete
     * @param row
     * @return
     */
    @SuppressWarnings("rawtypes")
	protected boolean processDeleteSingle(UpdateTableRow row) throws Exception{
        Class clazz = getClassForTableName(row.getTableName());

        if(clazz != null) {
            String className = clazz.getCanonicalName();

            // See if this object is in the cache.  If so, it will help us know which related objects to update.
            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            MappedClass mappedClass = mcm.getMappedClassByClassName(className);
            IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
            ClassIDPair pair = new ClassIDPair(row.getRowId(), className);
            IPerceroObject cachedObject = dataProvider.findById(pair, null, false);    // We are hoping to find this object in the cache...

            handleDeletedObject(cachedObject, clazz, className, row.getRowId());

            updateReferences(className);
        }

        return true;
    }

    /**
     * process a whole table with deletes
     * @param row
     * @return
     */
    @SuppressWarnings("rawtypes")
	protected boolean processDeleteTable(UpdateTableRow row) throws Exception{
        Class clazz = getClassForTableName(row.getTableName());

        if(clazz != null) {
            String className = clazz.getCanonicalName();

            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            MappedClass mappedClass = mcm.getMappedClassByClassName(className);
            IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);

            // Get the list of ALL ID's of this class type that have been accessed.
            Set<String> accessedIds = accessManager.getClassAccessJournalIDs(className);

            if (accessedIds != null && !accessedIds.isEmpty()) {
                // TODO: If ID "0", then that means someone wants to know about ALL
                // records of this type. How do we do this?

                // Get a list of ALL ID's of this class type.
                Set<ClassIDPair> allClassIdPairs = getAllClassIdPairsForTable(row.getTableName());

                // Remove ALL existing/current ID's from our list of accessed ID's.
                for (ClassIDPair nextClassIdPair : allClassIdPairs) {
                    accessedIds.remove(nextClassIdPair.getID());
                }

                // Now we have the list of ID's that have actually been deleted.
                for (String id : accessedIds) {
                    // Find the cached object first so that it is NOT removed if the same object is NOT found in the data store.
                    IPerceroObject cachedObject = dataProvider.findById(new ClassIDPair(id, className), null, false);
                    // We will know an object has been deleted IFF it does NOT exist in the data store.
                    IPerceroObject dataStoreObject = dataProvider.findById(new ClassIDPair(id, className), null, true);

                    if (dataStoreObject != null) {
                        // Object has NOT been deleted.
                        continue;
                    }

                    handleDeletedObject(cachedObject, clazz, className, id);
                }
            }

            updateReferences(className);
        }

        return true;
    }

    @SuppressWarnings("rawtypes")
	protected void handleDeletedObject(IPerceroObject cachedObject, Class clazz, String className, String id) throws Exception {
    	boolean isShellObject = false;
        if (cachedObject == null) {
        	cachedObject = (IPerceroObject) clazz.newInstance();
        	cachedObject.setID(id);
        	isShellObject = true;
        }

		cacheManager.handleDeletedObject(cachedObject, className, isShellObject);

        postDeleteHelper.postDeleteObject(new ClassIDPair(id, className), null, null, true);
    }

    @SuppressWarnings("rawtypes")
	protected Set<ClassIDPair> getAllClassIdPairsForTable(String tableName) throws Exception{
        Class clazz = getClassForTableName(tableName);
        String className = clazz.getCanonicalName();

        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mappedClass = mcm.getMappedClassByClassName(className);
        IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
        Set<ClassIDPair> results = dataProvider.getAllClassIdPairsByName(className);

        return results;
    }


    /**
     * Finds all back references to this class and pushes updates to all of them.
     * @param className
     */
    protected void updateReferences(String className){
        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mappedClass = mcm.getMappedClassByClassName(className);
		// Go through each mapped field and push all objects of that associated
		// type (just in case any has a reference to a new row
		// in the updated table)
        // --
		// TODO: is this right? Is it enough to only check the relationships on
		// this class or do we need to look
		// through all of the mapped classes?
        for(MappedFieldPerceroObject nextMappedField : mappedClass.externalizablePerceroObjectFields) {
            try {
                // Only care about it if it has a reverse relationship
                MappedField mappedField = nextMappedField.getReverseMappedField();
                if (mappedField != null) {
                    // Find all of this type and push down an update to all
                    Set<String> ids = accessManager.getClassAccessJournalIDs(mappedField.getMappedClass().className);

                    if (ids.contains("0")) {
                    	// If there is a 0 ID in the list, then we need to update ALL records of this type.
                    	Integer pageNumber = 0;
                    	Integer pageSize = 25;
                    	Integer total = -1;

                    	while (total < 0 || pageNumber * pageSize <= total) {
	                    	PerceroList<IPerceroObject> objectsToUpdate = mappedField.getMappedClass().getDataProvider().getAllByName(mappedField.getMappedClass().className, pageNumber, pageSize, true, null);
	                    	pageNumber++;
	                    	total = objectsToUpdate.getTotalLength();
	                    	if (total <= 0) {
	                    		break;
	                    	}

		        			Iterator<IPerceroObject> itrObjectsToUpdate = objectsToUpdate.iterator();
		        			while (itrObjectsToUpdate.hasNext()) {
		        				IPerceroObject nextObjectToUpdate = itrObjectsToUpdate.next();
		        				ClassIDPair pair = BaseDataObject.toClassIdPair(nextObjectToUpdate);
		        				Map<ClassIDPair, Collection<MappedField>> changedFields = new HashMap<ClassIDPair, Collection<MappedField>>();
		        				Collection<MappedField> changedMappedFields = new ArrayList<MappedField>(1);
		        				changedMappedFields.add(mappedField);
		        				changedFields.put(pair, changedMappedFields);

		        				// Remove from the cache.
		        				cacheManager.deleteObjectFromCache(pair);
		        				postPutHelper.postPutObject(pair, null, null, true, changedFields);
		        			}
                    	}
                    }
                    else {
	        			Iterator<String> itrIdsToUpdate = ids.iterator();
	        			while (itrIdsToUpdate.hasNext()) {
	        				String nextIdToUpdate = itrIdsToUpdate.next();
	        				ClassIDPair pair = new ClassIDPair(nextIdToUpdate, mappedField.getMappedClass().className);
	        				Map<ClassIDPair, Collection<MappedField>> changedFields = new HashMap<ClassIDPair, Collection<MappedField>>();
	        				Collection<MappedField> changedMappedFields = new ArrayList<MappedField>(1);
	        				changedMappedFields.add(mappedField);
	        				changedFields.put(pair, changedMappedFields);

	        				// Remove from the cache.
	        				cacheManager.deleteObjectFromCache(pair);
	        				postPutHelper.postPutObject(pair, null, null, true, changedFields);
	        			}
                    }
                }
            } catch(Exception e) {
                logger.error("Error in postCreateObject " + mappedClass.className + "." + nextMappedField.getField().getName(), e);
            }
        }
    }

    /**
     * Pulls a row off the update table and locks it so that other
     * processors don't duplicate the work
     * @return
     */
    public UpdateTableRow getRow(){
        UpdateTableRow row = null;

        try(Connection conn = connectionFactory.getConnection();
            Statement statement = conn.createStatement())
        {

            Random rand = new Random();

            int lockId = rand.nextInt();
            long now = System.currentTimeMillis();

            DateTime expireThreshold = new DateTime(now - EXPIRATION_TIME);

            if (StringUtils.hasText(connectionFactory.getStoredProcedureName())) {
                try {
                    row = getStoredProcRow(row, conn, statement, lockId, expireThreshold);
                }catch(Exception e){
                    row = getUpdateSelectRow(row, statement, lockId, expireThreshold);
                }
            }
            else {
            	row = getUpdateSelectRow(row, statement, lockId, expireThreshold);
            }

        } catch(SQLException e){
            logger.warn(e.getMessage(), e);
        }

        return row;
    }

	/**
	 * @param row
	 * @param statement
	 * @param lockId
	 * @param expireThreshold
	 * @return
	 * @throws SQLException
	 */
	private UpdateTableRow getUpdateSelectRow(UpdateTableRow row,
			Statement statement, int lockId, DateTime expireThreshold)
			throws SQLException {
		/**
		 * First try to lock a row
		 */
		String sql = connectionFactory.getUpdateStatementSql();
		sql = sql.replace(":tableName", tableName);
		sql = sql.replace(":lockId", lockId+"");
		sql = sql.replace(":expireThreshold", expireThreshold.toString("Y-MM-dd HH:mm:ss"));

		int numUpdated = statement.executeUpdate(sql);

		// Found a row to process
		if(numUpdated > 0){
		    sql = "select * from :tableName where lock_id=:lockId limit 1";
		    sql = sql.replace(":tableName", tableName);
		    sql = sql.replace(":lockId", lockId+"");

		    try(ResultSet rs = statement.executeQuery(sql)){
		        // If got a row back
		        if(rs.next())
		            row = fromResultSet(rs);
		        else
		            logger.warn("Locked a row but couldn't retrieve");
		    }
		}
		return row;
	}

	/**
	 * @param row
	 * @param conn
	 * @param lockId
	 * @param expireThreshold
	 * @return
	 * @throws SQLException
	 */
	private UpdateTableRow getStoredProcRow(UpdateTableRow row,
			Connection conn, Statement statement, int lockId, DateTime expireThreshold)
			throws SQLException {
		Integer updateTableId = null;
		try {
		    CallableStatement cstmt = conn.prepareCall("{call " + connectionFactory.getStoredProcedureName() + "(?, ?, ?)}");
		    cstmt.setInt(1, lockId);
		    cstmt.setString(2, expireThreshold.toString("Y-MM-dd HH:mm:ss"));
		    cstmt.registerOutParameter(3, Types.INTEGER);
		    cstmt.executeUpdate();
		    updateTableId = cstmt.getInt(3);
		} catch(SQLException e){
//            	return null;
		    logger.warn(e.getMessage(), e);

		    // If the stored proc doesn't exist, let's try and create it.
		    if (StringUtils.hasText(connectionFactory.getStoredProcedureDefinition()) &&
                    e.getMessage().toLowerCase().contains(connectionFactory.getStoredProcedureName().toLowerCase())
                    && e.getMessage().toLowerCase().contains("must be declared")) {
		    	try {
		    		Statement stmtCreateStoredProcedure = conn.createStatement();
		    		boolean createResult = stmtCreateStoredProcedure.execute(connectionFactory.getStoredProcedureDefinition()
                            .replaceAll(":tableName", tableName)
                            .replaceAll(":storedProcedureName", connectionFactory.getStoredProcedureName()));
		    		System.out.println(createResult);
		    	}
		    	catch(SQLSyntaxErrorException ssee) {
		    		logger.warn("Unable to create UpdateTable stored procedure: " + ssee.getMessage());
                    throw ssee;
		    	}
		    	catch(Exception e1) {
		    		logger.warn("Unable to create UpdateTable stored procedure: " + e1.getMessage());
                    throw e1;
		    	}
		    }
		}

		if (updateTableId != null && updateTableId > -1) {
		    String sql = "select * from :tableName where id=:id";
		    sql = sql.replace(":tableName", tableName);
		    sql = sql.replace(":id", updateTableId.toString());

		    try(ResultSet rs = statement.executeQuery(sql)){
		        // If got a row back
		        if(rs.next())
		            row = fromResultSet(rs);
		        else
		            logger.warn("Locked a row but couldn't retrieve");
		    }
		}
		return row;
	}

	/**
     * Deletes the row
     * @param row
     */
    protected void deleteRow(UpdateTableRow row){
        try(Connection conn = connectionFactory.getConnection()){
            String sql = "delete from :tableName where ID=:ID";
            sql = sql.replace(":tableName", tableName);
            sql = sql.replace(":ID", row.getID()+"");
            Statement statement = conn.createStatement();
            int numUpdated = statement.executeUpdate(sql);
            if(numUpdated != 1){
                logger.warn("Expected to delete 1, instead "+numUpdated);
            }
        }catch(SQLException e){
            logger.warn(e.getMessage(), e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public Class getClassForTableName(String tableName){
        Class result = null;

        // First look for the @Table annotation
        for(Class c : manifest.getClassList()){
            Table table = (Table) c.getAnnotation(Table.class);
            if(table != null && tableName.equals(table.name())) {
                result = c;
                break;
            }
        }

        // If we didn't find that now look for the simple class name to match
        if(result == null){
            for(Class c : manifest.getClassList()){
                if(tableName.equals(c.getSimpleName())) {
                    result = c;
                    break;
                }
            }
        }

        return result;
    }


    protected UpdateTableRow fromResultSet(ResultSet resultSet) throws SQLException{
        UpdateTableRow row = new UpdateTableRow();
        row.ID          = resultSet.getInt("ID");
        row.tableName   = resultSet.getString("tableName");
        row.rowId       = resultSet.getString("row_id");
        row.lockId      = resultSet.getInt("lock_id");
        row.lockDate    = resultSet.getDate("lock_date");
        try {
        	row.type    = UpdateTableRowType.valueOf(resultSet.getString("type"));
        } catch(IllegalArgumentException iae) {
        	logger.warn("Invalid UpdateTableRow TYPE, ignoring");
        	row.type    = UpdateTableRowType.NONE;
        }
        row.timestamp   = resultSet.getDate("time_stamp");

        return row;
    }
}

package com.percero.agents.sync.jobs;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.cache.CacheManager;
import com.percero.agents.sync.helpers.PostDeleteHelper;
import com.percero.agents.sync.helpers.PostPutHelper;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.metadata.MappedFieldPerceroObject;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.agents.sync.services.IDataProvider;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.bl.IManifest;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;

/**
 * Responsible for querying an update table and processing the rows.
 * Created by Jonathan Samples<jonnysamps@gmail.com> on 8/31/15.
 */
public class UpdateTableProcessor implements Runnable{

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

    private Connection connection;

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
        
        classNamesToUpdateReferences = new HashSet<String>();
    }

    
	// TODO: This should be moved to be stored in a database table. Could
	// possibly use the existing "entire table update" mechanism to handle this
	// (for each reverse mapped class)
    private Set<String> classNamesToUpdateReferences = null;

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
    public void run(){
        try {
            ProcessorResult result = new ProcessorResult();

            while (true) {
                Date startTime = new Date();
                int numRows = 160;
                List<UpdateTableRow> rows = getRows(numRows);
                if (rows.size() <= 0) break;

                List<UpdateTableRow> successfulRows = new ArrayList<>();
                for(UpdateTableRow row : rows) {
                    try {
                        if (processRow(row)) {
                            result.addResult(row.getType().toString());
                            successfulRows.add(row);
                        } else {
                            result.addResult(row.getType().toString(), false, "");
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to process update: " + e.getMessage(), e);
                        result.addResult(row.getType().toString(), false, e.getMessage());
                    }
                }
                deleteRows(successfulRows);

                Date endTime = new Date();
                UpdateTableProcessReporter.getInstance()
                        .submitCountAndTime(tableName, successfulRows.size(), endTime.getTime() - startTime.getTime());
            }
            
            for(String className : classNamesToUpdateReferences) {
            	updateReferences(className);
            }
            
            if (!result.isSuccess()) {
                logger.warn("Update table processor (" + tableName + ") failed. Details:");
                logger.warn(result);
            }
        }finally{
            try {
                Connection conn = getConnection();
                conn.close();
            }catch(Exception e){}
        }
    }

    protected boolean processRow(UpdateTableRow row) throws Exception{
        boolean result = true;
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
        List<Class> classes = getClassesForTableName(row.getTableName());
        
        for(Class clazz : classes) {
            String className = clazz.getCanonicalName();

            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            MappedClass mappedClass = mcm.getMappedClassByClassName(className);
            IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
            
            ClassIDPair pair = new ClassIDPair(row.getRowId(), className);
            handleUpdateClassIdPair(dataProvider, pair);
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
        List<Class> classes = getClassesForTableName(row.getTableName());

        for(Class clazz : classes) {
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
        // Attempt to retrieve from the cache so that we have the "OLD" value.
    	IPerceroObject oldValue = dataProvider.retrieveCachedObject(pair);
		// Now retrieve the object directly from the data source so that we
		// can compare it to the cached/old value.
        IPerceroObject perceroObject = dataProvider.findById(pair, null, true);
        // If PerceroObject is NULL, then it no longer exists and we can drop this update.
        if (perceroObject == null) {
        	return;
        }

        Map<ClassIDPair, Collection<MappedField>> changedFields = null;
        if (oldValue != null) {
			// dataProvider.getChangedMappedFields is typically used to
			// compare a new object, but we can use the cached object in
			// this case (we just need to tell getChangedMappedFields to NOT
			// use the cache).
			changedFields = dataProvider
					.getChangedMappedFields(perceroObject, oldValue);
			if (changedFields == null || changedFields.size() > 0) {
				// Something has changed.
				cacheManager.updateCachedObject(perceroObject, changedFields);
				postPutHelper.postPutObject(pair, null, null, true, changedFields);
		
				Iterator<Map.Entry<ClassIDPair, Collection<MappedField>>> itrChangedFieldEntryset = changedFields.entrySet().iterator();
				while (itrChangedFieldEntryset.hasNext()) {
					Map.Entry<ClassIDPair, Collection<MappedField>> nextEntry = itrChangedFieldEntryset.next();
					ClassIDPair thePair = nextEntry.getKey();
					Collection<MappedField> changedMappedFields = nextEntry.getValue();
					
					// If thePair is NOT the object being updated, then need to run the postPutHelper for the Pair object as well.
					if (!thePair.equals(pair)) {
						Map<ClassIDPair, Collection<MappedField>> thePairChangedFields = new HashMap<ClassIDPair, Collection<MappedField>>(1);
						thePairChangedFields.put(thePair, changedMappedFields);
						
						postPutHelper.postPutObject(thePair, null, null, true, thePairChangedFields);
					}
					else {
						Iterator<MappedField> itrChangedFields = changedMappedFields.iterator();
						String[] fieldNames = new String[changedMappedFields.size()];
						int i = 0;
						while (itrChangedFields.hasNext()) {
							MappedField nextChangedField = itrChangedFields.next();
							fieldNames[i] = nextChangedField.getField().getName();
							i++;
						}
						accessManager.checkChangeWatchers(thePair, fieldNames, null);
					}
				}
			}
        }
        else {
        	cacheManager.updateCachedObject(perceroObject, null);
        	postPutHelper.postPutObject(pair, null, null, true, null);

        	// We don't have any record of the old value, so we need to
			// update ALL referencing objects in the case that a
			// relationship was updated. Since we don't have the OLD object,
			// we have no way of telling what may have changed.
        	classNamesToUpdateReferences.add(pair.getClassName());
		}
    }

    /**
     * process a single record insert
     * @param row
     * @return
     */
    @SuppressWarnings("rawtypes")
    protected boolean processInsertSingle(UpdateTableRow row) throws Exception{
        List<Class> classes = getClassesForTableName(row.getTableName());

        for(Class clazz : classes) {
            String className = clazz.getCanonicalName();

            // We do not use PostCreateHelper here because we are going to do all
            // that extra work for the whole class in updateReferences.
            postPutHelper.postPutObject(new ClassIDPair(row.getRowId(), className), null, null, true, null);

        	classNamesToUpdateReferences.add(className);
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
        List<Class> classes = getClassesForTableName(row.getTableName());

        for(Class clazz : classes) {
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

        	classNamesToUpdateReferences.add(className);
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
        List<Class> classes = getClassesForTableName(row.getTableName());

        for(Class clazz : classes){
            String className = clazz.getCanonicalName();

            // See if this object is in the cache.  If so, it will help us know which related objects to update.
            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            MappedClass mappedClass = mcm.getMappedClassByClassName(className);
            IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
            ClassIDPair pair = new ClassIDPair(row.getRowId(), className);
            IPerceroObject cachedObject = dataProvider.findById(pair, null, false);    // We are hoping to find this object in the cache...

            handleDeletedObject(cachedObject, clazz, className, row.getRowId());

        	classNamesToUpdateReferences.add(className);
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
        List<Class> classes = getClassesForTableName(row.getTableName());

        for(Class clazz : classes){
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

        	classNamesToUpdateReferences.add(className);
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
        Set<ClassIDPair> results = new HashSet<>();
        List<Class> classes = getClassesForTableName(tableName);

        for(Class clazz : classes) {
            String className = clazz.getCanonicalName();
            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            MappedClass mappedClass = mcm.getMappedClassByClassName(className);
            IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
            results.addAll(dataProvider.getAllClassIdPairsByName(className));
        }

        return results;
    }


    /**
     * Finds all back references to this class and pushes updates to all of them.
     * @param className
     */
    // TODO: Need to batch this.
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
    public List<UpdateTableRow> getRows(int numRows){
        List<UpdateTableRow> rows = null;

        Random rand = new Random();

        int lockId = rand.nextInt();
        long now = System.currentTimeMillis();

        DateTime expireThreshold = new DateTime(now - EXPIRATION_TIME);

        if (StringUtils.hasText(connectionFactory.getStoredProcedureName())) {
            try {
                rows = getStoredProcRow(lockId, expireThreshold, numRows);
            } catch (Exception e) {
            	logger.error("Error running Update Table stored procedure for " + connectionFactory.getJdbcUrl() + "\n     Failing over to get UpdateTable with SELECT statement", e);
            		try {
						rows = getUpdateSelectRow(lockId, expireThreshold, numRows);
					} catch (SQLException e1) {
						logger.error("Error running Update Table SELECT statement as backup to Stored Procedure\n     UPDATE TABLE NON_FUNCTIONAL " + connectionFactory.getJdbcUrl(), e1);
					}
            }
        } else {
        	try {
        		rows = getUpdateSelectRow(lockId, expireThreshold, numRows);
			} catch (SQLException e1) {
				logger.error("Error running Update Table SELECT statement as backup to Stored Procedure\n     UPDATE TABLE NON_FUNCTIONAL " + connectionFactory.getJdbcUrl(), e1);
			}
        }


        return rows;
    }

    /**
     * @param lockId
     * @param expireThreshold
     * @return
     * @throws SQLException
     */
    private List<UpdateTableRow> getUpdateSelectRow(int lockId, DateTime expireThreshold, int numRows)throws SQLException {
        List<UpdateTableRow> list = new ArrayList<>();
        try(Statement statement = getConnection().createStatement()) {
            /**
             * First try to lock a row
             */
            String sql = connectionFactory.getUpdateStatementSql();
            sql = sql.replace(":tableName", tableName);
            sql = sql.replace(":lockId", lockId + "");
            sql = sql.replace(":expireThreshold", expireThreshold.toString("Y-MM-dd HH:mm:ss"));
            sql = sql.replace(":limit", numRows+"");

            int numUpdated = statement.executeUpdate(sql);

            // Found a row to process
            if (numUpdated > 0) {
                sql = "select * from :tableName where " + connectionFactory.getLockIdColumnName() + "=:lockId";
                sql = sql.replace(":tableName", tableName);
                sql = sql.replace(":lockId", lockId + "");

                try (ResultSet rs = statement.executeQuery(sql)) {
                    int count = 0;
                    while(rs.next()) {
                        UpdateTableRow row = fromResultSet(rs);
                        list.add(row);
                        count++;
                    }

                    if(count != numUpdated)
                        logger.warn("Locked a "+numUpdated+" rows but found "+count);
                }
            }
        }
        return list;
    }

    /**
     * @param lockId
     * @param expireThreshold
     * @return
     * @throws SQLException
     */
    private List<UpdateTableRow> getStoredProcRow(int lockId, DateTime expireThreshold, int numRows) throws SQLException {
        List<UpdateTableRow> list = new ArrayList<>();
        Integer updateNum = 0;
        String storedProcSql = connectionFactory.getStoredProcedureName();
        try(CallableStatement cstmt = getConnection().prepareCall("{call " + storedProcSql + "(?, ?, ?, ?)}")) {
            cstmt.setInt(1, lockId);
            cstmt.setString(2, expireThreshold.toString("Y-MM-dd HH:mm:ss"));
            cstmt.setInt(3, numRows);
            cstmt.registerOutParameter(4, Types.INTEGER);
            cstmt.executeUpdate();
            updateNum = cstmt.getInt(4);
        } catch(SQLException e){
//            	return null;
            logger.warn(e.getMessage(), e);

            // If the stored proc doesn't exist, let's try and create it.
            if (StringUtils.hasText(connectionFactory.getStoredProcedureDefinition()) &&
                    e.getMessage().toLowerCase().contains(connectionFactory.getStoredProcedureName().toLowerCase())
                    && e.getMessage().toLowerCase().contains("must be declared")) {
                try(Statement stmtCreateStoredProcedure = getConnection().createStatement()) {
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

        if (updateNum != null && updateNum > 0) {
            String sql = "select * from :tableName where " + connectionFactory.getLockIdColumnName() + "=:lock_id";
            sql = sql.replace(":tableName", tableName);
            sql = sql.replace(":lock_id", lockId+"");

            try(Statement statement = getConnection().createStatement(); ResultSet rs = statement.executeQuery(sql)){
                rs.setFetchSize(updateNum);
                int count = 0;
                while(rs.next()) {
                    UpdateTableRow row = fromResultSet(rs);
                    list.add(row);
                    count++;
                }

                if(count != updateNum)
                    logger.warn("Locked a "+updateNum+" rows but found "+count);
            }
        }

        return list;
    }

    /**
     * Deletes the row
     * @param rows
     */
    protected void deleteRows(List<UpdateTableRow> rows){
        try(Statement statement = getConnection().createStatement()){

            String sql = "delete from :tableName where ID in (:idList)";
            sql = sql.replace(":tableName", tableName);
            sql = sql.replace(":idList", listIdJoin(rows));

            int numUpdated = statement.executeUpdate(sql);
            if(numUpdated != rows.size()){
                logger.warn("Expected to delete "+rows.size()+", instead "+numUpdated);
            }
        }catch(SQLException e){
            logger.warn(e.getMessage(), e);
        }
    }

    /**
     * Utility function that takes a list of rows and returns a comma separated string
     * @param list
     */
    private String listIdJoin(List<UpdateTableRow> list){
        StringBuilder sb = new StringBuilder();
        String separator = ",";
        for (UpdateTableRow row : list) {
            if(sb.length() != 0)
                sb.append(separator);

            sb.append(row.getID());
        }
        return sb.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<Class> getClassesForTableName(String tableName){
        List<Class> result = new ArrayList<>();

        UpdateTableMapping mapping = UpdateTableRegistry.getInstance().getTableMapping(tableName);
        if(mapping != null){
            result.addAll(mapping.classes);
        }

        return result;
    }


    protected UpdateTableRow fromResultSet(ResultSet resultSet) throws SQLException{
        UpdateTableRow row = new UpdateTableRow();
        row.ID          = resultSet.getInt("ID");
        row.tableName   = resultSet.getString("tableName");
        row.rowId       = resultSet.getString("row_id");
        row.lockId      = resultSet.getInt(connectionFactory.getLockIdColumnName());
        row.lockDate    = resultSet.getDate(connectionFactory.getLockDateColumnName());
        try {
            row.type    = UpdateTableRowType.valueOf(resultSet.getString("type"));
        } catch(IllegalArgumentException iae) {
            logger.warn("Invalid UpdateTableRow TYPE, ignoring");
            row.type    = UpdateTableRowType.NONE;
        }
        row.timestamp   = resultSet.getDate(connectionFactory.getTimestampColumnName());

        return row;
    }

    private Connection getConnection() throws SQLException{
        if(this.connection == null || this.connection.isClosed()) // 10 second timeout
            this.connection = this.connectionFactory.getConnection();

        return this.connection;
    }
}

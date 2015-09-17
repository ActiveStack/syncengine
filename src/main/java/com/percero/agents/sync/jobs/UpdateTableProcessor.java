package com.percero.agents.sync.jobs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

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
public class UpdateTableProcessor {

    private static Logger logger = Logger.getLogger(UpdateTableProcessor.class);

    private static final int EXPIRATION_TIME = 1000*60*30; // 30 minutes
    private String tableName;
    private UpdateTableConnectionFactory connectionFactory;
    private PostDeleteHelper postDeleteHelper;
    private PostPutHelper postPutHelper;
    private IManifest manifest;
    private CacheManager cacheManager;
    private DataProviderManager dataProviderManager;
    private IAccessManager accessManager;

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
        
        insertTasks(1000, "9");
    }
    
    private static Boolean tasksInserted = false;
    
    
    private void insertTasks(int numTasks, String ownerId) {
    	if (tasksInserted) {
    		return;
    	}
        try(Connection conn = connectionFactory.getConnection();
                Statement statement = conn.createStatement())
            {

                /**
                 * First try to lock a row
                 */
                int counter = 0;
                for(counter = 0; counter<numTasks; counter++) {
                	String sql = "INSERT INTO `Task` (ID, name, owner_ID) VALUES (':id', ':name', ':ownerId')";
	                sql = sql.replace(":id", UUID.randomUUID().toString());
	                sql = sql.replace(":name", counter+"");
	                sql = sql.replace(":ownerId", ownerId);
	
	                int numUpdated = statement.executeUpdate(sql);
                }
                
                tasksInserted = true;

            } catch(SQLException e){
                logger.warn(e.getMessage(), e);
            }
    }

    /**
     * Update table schema looks like this
     *
     * `ID` int(11) NOT NULL AUTO_INCREMENT,
     * `tableName` varchar(255) DEFAULT NULL,
     * `rowID` varchar(255) DEFAULT NULL,
     * `type` enum('INSERT','UPDATE','DELETE') NOT NULL DEFAULT 'UPDATE',
     * `lockID` int(11) DEFAULT NULL,
     * `lockDate` datetime DEFAULT NULL,
     * `timestamp` timestamp DEFAULT CURRENT_TIMESTAMP
     *
     * @return
     */
    public ProcessorResult process(){
        ProcessorResult result = new ProcessorResult();

        while(true) {
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

        }

        return result;
    }

    private boolean processRow(UpdateTableRow row) throws Exception{
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
    private boolean processUpdateSingle(UpdateTableRow row) throws Exception{
        Class clazz = getClassForTableName(row.getTableName());
        String className = clazz.getCanonicalName();

        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mappedClass = mcm.getMappedClassByClassName(className);
        IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
        
        ClassIDPair pair = new ClassIDPair(row.getRowId(), className);
        handleUpdateClassIdPair(dataProvider, pair);
        
        updateReferences(className);
        return true;
    }

    /**
     * Process a whole table with updates
     * @param row
     * @return
     */
    @SuppressWarnings("rawtypes")
	private boolean processUpdateTable(UpdateTableRow row) throws Exception{
    	Class clazz = getClassForTableName(row.getTableName());

        // If there are any clients that have asked for all objects in a class then we have to push everything
        if(accessManager.getNumClientsInterestedInWholeClass(clazz.getName()) > 0) {
        	processUpdates(getAllClassIdPairsForTable(row.getTableName()));
        }
        else {
        	processUpdates(clazz.getName(), accessManager.getClassAccessJournalIDs(clazz.getName()));
        }

        return true;
    }

    /**
     * Takes a class and list of ids that need to be pushed out
     * @param className
     * @param Ids
     * @throws Exception
     */
    private void processUpdates(String className, Collection<String> Ids) throws Exception{
        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mappedClass = mcm.getMappedClassByClassName(className);
        IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);

        for(String ID : Ids) {
            ClassIDPair pair = new ClassIDPair(ID, className);
            handleUpdateClassIdPair(dataProvider, pair);
        }

        updateReferences(className);
    }
    
    private void processUpdates(Set<ClassIDPair> classIdPairs) throws Exception{
    	Set<String> classNamesToUpdateReferences = new HashSet<String>();
    	for(ClassIDPair classIdPair : classIdPairs) {
    		classNamesToUpdateReferences.add(classIdPair.getClassName());
    		
	    	IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
	    	MappedClass mappedClass = mcm.getMappedClassByClassName(classIdPair.getClassName());
	    	IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
    	
    		handleUpdateClassIdPair(dataProvider, classIdPair);
    	}
    	
    	for(String className : classNamesToUpdateReferences) {
    		updateReferences(className);
    	}
    }
    
    private void handleUpdateClassIdPair(IDataProvider dataProvider, ClassIDPair pair) throws Exception {
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
	private boolean processInsertSingle(UpdateTableRow row) throws Exception{
        Class clazz = getClassForTableName(row.getTableName());
        
		// We do not use PostCreateHelper here because we are going to do all
		// that extra work for the whole class in updateReferences.
		postPutHelper.postPutObject(new ClassIDPair(row.getRowId(), clazz.getCanonicalName()), null, null, true, null);
        updateReferences(clazz.getName());
        return true;
    }

    /**
     * Process a whole table with inserts
     * @param row
     * @return
     */
    private boolean processInsertTable(UpdateTableRow row) throws Exception {

        MappedClass mappedClass = getMappedClassForTableName(row.getTableName());

        // if any client needs all of this class then the only choice we have is to push everything
        if(accessManager.getNumClientsInterestedInWholeClass(mappedClass.className) > 0 /* || true */){
            Set<ClassIDPair> allClassIdPairs = getAllClassIdPairsForTable(row.getTableName());
            for(ClassIDPair classIdPair : allClassIdPairs) {
				// We do not use PostCreateHelper here because we are going to
				// do all that extra work for the whole class in
				// updateReferences.
                postPutHelper.postPutObject(classIdPair,null, null, true, null);
            }
        }

        updateReferences(mappedClass.className);

        return true;
    }

    /**
     * Process a single record delete
     * @param row
     * @return
     */
    @SuppressWarnings("rawtypes")
	private boolean processDeleteSingle(UpdateTableRow row) throws Exception{
        Class clazz = getClassForTableName(row.getTableName());
        String className = clazz.getCanonicalName();
        
        // See if this object is in the cache.  If so, it will help us know which related objects to update.
        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mappedClass = mcm.getMappedClassByClassName(className);
        IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
        ClassIDPair pair = new ClassIDPair(row.getRowId(), className);
        IPerceroObject cachedObject = dataProvider.findById(pair, null, false);	// We are hoping to find this object in the cache...

		handleDeletedObject(cachedObject, clazz, className, row.getRowId());
        
        updateReferences(className);
        return true;
    }

    /**
     * process a whole table with deletes
     * @param row
     * @return
     */
    @SuppressWarnings("rawtypes")
	private boolean processDeleteTable(UpdateTableRow row) throws Exception{
        Class clazz = getClassForTableName(row.getTableName());
        String className = clazz.getCanonicalName();
        
        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mappedClass = mcm.getMappedClassByClassName(clazz.getCanonicalName());
        IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
        
        // Get the list of ALL ID's of this class type that have been accessed.
        Set<String> accessedIds = accessManager.getClassAccessJournalIDs(clazz.getName());
        
        // Get a list of ALL ID's of this class type.
        Set<ClassIDPair> allClassIdPairs = getAllClassIdPairsForTable(row.getTableName());

        // Remove ALL existing/current ID's from our list of accessed ID's.
        for(ClassIDPair nextClassIdPair : allClassIdPairs) {
        	accessedIds.remove(nextClassIdPair.getID());
        }

        // Now we have the list of ID's that have actually been deleted.
        for(String id : accessedIds){
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

        updateReferences(clazz.getName());

        return true;
    }
    
    @SuppressWarnings("rawtypes")
	private void handleDeletedObject(IPerceroObject cachedObject, Class clazz, String className, String id) throws Exception {
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
	private Set<ClassIDPair> getAllClassIdPairsForTable(String tableName) throws Exception{
        Class clazz = getClassForTableName(tableName);
        String className = clazz.getCanonicalName();
    	
        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mappedClass = mcm.getMappedClassByClassName(className);
        IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
        Set<ClassIDPair> results = dataProvider.getAllClassIdPairsByName(className);
        
        return results;
    }

    private MappedClass getMappedClassForTableName(String tableName){
        Class clazz = getClassForTableName(tableName);
        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mappedClass = mcm.getMappedClassByClassName(clazz.getName());
        return mappedClass;
    }

    /**
     * Finds all back references to this class and pushes updates to all of them.
     * @param className
     */
    private void updateReferences(String className){
        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mappedClass = mcm.getMappedClassByClassName(className);
        // Go through each mapped field and push all objects of that associated type (just in case any has a reference to a new row
        // in the updated table)
        // --
        // TODO: is this right? Is it enough to only check the relationships on this class or do we need to look
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
                    
//                    processUpdates(mappedField.getMappedClass().className, ids);
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

            /**
             * First try to lock a row
             */
            String sql = "update `:tableName` set lockID=:lockId, lockDate=NOW() " +
                    "where lockID is null or " +
                    "lockDate < ':expireThreshold' " +
                    "order by timestamp limit 1";
            sql = sql.replace(":tableName", tableName);
            sql = sql.replace(":lockId", lockId+"");
            sql = sql.replace(":expireThreshold", expireThreshold.toString("Y-MM-dd HH:mm:ss"));

            int numUpdated = statement.executeUpdate(sql);

            // Found a row to process
            if(numUpdated > 0){
                sql = "select * from :tableName where lockId=:lockId limit 1";
                sql = sql.replace(":tableName", tableName);
                sql = sql.replace(":lockId", lockId+"");

                try(ResultSet rs = statement.executeQuery(sql)){
                    // If got a row back
                    if(rs.next())
                        row = UpdateTableRow.fromResultSet(rs);
                    else
                        logger.warn("Locked a row but couldn't retrieve");
                }
            }

        } catch(SQLException e){
            logger.warn(e.getMessage(), e);
        }

        return row;
    }

    /**
     * Deletes the row
     * @param row
     */
    private void deleteRow(UpdateTableRow row){
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
}

package com.percero.agents.sync.jobs;

import com.percero.agents.sync.cache.CacheManager;
import com.percero.agents.sync.helpers.PostDeleteHelper;
import com.percero.agents.sync.helpers.PostPutHelper;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.agents.sync.services.IDataProvider;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.bl.IManifest;
import com.percero.framework.vo.IPerceroObject;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.persistence.Table;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Random;

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

    public UpdateTableProcessor(String tableName,
                                UpdateTableConnectionFactory connectionFactory,
                                IManifest manifest,
                                PostDeleteHelper postDeleteHelper,
                                PostPutHelper postPutHelper,
                                CacheManager cacheManager,
                                DataProviderManager dataProviderManager)
    {
        this.tableName          = tableName;
        this.connectionFactory  = connectionFactory;
        this.postDeleteHelper   = postDeleteHelper;
        this.postPutHelper      = postPutHelper;
        this.manifest           = manifest;
        this.cacheManager       = cacheManager;
        this.dataProviderManager= dataProviderManager;
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
     * Process a single record delete
     * @param row
     * @return
     */
    private boolean processDeleteSingle(UpdateTableRow row) throws Exception{
        Class clazz = getClassForTableName(row.getTableName());
        postDeleteHelper.postDeleteObject(new ClassIDPair(row.getRowId(), clazz.getCanonicalName()), null, null, true);
        return true;
    }

    /**
     * Process a single record update
     * @param row
     * @return
     */
    private boolean processUpdateSingle(UpdateTableRow row) throws Exception{
        Class clazz = getClassForTableName(row.getTableName());
        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mappedClass = mcm.getMappedClassByClassName(clazz.getName());
        IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
        ClassIDPair pair = new ClassIDPair(row.getRowId(), clazz.getCanonicalName());
        IPerceroObject object = dataProvider.systemGetById(pair, true);

        if(object != null){
            cacheManager.updateCachedObject(object, null);
            postPutHelper.postPutObject(pair, null, null, true, null);
        }
        return true;
    }

    /**
     * process a single record insert
     * @param row
     * @return
     */
    private boolean processInsertSingle(UpdateTableRow row) throws Exception{
        Class clazz = getClassForTableName(row.getTableName());
        postPutHelper.postPutObject(new ClassIDPair(row.getRowId(), clazz.getCanonicalName()), null, null, true, null);
        return true;
    }

    /**
     * process a whole table with deletes
     * @param row
     * @return
     */
    private boolean processDeleteTable(UpdateTableRow row){
        return true;
    }

    /**
     * Process a whole table with updates
     * @param row
     * @return
     */
    private boolean processUpdateTable(UpdateTableRow row){
        return true;
    }

    /**
     * Process a whole table with inserts
     * @param row
     * @return
     */
    private boolean processInsertTable(UpdateTableRow row){
        return true;
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

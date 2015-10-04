package com.percero.agents.sync.jobs;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.cache.CacheManager;
import com.percero.agents.sync.helpers.PostDeleteHelper;
import com.percero.agents.sync.helpers.PostPutHelper;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.framework.bl.IManifest;

/**
 * Created by jonnysamps on 8/31/15.
 */
@Component
public class UpdateTablePoller {

    private static Logger logger = Logger.getLogger(UpdateTablePoller.class);

    @Autowired
    UpdateTableRegistry updateTableRegistry;

    @Autowired
    IManifest manifest;

    @Autowired
    PostDeleteHelper postDeleteHelper;

    @Autowired
    PostPutHelper postPutHelper;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    DataProviderManager dataProviderManager;

    @Autowired
    IAccessManager accessManager;

    public boolean enabled = true;

    /**
     * Run every minute
     */
    @Scheduled(fixedDelay=5000, initialDelay=10000)	// Every 5 seconds
    public void pollUpdateTables(){
        logger.debug("|> Starting poller");
        if(enabled) {
            // Loop until we didn't find any rows to work with
            while(true) {
                int count = 0;
                for (UpdateTableConnectionFactory updateTableConnectionFactory : updateTableRegistry.getConnectionFactories()) {
                    for (String tableName : updateTableConnectionFactory.getTableNames()) {
                        count += doProcessingForTable(updateTableConnectionFactory, tableName);
                    }
                }
                if(count <= 0) break;
            }
        }
        logger.debug("[] Stopping poller");
    }

    public int doProcessingForTable(UpdateTableConnectionFactory connectionFactory, String tableName){
        UpdateTableProcessor processor = getProcessor(connectionFactory, tableName);
        ProcessorResult result = processor.process();
        if(result.isSuccess()){
            logger.debug("Update table processor ("+tableName+") finished successfully. Total rows ("+result.getTotal()+")");
        }
        else{
            logger.warn("Update table processor ("+tableName+") failed. Details:");
            logger.warn(result);
        }

        return result.getTotal();
    }

    public UpdateTableProcessor getProcessor(UpdateTableConnectionFactory connectionFactory, String tableName){
        return new UpdateTableProcessor(tableName, connectionFactory, manifest,
        		postDeleteHelper, postPutHelper, cacheManager, dataProviderManager, accessManager);
    }


}

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
        if(enabled) {
        	for(UpdateTableConnectionFactory updateTableConnectionFactory : updateTableRegistry.getConnectionFactories()) {
	            for(String tableName : updateTableConnectionFactory.getTableNames()){
	                UpdateTableProcessor processor = getProcessor(updateTableConnectionFactory, tableName);
	                ProcessorResult result = processor.process();
	                if(result.isSuccess()){
	                    logger.debug("Update table processor ("+tableName+") finished successfully. Total rows ("+result.getTotal()+")");
	                }
	                else{
	                    logger.warn("Update table processor ("+tableName+") failed. Details:");
	                    logger.warn(result);
	                }
	            }
        	}
        }
    }

    public UpdateTableProcessor getProcessor(UpdateTableConnectionFactory connectionFactory, String tableName){
        return new UpdateTableProcessor(tableName, connectionFactory, manifest,
        		postDeleteHelper, postPutHelper, cacheManager, dataProviderManager, accessManager);
    }


}

package com.percero.agents.sync.jobs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.cache.CacheManager;
import com.percero.agents.sync.helpers.PostDeleteHelper;
import com.percero.agents.sync.helpers.PostPutHelper;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.framework.bl.IManifest;

import edu.emory.mathcs.backport.java.util.Collections;

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

	@Autowired @Qualifier("executorWithCallerRunsPolicy")
	TaskExecutor taskExecutor;
	
	UpdateTableProcessReporter reporter = null;

	Map<String, Set<UpdateTableProcessor>> runningProcessors = java.util.Collections.synchronizedMap(new HashMap<String, Set<UpdateTableProcessor>>());

    public boolean enabled = true;

    /**
     * Run every minute
     */
    @Scheduled(cron="0/5 * * * * *")	// Every 5 seconds
    public void pollUpdateTables() {
    	List<UpdateTableConnectionFactory> connectionFactories = updateTableRegistry.getConnectionFactories();
    	
    	// Only attempt to process Update Tables if they are configured.
    	if (connectionFactories != null && !connectionFactories.isEmpty()) {
    		if (reporter == null) {
    	        // Get the reporter going
    			reporter = UpdateTableProcessReporter.getInstance();
    		}
	        logger.debug("*** UpdateTablePoller running...");
	        for (UpdateTableConnectionFactory updateTableConnectionFactory : connectionFactories) {
	            for (String tableName : updateTableConnectionFactory.getTableNames()) {
	        		doProcessingForTable(updateTableConnectionFactory, tableName);
	            }
	        }
    	}
    }

    @SuppressWarnings("unchecked")
	public void doProcessingForTable(UpdateTableConnectionFactory connectionFactory, String tableName){
        // Spin `weight` new threads... weight is supposed to be a balancing scale.. but right now we
        // Use it to see how many threads to create.
    	String processName = connectionFactory.getJdbcUrl() + "::" + tableName;
    	Set<UpdateTableProcessor> processSet = runningProcessors.get(processName);
    	if (processSet == null) {
    		processSet = Collections.synchronizedSet(new HashSet<UpdateTableProcessor>());
    		runningProcessors.put(processName, processSet);
    	}
    	
    	if (processSet.size() < connectionFactory.getWeight()) {
	    	while (processSet.size() < connectionFactory.getWeight()) {
	            UpdateTableProcessor processor = getProcessor(connectionFactory, tableName);
	            taskExecutor.execute(processor);
	            processSet.add(processor);
	            logger.debug("Starting UpdateTable processor " + processor.getProcessorName() + " [" + processSet.size() + "/" + connectionFactory.getWeight() + "]");
	        }
    	}
    	else {
    		logger.debug("Processor " + processName + " already at capacity [" + processSet.size() + "/" + connectionFactory.getWeight() + "]");
    	}
    }
    
    public void processorCallback(UpdateTableProcessor processor) {
    	Set<UpdateTableProcessor> processSet = runningProcessors.get(processor.getProcessorName());
    	if (processSet != null) {
    		processSet.remove(processor);
    		logger.debug("Removing UpdateTable processor " + processor.getProcessorName() + " [" + processSet.size() + "/" + processor.connectionFactory.getWeight() + "]");
    	}
    }

    public UpdateTableProcessor getProcessor(UpdateTableConnectionFactory connectionFactory, String tableName){
        return new UpdateTableProcessor(tableName, connectionFactory, manifest,
                postDeleteHelper, postPutHelper, cacheManager, dataProviderManager, accessManager, this);
    }
}

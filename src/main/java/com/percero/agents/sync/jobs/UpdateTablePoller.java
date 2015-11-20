package com.percero.agents.sync.jobs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	TaskExecutor taskExecutor;

	Map<String, Set<UpdateTableProcessor>> runningProcessors = java.util.Collections.synchronizedMap(new HashMap<String, Set<UpdateTableProcessor>>());

    public boolean enabled = true;

    @PostConstruct
    public void init(){
        // Get the reporter going
        UpdateTableProcessReporter.getInstance();
    }

    /**
     * Run every minute
     */
//    @Scheduled(fixedRate=5000, initialDelay=10000)	// Every 5 seconds
    @Scheduled(fixedRate=5000)	// Every 5 seconds
    public void pollUpdateTables() {
        for (UpdateTableConnectionFactory updateTableConnectionFactory : updateTableRegistry.getConnectionFactories()) {
            for (String tableName : updateTableConnectionFactory.getTableNames()) {
//            	boolean processorRunning = false;
//            	for(UpdateTableProcessor runningProcessor : runningProcessors) {
//            		if (runningProcessor.connectionFactory == updateTableConnectionFactory) {
//            			if (runningProcessor.tableName.equals(tableName)) {
//            				processorRunning = true;
//            				break;
//            			}
//            		}
//            	}
//            	if (!processorRunning) {
            		doProcessingForTable(updateTableConnectionFactory, tableName);
//            	}
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
    	while (processSet.size() < connectionFactory.getWeight()) {
            UpdateTableProcessor processor = getProcessor(connectionFactory, tableName);
            taskExecutor.execute(processor);
            processSet.add(processor);
        }
    }
    
    public void processorCallback(UpdateTableProcessor processor) {
    	Set<UpdateTableProcessor> processSet = runningProcessors.get(processor.getProcessorName());
    	if (processSet != null) {
    		processSet.remove(processor);
    	}
    }

    public UpdateTableProcessor getProcessor(UpdateTableConnectionFactory connectionFactory, String tableName){
        return new UpdateTableProcessor(tableName, connectionFactory, manifest,
                postDeleteHelper, postPutHelper, cacheManager, dataProviderManager, accessManager, this);
    }
}

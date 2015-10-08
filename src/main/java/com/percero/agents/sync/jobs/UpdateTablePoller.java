package com.percero.agents.sync.jobs;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.cache.CacheManager;
import com.percero.agents.sync.helpers.PostDeleteHelper;
import com.percero.agents.sync.helpers.PostPutHelper;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.framework.bl.IManifest;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

    List<Thread> threads = new ArrayList<Thread>();

    public boolean enabled = true;

    /**
     * Run every minute
     */
    @Scheduled(fixedDelay=5000, initialDelay=10000)	// Every 5 seconds
    public void pollUpdateTables() {
        boolean lastDone = true;
        for (Thread thread : threads){
            if (thread.isAlive()) {
                lastDone = false;
                break;
            }
        }

        if(enabled && lastDone) {
            threads.clear();
            for (UpdateTableConnectionFactory updateTableConnectionFactory : updateTableRegistry.getConnectionFactories()) {
                for (String tableName : updateTableConnectionFactory.getTableNames()) {
                    doProcessingForTable(updateTableConnectionFactory, tableName);
                }
            }
        }
    }

    public void doProcessingForTable(UpdateTableConnectionFactory connectionFactory, String tableName){
        // Spin `weight` new threads... weight is supposed to be a balancing scale.. but right now we
        // Use it to see how many threads to create.
        for(int i = 0; i < connectionFactory.getWeight(); i++) {
            logger.info("Creating new processor thread");
            UpdateTableProcessor processor = getProcessor(connectionFactory, tableName);
            Thread thread = new Thread(processor);
            threads.add(thread);
            thread.start();
        }
    }

    public UpdateTableProcessor getProcessor(UpdateTableConnectionFactory connectionFactory, String tableName){
        return new UpdateTableProcessor(tableName, connectionFactory, manifest,
                postDeleteHelper, postPutHelper, cacheManager, dataProviderManager, accessManager);
    }
}

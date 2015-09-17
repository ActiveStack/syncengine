package com.percero.agents.sync.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.cache.CacheManager;
import com.percero.agents.sync.helpers.PostCreateHelper;
import com.percero.agents.sync.helpers.PostDeleteHelper;
import com.percero.agents.sync.helpers.PostPutHelper;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.framework.bl.IManifest;

/**
 * Creates UpdateTableProcessor for the poller
 * Created by jonnysamps on 9/4/15.
 */
@Component
public class UpdateTableProcessorFactory {

    @Autowired
    UpdateTableConnectionFactory connectionFactory;

    @Autowired
    IManifest manifest;

    @Autowired
    PostDeleteHelper postDeleteHelper;

    @Autowired
    PostPutHelper postPutHelper;

    @Autowired
    PostCreateHelper postCreateHelper;
    
    @Autowired
    CacheManager cacheManager;

    @Autowired
    DataProviderManager dataProviderManager;

    @Autowired
    IAccessManager accessManager;

    public UpdateTableProcessor getProcessor(String tableName){
        return new UpdateTableProcessor(tableName, connectionFactory, manifest,
                postDeleteHelper, postPutHelper, postCreateHelper, cacheManager, dataProviderManager, accessManager);
    }
}

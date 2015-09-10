package com.percero.agents.sync.jobs;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.cache.CacheManager;
import com.percero.agents.sync.helpers.PostDeleteHelper;
import com.percero.agents.sync.helpers.PostPutHelper;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.framework.bl.IManifest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    CacheManager cacheManager;

    @Autowired
    DataProviderManager dataProviderManager;

    @Autowired
    IAccessManager accessManager;

    public UpdateTableProcessor getProcessor(String tableName){
        return new UpdateTableProcessor(tableName, connectionFactory, manifest,
                postDeleteHelper, postPutHelper, cacheManager, dataProviderManager, accessManager);
    }
}

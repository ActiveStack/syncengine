package com.percero.client;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.percero.agents.sync.jobs.UpdateTablePoller;
import com.percero.test.utils.CleanerUtil;

/**
 * Created by jonnysamps on 9/14/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/update_table_processor.xml" })
public class AStackClientTest {

    @Autowired
    AStackClientFactory clientFactory;

    @Autowired
    CleanerUtil cleanerUtil;

    @Autowired
    UpdateTablePoller poller;

    @Before
    public void before(){
        poller.enabled = false;
        cleanerUtil.cleanAll();
    }
    
    @Test
    public void authenticateAnonymously(){
        AStackClient client = clientFactory.getClient();
        boolean result = client.authenticateAnonymously();
        assertTrue(result);
    }
}

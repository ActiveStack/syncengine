package com.percero.agents.auth.services;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.percero.agents.auth.services.AuthProviderRegistry;
import com.percero.agents.auth.services.AuthService2;
import com.percero.agents.auth.services.DatabaseHelper;
import com.percero.agents.sync.services.ISyncAgentService;

/**
 * Props up a BasicAuthProvider, which provides basic username/password authentication.
 * @author Collin Brown
 *
 */
@Component
public class BasicAuthProviderFactory {

    private static Logger logger = Logger.getLogger(BasicAuthProviderFactory.class);

    @Autowired
    AuthProviderRegistry authProviderRegistry;

    @Autowired
    ISyncAgentService syncAgentService;
    
    @Autowired
    AuthService2 authService2;
    
    @Autowired
    DatabaseHelper authDatabaseHelper;
    
    @Autowired @Value("$pf{auth.basic.enabled:false}")
    Boolean basicAuthEnabled = false;

    @PostConstruct
    public void init(){
    	if (basicAuthEnabled != null && basicAuthEnabled.booleanValue()) {
	        BasicAuthProvider provider = new BasicAuthProvider(syncAgentService, authService2, authDatabaseHelper);
	        logger.info("BasicAuth:.............ENABLED");
	        authProviderRegistry.addProvider(provider);
    	}
    	else {
    		logger.info("BasicAuth:............DISABLED");
    	}
    }
}

package com.percero.agents.auth.services;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Props up a BasicAuthProvider, which provides basic username/password
 * authentication against the auth database.
 * 
 * @author Collin Brown
 *
 */
@Component
public class BasicAuthProviderFactory {

    private static Logger logger = Logger.getLogger(BasicAuthProviderFactory.class);

    @Autowired
    AuthProviderRegistry authProviderRegistry;

    @Autowired
    DatabaseHelper authDatabaseHelper;
    
    @Autowired @Value("$pf{auth.basic.enabled:false}")
    Boolean basicAuthEnabled = false;

    @PostConstruct
    public void init(){
    	if (basicAuthEnabled != null && basicAuthEnabled.booleanValue()) {
	        BasicAuthProvider provider = new BasicAuthProvider(authDatabaseHelper);
	        logger.info("BasicAuth:.............ENABLED");
	        authProviderRegistry.addProvider(provider);
    	}
    	else {
    		logger.info("BasicAuth:............DISABLED");
    	}
    }
}

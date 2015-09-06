package com.convergys.pulse.service;

import com.percero.agents.auth.services.AuthProviderRegistry;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by jonnysamps on 8/27/15.
 */
@Component
public class PulseHttpAuthProviderFactory {

    private static Logger logger = Logger.getLogger(PulseHttpAuthProviderFactory.class);

    @Autowired @Value("$pf{pulseHttpAuth.hostPortAndContext}")
    String hostPortAndContext = null;

    @Autowired @Value("$pf{pulseHttpAuth.trustAllCerts:false}")
    Boolean trustAllCerts = false;

    @Autowired
    AuthProviderRegistry authProviderRegistry;

    @Autowired
    ObjectMapper objectMapper;

    @PostConstruct
    public void init(){
        if(hostPortAndContext != null){
            logger.info("Using PulseHttpAuthProvider with endpoint: "+hostPortAndContext);
            PulseHttpAuthProvider provider = new PulseHttpAuthProvider(hostPortAndContext, objectMapper, trustAllCerts);
            authProviderRegistry.addProvider(provider);
        }
    }
}

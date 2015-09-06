package com.percero.agents.auth.services;

import com.percero.agents.auth.vo.InMemoryAuthProviderUser;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The purpose of this class is to be the initializer for the InMemoryAuthProvider
 * in the case that the auth provider does not have what it needs to initialize
 * we do not register it with the AuthRegistry.
 *
 * Created by jonnysamps on 8/25/15.
 */
@Component
public class FileAuthProviderFactory {

    private static Logger logger = Logger.getLogger(FileAuthProviderFactory.class);

    @Autowired
    AuthProviderRegistry authProviderRegistry;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired @Value("$pf{fileAuth.fileLocation:users.json}")
    String fileLocation = "";

    @Autowired @Value("$pf{fileAuth.providerID:file}")
    String providerID = "";

    @PostConstruct
    public void init(){
        File file = new File(fileLocation);
        try {
            List<InMemoryAuthProviderUser> list =
                    objectMapper.readValue(file, new TypeReference<List<InMemoryAuthProviderUser>>() {});
            InMemoryAuthProvider provider = new InMemoryAuthProvider(providerID, list);
            authProviderRegistry.addProvider(provider);
            logger.info("Using FileAuthProvider ("+providerID+"). Found "+list.size()+" users.");
        }catch(IOException e){
            logger.info("Not using FileAuthProvider");
            logger.debug(e.getMessage(),e);
        }
    }
}

package com.percero.agents.auth.services;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class is the registration point for IAuthProviders.
 */
@Component
public class AuthProviderRegistry {
    private static Logger logger = Logger.getLogger(AuthProviderRegistry.class);
    private Map<String, IAuthProvider> providerMap = new HashMap<String, IAuthProvider>();

    /**
     * Intended to be a default registration point for all IAuthProviders in the application context
     * @param providers
     */
    @Autowired(required = false)
    public void setProviders(Collection<IAuthProvider> providers){
        for(IAuthProvider provider : providers){
            this.addProvider(provider);
        }
    }

    /**
     * Add a provider to the registry
     * @param provider
     */
    public void addProvider(IAuthProvider provider){
        if(providerMap.containsKey(provider.getID()))
            logger.warn("Non-unique auth provider ID: "+provider.getID());

        providerMap.put(provider.getID().toLowerCase(), provider);
    }

    /**
     * Find a provider by ID
     * @param ID
     * @return
     */
    public IAuthProvider getProvider(String ID){
        return providerMap.get(ID.toLowerCase());
    }

    /**
     * Returns true if registry has a provider registered for this key
     * @param ID
     * @return
     */
    public boolean hasProvider(String ID){
        return providerMap.containsKey(ID.toLowerCase());
    }
}

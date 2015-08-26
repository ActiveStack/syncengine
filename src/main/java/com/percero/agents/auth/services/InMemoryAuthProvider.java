package com.percero.agents.auth.services;

import com.percero.agents.auth.vo.InMemoryAuthProviderUser;
import com.percero.agents.auth.vo.ServiceIdentifier;
import com.percero.agents.auth.vo.ServiceUser;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Will lookup user information from a JSON file
 * Created by jonnysamps on 8/25/15.
 */
public class InMemoryAuthProvider implements IAuthProvider {

    private String ID;
    public String getID() {
        return ID;
    }

    public ServiceUser authenticate(String credential) {
        ServiceUser result = null;
        String[] parts = credential.split(":");
        if(parts.length == 2){
            String username = parts[0];
            String password = parts[1];
            String hashPass = DigestUtils.sha1Hex(password);

            InMemoryAuthProviderUser user = users.get(username);
            if(user != null && user.getPassHash().equals(hashPass)) {
                result = new ServiceUser();
                result.setAuthProviderID(getID());
                result.setId(username);
                result.setFirstName(user.getFirstName());
                result.setLastName(user.getLastName());
                result.getEmails().add(user.getEmail());
                result.setAreRoleNamesAccurate(true);
                result.getIdentifiers().add(new ServiceIdentifier("email", user.getEmail()));
            }
        }

        return result;
    }

    private Map<String, InMemoryAuthProviderUser> users = new HashMap<String, InMemoryAuthProviderUser>();

    public InMemoryAuthProvider(String id, List<InMemoryAuthProviderUser> users) {
        this.ID = id;
        for(InMemoryAuthProviderUser user : users){
            this.users.put(user.getEmail(), user);
        }
    }

    public static void main(String[] args){
        System.out.println(DigestUtils.sha1Hex("qwer"));
    }
}

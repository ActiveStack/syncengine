package com.percero.agents.auth.services;

import com.percero.agents.auth.vo.*;
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

    public AuthProviderResponse authenticate(String credential) {
        AuthProviderResponse result = new AuthProviderResponse();
        BasicAuthCredential cred = BasicAuthCredential.fromString(credential);

        String hashPass = DigestUtils.sha1Hex(cred.getPassword());

        InMemoryAuthProviderUser user = users.get(cred.getUsername());
        if(user != null && user.getPassHash().equals(hashPass)) {
            ServiceUser serviceUser = new ServiceUser();
            serviceUser.setAuthProviderID(getID());
            serviceUser.setId(cred.getUsername());
            serviceUser.setFirstName(user.getFirstName());
            serviceUser.setLastName(user.getLastName());
            serviceUser.getEmails().add(user.getEmail());
            serviceUser.setAreRoleNamesAccurate(true);
            serviceUser.getIdentifiers().add(new ServiceIdentifier("email", user.getEmail()));
            result.serviceUser = serviceUser;
            result.authCode = AuthCode.SUCCESS;
        }
        else
            result.authCode = AuthCode.FORBIDDEN;

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

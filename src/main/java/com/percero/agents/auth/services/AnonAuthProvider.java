package com.percero.agents.auth.services;

import com.percero.agents.auth.vo.AuthProvider;
import com.percero.agents.auth.vo.ServiceIdentifier;
import com.percero.agents.auth.vo.ServiceOrganization;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.util.RandomStringGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonnysamps on 8/14/15.
 */
@Component
public class AnonAuthProvider implements IAuthProvider {
    private static String ID = "anonymous";

    @Autowired
    @Value("$pf{anonAuth.enabled:false}")
    Boolean anonAuthEnabled = false;
    @Autowired @Value("$pf{anonAuth.code:ANON}")
    String anonAuthCode = "ANON";
    @Autowired @Value("$pf{anonAuth.roleNames:}")
    String anonAuthRoleNames = "";

    public String getID() {
        return ID;
    }

    public ServiceUser authenticate(String credential) {
        ServiceUser serviceUser = new ServiceUser();
        serviceUser.setFirstName("ANON");
        serviceUser.setLastName("ANON");
        serviceUser.setId("ANON");
        serviceUser.setAuthProviderID(AuthProvider.ANON.toString());
        serviceUser.setRefreshToken(anonAuthCode);

        List<String> roles = new ArrayList<String>();
        String[] roleNames = anonAuthRoleNames.split(",");
        for(int i = 0; i < roleNames.length; i++) {
            if (roleNames[i] != null && !roleNames[i].isEmpty())
                roles.add(roleNames[i]);
        }
        serviceUser.setRoleNames(roles);
        serviceUser.setAreRoleNamesAccurate(true);
        return serviceUser;
    }
}

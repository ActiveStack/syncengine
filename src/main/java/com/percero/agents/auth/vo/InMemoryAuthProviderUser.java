package com.percero.agents.auth.vo;

/**
 * User object structure to be used in json file that is fed to InMemoryAuthProvider
 * Created by jonnysamps on 8/25/15.
 */
public class InMemoryAuthProviderUser {

    private String firstName;
    private String lastName;
    private String email;
    private String passHash;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassHash() {
        return passHash;
    }

    public void setPassHash(String passHash) {
        this.passHash = passHash;
    }


}

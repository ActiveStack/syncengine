package com.percero.agents.auth.vo;

/**
 * Created by jonnysamps on 8/27/15.
 */
public class BasicAuthCredential {
    private String username;
    public String getUsername(){
        return username;
    }
    public void setUsername(String val){
        this.username = val;
    }

    private String password;
    public String getPassword(){
        return password;
    }
    public void setPassword(String val){
        this.password = val;
    }

    public BasicAuthCredential(String username, String password){
        this.username = username;
        this.password = password;
    }

    /**
     * Used to deserialize from "<USERNAME>:<PASSWORD>" format to an object
     * @param credential
     * @return
     */
    public static BasicAuthCredential fromString(String credential){
        BasicAuthCredential result = new BasicAuthCredential("","");
        String[] parts = credential.split(":");
        if(parts.length == 2) {
            result = new BasicAuthCredential(parts[0],parts[1]);
        }
        return result;
    }
}

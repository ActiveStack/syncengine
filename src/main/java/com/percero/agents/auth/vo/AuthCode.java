package com.percero.agents.auth.vo;

/**
 * Created by jonnysamps on 10/19/15.
 */
public class AuthCode {
    public static final AuthCode SUCCESS        = new AuthCode(200, "Success");
    public static final AuthCode UNAUTHORIZED   = new AuthCode(401, "Unauthorized");
    public static final AuthCode FORBIDDEN      = new AuthCode(401, "Forbidden");

    private int code;
    private String message;
    public AuthCode(int code, String message){
        this.code = code;
        this.message = message;
    }

    public int getCode(){ return this.code; }
    public String getMessage(){ return this.message; }
}

package com.percero.agents.auth.services;

import com.percero.agents.auth.vo.AuthCode;

public class BasicAuthCode extends AuthCode {

    public static final BasicAuthCode BAD_USER_PASS = new BasicAuthCode(433, "Bad username or password");

    private BasicAuthCode(int code, String message){
        super(code, message);
    }

}

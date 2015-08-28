package com.convergys.pulse.vo;

/**
 * Class to represent the response from the Pulse /retrieve_user endpoint
 * Created by Jonathan Samples on 8/27/15.
 */
public class PulseUserInfo {
    private String userLogin;
    private String employeeId;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }
}

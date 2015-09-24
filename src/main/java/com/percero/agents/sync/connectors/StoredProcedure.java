package com.percero.agents.sync.connectors;

import java.sql.Types;
import java.util.List;

import org.springframework.util.StringUtils;


public class StoredProcedure {

	public StoredProcedure() {
		
	}
	
    private String driverClassName;
    public void setDriverClassName(String val){
        this.driverClassName = val;
    }
    public String getDriverClassName(){
    	return driverClassName;
    }

    private String username;
    public void setUsername(String val){
        this.username = val;
    }
    public String getUsername(){
    	return username;
    }

    private String password;
    public void setPassword(String val){
        this.password = val;
    }
    public String getPassword(){
    	return password;
    }

    private String jdbcUrl;
    public void setJdbcUrl(String val){
        this.jdbcUrl = val;
    }
    public String getJdbcUrl(){
    	return jdbcUrl;
    }
    
    private String name;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	private String storedProcedureDefinition;
	public String getStoredProcedureDefinition() {
		return storedProcedureDefinition;
	}
	public void setStoredProcedureDefinition(String storedProcedureDefinition) {
		this.storedProcedureDefinition = storedProcedureDefinition;
	}
	
	private List<StoredProcedureParameter> parameters;
	public List<StoredProcedureParameter> getParameters() {
		return parameters;
	}
	public void setParameters(List<StoredProcedureParameter> parameters) {
		this.parameters = parameters;
	}

	
}

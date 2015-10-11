package com.percero.agents.sync.jobs;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.log4j.Logger;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by jonnysamps on 9/2/15.
 */
//@Component
public class UpdateTableConnectionFactory {

    private static Logger logger = Logger.getLogger(UpdateTableConnectionFactory.class);

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
    
    private String[] tableNames;
    public void setTableNames(String[] val) {
    	tableNames = val;
    }
    public String[] getTableNames() {
    	return tableNames;
    }
    
    private String storedProcedureName;
	private String storedProcedureDefinition;

	public String getStoredProcedureName() {
		return storedProcedureName;
	}
	public void setStoredProcedureName(String storedProcedureName) {
		this.storedProcedureName = storedProcedureName;
	}
	public String getStoredProcedureDefinition() {
		return storedProcedureDefinition;
	}
	public void setStoredProcedureDefinition(String storedProcedureDefinition) {
		this.storedProcedureDefinition = storedProcedureDefinition;
	}

    private String updateStatementSql = "update :tableName set lock_id=:lockId, lock_date=NOW() " +
            "where lock_id is null or " +
            "lock_date < ':expireThreshold' " +
            "order by time_stamp limit :limit";
	public String getUpdateStatementSql() {
		return updateStatementSql;
	}
	public void setUpdateStatementSql(String updateStatementSql) {
		this.updateStatementSql = updateStatementSql;
	}

    /**
     * How much should we favor this connection compared to another
     */
    private int weight = UpdateTableProcessor.INFINITE_ROWS;
    public void setWeight(int weight){
        this.weight = weight;
    }
    public int getWeight(){
        return this.weight;
    }

    private ComboPooledDataSource cpds;

    public void init() throws PropertyVetoException{
        try {
            cpds = new ComboPooledDataSource();
            cpds.setDriverClass(driverClassName); //loads the jdbc driver
            cpds.setJdbcUrl(jdbcUrl);
            cpds.setUser(username);
            cpds.setPassword(password);

            // the settings below are optional -- c3p0 can work with defaults
            cpds.setMinPoolSize(10);
            cpds.setAcquireIncrement(5);
            cpds.setMaxPoolSize(this.weight);
            cpds.setTestConnectionOnCheckout(true);

        }catch(PropertyVetoException pve){
            logger.error(pve.getMessage(), pve);
            throw pve;
        }
    }

    public Connection getConnection() throws SQLException{
        try{
        	if (cpds == null) {
                init();
            }
            return cpds.getConnection();
        }
        catch(PropertyVetoException e){
            logger.error(e.getMessage(), e);
            throw new SQLException(e);
        }
        catch(SQLException e){
        	logger.error(e.getMessage(), e);
        	throw e;
        }
    }
}

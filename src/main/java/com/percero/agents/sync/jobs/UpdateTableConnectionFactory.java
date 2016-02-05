package com.percero.agents.sync.jobs;

import com.percero.datasource.BaseConnectionFactory;

/**
 * Created by jonnysamps on 9/2/15.
 */
public class UpdateTableConnectionFactory extends BaseConnectionFactory {

    
    private String[] tableNames;
    public void setTableNames(String[] val) {
    	tableNames = val;
    }
    public String[] getTableNames() {
    	return tableNames;
    }
    
    private String storedProcedureName;
	public String getStoredProcedureName() {
		return storedProcedureName;
	}
	public void setStoredProcedureName(String storedProcedureName) {
		this.storedProcedureName = storedProcedureName;
	}
	
	private String storedProcedureDefinition;
	public String getStoredProcedureDefinition() {
		return storedProcedureDefinition;
	}
	public void setStoredProcedureDefinition(String storedProcedureDefinition) {
		this.storedProcedureDefinition = storedProcedureDefinition;
	}

	private String lockIdColumnName = "lock_id";
	public String getLockIdColumnName() {
		return lockIdColumnName;
	}
	public void setLockIdColumnName(String lockIdColumnName) {
		this.lockIdColumnName = lockIdColumnName;
	}
	
	private String lockDateColumnName = "lock_date";
	public String getLockDateColumnName() {
		return lockDateColumnName;
	}
	public void setLockDateColumnName(String lockDateColumnName) {
		this.lockDateColumnName = lockDateColumnName;
	}
	
	private String timestampColumnName = "time_stamp";
	public String getTimestampColumnName() {
		return timestampColumnName;
	}
	public void setTimestampColumnName(String timestampColumnName) {
		this.timestampColumnName = timestampColumnName;
	}
	
    private String updateStatementSql = "update :tableName set " + getLockIdColumnName() + "=:lockId, " + getLockDateColumnName() + "=NOW() " +
            "where " + getLockIdColumnName() + " is null or " +
            getLockDateColumnName() + " < ':expireThreshold' " +
            "order by " + getTimestampColumnName() + " limit :limit";
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

}

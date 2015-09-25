package com.percero.agents.sync.connectors;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Component;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.percero.agents.sync.jobs.UpdateTableRegistry;

@Component
public class StoredProcedureConnector implements ILogicConnector {

    private static Logger logger = Logger.getLogger(StoredProcedureConnector.class);

	public StoredProcedureConnector() {
		// TODO Auto-generated constructor stub
	}
	
    private Map<String, ComboPooledDataSource> dataSources = Collections.synchronizedMap(new HashMap<String, ComboPooledDataSource>());
    private Map<String, StoredProcedure> storedProcedures = Collections.synchronizedMap(new HashMap<String, StoredProcedure>());


	@PostConstruct
	public void init() throws YamlException {
        try {
        	URL ymlUrl = UpdateTableRegistry.class.getClassLoader().getResource("storedProcedures.yml");
        	if (ymlUrl == null) {
            	logger.warn("No configuration found for StoredProcedures (storedProcedures.yml), skipping StoredProcedures");
            	return;
        	}
        	File configFile = new File(ymlUrl.getFile());
        	YamlReader reader = new YamlReader(new FileReader(configFile));
        	reader.getConfig().setPropertyElementType(StoredProcedure.class, "parameters", StoredProcedureParameter.class);

        	while (true) {
        		StoredProcedure storedProc = reader.read(StoredProcedure.class);
        		if (storedProc == null) {
        			break;
        		}
        		
				storedProcedures.put(storedProc.getName(), storedProc);
        	}
        }
        catch (FileNotFoundException e) {
        	logger.warn("No configuration found for StoredProcedures (storedProcedures.yml), skipping StoredProcedures");
		}
	}

	public static final String CONNECTOR_PREFIX = "SQL_PROC";
	
	@Override
	public String getConnectorPrefix() {
		return CONNECTOR_PREFIX;
	}

	@Override
	public String runOperation(String operationName, String clientId,
			Object parameters) throws ConnectorException {
		String result = "";
		
		StoredProcedure storedProc = storedProcedures.get(operationName);
		result = runStoredProcedure(storedProc, parameters);
		
		return result;
	}

    @SuppressWarnings("unchecked")
	private String runStoredProcedure(StoredProcedure storedProc,
			Object parameters) throws ConnectorException {
    	String result = "";
        try(Connection conn = getConnection(storedProc.getDriverClassName(), storedProc.getJdbcUrl(), storedProc.getUsername(), storedProc.getPassword());
                Statement statement = conn.createStatement())
            {

			String storedProcedureCallSql = "{call " + storedProc.getName() + "(";
			if (storedProc.getParameters() != null && !storedProc.getParameters().isEmpty()) {
				for(int i=0; i<storedProc.getParameters().size(); i++) {
					if (i > 0) {
						storedProcedureCallSql += ",";
					}
					storedProcedureCallSql += "?";
				}
			}
			storedProcedureCallSql += ")}";

			Map<String, Object> arrayParams = null;
			if (parameters != null) {
				if (parameters instanceof Map) {
					arrayParams = (Map<String, Object>) parameters;
				}
			}
			
		    CallableStatement cstmt = conn.prepareCall(storedProcedureCallSql);

		    // Set input parameter values and output parameters types.
			if (storedProc.getParameters() != null && !storedProc.getParameters().isEmpty()) {
				Iterator<StoredProcedureParameter> itrStoredProcParameters = storedProc.getParameters().iterator();
				while (itrStoredProcParameters.hasNext()) {
					StoredProcedureParameter nextParameter = itrStoredProcParameters.next();
					if ( "IN".equalsIgnoreCase(nextParameter.getDirection()) || "INOUT".equalsIgnoreCase(nextParameter.getDirection()) ) {
						if (arrayParams != null) {
							Object val = arrayParams.get(nextParameter.getName());
							cstmt.setObject(nextParameter.getPosition(), val);
						}
					}
					else if ( "INOUT".equalsIgnoreCase(nextParameter.getDirection()) ) {
						if (arrayParams != null) {
							Object val = arrayParams.get(nextParameter.getName());
							cstmt.setObject(nextParameter.getPosition(), val);
						}
						cstmt.registerOutParameter(nextParameter.getPosition(), nextParameter.retrievePreparedStatementType());
					}
					else if ( "OUT".equalsIgnoreCase(nextParameter.getDirection()) ) {
						cstmt.registerOutParameter(nextParameter.getPosition(), nextParameter.retrievePreparedStatementType());
					}
				}
			}
		    
			cstmt.executeUpdate();
		    
		    // Retrieve output parameters
		    result = "{";
		    if (storedProc.getParameters() != null && !storedProc.getParameters().isEmpty()) {
		    	ObjectMapper om = new ObjectMapper();
		    	int resultCounter = 0;
		    	Iterator<StoredProcedureParameter> itrStoredProcParameters = storedProc.getParameters().iterator();
		    	while (itrStoredProcParameters.hasNext()) {
		    		StoredProcedureParameter nextParameter = itrStoredProcParameters.next();
		    		if ( "OUT".equalsIgnoreCase(nextParameter.getDirection()) || "INOUT".equalsIgnoreCase(nextParameter.getDirection()) ) {
					    Object nextParamResult = cstmt.getObject(nextParameter.getPosition());
					    if (resultCounter > 0) {
					    	result += ",";
					    }
					    result += "\"" + nextParameter.getName() + "\":";
					    try {
							result += om.writeValueAsString(nextParamResult);
						} catch (IOException e) {
							logger.error("Unable to process returned result", e);
							result += "null";
						}
					    
					    resultCounter++;
		    		}
		    	}
		    }
		    result += "}";
        } catch(SQLException e) {
            logger.error(e.getMessage(), e);
            throw new ConnectorException(e);
        }
        
        return result;
	}

	private ComboPooledDataSource initConnection(String driverClassName, String jdbcUrl, String username, String password) throws PropertyVetoException{
        try {
        	ComboPooledDataSource cpds = new ComboPooledDataSource();
            cpds.setDriverClass(driverClassName); //loads the jdbc driver
            cpds.setJdbcUrl(jdbcUrl);
            cpds.setUser(username);
            cpds.setPassword(password);

            // the settings below are optional -- c3p0 can work with defaults
            cpds.setMinPoolSize(5);
            cpds.setAcquireIncrement(5);
            
            dataSources.put(driverClassName + jdbcUrl + username + password, cpds);
            
            return cpds;
        }catch(PropertyVetoException pve){
            logger.error(pve.getMessage(), pve);
            throw pve;
        }
    }

    public Connection getConnection(String driverClassName, String jdbcUrl, String username, String password) throws SQLException{
        try{
        	ComboPooledDataSource cpds = dataSources.get(driverClassName + jdbcUrl + username + password);
        	if (cpds == null) {
        		cpds = initConnection(driverClassName, jdbcUrl, username, password);
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


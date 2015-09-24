package com.percero.agents.sync.connectors;


public interface ILogicConnector {

	public abstract String getConnectorPrefix();

	public abstract String runOperation(String operationName, String clientId,
			Object parameters) throws ConnectorException;

}
package com.percero.agents.sync.exceptions;

public class ClientException extends SyncException{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4029076748936883021L;

	public static final String INVALID_CLIENT = "invalidClient";
	public static final Integer INVALID_CLIENT_CODE = -100;
	
	public ClientException(String name, Integer code, String desc, Throwable t) {
		super(name, code, desc, t);
	}
	
	public ClientException(String name, Integer code, String desc, String clientId) {
		super(name, code, desc);
		this.clientId = clientId;
	}
	
	public ClientException() {
		super();
	}

	private String clientId;
	
	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
}

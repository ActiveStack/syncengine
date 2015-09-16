package com.percero.agents.sync.exceptions;

public class SyncException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3107854318936883436L;

	public static final String INVALID_TRANS_ID = "invalidTransactionId";
	public static final Integer INVALID_TRANS_ID_CODE = -101;
	public static final String INVALID_CLIENT_USER = "invalidClientUser";
	public static final Integer INVALID_CLIENT_USER_CODE = -102;
	public static final String RUN_PROCESS_ERROR = "runProcessError";
	public static final Integer RUN_PROCESS_ERROR_CODE = -103;
	public static final String MISSING_MAPPED_CLASS_ERROR = "missingMappedClass";
	public static final Integer MISSING_MAPPED_CLASS_ERROR_CODE = -104;
	public static final String METHOD_UNSUPPORTED = "methodUnsupported";
	public static final Integer METHOD_UNSUPPORTED_CODE = -105;
	public static final String METHOD_NOT_IMPLEMENTED = "methodNotImplemented";
	public static final Integer METHOD_NOT_IMPLEMENTED_CODE = -106;
	
	
	public SyncException(String name, Integer code, String desc, Throwable t) {
		super(desc, t);
		this.setCode(code);
		this.setName(name);
	}
	
	public SyncException(String name, Integer code, String desc) {
		super(desc);
		this.setName(name);
		this.setCode(code);
	}
	
	public SyncException(String name, Integer code) {
		super();
		this.setName(name);
		this.setCode(code);
	}
	
	public SyncException(Exception e) {
		super(e);
	}
	
	public SyncException() {
		super();
	}
	
	private Integer code;
	public void setCode(Integer value) {
		this.code = value;
	}
	public Integer getCode() {
		return this.code;
	}
	
	private String name;
	public String getName() {
		return name;
	}
	public void setName(String value) {
		this.name = value;
	}
}

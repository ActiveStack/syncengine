package com.percero.agents.sync.exceptions;

public class SyncDataException extends SyncException{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4029076748936883021L;

	public static final String CREATE_OBJECT_ERROR = "createObjectError";
	public static final Integer CREATE_OBJECT_ERROR_CODE = -200;

	public static final String READ_ONLY_ERROR = "readOnlyObject";
	public static final Integer READ_ONLY_ERROR_CODE = -2001;

	public static final String UPDATE_OBJECT_ERROR = "updateObjectError";
	public static final Integer UPDATE_OBJECT_ERROR_CODE = -201;
	
	public static final String DELETE_OBJECT_ERROR = "deleteObjectError";
	public static final Integer DELETE_OBJECT_ERROR_CODE = -202;
	
	public static final String MISSING_REQUIRED_FIELD = "missingRequiredField";
	public static final Integer MISSING_REQUIRED_FIELD_CODE = -251;
	
	public String fieldName = null;
	
	public SyncDataException(String name, Integer code, String desc, Throwable t) {
		super(name, code, desc, t);
	}
	
	public SyncDataException(String name, Integer code, String desc) {
		super(name, code, desc);
	}
	
	public SyncDataException(String name, Integer code) {
		super(name, code);
	}
	
	public SyncDataException(Exception e) {
		super(e);
	}
	
	public SyncDataException() {
		super();
	}
	
}

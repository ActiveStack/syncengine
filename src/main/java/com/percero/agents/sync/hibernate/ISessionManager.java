package com.percero.agents.sync.hibernate;


public interface ISessionManager {

	public Object createObject(Object theObject, String userId) throws Exception;
	public Object putObject(Object theObject, String userId) throws Exception;
	public Object deleteObject(Object theObject, String userId) throws Exception;
}
package com.percero.agents.sync.dao;

import com.percero.framework.vo.IPerceroObject;
import edu.emory.mathcs.backport.java.util.Collections;

import java.util.HashMap;
import java.util.Map;

public class DAORegistry {
	
	private static DAORegistry instance = null;
	
	public static DAORegistry getInstance() {
		if (instance == null) {
			instance = new DAORegistry();
		}
		return instance;
	}

	public DAORegistry() {
		instance = this;
	}
	
	@SuppressWarnings({ "unchecked" })
	private Map<String, IDataAccessObject<? extends IPerceroObject>> dataAccessObjects = Collections.synchronizedMap(new HashMap<String, IDataAccessObject<? extends IPerceroObject>>());
	
	public Map<String, IDataAccessObject<? extends IPerceroObject>> getDataAccessObjects() {
		return dataAccessObjects;
	}
	
	public void registerDataAccessObject(String name, IDataAccessObject<? extends IPerceroObject> dataAccessObject) {
		dataAccessObjects.put(name, dataAccessObject);
	}
	
	public IDataAccessObject<? extends IPerceroObject> getDataAccessObject(String name) {
		return dataAccessObjects.get(name);
	}
}

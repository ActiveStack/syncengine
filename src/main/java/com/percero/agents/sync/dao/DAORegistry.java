package com.percero.agents.sync.dao;

import java.util.HashMap;
import java.util.Map;

import com.percero.agents.sync.services.DAODataProvider;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.framework.vo.IPerceroObject;

import edu.emory.mathcs.backport.java.util.Collections;

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
		DataProviderManager.getInstance().addDataProvider(DAODataProvider.getInstance()); 
	}
	
	@SuppressWarnings({ "unchecked" })
	private Map<String, IDataAccessObject<? extends IPerceroObject>> dataAccessObjects = Collections.synchronizedMap(new HashMap<String, IDataAccessObject<? extends IPerceroObject>>());
	
	public void registerDataAccessObject(String name, IDataAccessObject<? extends IPerceroObject> dataAccessObject) {
		dataAccessObjects.put(name, dataAccessObject);
	}
	
	public IDataAccessObject<? extends IPerceroObject> getDataAccessObject(String name) {
		return dataAccessObjects.get(name);
	}
}

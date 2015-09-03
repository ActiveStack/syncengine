package com.percero.agents.sync.services;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.dao.IDataAccessObject;
import com.percero.framework.vo.IPerceroObject;

@Component
public class DAORegistry {
	
	private static DAORegistry instance = null;
	
	public static DAORegistry getInstance() {
		return instance;
	}

	public DAORegistry() {
		instance = this;
	}

	private static Map<String, IDataAccessObject<IPerceroObject>> dataAccessObjects;
	
	@PostConstruct
	public void init() {
		// Init the data access objects here.
		dataAccessObjects = new HashMap<String, IDataAccessObject<IPerceroObject>>();
	}
	
	public IDataAccessObject<IPerceroObject> getDataAccessObject(String name) {
		return dataAccessObjects.get(name);
	}
}

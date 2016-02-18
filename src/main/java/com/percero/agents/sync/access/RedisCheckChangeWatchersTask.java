package com.percero.agents.sync.access;

import org.apache.log4j.Logger;

import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.vo.IPerceroObject;

public class RedisCheckChangeWatchersTask implements Runnable {

	private static final Logger log = Logger.getLogger(RedisCheckChangeWatchersTask.class);

	public RedisCheckChangeWatchersTask(RedisAccessManager redisAccessManager,
			ClassIDPair classIdPair, String[] fieldNames, String[] params, boolean removeObjectChangeWatchers, IPerceroObject oldValue) {
		this.redisAccessManager = redisAccessManager;
		this.classIdPair = classIdPair;
		this.fieldNames = fieldNames;
		this.params = params;
		this.removeObjectChangeWatchers = removeObjectChangeWatchers;
		this.oldValue = oldValue;
	}
	
	private RedisAccessManager redisAccessManager;
	private ClassIDPair classIdPair;
	private String[] fieldNames;
	private String[] params;
	private boolean removeObjectChangeWatchers = false;
	private IPerceroObject oldValue;
	
	public void run() {
		try {
			redisAccessManager.internalCheckChangeWatchers(classIdPair, fieldNames, params, oldValue);
			
			if (removeObjectChangeWatchers) {
				redisAccessManager.removeChangeWatchersByObject(classIdPair);
			}
			
		} catch(Exception e) {
			log.error("Error running process", e);
		}
	}
}

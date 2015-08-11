package com.percero.agents.sync.helpers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class RedisPostClientTask implements Runnable {

	private static final Logger log = Logger.getLogger(RedisPostClientTask.class);

	public RedisPostClientTask(RedisPostClientHelper theClientHelper, String theClientId) {
		helper = theClientHelper;
		clientId = theClientId;
	}
	
	public RedisPostClientTask(RedisPostClientHelper theClientHelper, Collection<String> theClientIds) {
		helper = theClientHelper;
		clientIds = theClientIds;
	}
	
	private RedisPostClientHelper helper;
	private String clientId = null;
	private Collection<String> clientIds = null;
	
	public void run() {
		try {
			if (clientId != null)
				helper.postClient(clientId);
			
			if (clientIds != null) {
				Iterator<String> itrClientIds = clientIds.iterator();
				Collection<String> clientIdsToRemove = new HashSet<String>();
				while (itrClientIds.hasNext()) {
					String nextClientId = itrClientIds.next();
					helper.postClient(nextClientId);
					clientIdsToRemove.add(nextClientId);
				}
				
				Iterator<String> itrClientIdsToRemove = clientIdsToRemove.iterator();
				while (itrClientIdsToRemove.hasNext()) {
					clientIds.remove(itrClientIdsToRemove.next());
				}
			}
		} catch(Exception e) {
			log.error("Error running process", e);
		}
	}
}

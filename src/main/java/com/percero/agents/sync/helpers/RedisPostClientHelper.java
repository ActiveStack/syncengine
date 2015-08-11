package com.percero.agents.sync.helpers;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.datastore.RedisDataStore;

@Component
public class RedisPostClientHelper {
	
	private static final Logger log = Logger.getLogger(RedisPostClientHelper.class);
	
	@Autowired
	private RedisDataStore redisDataStore;
	
	@Autowired
	IAccessManager accessManager;
	
	@Autowired
	Long userDeviceTimeout = Long.valueOf(60 * 60 * 24 * 14);	// Two weeks
	public void setUserDeviceTimeout(Long value) {
		userDeviceTimeout = value;
	}

	@Transactional
	public void postClient(String clientId) throws Exception {
		// Get the client's userId.
		String userId = (String) redisDataStore.getValue(RedisKeyUtils.client(clientId));
		
		if (userId != null && userId.length() > 0) {
			// Timeout Client's User ID.
			redisDataStore.expire(RedisKeyUtils.client(clientId), userDeviceTimeout, TimeUnit.SECONDS);
			
			// Remove from NonPersistent Clients
			//redisDataStore.removeSetValue(RedisKeyUtils.clientsNonPersistent(), clientId);
			
			// Remove from Persistent Clients
			//redisDataStore.removeSetValue(RedisKeyUtils.clientsPersistent(), clientId);
			
			// Timeout Client User
			redisDataStore.expire(RedisKeyUtils.clientUser(userId), userDeviceTimeout, TimeUnit.SECONDS);
			
			// Timeout UpdateJournals for this Client.
			redisDataStore.expire(RedisKeyUtils.updateJournal(clientId), userDeviceTimeout, TimeUnit.SECONDS);
			
			// Timeout DeleteJournals for this Client.
			redisDataStore.expire(RedisKeyUtils.deleteJournal(clientId), userDeviceTimeout, TimeUnit.SECONDS);
			
//			// Timeout TransactionJournals for this Client.
//			Collection<String> transJournalKeys = redisDataStore.keys(RedisKeyUtils.transactionJournal(clientId, "*"));
//			Iterator<String> itrTransJournalKeys = transJournalKeys.iterator();
//			while (itrTransJournalKeys.hasNext()) {
//				redisDataStore.expire(itrTransJournalKeys.next(), userDeviceTimeout, TimeUnit.SECONDS);
//			}
			
			// Timeout Client's UserDevice.
//			Collection<String> userDeviceKeys = redisDataStore.keys(RedisKeyUtils.userDevice(userId, "*"));
			// TODO: This expire no longer works!!!
			Collection<Object> userDeviceKeys = redisDataStore.getHashKeys(RedisKeyUtils.userDeviceHash(userId));
			Iterator<Object> itrUserDevices = userDeviceKeys.iterator();
			while (itrUserDevices.hasNext()) {
				String nextKey = (String) itrUserDevices.next();
				if (clientId.equals(redisDataStore.getValue(nextKey))) {
					redisDataStore.expire(RedisKeyUtils.deviceHash(nextKey), userDeviceTimeout, TimeUnit.SECONDS);
					redisDataStore.expire(nextKey, userDeviceTimeout, TimeUnit.SECONDS);
				}
			}
		}
		else {
			// Clear out any AccessJournals for this client.
			log.debug("Client " + clientId + " had no valid User, clearing out AccessJournals for this client");
			accessManager.destroyClient(clientId);
		}
	}
}

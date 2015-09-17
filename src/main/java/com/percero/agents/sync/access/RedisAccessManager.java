package com.percero.agents.sync.access;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.percero.agents.auth.services.IAuthService;
import com.percero.agents.sync.cw.ChangeWatcherReporting;
import com.percero.agents.sync.cw.IChangeWatcherHelper;
import com.percero.agents.sync.cw.IChangeWatcherHelperFactory;
import com.percero.agents.sync.datastore.ICacheDataStore;
import com.percero.agents.sync.exceptions.ClientException;
import com.percero.agents.sync.helpers.RedisPostClientHelper;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.Client;

@Component
public class RedisAccessManager implements IAccessManager {
	
	private static Logger log = Logger.getLogger(RedisAccessManager.class);

	// TODO: Remove all AccessJournal objects for a User after they have been inactive for some amount of time.
	//	What should that amount of time be?...
	
	public RedisAccessManager() {
	}

	@Autowired
	IAuthService authService;
	
	@Autowired
	protected IChangeWatcherHelperFactory changeWatcherHelperFactory;
	public void setChangeWatcherHelperFactory(IChangeWatcherHelperFactory value) {
		changeWatcherHelperFactory = value;
	}
	
	@Autowired
	protected Long userDeviceTimeout = Long.valueOf(60 * 60 * 24 * 14);	// Two weeks
	public void setUserDeviceTimeout(Long value) {
		userDeviceTimeout = value;
	}
	
	@Autowired
	protected TaskExecutor taskExecutor;
	public void setTaskExecutor(TaskExecutor value) {
		taskExecutor = value;
	}

	@Autowired
	protected RedisPostClientHelper postClientHelper;
	public void setPostClientHelper(RedisPostClientHelper value) {
		postClientHelper = value;
	}

	//@Autowired
	//MongoOperations mongoOperations;

	@Autowired
	ICacheDataStore cacheDataStore;
	public void setCacheDataStore(ICacheDataStore cacheDataStore) {
		this.cacheDataStore = cacheDataStore;
	}
	
	@Autowired
	Boolean useChangeWatcherQueue = false;
	public void setUseChangeWatcherQueue(Boolean useChangeWatcherQueue) {
		this.useChangeWatcherQueue = useChangeWatcherQueue;
	}
	
	@Autowired
	String changeWatcherRouteName;
	public void setChangeWatcherRouteName(String changeWatcherRouteName) {
		this.changeWatcherRouteName = changeWatcherRouteName;
	}
	
	@Autowired
	IPushSyncHelper pushSyncHelper;
	public void setPushSyncHelper(IPushSyncHelper pushSyncHelper) {
		this.pushSyncHelper = pushSyncHelper;
	}
	

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#createClient(java.lang.String, java.lang.Integer, java.lang.String)
	 */
	public void createClient(String clientId, String userId, String deviceType, String deviceId) throws Exception {
		
		String clientUserKey = RedisKeyUtils.clientUser(userId);
		cacheDataStore.addSetValue(clientUserKey, clientId);

		if (StringUtils.hasText(deviceId)) {
			String userDeviceKey = RedisKeyUtils.userDeviceHash(userId);
			cacheDataStore.setHashValue(userDeviceKey, deviceId, clientId);
			String deviceKey = RedisKeyUtils.deviceHash(deviceId);
			cacheDataStore.addSetValue(deviceKey, clientId);
		}
		
		// Set the client's userId.
		cacheDataStore.setValue(RedisKeyUtils.client(clientId), userId);
		
		// Add to ClientUser list
		cacheDataStore.addSetValue(clientUserKey, clientId);
		
		if (Client.PERSISTENT_TYPE.equalsIgnoreCase(deviceType)) {
			cacheDataStore.addSetValue(RedisKeyUtils.clientsPersistent(), clientId);
		}
		else {
			cacheDataStore.addSetValue(RedisKeyUtils.clientsNonPersistent(), clientId);
		}
	}
	
	public Boolean isNonPersistentClient(String clientId) {
		return cacheDataStore.getSetIsMember(RedisKeyUtils.clientsNonPersistent(), clientId);
	}
	
	public String getClientUserId(String clientId) {
		return (String) cacheDataStore.getValue(RedisKeyUtils.client(clientId));
	}
	
	public Boolean findClientByClientIdUserId(String clientId, String userId) throws Exception {
		return cacheDataStore.getSetIsMember(RedisKeyUtils.clientUser(userId), clientId);
	}
	
	@SuppressWarnings("unchecked")
	public Set<String> findClientByUserIdDeviceId(String userId, String deviceId) throws Exception {
		Set<String> deviceClientIds = (Set<String>) cacheDataStore.getSetValue(RedisKeyUtils.deviceHash(deviceId));
		if (deviceClientIds == null) {
			deviceClientIds = new HashSet<String>(1);
		}
		String result = (String) cacheDataStore.getHashValue(RedisKeyUtils.userDeviceHash(userId), deviceId);
		if (!StringUtils.hasText(result)) {
			deviceClientIds.add(result);
		}
		return deviceClientIds;
//		return (String) redisDataStore.getHashValue(RedisKeyUtils.userDeviceHash(userId), deviceId);
	}
	
	public Boolean findClientByClientId(String clientId) {
		if (cacheDataStore.getSetIsMember(RedisKeyUtils.clientsPersistent(), clientId))
			return true;
		else if (cacheDataStore.getSetIsMember(RedisKeyUtils.clientsNonPersistent(), clientId))
			return true;
		else
			return false;
	}
	
	public Boolean validateClientByClientId(String clientId) {
		return validateClientByClientId(clientId, true);
	}
	
	public Boolean validateClientByClientId(String clientId, Boolean setClientTimeouts) {
		if (findClientByClientId(clientId)) {
			if (setClientTimeouts) {
				postClient(clientId);
			}
			return true;
		}
		else
			return false;
	}

	private static final Set<String> CLIENT_KEYS_SEY = new HashSet<String>(2);
	static {
		CLIENT_KEYS_SEY.add(RedisKeyUtils.clientsPersistent());
		CLIENT_KEYS_SEY.add(RedisKeyUtils.clientsNonPersistent());
	}
	
	@SuppressWarnings("unchecked")
	public Set<String> validateClients(Collection<String> clientIds) throws Exception {
		Set<String> validClients = (Set<String>) cacheDataStore.getSetsContainsMembers(CLIENT_KEYS_SEY, clientIds.toArray());
		return validClients;
	}
	
	@SuppressWarnings("unchecked")
	public Set<String> validateClientsIncludeFromDeviceHistory(Map<String, String> clientDevices) throws Exception {
		Set<String> validClients = (Set<String>) cacheDataStore.getSetsContainsMembers(CLIENT_KEYS_SEY, clientDevices.keySet().toArray());
		
		// Now check each device to see if it has a corresponding clientId.
		Iterator<Map.Entry<String, String>> itrClientDevices = clientDevices.entrySet().iterator();
		while (itrClientDevices.hasNext()) {
			Map.Entry<String, String> nextClientDevice = itrClientDevices.next();
			String nextClient = nextClientDevice.getKey();
			String nextDevice = nextClientDevice.getValue();
			
			if (cacheDataStore.getSetIsMember(RedisKeyUtils.deviceHash(nextDevice), nextClient)) {
				validClients.add(nextClient);
			}
		}
		
		return validClients;
	}
	
	@SuppressWarnings("unchecked")
	public String validateAndRetrieveCurrentClientId(String clientId, String deviceId) {
		if (validateClientByClientId(clientId)) {
			// Client is both Valid AND Current.
			return clientId;
		}
		else {
			// Client is NOT current, but could still be valid.
			if (cacheDataStore.getSetIsMember(RedisKeyUtils.deviceHash(deviceId), clientId)) {
				// Client IS valid, now get current Client.
				Set<String> validDeviceClientIds = (Set<String>) cacheDataStore.getSetValue(RedisKeyUtils.deviceHash(deviceId));
				Iterator<String> itrValidDeviceClientIds = validDeviceClientIds.iterator();
				while (itrValidDeviceClientIds.hasNext()) {
					String nextClientId = itrValidDeviceClientIds.next();
					if (validateClientByClientId(nextClientId)) {
						// Found ClientID that is both Valid AND Current.
						return nextClientId;
					}
				}
				
				// If we have gotten here then there is NO current and valid ClientId.
			}
			
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#registerClient(java.lang.String, java.lang.Integer)
	 */
	public void registerClient(String clientId, String userId, String deviceId) throws Exception {
		registerClient(clientId, userId, deviceId, null);
	}
	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#registerClient(java.lang.String, java.lang.Integer)
	 */
	public void registerClient(String clientId, String userId, String deviceId, String deviceType) throws Exception {
		Boolean isValidClient = findClientByClientIdUserId(clientId, userId);
		if (!isValidClient)
			createClient(clientId, userId, deviceType, deviceId);
		
		cacheDataStore.addSetValue(RedisKeyUtils.clientsLoggedIn(), clientId);
		cacheDataStore.deleteHashKey(RedisKeyUtils.clientsHibernated(), clientId);
		postClient(clientId);
	}
	
	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.access.IAccessManager#hibernateClient(java.lang.String, java.lang.String)
	 */
	// TODO: Is it possible for this to get hit out of order and inadvertantly hibernate a client that has already logged back in?
	public Boolean hibernateClient(String clientId, String userId) throws Exception {
		// Make sure this client/user combo is a valid one.
		if (findClientByClientIdUserId(clientId, userId)) {
			// Remove the client from the LoggedIn list and add it to the Hibernated list.
			cacheDataStore.setHashValue(RedisKeyUtils.clientsHibernated(), clientId, System.currentTimeMillis());
			cacheDataStore.removeSetValue(RedisKeyUtils.clientsLoggedIn(), clientId);
			
			// NOTE: The client is still in either the "client persistent" or "client non-persistent" list and
			//	is therefore still considered a valid client.
			postClient(clientId);
			return true;
		}
		else {
			return false;
		}
	}
	
	public Boolean upgradeClient(String clientId, String deviceId, String deviceType, String userId) throws Exception {
		
		Boolean result = false;
		
		// Make sure this is a valid client.
		Boolean isValidClient = findClientByClientIdUserId(clientId, userId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		Set<String> previousClientIds = null;
		if (deviceId != null && deviceId.length() > 0) {
			previousClientIds = findClientByUserIdDeviceId(userId, deviceId);
		}
		
		if (StringUtils.hasText(clientId)) {
			
			try {
				// Remove from NonPersistent list.
				cacheDataStore.removeSetValue(RedisKeyUtils.clientsNonPersistent(), clientId);
				
				// Add to Persistent List.
				cacheDataStore.addSetValue(RedisKeyUtils.clientsPersistent(), clientId);
				
				// Add to LoggedIn list
				cacheDataStore.addSetValue(RedisKeyUtils.clientsLoggedIn(), clientId);
				
				// Remove from Hibernated list
				cacheDataStore.deleteHashKey(RedisKeyUtils.clientsHibernated(), clientId);

				if (previousClientIds != null && !previousClientIds.isEmpty()) {
					Iterator<String> itrPreviousClientIds = previousClientIds.iterator();
					while (itrPreviousClientIds.hasNext()) {
						String nextPreviousClient = itrPreviousClientIds.next();
						if (StringUtils.hasText(nextPreviousClient) && nextPreviousClient.equals(clientId)) {
							// Remove from NonPersistent list.
							cacheDataStore.removeSetValue(RedisKeyUtils.clientsNonPersistent(), nextPreviousClient);
		
							renameClient(nextPreviousClient, clientId);
						}
					}
				}
				else {
					// Add to ClientUser list
					cacheDataStore.addSetValue(RedisKeyUtils.clientUser(userId), clientId);
				}

				result = true;

			} catch(Exception e) {
				log.error("Error upgrading client " + clientId + ", user " + userId, e);
				result = false;
			}
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public void renameClient(String thePreviousClient, String clientId) {
		
		if (thePreviousClient == null || clientId == null) {
			return;
		}
		else if (thePreviousClient.equals(clientId)) {
			return;
		}

		// Get the existing UserId.
		String userId = (String) cacheDataStore.getValue(RedisKeyUtils.client(thePreviousClient));
		log.debug("Renaming client " + thePreviousClient + " to " + clientId);
		
		if (StringUtils.hasText(userId)) {
			log.debug("Renaming user " + userId + " from client " + thePreviousClient + " to " + clientId);
			// Remove Previous Client from ClientUser list
			cacheDataStore.removeSetValue(RedisKeyUtils.clientUser(userId), thePreviousClient);
			// Add to ClientUser list
			cacheDataStore.addSetValue(RedisKeyUtils.clientUser(userId), clientId);
			
			// Update the UserDevice ClientID.
			String userDeviceHashKey = RedisKeyUtils.userDeviceHash(userId);
			Collection<String> userDeviceKeys = cacheDataStore.getHashKeys(userDeviceHashKey);
			Iterator<String> itrUserDevices = userDeviceKeys.iterator();
			while (itrUserDevices.hasNext()) {
				String nextDeviceKey = itrUserDevices.next();
				if (thePreviousClient.equals(cacheDataStore.getHashValue(userDeviceHashKey, nextDeviceKey))) {
					cacheDataStore.addSetValue(RedisKeyUtils.deviceHash(nextDeviceKey), clientId);
					cacheDataStore.setHashValue(userDeviceHashKey, nextDeviceKey, clientId);
				}
			}

			// Set the Client's userId
			cacheDataStore.setValue(RedisKeyUtils.client(clientId), userId);
			// Remove the Previous Client's userId
			cacheDataStore.deleteKey(RedisKeyUtils.client(thePreviousClient));
		}
		else {
			log.debug("Previous Client" + thePreviousClient + " has no corresponding UserID");
		}
		
		// Swap NonPersistent list.
		cacheDataStore.swapSetValue(RedisKeyUtils.clientsNonPersistent(), thePreviousClient, clientId);

		// Swap Persistent list.
		cacheDataStore.swapSetValue(RedisKeyUtils.clientsPersistent(), thePreviousClient, clientId);
		
		// Swap LoggedIn list.
		cacheDataStore.swapSetValue(RedisKeyUtils.clientsLoggedIn(), thePreviousClient, clientId);
		
		// Swap Hibernated list.
		cacheDataStore.swapHashKey(RedisKeyUtils.clientsHibernated(), thePreviousClient, clientId);
		
		// Rename the UpdateJournals list.
		String prevUpdateJournalKey = RedisKeyUtils.updateJournal(thePreviousClient);
		if (cacheDataStore.hasKey(prevUpdateJournalKey)) {
			if (!cacheDataStore.renameIfAbsent(prevUpdateJournalKey, RedisKeyUtils.updateJournal(clientId))) {
				// If new list already exists, then merge the two.
				cacheDataStore.setUnionAndStore(RedisKeyUtils.updateJournal(thePreviousClient), RedisKeyUtils.updateJournal(clientId), RedisKeyUtils.updateJournal(clientId));
				// Delete the old Key
				cacheDataStore.deleteKey(RedisKeyUtils.updateJournal(thePreviousClient));
			}
		}
		
		// Rename the DeleteJournals list.
		String prevDeleteJournalKey = RedisKeyUtils.deleteJournal(thePreviousClient);
		if (cacheDataStore.hasKey(prevDeleteJournalKey)) {
			if (!cacheDataStore.renameIfAbsent(RedisKeyUtils.deleteJournal(thePreviousClient), RedisKeyUtils.deleteJournal(clientId))) {
				// If new list already exists, then merge the two.
				cacheDataStore.setUnionAndStore(RedisKeyUtils.deleteJournal(thePreviousClient), RedisKeyUtils.deleteJournal(clientId), RedisKeyUtils.deleteJournal(clientId));
				// Delete the old Key
				cacheDataStore.deleteKey(RedisKeyUtils.deleteJournal(thePreviousClient));
			}
		}
		
		// Merge sets of Access Journals.
		String prevClientAccessJournalKey = RedisKeyUtils.clientAccessJournal(thePreviousClient);
		if (cacheDataStore.hasKey(prevClientAccessJournalKey)) {
			Set<String> accessJournalIds = (Set<String>)cacheDataStore.getSetValue(prevClientAccessJournalKey);
			Iterator<String> itrAccessJournalIds = accessJournalIds.iterator();
			while (itrAccessJournalIds.hasNext()) {
				String nextAccessJournalId = itrAccessJournalIds.next();
				cacheDataStore.swapSetValue(RedisKeyUtils.accessJournal(nextAccessJournalId), thePreviousClient, clientId);
			}
		}
		cacheDataStore.setUnionAndStore(RedisKeyUtils.clientAccessJournal(clientId), RedisKeyUtils.clientAccessJournal(thePreviousClient), RedisKeyUtils.clientAccessJournal(clientId));
		
		// Merge sets of ChangeWatchers.
		String prevWatcherClientKey = RedisKeyUtils.watcherClient(thePreviousClient);
		if (cacheDataStore.hasKey(prevWatcherClientKey)) {
			Set<String> changeWatcherIds = (Set<String>)cacheDataStore.getSetValue(prevWatcherClientKey);
			Iterator<String> itrChangeWatcherIds = changeWatcherIds.iterator();
			while (itrChangeWatcherIds.hasNext()) {
				String nextChangeWatcherId = itrChangeWatcherIds.next();
				cacheDataStore.swapSetValue(nextChangeWatcherId, thePreviousClient, clientId);
			}
		}
		cacheDataStore.setUnionAndStore(RedisKeyUtils.watcherClient(clientId), RedisKeyUtils.watcherClient(thePreviousClient), RedisKeyUtils.watcherClient(clientId));
		
//		// Rename the TransactionJournals list.
//		Collection<String> transKeys = redisDataStore.keys(RedisKeyUtils.transactionJournal(clientId, "*"));
//		Iterator<String> itrTransKeys = transKeys.iterator();
//		while(itrTransKeys.hasNext()) {
//			String nextKey = itrTransKeys.next();
//			redisDataStore.rename(nextKey, RedisKeyUtils.transactionJournal(clientId, RedisKeyUtils.transJournalTransactionId(nextKey)));
//		}
	}
	
	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#logoutClient(java.lang.String)
	 */
	@Transactional
	public void logoutClient(String clientId, Boolean pleaseDestroyClient) {
		if (!StringUtils.hasText(clientId)) {
			return;
		}
		
		log.debug("Logging out client " + clientId + " [" + (pleaseDestroyClient != null && pleaseDestroyClient ? "T" : "F") + "]");
		
		// Remove from LoggedIn Clients
		cacheDataStore.removeSetValue(RedisKeyUtils.clientsLoggedIn(), clientId);
		
		// Check to see if this is a non-persistent client type.
		Boolean isNonPersistent = cacheDataStore.getSetIsMember(RedisKeyUtils.clientsNonPersistent(), clientId);
		
		if (isNonPersistent || pleaseDestroyClient) {
			destroyClient(clientId);
		}
		else {
			// Set timeouts for client.
			postClient(clientId);
		}
	}
	
	@Transactional
	public void destroyClient(String clientId) {
		if (!StringUtils.hasText(clientId)) {
			return;
		}

		// Get the client's userId.
		String userId = (String) cacheDataStore.getValue(RedisKeyUtils.client(clientId));
		Boolean validUser = StringUtils.hasText(userId);
		
		// Remove from ClientUser list
		if (validUser) {
			cacheDataStore.removeSetValue(RedisKeyUtils.clientUser(userId), clientId);
		}
		
		// Remove from Client's User ID.
		cacheDataStore.deleteKey(RedisKeyUtils.client(clientId));
		
		// Remove from LoggedIn Clients
		cacheDataStore.removeSetValue(RedisKeyUtils.clientsLoggedIn(), clientId);

		// Remove from Hibernated Clients
		cacheDataStore.deleteHashKey(RedisKeyUtils.clientsHibernated(), clientId);
		
		// Remove from NonPersistent Clients
		cacheDataStore.removeSetValue(RedisKeyUtils.clientsNonPersistent(), clientId);
		
		// Remove from Persistent Clients
		cacheDataStore.removeSetValue(RedisKeyUtils.clientsPersistent(), clientId);
		
		// Delete all UpdateJournals for this Client.
		String updateJournalKey = RedisKeyUtils.updateJournal(clientId);
//		Set<String> updateJournalIds = (Set<String>)redisDataStore.getSetValue(updateJournalKey);
//		redisDataStore.removeSetsValue(updateJournalIds, clientId);
		cacheDataStore.deleteKey(updateJournalKey);
		
		// Delete all DeleteJournals for this Client.
		cacheDataStore.deleteKey(RedisKeyUtils.deleteJournal(clientId));
		
//		// Delete all TransactionJournals for this Client.
//		Collection<String> transJournalKeys = redisDataStore.keys(RedisKeyUtils.transactionJournal(clientId, "*"));
//		redisDataStore.deleteKeys(transJournalKeys);
		
		// Delete Client's UserDevice.
		if (validUser) {
			String userDeviceHashKey = RedisKeyUtils.userDeviceHash(userId);
			Collection<String> userDeviceKeys = cacheDataStore.getHashKeys(userDeviceHashKey);
			int originalSize = userDeviceKeys.size();
			int countRemoved = 0;
			Iterator<String> itrUserDevices = userDeviceKeys.iterator();
			while (itrUserDevices.hasNext()) {
				String nextDeviceKey = itrUserDevices.next();
				if (clientId.equals(cacheDataStore.getHashValue(userDeviceHashKey, nextDeviceKey))) {
					// Since the client id being destroyed, the whole device history related to this client is also to be destroyed. 
					//	However, as a safety precaution, set the expiration for the device hash.
					cacheDataStore.expire(RedisKeyUtils.deviceHash(nextDeviceKey), userDeviceTimeout, TimeUnit.SECONDS);
					cacheDataStore.removeSetValue(RedisKeyUtils.deviceHash(nextDeviceKey), clientId);
					cacheDataStore.deleteHashKey(userDeviceHashKey, nextDeviceKey);
					countRemoved++;
				}
			}
			
			if (countRemoved >= originalSize) {
				// Remove the UserDevice Hash.
				cacheDataStore.deleteKey(userDeviceHashKey);
			}
		}
		
		deleteClientAccessJournals(clientId);
		deleteClientWatchers(clientId);
		
//		// Remove Change Watcher.
//		String watcherClient = RedisKeyUtils.watcherClient(clientId);
//		if (redisDataStore.hasKey(watcherClient)) {
//			Collection<String> changeWatcherIds = (Collection<String>) redisDataStore.getSetValue(watcherClient);
//			Iterator<String> itrChangeWatchers = changeWatcherIds.iterator();
//			while (itrChangeWatchers.hasNext()) {
//				String nextChangeWatcherId = itrChangeWatchers.next();
//				// Remove from the ChangeWatchers List.
//				redisDataStore.removeSetValue(nextChangeWatcherId, clientId);
//				
//				//Long size = redisDataStore.removeSetValueAndGetSize(nextChangeWatcherId, clientId);
//			}
//			redisDataStore.deleteKey(watcherClient);
//		}

		/** CLEANUP **/
		// Remove from PushSyncHelper.
		pushSyncHelper.removeClient(clientId);
		// Remove from AuthService.
		authService.logoutUser(userId, null, clientId);
	}
	
	/**
	 * This function checks to see if a User has no more UserDevices. If they do not have
	 * 	any, then it removes all the User's entries in ALL AccessJournal sets.
	 * 
    	 * @param clientId
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	private void deleteClientAccessJournals(String clientId) {
		String clientAccessJournalsKey = RedisKeyUtils.clientAccessJournal(clientId);
		Set<String> clientAccessJournals = (Set<String>) cacheDataStore.getSetValue(clientAccessJournalsKey);
		cacheDataStore.removeSetsValue(RedisKeyUtils.ACCESS_JOURNAL_PREFIX, clientAccessJournals, clientId);
		
		// Now remove the Client's AccessJournal key.
		cacheDataStore.deleteKey(clientAccessJournalsKey);

        // Check to see if each Object's access journal set is empty and if so remove it
        // from the class access journal set
        for(String caj : clientAccessJournals){
            if(cacheDataStore.getSetIsEmpty(RedisKeyUtils.ACCESS_JOURNAL_PREFIX+caj)){
                String[] parts = caj.split(":");
                String className = parts[0];
                String ID = parts[1];
                String classAccessJournalKey = RedisKeyUtils.classAccessJournal(className);
                cacheDataStore.removeSetValue(classAccessJournalKey, ID);
            }
        }
	}
	
	@SuppressWarnings("unchecked")
	@Transactional
	private void deleteClientWatchers(String clientId) {
		String watcherClientKey = RedisKeyUtils.watcherClient(clientId);
		Set<String> watcherClientIds = (Set<String>) cacheDataStore.getSetValue(watcherClientKey);
		cacheDataStore.removeSetsValue("", watcherClientIds, clientId);
		
		// Now remove the Client's Watcher key.
		cacheDataStore.deleteKey(watcherClientKey);
	}
	
	/**
	 * Looks through a list of post AccessJournals and removes all Clients that are no longer valid.
	 */
	@SuppressWarnings("unchecked")
//	@Scheduled(fixedRate=600000)	// 10 Minutes
	@Scheduled(fixedRate=60000)	// 60 Seconds
//	@Scheduled(fixedRate=300000)	// 5 Minutes
	public void postAccessJournals() {
		log.info("Posting " +  postAccessJournals.size() + " Access Journal" + (postAccessJournals.size() == 1 ? "" : "s"));
		/**if (taskExecutor != null) {
			taskExecutor.execute(new RedisPostClientTask(postClientHelper, postClientIds));
		} else {*/
		
		Set<String> invalidClients = new HashSet<String>();
		Set<String> accessJournalsToDelete = new HashSet<String>(postAccessJournals.size());
		
		synchronized (postAccessJournals) {
			Iterator<String> itrAccessJournals = postAccessJournals.iterator();
			while (itrAccessJournals.hasNext()) {
				String nextAccessJournal = itrAccessJournals.next();
				accessJournalsToDelete.add(nextAccessJournal);
			}
		}

		// We are looking for invalid Client's in Access Journals to keep those Access Journals clean.
		Iterator<String> itrAccessJournals = accessJournalsToDelete.iterator();
		while (itrAccessJournals.hasNext()) {
			String nextAccessJournal = itrAccessJournals.next();
			Set<String> accessJournalClients = (Set<String>)cacheDataStore.getSetValue(nextAccessJournal);
			
			if (accessJournalClients != null) {
				Set<String> clientIdsToRemove = new HashSet<String>();
				Iterator<String> itrAccessJournalClients = accessJournalClients.iterator();
				while (itrAccessJournalClients.hasNext()) {
					String nextClientId = itrAccessJournalClients.next();
					
					if (invalidClients.contains(nextClientId)) {
						clientIdsToRemove.add(nextClientId);
					}
					else if (!findClientByClientId(nextClientId) || !StringUtils.hasText(getClientUserId(nextClientId)) ) {
						// This is an invalid Client, so add to the list of clients to remove and the list of invalid clients.
						invalidClients.add(nextClientId);
						clientIdsToRemove.add(nextClientId);
					}
				}
				
				if (!clientIdsToRemove.isEmpty()) {
					cacheDataStore.removeSetValues(nextAccessJournal, clientIdsToRemove);
				}
			}
		}
		
		synchronized (postAccessJournals) {
			postAccessJournals.removeAll(accessJournalsToDelete);
		}
		
		Iterator<String> itrInvalidClients = invalidClients.iterator();
		while (itrInvalidClients.hasNext()) {
			String nextInvalidClientId = itrInvalidClients.next();
			logoutClient(nextInvalidClientId, true);
		}
	}
	
//	@Scheduled(fixedRate=600000)	// 10 Minutes
	@Scheduled(fixedRate=300000)	// 5 Minutes
//	@Scheduled(fixedRate=120000)	// 2 Minutes
	public void postClients() {
		log.info("Posting " +  postClientIds.size() + " client" + (postClientIds.size() == 1 ? "" : "s"));
		/**if (taskExecutor != null) {
			taskExecutor.execute(new RedisPostClientTask(postClientHelper, postClientIds));
		} else {*/
		Collection<String> clientIdsToRemove = new HashSet<String>();
		synchronized (postClientIds) {
			Iterator<String> itrClientIds = postClientIds.iterator();
			while (itrClientIds.hasNext()) {
				String nextClientId = itrClientIds.next();
				clientIdsToRemove.add(nextClientId);
			}
		}
		
		Iterator<String> itrClientIdsToRemove = clientIdsToRemove.iterator();
		while (itrClientIdsToRemove.hasNext()) {
			try {
				String nextClientId = itrClientIdsToRemove.next();
				postClientHelper.postClient(nextClientId);
			} catch(Exception e) {
				log.error("Unable to post client", e);
			}
		}

		synchronized (postClientIds) {
			itrClientIdsToRemove = clientIdsToRemove.iterator();
			while (itrClientIdsToRemove.hasNext()) {
				postClientIds.remove(itrClientIdsToRemove.next());
			}
		}
			
		// Now check Hibernating clients to see if any of those need to be removed
		Map<String, Object> hibernatingClients = cacheDataStore.getHashEntries(RedisKeyUtils.clientsHibernated());
		Iterator<Map.Entry<String,Object>> itrHibernatingClientsEntries = hibernatingClients.entrySet().iterator();
		while (itrHibernatingClientsEntries.hasNext()) {
			Map.Entry<String,Object> nextEntry = itrHibernatingClientsEntries.next();
			String nextHibernatingClientId = null;
			try {
				nextHibernatingClientId = (String) nextEntry.getKey();
			} catch(Exception e) {
				// This is an invalid key, so remove it from the hash.
				try {
					cacheDataStore.deleteHashKey(RedisKeyUtils.clientsHibernated(), nextEntry.getKey());
				} catch(Exception e1) {
					log.error("Invalid hash key in " + RedisKeyUtils.clientsHibernated());
				}
				continue;
			}

			Long nextHibernateTime = null;
			try {
				nextHibernateTime = (Long) nextEntry.getValue();
			} catch(Exception e) {
				// This timeout is not in a valid format, set to current time and move on.
				try {
					cacheDataStore.setHashValue(RedisKeyUtils.clientsHibernated(), nextHibernatingClientId, nextEntry.getValue());
				} catch(Exception e1) {
					log.error("Invalid hash value in " + RedisKeyUtils.clientsHibernated());
				}
				continue;
			}
			
			// Now check the time.
			long currenTimeMillis = System.currentTimeMillis();
			if ((currenTimeMillis-nextHibernateTime) > userDeviceTimeout * 1000) {
				// The client has timed out, so logout and destroy this client.
				try {
					logoutClient(nextHibernatingClientId, true);
				} catch (Exception e) {
					log.error("Unable to remove hibernating client " + nextHibernatingClientId);
				}
			}
		}
			
		/**}*/
	}
	
	private Collection<String> postClientIds = Collections.synchronizedSet(new HashSet<String>());
	public void postClient(String clientId) {
		postClientIds.add(clientId);
	}
	
	private Collection<String> postAccessJournals = Collections.synchronizedSet(new HashSet<String>());
	public void postAccessJournal(String id) {
		postAccessJournals.add(id);
	}
	
	public void saveUpdateJournalClients(ClassIDPair pair, Collection<String> clientIds, Boolean guaranteeDelivery, String pusherClient, Boolean sendToPusher) throws Exception {
		if (clientIds != null && clientIds.size() > 0) {
			try {
				String classValue = RedisKeyUtils.classIdPair(pair.getClassName(), pair.getID());

				for(String nextClient : clientIds) {
					if (!sendToPusher && pusherClient != null) {
						// Don't send to pushing Client
						if (nextClient.equals(pusherClient)) {
							continue;
						}
					}
					
					String key = RedisKeyUtils.updateJournal(nextClient);
					cacheDataStore.addSetValue(key, classValue);
				}

			} catch(Exception e) {
				log.error("Error saving UpdateJournal for " + pair.toString(), e);
			}
		}
	}
	
	public Long deleteUpdateJournal(String clientId, String className, String classId) {
		return cacheDataStore.removeSetValue(RedisKeyUtils.updateJournal(clientId), RedisKeyUtils.classIdPair(className, classId));
	}
	
	public void deleteUpdateJournals(String clientId, ClassIDPair[] objects) {
		for(ClassIDPair nextObject : objects) {
			cacheDataStore.removeSetValue(RedisKeyUtils.updateJournal(clientId), RedisKeyUtils.classIdPair(nextObject.getClassName(), nextObject.getID()));
		}
	}
	
	public void saveDeleteJournalClients(ClassIDPair pair, Collection<String> clientIds, Boolean guaranteeDelivery, String pusherClient, Boolean sendToPusher) throws Exception {
		if (clientIds != null && clientIds.size() > 0) {
			try {
				String classValue = RedisKeyUtils.classIdPair(pair.getClassName(), pair.getID());

				for(String nextClient : clientIds) {
					if (!sendToPusher && pusherClient != null) {
						// Don't send to pushing Client
						if (nextClient.equals(pusherClient)) {
							continue;
						}
					}
					
					String key = RedisKeyUtils.deleteJournal(nextClient);
					cacheDataStore.addSetValue(key, classValue);
				}

			} catch(Exception e) {
				log.error("Error saving DeleteJournal for " + pair.toString(), e);
			}
		}
	}
	
	public Long deleteDeleteJournal(String clientId, String className, String classId) {
		return cacheDataStore.removeSetValue(RedisKeyUtils.deleteJournal(clientId), RedisKeyUtils.classIdPair(className, classId));
	}
	
	public void deleteDeleteJournals(String clientId, ClassIDPair[] objects) {
		for(ClassIDPair nextObject : objects) {
			cacheDataStore.removeSetValue(RedisKeyUtils.deleteJournal(clientId), RedisKeyUtils.classIdPair(nextObject.getClassName(), nextObject.getID()));
		}
	}
	
	public boolean saveAccessJournal(List<ClassIDPair> theList, String userId, String clientId) throws Exception {
		boolean saveAccessJournalFailure = false;
		for(ClassIDPair nextObject : theList) {
			if (!saveAccessJournal(nextObject, userId, clientId))
				saveAccessJournalFailure = true;
		}
		
		return !saveAccessJournalFailure;
	}

	public boolean saveAccessJournal(ClassIDPair classIdPair, String userId, String clientId) throws Exception {
		return (upsertRedisAccessJournal(userId, clientId, classIdPair.getClassName(), classIdPair.getID()) >= 0);
	}
	
	private Long upsertRedisAccessJournal(String userId, String clientId, String className, String classId) {
		String key = RedisKeyUtils.accessJournal(className, classId);
		try {
			// Need to add the ObjectID to the Client's AccessJournals set.
			String clientAccessJournalKey = RedisKeyUtils.clientAccessJournal(clientId);
			cacheDataStore.addSetValue(clientAccessJournalKey, RedisKeyUtils.objectId(className, classId));
			
            // Add to the class's AccessJournals set
            if(classId != null && !classId.isEmpty()) { // && !classId.equals("0")) {
                log.info("Adding to class AccessJournals: "+classId);
                String classAccessJournalKey = RedisKeyUtils.classAccessJournal(className);
                cacheDataStore.addSetValue(classAccessJournalKey, classId);
            }

			// Need to add the ClientID to the Object's AccessJournal set.
			return cacheDataStore.addSetValue(key, clientId);
		} catch(Exception e) {
			log.error("Unable to upsertRedisAccessJournal", e);
		} finally {
			postAccessJournal(key);
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#getObjectAccessJournals(java.lang.String, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	//@SuppressWarnings("unchecked")
	@Transactional
	public List<String> getObjectAccessJournals(String className, String classId) throws Exception {
		List<String> result = new ArrayList<String>();
		
//		Collection<String> keysNames = new HashSet<String>();
//		keysNames.add(RedisKeyUtils.accessJournal(className, classId));
		String allObjectAccessJournalKey = RedisKeyUtils.accessJournal(className, "0");
		String objectAccessJournalKey = RedisKeyUtils.accessJournal(className, classId);
//		keysNames.add(allObjectAccessJournalKey);

		// Need to get list of all classes that inherit from this class.
/*		MappedClass mc = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(className);
		while (mc != null && mc.parentMappedClass != null) {
			keysNames.add(RedisKeyUtils.accessJournal(mc.parentMappedClass.className, classId));
			keysNames.add(RedisKeyUtils.accessJournal(mc.parentMappedClass.className, "0"));
			mc = mc.parentMappedClass;
		}*/
		
		//moveAccessJournals();
		// TODO: Use a class id map instead of class name for faster lookup.
		//	Use hash of full class name to generate ID?
		Set<String> redisClassIdSet = (Set<String>) cacheDataStore.getSetUnion(objectAccessJournalKey, allObjectAccessJournalKey);
		result.addAll(redisClassIdSet);
//				RedisKeyUtils.accessJournal(className, classId),
//				RedisKeyUtils.accessJournal(className, "0"));
		//System.out.println(redisClassIdSet.size());
//		result = checkUserListAccessRights(redisClassIdSet, className, classId);
		
		postAccessJournal(allObjectAccessJournalKey);
		postAccessJournal(objectAccessJournalKey);

		return result;
	}

    @SuppressWarnings("unchecked")
	public Set<String> getClassAccessJournalIDs(String className){
        return (Set<String>) cacheDataStore.getSetValue(RedisKeyUtils.classAccessJournal(className));
    }

    public long getNumClientsInterestedInWholeClass(String className){
        return cacheDataStore.getSetSize(RedisKeyUtils.accessJournal(className,"0"));
    }

	public List<String> checkUserListAccessRights(Collection<Object> clientIdList, String className, String classId) throws Exception {
		List<String> result = new ArrayList<String>();
		
		for(Object nextClientId : clientIdList) {
			if (StringUtils.hasText((String)nextClientId))
				result.add((String)nextClientId);
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#getClientUpdateJournals(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public Collection<String> getClientUpdateJournals(String clientId, Boolean loggedInOnly) throws Exception {
		String key = RedisKeyUtils.updateJournal(clientId);
		Collection<String> updateJournalSet = null;
		
		if (loggedInOnly) {
			if (cacheDataStore.getSetIsMember(RedisKeyUtils.clientsLoggedIn(), clientId)) {
				updateJournalSet = (Set<String>) cacheDataStore.getSetValue(key);
			}
			else {
				updateJournalSet = new HashSet<String>(0);
			}
		}
		else {
			updateJournalSet = (Set<String>) cacheDataStore.getSetValue(key);
		}
		
		return updateJournalSet;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#getClientDeleteJournals(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public Collection<String> getClientDeleteJournals(String clientId, Boolean loggedInOnly) throws Exception {
		String key = RedisKeyUtils.deleteJournal(clientId);
		Collection<String> deleteJournalSet = null;
		
		if (loggedInOnly) {
			if (cacheDataStore.getSetIsMember(RedisKeyUtils.clientsLoggedIn(), clientId)) {
				deleteJournalSet = (Set<String>) cacheDataStore.getSetValue(key);
			}
			else {
				deleteJournalSet = new HashSet<String>(0);
			}
		}
		else {
			deleteJournalSet = (Set<String>) cacheDataStore.getSetValue(key);
		}
		
		return deleteJournalSet;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#removeUpdateJournals(java.util.List)
	 */
	public void removeUpdateJournals(String clientId, Collection<Object> updateJournalsToRemove) throws Exception {
		String key = RedisKeyUtils.updateJournal(clientId);
		cacheDataStore.removeSetValues(key, updateJournalsToRemove);
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#removeUpdateJournalsByObject(ClassIDPair)
	 */
	public void removeUpdateJournalsByObject(ClassIDPair classIdPair) throws Exception {
		// Don't do anything since having removed objects in the UpdateJournal are not a problem.
	}

	
	@SuppressWarnings("unchecked")
	public void removeAccessJournalsByObject(ClassIDPair classIdPair) {
		String objectId = RedisKeyUtils.objectId(classIdPair.getClassName(), classIdPair.getID());
		String accessJournalKey = RedisKeyUtils.accessJournal(classIdPair.getClassName(), classIdPair.getID());

		Set<String> clientIds = (Set<String>) cacheDataStore.getSetValue(accessJournalKey);
		Set<String> clientAccessJournalKeys = new HashSet<String>(clientIds.size());
		Iterator<String> itrClientIds = clientIds.iterator();
		while (itrClientIds.hasNext()) {
			String nextClientId = itrClientIds.next();
			// Remove Class ID Pair from Client's AccessJournal
			String clientAccessJournalsKey = RedisKeyUtils.clientAccessJournal(nextClientId);
			clientAccessJournalKeys.add(clientAccessJournalsKey);
		}
		
		cacheDataStore.removeSetsValue(clientAccessJournalKeys, objectId);
		
//		Iterator<String> itrClientIds = clientIds.iterator();
//		while (itrClientIds.hasNext()) {
//			String nextClientId = itrClientIds.next();
//			// Remove Class ID Pair from Client's AccessJournal
//			String clientAccessJournalsKey = RedisKeyUtils.clientAccessJournal(nextClientId);
//			redisDataStore.removeSetValue(clientAccessJournalsKey, objectId);
//			
//			// If Client's AccessJournal set is empty, then remove that key.
//			if (redisDataStore.getSetIsEmpty(clientAccessJournalsKey)) {
//				redisDataStore.deleteKey(clientAccessJournalsKey);
//			}
//		}

        String classAccessJournalKey = RedisKeyUtils.classAccessJournal(classIdPair.getClassName());
        cacheDataStore.removeSetValue(classAccessJournalKey, classIdPair.getID());
        
		// Now delete the AccessJournal record.
		cacheDataStore.deleteKey(accessJournalKey);
	}

	public void removeObjectModJournalsByObject(ClassIDPair classIdPair) {
		cacheDataStore.deleteKey(RedisKeyUtils.objectModJournal(classIdPair.getClassName(), classIdPair.getID()));
	}
	
	public void removeHistoricalObjectsByObject(ClassIDPair classIdPair) {
		cacheDataStore.deleteKey(RedisKeyUtils.historicalObject(classIdPair.getClassName(), classIdPair.getID()));
	}
	
	
	
	////////////////////////////////////////////////////////////
	//	HELPER FUNCTIONS
	////////////////////////////////////////////////////////////
	
	
	
	
	////////////////////////////////////////////////////////
	//	CHANGE WATCHER
	////////////////////////////////////////////////////////
	public void addWatcherField(ClassIDPair classIdPair, String fieldName, Collection<String> collection) {
		addWatcherField(classIdPair, fieldName, collection, null);
	}
	public void addWatcherField(ClassIDPair classIdPair, String fieldName, Collection<String> collection, String[] params) {
		addWatcherField(classIdPair.getClassName(), classIdPair.getID(), fieldName, collection, params);
	}
	public void addWatcherField(String category, String subCategory, String fieldName, Collection<String> collection) {
		addWatcherField(category, subCategory, fieldName, collection, null);
	}
	public void addWatcherField(String category, String subCategory, String fieldName, Collection<String> collection, String[] params) {
		String fieldWatcherId = "";
		if (params != null)
			fieldWatcherId = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcherWithParams(category, subCategory, fieldName, params));
		else
			fieldWatcherId = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcher(category, subCategory, fieldName));
		if (collection != null)
			collection.add(fieldWatcherId);
	}
	
	@Transactional
	public void addWatcherClient(ClassIDPair classIdPair, String fieldName, String clientId) {
		addWatcherClient(classIdPair, fieldName, clientId, null);
	}
	
	@Transactional
	public void addWatcherClient(ClassIDPair classIdPair, String fieldName, String clientId, String[] params) {
		String changeWatcherId = "";
		if (params != null)
			changeWatcherId = RedisKeyUtils.clientWatcher(RedisKeyUtils.changeWatcherWithParams(classIdPair.getClassName(), classIdPair.getID(), fieldName, params));
		else
			changeWatcherId = RedisKeyUtils.clientWatcher(RedisKeyUtils.changeWatcher(classIdPair.getClassName(), classIdPair.getID(), fieldName));

		// Add the ClientID to the ChangeWatcher client Set
		cacheDataStore.addSetValue(changeWatcherId, clientId);
		// Also add the ChangeWatcher ID to the client ChangeWatcher Set
		cacheDataStore.addSetValue(RedisKeyUtils.watcherClient(clientId), changeWatcherId);
	}
	
	public void updateWatcherFields(ClassIDPair classIdPair, String fieldName, Collection<String> fieldsToWatch) {
		updateWatcherFields(classIdPair, fieldName, fieldsToWatch, null);
	}
	
	public void updateWatcherFields(ClassIDPair classIdPair, String fieldName, Collection<String> fieldsToWatch, String[] params) {
		updateWatcherFields(classIdPair.getClassName(), classIdPair.getID(), fieldName, fieldsToWatch, params);
	}

	public void updateWatcherFields(String category, String subCategory, String fieldName, Collection<String> fieldsToWatch) {
		updateWatcherFields(category, subCategory, fieldName, fieldsToWatch, null);
	}
	
	@SuppressWarnings("unchecked")
	@Transactional
	public void updateWatcherFields(String category, String subCategory, String fieldName, Collection<String> fieldsToWatch, String[] params) {
		String changeWatcherId = "";
		String watcherField = "";
		if (params != null) {
			changeWatcherId = RedisKeyUtils.changeWatcherWithParams(category, subCategory, fieldName, params);
			watcherField = RedisKeyUtils.watcherField(RedisKeyUtils.changeWatcher(category, subCategory, fieldName));
		}
		else {
			changeWatcherId = RedisKeyUtils.changeWatcher(category, subCategory, fieldName);
			watcherField = RedisKeyUtils.watcherField(changeWatcherId);
		}

		Collection<String> watchedFields = (Set<String>) cacheDataStore.getSetValue(watcherField);
		
		// Remove current set of fields to watch and replace with new set.
		cacheDataStore.replaceSet(watcherField, fieldsToWatch);
		
		if (fieldsToWatch != null) {
			Iterator<String> itrFieldsToWatch = fieldsToWatch.iterator();
			while (itrFieldsToWatch.hasNext()) {
				String nextField = itrFieldsToWatch.next();
				
				// Also add this ChangeWatcher from the FieldWatcher.
				cacheDataStore.addSetValue(nextField, changeWatcherId);
				
				// Keep track of field that is being watched at an object level.
				String[] nextFieldArr = nextField.split(":");
				if (nextFieldArr.length >= 7) {
					String nextFieldClassName = nextFieldArr[4];
					String nextFieldClassId = nextFieldArr[5];
					String nextFieldFieldName = nextFieldArr[6];
					String hashKey = "";

					if (nextFieldArr.length > 7) {
						String[] nextFieldParams = new String[nextFieldArr.length-7];
						for(int i=7; i<nextFieldArr.length; i++) {
							nextFieldParams[i-7] = nextFieldArr[i];
						}
						hashKey = RedisKeyUtils.fieldWithParams(nextFieldFieldName, nextFieldParams);
					}
					else {
						hashKey = RedisKeyUtils.field(nextFieldFieldName);
					}
					
					String objectWatchedFieldKey = RedisKeyUtils.objectWatchedFields(nextFieldClassName, nextFieldClassId);
					cacheDataStore.setHashValue(objectWatchedFieldKey, hashKey, nextField);
				}
				
				if (watchedFields != null) {
					watchedFields.remove(nextField);
				}
			}
		}
		
		// Now remove any remaining watchedFields (since they no longer belong).
		if (watchedFields != null) {
			Iterator<String> itrOldWatchedFields = watchedFields.iterator();
			
			while (itrOldWatchedFields.hasNext()) {
				String nextOldField = itrOldWatchedFields.next();
				
				// Also remove this ChangeWatcher from the FieldWatcher.
				cacheDataStore.removeSetValue(nextOldField, changeWatcherId);
			}
		}
	}
	
	public void removeChangeWatchersByObject(ClassIDPair classIdPair) {
		removeChangeWatchers(classIdPair.getClassName(), classIdPair.getID());
	}
	@SuppressWarnings("unchecked")
	public void removeChangeWatchers(String category, String subCategory) {
		String categoryKey = RedisKeyUtils.changeWatcherClass(category, subCategory);
		
		// Get all change watcher values associated with this object.
		Set<String> changeWatcherValueKeys = cacheDataStore.getHashKeys(categoryKey);
		Iterator<String> itrChangeWatcherValueKeys = changeWatcherValueKeys.iterator();
		while (itrChangeWatcherValueKeys.hasNext()) {
			String nextChangeWatcherValueKey = itrChangeWatcherValueKeys.next();
			// If this is a RESULT key, then add it to the list to check.
			if (nextChangeWatcherValueKey.endsWith(":" + RedisKeyUtils.RESULT)) {
				int resultIndex = nextChangeWatcherValueKey.lastIndexOf(":" + RedisKeyUtils.RESULT);
				String changeWatcherKey = categoryKey + ":" + nextChangeWatcherValueKey.substring(0, resultIndex);
				String key = changeWatcherKey;
				String clientWatcherKey = RedisKeyUtils.clientWatcher(changeWatcherKey);
				
				// Remove WatcherFields.
				// For every watcherField, find the corresponding set and remove this changeWatcherKey
				//	from that set.
				String watcherField = RedisKeyUtils.watcherField(changeWatcherKey);
				Set<String> watcherFieldValues = (Set<String>) cacheDataStore.getSetValue(watcherField);
				Iterator<String> itrWatcherFieldValues = watcherFieldValues.iterator();
				while (itrWatcherFieldValues.hasNext()) {
					String nextWatcherFieldValue = itrWatcherFieldValues.next();
					cacheDataStore.removeSetValue(nextWatcherFieldValue, changeWatcherKey);
				}
				
				String fieldWatcher = RedisKeyUtils.fieldWatcher(changeWatcherKey);
				
				// Now remove all keys associated with this Change Watcher Value.
				cacheDataStore.deleteKey(key);
				cacheDataStore.deleteKey(clientWatcherKey);
				cacheDataStore.deleteKey(watcherField);
				cacheDataStore.deleteKey(fieldWatcher);
			}
		}

		String objectWatchedFieldKey = RedisKeyUtils.objectWatchedFields(category, subCategory);
		Set<String> objectWatcherValueKeys = cacheDataStore.getHashKeys(objectWatchedFieldKey);
		Iterator<String> itrObjectWatcherValueKeys = objectWatcherValueKeys.iterator();
		while (itrObjectWatcherValueKeys.hasNext()) {
			String nextObjectWatcherValueKey = itrObjectWatcherValueKeys.next();

			// Get all fields for this object that are being watched and remove them.
			String changeWatcherKey = (String) cacheDataStore.getHashValue(objectWatchedFieldKey, nextObjectWatcherValueKey);
			cacheDataStore.deleteKey(changeWatcherKey);
		}
		cacheDataStore.deleteKey(objectWatchedFieldKey);	// Removes ALL change watcher values for this Object.
		
		cacheDataStore.deleteKey(categoryKey);	// Removes ALL change watcher values for this Object.
	}

	public void saveChangeWatcherResult(ClassIDPair classIdPair, String fieldName, Object result) {
		saveChangeWatcherResult(classIdPair, fieldName, result, null);
	}
	
	@Transactional
	public void saveChangeWatcherResult(ClassIDPair classIdPair, String fieldName, Object result, String[] params) {
		String classKey = RedisKeyUtils.changeWatcherClass(classIdPair.getClassName(), classIdPair.getID());

		HashMap<String, Object> resultHash = new HashMap<String, Object>();
		resultHash.put(RedisKeyUtils.changeWatcherValueTimestamp(fieldName, params), System.currentTimeMillis());
		resultHash.put(RedisKeyUtils.changeWatcherValueResult(fieldName, params), result);
		cacheDataStore.setAllHashValues(classKey, resultHash);
	}
	
	public void removeChangeWatcherResult(ClassIDPair classIdPair, String fieldName) {
		removeChangeWatcherResult(classIdPair, fieldName, null);
	}
	
	@Transactional
	public void removeChangeWatcherResult(ClassIDPair classIdPair, String fieldName, String[] params) {
		String key = RedisKeyUtils.changeWatcherClass(classIdPair.getClassName(), classIdPair.getID());

		cacheDataStore.deleteHashKey(key, RedisKeyUtils.changeWatcherValueTimestamp(fieldName, params));
		cacheDataStore.deleteHashKey(key, RedisKeyUtils.changeWatcherValueResult(fieldName, params));
	}
	
	public Long getChangeWatcherResultTimestamp(ClassIDPair classIdPair, String fieldName) {
		return getChangeWatcherResultTimestamp(classIdPair, fieldName, null);
	}
	
	public Long getChangeWatcherResultTimestamp(ClassIDPair classIdPair, String fieldName, String[] params) {
		String key = RedisKeyUtils.changeWatcherClass(classIdPair.getClassName(), classIdPair.getID());

		Long result = (Long) cacheDataStore.getHashValue(key, RedisKeyUtils.changeWatcherValueTimestamp(fieldName, params));
		return result;
	}
	
	protected Object[] parseChangeWatcherId(String changeWatcherId) {
		Object[] result = new Object[3];
		
		if (!StringUtils.hasText(changeWatcherId)) {
			return null;
		}
		else {
			String[] params = changeWatcherId.split(":");
			if (params.length >= 5) {
				String className = params[2];
				String classId = params[3];
				result[0] = new ClassIDPair(classId, className);
				String fieldName = params[4];
				result[1] = fieldName;
				
				String[] otherParams = null;
				if (params.length > 5) {
					otherParams = new String[params.length-5];
					for(int i=5; i < params.length; i++) {
						try {
							otherParams[i-5] = URLDecoder.decode(params[i], "UTF-8");
						} catch (Exception e) {
							log.error(e);
						}
					}
				}
				
				result[2] = otherParams;
			}
			
			return result;
		}
	}
	
	public Boolean getChangeWatcherResultExists(String changeWatcherId) {
		Object[] parsed = parseChangeWatcherId(changeWatcherId);
		if (parsed != null) {
			return getChangeWatcherResultExists((ClassIDPair) parsed[0], (String) parsed[1], (String[]) parsed[2]);
		}
		else {
			return false;
		}
	}
	
	public Boolean getChangeWatcherResultExists(ClassIDPair classIdPair, String fieldName) {
		return getChangeWatcherResultExists(classIdPair, fieldName, null);
	}

	public Boolean getChangeWatcherResultExists(ClassIDPair classIdPair, String fieldName, String[] params) {
		String key = RedisKeyUtils.changeWatcherClass(classIdPair.getClassName(), classIdPair.getID());

		Boolean hasKey = cacheDataStore.hasHashKey(key, RedisKeyUtils.changeWatcherValueResult(fieldName, params));
		
		return hasKey;
	}
	
	public Object getChangeWatcherResult(ClassIDPair classIdPair, String fieldName) {
		return getChangeWatcherResult(classIdPair, fieldName, null);
	}
	
	public Object getChangeWatcherResult(ClassIDPair classIdPair, String fieldName, String[] params) {
		String key = RedisKeyUtils.changeWatcherClass(classIdPair.getClassName(), classIdPair.getID());

		return cacheDataStore.getHashValue(key, RedisKeyUtils.changeWatcherValueResult(fieldName, params));
	}
	
	public void checkChangeWatchers(ClassIDPair classIdPair) {
		checkChangeWatchers(classIdPair, null, null);
	}
	
	public void checkChangeWatchers(ClassIDPair classIdPair, String[] fieldNames, String[] params) {
		Collection<String> changeWatchers = getChangeWatchersForField(classIdPair, fieldNames, params);
		
		// If there are ChangeWatchers, then recalculate for each one.
		if (changeWatchers != null) {
			Iterator<String> itrChangeWatchers = changeWatchers.iterator();
			while (itrChangeWatchers.hasNext()) {
				String nextChangeWatcher = itrChangeWatchers.next();
				// TODO: Test this optimization.
//				if (getChangeWatcherResultExists(nextChangeWatcher)) {
					setupRecalculateChangeWatcher(nextChangeWatcher);
//				}
//				else {
//					// Remove this change watcher from the list.
//					log.warn("Change Watcher Result does not exist, NOT setting up to recalculate: " + nextChangeWatcher + " (" + classIdPair.toString() + ")");
//					removeChangeWatcherForField(classIdPair, fieldNames, params, nextChangeWatcher);
//				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected Collection<String> getChangeWatchersForField(ClassIDPair classIdPair, String[] fieldNames, String[] params) {
		Set<String> fieldWatcherKeys = new HashSet<String>();
		if (fieldNames == null || fieldNames.length == 0) {
			fieldWatcherKeys = getAllFieldWatchersForObject(classIdPair);
		}
		else {
			for(String fieldName : fieldNames) {
				if (fieldName.equals("*")) {
					fieldWatcherKeys.addAll(getAllFieldWatchersForObject(classIdPair));
				}
				else {
					String fieldWatcherKey = "";
					// Also add for ALL objects of this class type
					String allClassObjectsFieldWatcherKey = "";
					if (params != null) {
						fieldWatcherKey = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcherWithParams(classIdPair.getClassName(), classIdPair.getID(), fieldName, params));
						allClassObjectsFieldWatcherKey = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcherWithParams(classIdPair.getClassName(), "0", fieldName, params));
					}
					else {
						fieldWatcherKey = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcher(classIdPair.getClassName(), classIdPair.getID(), fieldName));
						allClassObjectsFieldWatcherKey = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcher(classIdPair.getClassName(), "0", fieldName));
					}
					
					fieldWatcherKeys.add(fieldWatcherKey);
					fieldWatcherKeys.add(allClassObjectsFieldWatcherKey);
				}
			}
		}
		
		// Also add for this object of this class type with NO field name
		String noFieldWatcherKey = "";
		// Also add for ALL objects of this class type with NO field name
		String allClassObjectsNoFieldWatcherKey = "";
		if (params != null) {
			noFieldWatcherKey = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcherWithParams(classIdPair.getClassName(), classIdPair.getID(), "", params));
			allClassObjectsNoFieldWatcherKey = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcherWithParams(classIdPair.getClassName(), "0", "", params));
		}
		else {
			noFieldWatcherKey = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcher(classIdPair.getClassName(), classIdPair.getID(), ""));
			allClassObjectsNoFieldWatcherKey = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcher(classIdPair.getClassName(), "0", ""));
		}
		
		fieldWatcherKeys.add(noFieldWatcherKey);
		fieldWatcherKeys.add(allClassObjectsNoFieldWatcherKey);
		
		Collection<String> changeWatchers = null;
		if (fieldWatcherKeys.size() > 0) {
			changeWatchers = (Set<String>) cacheDataStore.getSetUnion(noFieldWatcherKey, fieldWatcherKeys);
		}
		
		return changeWatchers;
	}
	
	protected Set<String> getAllFieldWatchersForObject(ClassIDPair classIdPair) {
		Set<String> fieldWatcherKeys = new HashSet<String>();
		
		String objectWatchedFieldKey = RedisKeyUtils.objectWatchedFields(classIdPair.getClassName(), classIdPair.getID());
		// Also add for ALL objects of this class type
		String allObjectsWatchedFieldKey = RedisKeyUtils.objectWatchedFields(classIdPair.getClassName(), "0");

		String[] fieldKeys = {objectWatchedFieldKey, allObjectsWatchedFieldKey};
		
		for(int i=0; i<fieldKeys.length; i++) {
			String nextKey = fieldKeys[i];
			Set<String> objectWatcherValueKeys = cacheDataStore.getHashKeys(nextKey);
			Iterator<String> itrObjectWatcherValueKeys = objectWatcherValueKeys.iterator();
			while (itrObjectWatcherValueKeys.hasNext()) {
				String nextObjectWatcherValueKey = itrObjectWatcherValueKeys.next();
	
				// Get all fields for this object that are being watched and remove them.
				String changeWatcherKey = (String) cacheDataStore.getHashValue(nextKey, nextObjectWatcherValueKey);
				fieldWatcherKeys.add(changeWatcherKey);
			}
		}
		
		return fieldWatcherKeys;
	}
	
	protected Long removeChangeWatcherForField(ClassIDPair classIdPair, String[] fieldNames, String[] params, String changeWatcherId) {
		Long result = Long.valueOf(0);
		Set<String> fieldWatcherKeys = new HashSet<String>();

		if (fieldNames == null || fieldNames.length == 0) {
			fieldWatcherKeys = getAllFieldWatchersForObject(classIdPair);
		}
		else {
			for(String fieldName : fieldNames) {
				if (fieldName.equals("*")) {
					fieldWatcherKeys.addAll(getAllFieldWatchersForObject(classIdPair));
				}
				else {
					String fieldWatcherKey = "";
					if (params != null)
						fieldWatcherKey = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcherWithParams(classIdPair.getClassName(), classIdPair.getID(), fieldName, params));
					else
						fieldWatcherKey = RedisKeyUtils.fieldWatcher(RedisKeyUtils.changeWatcher(classIdPair.getClassName(), classIdPair.getID(), fieldName));
					fieldWatcherKeys.add(fieldWatcherKey);
				}
			}
		}
		
		if (fieldWatcherKeys.size() > 0) {
			Iterator<String> itrFieldWatcherKeys = fieldWatcherKeys.iterator();
			while (itrFieldWatcherKeys.hasNext()) {
				String nextFieldWatcherKey = itrFieldWatcherKeys.next();
				Long countRemoved = cacheDataStore.removeSetValue(nextFieldWatcherKey, changeWatcherId);
				result += countRemoved;
			}
		}
		
		return result;
	}
	
	protected void setupRecalculateChangeWatcher(String changeWatcherId) {
		ChangeWatcherReporting.internalRequestsCounter++;
		if (useChangeWatcherQueue && pushSyncHelper != null) {
			pushSyncHelper.pushStringToRoute( (new StringBuilder(changeWatcherId).append(":TS:").append(System.currentTimeMillis())).toString(), changeWatcherRouteName);
		}
		else {
			recalculateChangeWatcher(changeWatcherId);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void recalculateChangeWatcher(String changeWatcherId) {
		try {
			// Check to see if a timestamp has been included.
			String[] changeWatcherTsArray = changeWatcherId.split(":TS:");
			Long requestTimestamp = System.currentTimeMillis();
			if (changeWatcherTsArray.length > 1) {
				requestTimestamp = Long.valueOf(changeWatcherTsArray[1]);
				changeWatcherId = changeWatcherTsArray[0];
			}
			
			// Need to break up
			String[] params = changeWatcherId.split(":");
			
			// 3rd var should be category.
			String category = params[2];
			
			// 4th var should be subCategory.
			String subCategory = params[3];
			
			// 5th var should be fieldName.
			String fieldName = params[4];
			
			String[] otherParams = null;
			if (params.length > 5) {
				otherParams = new String[params.length-5];
				for(int i=5; i < params.length; i++) {
					otherParams[i-5] = URLDecoder.decode(params[i], "UTF-8");
				}
			}
			
			if (changeWatcherHelperFactory != null)
			{
				Collection <String> clientIds = (Set<String>) cacheDataStore.getSetValue(RedisKeyUtils.clientWatcher(changeWatcherId));	// The list of ClientID's who are interested in this object.
				
				IChangeWatcherHelper cwh = changeWatcherHelperFactory.getHelper(category);
				cwh.reprocess(category, subCategory, fieldName, clientIds, otherParams, requestTimestamp);

				/**
				// If no clients interested in this value, then remove it from the cache.
				if (clientIds == null || clientIds.isEmpty()) {
					removeChangeWatcherResult(pair, fieldName, otherParams);
					updateWatcherFields(pair, fieldName, fieldsToWatch, params);

					if (watchedFields != null) {
						Iterator<Object> itrOldWatchedFields = watchedFields.iterator();
						
						while (itrOldWatchedFields.hasNext()) {
							String nextOldField = (String) itrOldWatchedFields.next();
							redisDataStore.removeSetValue(watcherField, nextOldField);
							
							// Also remove this ChangeWatcher from the FieldWatcher.
							redisDataStore.removeSetValue(nextOldField, changeWatcherId);
						}
					}
				}**/
			}
			
		} catch(Exception e) {
			log.error("Error recalculating Change Watcher: " + changeWatcherId, e);
		}
	}
	
	public class ChangeWatcherTree {
		
		public ChangeWatcherTree() {
			this.changeWatcherMap = new TreeMap<String, List<String>>();
			this.circularKeys = new TreeMap<String, List<String>>();
		}
		
		private SortedMap<String, List<String>> changeWatcherMap;
		
		public SortedMap<String, List<String>> getChangeWatcherMap() {
			return changeWatcherMap;
		}

		public void setChangeWatcherMap(SortedMap<String, List<String>> changeWatcherMap) {
			this.changeWatcherMap = changeWatcherMap;
		}
		
		private SortedMap<String, List<String>> circularKeys;

		public SortedMap<String, List<String>> getCircularKeys() {
			return circularKeys;
		}

		public void setCircularKeys(SortedMap<String, List<String>> circularKeys) {
			this.circularKeys = circularKeys;
		}
	}
	
	/**
	 * 
	private static Boolean isWalkingChangeWatcherTree = false;
	@Scheduled(fixedDelay=1000)
	public void walkChangeWatcherTree() throws ClassNotFoundException {
		if (isWalkingChangeWatcherTree) {
			return;
		}
		else {
			isWalkingChangeWatcherTree = true;
		}
		
		// Get all Change Watcher Keys
		String fieldWatcherKey = RedisKeyUtils.fieldWatcher("*");
		Collection<String> fieldWatcherKeys = new HashSet<String>();
		fieldWatcherKeys.addAll(redisDataStore.keys(fieldWatcherKey));
		Map<String, LinkedHashSet<String>> parentFieldWatcherMap = new HashMap<String, LinkedHashSet<String>>();
		ChangeWatcherTree cwTree = new ChangeWatcherTree();
		getChangeWatchersTree(cwTree, fieldWatcherKey, null, parentFieldWatcherMap);
		
		for(String nextKey : parentFieldWatcherMap.keySet()) {
			fillInParentChangeWatchers(nextKey, parentFieldWatcherMap);
		}
		
		Collection<Object> changeWatchers = null;
		if (fieldWatcherKeys.size() > 0) {
			changeWatchers = redisDataStore.getSetUnion(null, fieldWatcherKeys);
		}
		
		Map<Class, Map<String, Object>> watchersMap = new HashMap<Class, Map<String, Object>>();
		
		for(Object nextChangeWatcher : changeWatchers) {
			
			// Should be of the form: cw:cf:com.psiglobal.mo.LongHaul:9d23fbaa-cf9a-49a0-becb-e3bc4e458282:vessels:...
			// Break down into parts
			String strChangeWatcher = (String) nextChangeWatcher;
			String[] parts = strChangeWatcher.split(":");
			if (parts.length >= 5) {
				String className = parts[2];
				Class clazz = Class.forName(className);
				String classId = parts[3];
				String fieldName = parts[4];
				
				
				
				Map<String, Object> classMap = watchersMap.get(clazz);
				if (classMap == null) {
					classMap = new HashMap<String, Object>();
					watchersMap.put(clazz, classMap);
				}
				
				
			}
			System.out.println("Next Change Watcher");
		}
		
		isWalkingChangeWatcherTree = false;
	}
	
	public void fillInParentChangeWatchers(String nextKey, Map<String, LinkedHashSet<String>> parentFieldWatcherMap) {
		LinkedHashSet<String> nextSet = parentFieldWatcherMap.get(nextKey);
		Set<String> itemsToAdd = new HashSet<String>();
		for(String nextSetKey : nextSet) {
			fillInParentChangeWatchers(nextSetKey, parentFieldWatcherMap);
			LinkedHashSet<String> nextSetSet = parentFieldWatcherMap.get(nextSetKey);
			itemsToAdd.addAll(nextSetSet);
			
			if (nextSetSet.contains(nextKey)) {
				System.out.println("Houston, we have a problem: " + nextKey);
				String strProblemPath = "";
				for(String nextProblemKey : nextSetSet) {
					strProblemPath += ", " + nextProblemKey;
				}
				System.out.println(strProblemPath);
			}
			
//			if (nextSetSet != null) {
//				for(String nextSetSetKey : nextSetSet) {
//					if (nextSetSetKey.equalsIgnoreCase(nextKey)) {
//						System.out.println("Houston, we have a problem...");
//					}
//					else {
//						nextSet.add(nextSetSetKey);
//					}
//				}
//			}
		}
		
		nextSet.addAll(itemsToAdd);
	}
	**/
}

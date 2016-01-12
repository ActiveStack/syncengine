package com.percero.agents.sync.access;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.Client;

/**
 * Manages client registration and activation throughout the system. The scope of this client 
 * is across one server (inasmuch as a client connects to a "server"). A Client is a device that
 * a valid user uses to connect to the system. Data is sent to a client, which therefore means
 * data is sent to the user.
 * 
 * Clients may be PERSISTENT or NON-PERSISTENT. Persistent clients store a copy of the data locally
 * and that data persists beyond the application life-span. Non-persistent clients do NOT store data
 * locally and therefore need to retrieve all data used from the server for each application 
 * life-span.
 * 
 * @author Collin Brown
 *
 */
public interface IAccessManager {
	
	/**
	 * Creates the client/user and sets them to inactive (logged out).
	 * 
	 * @param clientId
	 * @param userId
	 * @param deviceType
	 * @return
	 * @throws Exception
	 */
	public void createClient(String clientId, String userId, String deviceType, String deviceId) throws Exception;

	/**
	 * Searches for the client/user combo.
	 * 
	 * @param clientId
	 * @param userId
	 * @param deviceType
	 * @return
	 * @throws Exception
	 */
	public Boolean validateClientByClientId(String clientId) throws Exception;
	public Boolean validateClientByClientId(String clientId, Boolean setClientTimeouts) throws Exception;
	public String getClientUserId(String clientId);
	public Boolean findClientByClientIdUserId(String clientId, String userId) throws Exception;
	public Set<String> findClientByUserIdDeviceId(String deviceId, String userId) 
			throws Exception;
	public Boolean findClientByClientId(String clientId) throws Exception;

	/**
	 * Remove a client.  Typically called when a client logs out or the client device times out.
	 * 
	 * @param clientId
	 */
	public void destroyClient(String clientId);
	
	/**
	 * Checks to see if the client is a non-persistent client.
	 * 
	 * @return
	 */
	public Boolean isNonPersistentClient(String clientId);

	/**
	 * Searches for the client/user combination and sets the user as active (logged in). 
	 * 
	 * If createIfNotExist is true, then creates the client/user if they do not exist.  Otherwise, if the client/user
	 * combination is not found, an exception is thrown.
	 * 
	 * @param clientId
	 * @return {@link Client}
	 * @throws Exception
	 */
	public void registerClient(String clientId, String userId, String deviceId) throws Exception;
	public void registerClient(String clientId, String userId, String deviceId, String deviceType) throws Exception;
	
	/**
	 * Makes the client inactive (logged out) regardless of the client type. Hibernated clients should
	 * still be considered for data updates that the client is interested, however these data updates
	 * will not be attempted to be pushed to the client until it has woken up (i.e. logged back in).
	 * 
	 * @param clientId
	 * @param userId
	 * @return {@link Boolean}
	 * @throws Exception
	 */
	public Boolean hibernateClient(String clientId, String userId) throws Exception;

	/**
	 * Upgrades the client to the specified device Type.  Typically used post-authentication
	 * by the SyncAgent on the client.
	 * 
	 * @param clientId
	 * @param deviceId
	 * @param deviceType
	 * @param userId
	 * @return {@link Boolean}
	 * @throws Exception
	 */
	public Boolean upgradeClient(String clientId, String deviceId, String deviceType, String userId) throws Exception;

	/**
	 * Renames a client. This is most useful for handles client connectivity going in/out. Whenever a client
	 * reconnects they get a new client ID from the gateway. Renaming a client allows the Percero framework
	 * to recognize the newly assigned client ID as belonging to the original client.
	 * 
	 * @param thePreviousClient
	 * @param clientId
	 */
	public void renameClient(String thePreviousClient, String clientId);
	
	/**
	 * Makes the client inactive (logged out). If the client is PERSISTENT, this will flag the client
	 * as inactive (logged out). If the client is NON-PERSISTENT, this will remove the client altogether.
	 * 
	 * @param clientId
	 * @return
	 * @throws Exception
	 */
	public void logoutClient(String clientId, Boolean pleaseDestroyClient) throws Exception;
	
	public void saveUpdateJournalClients(ClassIDPair pair, Collection<String> clientIds, Boolean guaranteeDelivery, String pusherClient, Boolean sendToPusher) throws Exception;
	public Long deleteUpdateJournal(String clientId, String className, String classId);
	public void deleteUpdateJournals(String clientId, ClassIDPair[] objects);
	
	public void saveDeleteJournalClients(ClassIDPair pair, Collection<String> clientIds, Boolean guaranteeDelivery, String pusherClient, Boolean sendToPusher) throws Exception;
	public Long deleteDeleteJournal(String clientId, String className, String classId);
	public void deleteDeleteJournals(String clientId, ClassIDPair[] objects);
	
	/**
	 * Logs a record that the user has accessed the object. This is used after a record is PUT, sending
	 * a copy of the newly PUT object out to each User (via corresponding Client(s)).
	 * 
	 * @param classIdPair
	 * @param userId
	 * @return
	 * @throws Exception
	 */
	public boolean saveAccessJournal(ClassIDPair classIdPair, String userId, String clientId) throws Exception;
	public boolean saveAccessJournal(List<ClassIDPair> classIdPairs, String userId, String clientId) throws Exception;
	
	/**
	 * Returns a list of all clients (as AccessJournal objects) who have accessed the object.
	 * 
	 * @param className
	 * @param classId
	 * @return
	 * @throws Exception
	 */
	public List<String> getObjectAccessJournals(String className, String classId) throws Exception;
	Set<String> getClassAccessJournalIDs(String className);
    long getNumClientsInterestedInWholeClass(String className);

	public void removeAccessJournalsByObject(ClassIDPair classIdPair);
	
	public void removeObjectModJournalsByObject(ClassIDPair classIdPair);
	public void removeHistoricalObjectsByObject(ClassIDPair classIdPair);
	
	/**
	 * Returns a list of all UpdateJournal objects for the client. This is used when a client is
	 * requesting all updated objects that it cares about.
	 * 
	 * @param clientId
	 * @param loggedInOnly
	 * @return
	 * @throws Exception
	 */
	public Collection<String> getClientUpdateJournals(String clientId, Boolean loggedInOnly) throws Exception;
	
	/**
	 * Returns a list of all DeleteJournal objects for the client. This is used when a client is
	 * requesting all deleted objects that it cares about.
	 * 
	 * @param clientId
	 * @return
	 * @throws Exception
	 */
	public Collection<String> getClientDeleteJournals(String clientId, Boolean loggedInOnly) throws Exception;
	
	/**
	 * Removes all UpdateJournals related to the Object.  This is used when removing an object.
	 * 
	 * @param object
	 * @return
	 * @throws Exception
	 */
	public void removeUpdateJournalsByObject(ClassIDPair classIdPair) throws Exception;
	
	
	
	////////////////////////////////////////////////////////
	//	CHANGE WATCHER
	////////////////////////////////////////////////////////
	public void addWatcherField(ClassIDPair classIdPair, String fieldName, Collection<String> collection);
	public void addWatcherField(ClassIDPair classIdPair, String fieldName, Collection<String> collection, String[] params);
	public void addWatcherClient(ClassIDPair classIdPair, String fieldName, String clientId);
	public void addWatcherClient(ClassIDPair classIdPair, String fieldName, String clientId, String[] params);
	public void updateWatcherFields(ClassIDPair classIdPair, String fieldName, Collection<String> fieldsToWatch);
	public void updateWatcherFields(ClassIDPair classIdPair, String fieldName, Collection<String> fieldsToWatch, String[] params);
	public void saveChangeWatcherResult(ClassIDPair classIdPair, String fieldName, Object result);
	public void saveChangeWatcherResult(ClassIDPair classIdPair, String fieldName, Object result, String[] params);
	public Boolean getChangeWatcherResultExists(ClassIDPair classIdPair, String fieldName);
	public Boolean getChangeWatcherResultExists(ClassIDPair classIdPair, String fieldName, String[] params);
	public Long getChangeWatcherResultTimestamp(ClassIDPair classIdPair, String fieldName, String[] params);
	public Object getChangeWatcherResult(ClassIDPair classIdPair, String fieldName);
	public Object getChangeWatcherResult(ClassIDPair classIdPair, String fieldName, String[] params);
	public void checkChangeWatchers(ClassIDPair classIdPair, String[] fieldNames, String[] params);
	public void removeChangeWatchersByObject(ClassIDPair classIdPair);
	public void recalculateChangeWatcher(String changeWatcherId);
	
	/**
	 * Takes in a Collection of ClientIDs and returns the sub-set that are valid Clients.
	 * 
	 * @param clientIds
	 * @return
	 * @throws Exception
	 */
	public Set<String> validateClients(Collection<String> clientIds) throws Exception;

	/**
	 * Takes in a Map of ClientIDs and DeviceIDs and returns the sub-set that are valid Clients.
	 * 
	 * @param clientDevices
	 * @return
	 * @throws Exception
	 */
	public Set<String> validateClientsIncludeFromDeviceHistory(Map<String, String> clientDevices) throws Exception;
	
	public String validateAndRetrieveCurrentClientId(String clientId, String deviceId);

}

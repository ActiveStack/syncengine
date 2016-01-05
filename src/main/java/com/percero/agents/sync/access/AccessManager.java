package com.percero.agents.sync.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Example;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.hibernate.AssociationExample;
import com.percero.agents.sync.hibernate.BaseDataObjectPropertySelector;
import com.percero.agents.sync.hibernate.SyncHibernateUtils;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.vo.AccessJournal;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.Client;
import com.percero.agents.sync.vo.DeleteJournal;
import com.percero.agents.sync.vo.UpdateJournal;
import com.percero.framework.vo.IPerceroObject;

@Component
public class AccessManager implements IAccessManager {
	
	private static Logger log = Logger.getLogger(AccessManager.class);

	// TODO: Remove all AccessJournal objects for a User after they have been inactive for some amount of time.
	//	What should that amount of time be?...
	
	public AccessManager() {
	}
	
	public void postClients() {
	}

	@Autowired
	SessionFactory sessionFactorySync;
	public void setSessionFactorySync(SessionFactory value) {
		sessionFactorySync = value;
	}
	
	@Autowired
	SessionFactory appSessionFactory;
	public void setAppSessionFactory(SessionFactory value) {
		appSessionFactory = value;
	}
	
	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#createClient(java.lang.String, java.lang.Integer, java.lang.String)
	 */
	public void createClient(String clientId, String userId, String deviceType, String deviceId) throws Exception {
		Session s = sessionFactorySync.openSession();
		
		try {
			Client theClient = new Client();
			theClient.setClientId(clientId);
			theClient.setDeviceId(deviceId);
			theClient.setBelongsToUserId(userId);
			theClient.setDateCreated(new Date());
			theClient.setDeviceType(deviceType);
			theClient.setIsLoggedIn(false);
			Transaction tx = s.beginTransaction();
			tx.begin();
			s.save(theClient);
			tx.commit();
		} catch(Exception e) {
			log.error("Error creating client " + clientId + ", user " + userId.toString(), e);
		} finally {
			s.close();
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Client> getLoggedInClients() {
		List<Client> result = null;
		Session s = sessionFactorySync.openSession();
		
		try {
			Client theClient = new Client();
			theClient.setIsLoggedIn(true);

			Criteria criteriaClient = s.createCriteria(Client.class);
			Example exampleClient = Example.create(theClient);
			criteriaClient.add(exampleClient);
			return (List<Client>) criteriaClient.list();
		} catch(Exception e) {
			log.error("Error finding clients logged in", e);
			result = null;
		} finally {
			s.close();
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getAllUserIDs() {
		List<String> result = null;
		Session s = sessionFactorySync.openSession();
		
		try {
			Query query = s.createQuery("SELECT DISTINCT(belongsToUserId) FROM Client");
			return (List<String>) query.list();
		} catch(Exception e) {
			log.error("Error getting ALL User IDs", e);
			result = null;
		} finally {
			s.close();
		}
		
		return result;
	}
	
	// TODO: Implement getClientUserId
	public String getClientUserId(String clientId) {
		System.out.println("TODO: Implement getClientUserId");
		return null;
		//return (String) redisDataStore.getValue(RedisKeyUtils.client(clientId));
	}
	

	public Boolean findClientByClientIdUserId(String clientId, String userId) throws Exception {
		Boolean result = false;
		Session s = sessionFactorySync.openSession();
		
		if (StringUtils.hasText(clientId) && StringUtils.hasText(userId)) {
			try {
				Client theClient = new Client();
				theClient.setClientId(clientId);
				theClient.setBelongsToUserId(userId);
	
				Criteria criteriaClient = s.createCriteria(Client.class);
				Example exampleClient = Example.create(theClient);
				criteriaClient.add(exampleClient);
				return criteriaClient.uniqueResult() != null;
			} catch(Exception e) {
				log.error("Error finding client " + clientId + ", user " + userId, e);
				result = null;
			} finally {
				s.close();
			}
		}
		
		return result;
	}
	
	public Set<String> findClientByUserIdDeviceId(String deviceId, String userId) throws Exception {
		Set<String> result = new HashSet<String>();
		Session s = sessionFactorySync.openSession();
		
		if (StringUtils.hasText(deviceId) && StringUtils.hasText(userId)) {
			try {
				Client theClient = new Client();
				theClient.setDeviceId(deviceId);
				theClient.setBelongsToUserId(userId);
				
				Criteria criteriaClient = s.createCriteria(Client.class);
				Example exampleClient = Example.create(theClient);
				criteriaClient.add(exampleClient);
				String clientId = ((Client) criteriaClient.uniqueResult()).getClientId();
				result.add(clientId);
			} catch(Exception e) {
				log.error("Error finding client for device " + deviceId + ", user " + userId, e);
				result = null;
			} finally {
				s.close();
			}
		}
		
		return result;
	}
	
	public Boolean validateClientByClientId(String clientId) throws Exception {
		return validateClientByClientId(clientId, true);
	}
	public Boolean validateClientByClientId(String clientId, Boolean setClientTimeouts) throws Exception {
		return findClientByClientId(clientId);
	}
	public Boolean findClientByClientId(String clientId) throws Exception {
		Boolean result = false;
		
		if (StringUtils.hasText(clientId)) {
			Session s = sessionFactorySync.openSession();
			
			try {
				Client theClient = new Client();
				theClient.setClientId(clientId);
	
				Criteria criteriaClient = s.createCriteria(Client.class);
				Example exampleClient = Example.create(theClient);
				criteriaClient.add(exampleClient);
				result = criteriaClient.uniqueResult() != null;
			} catch(Exception e) {
				log.error("Error finding client " + clientId, e);
				result = null;
			} finally {
				s.close();
			}
		}
		
		return result;
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

		Session s = sessionFactorySync.openSession();
		
		try {
			Transaction tx = s.beginTransaction();
			tx.begin();
			Query q = s.createQuery("UPDATE Client c SET c.isLoggedIn=:isLoggedIn WHERE c.clientId=:clientId");
			q.setBoolean("isLoggedIn", true);
			q.setString("clientId", clientId);
			q.executeUpdate();
			tx.commit();
		} catch(Exception e) {
			log.error("Error registering client " + clientId + ", user " + userId.toString(), e);
		} finally {
			s.close();
		}
	}
	
	public Boolean hibernateClient(String clientId, String userId) throws Exception {
		Boolean result = false;
		Boolean isValidClient = findClientByClientIdUserId(clientId, userId);
		if (isValidClient) {

			Session s = sessionFactorySync.openSession();
			
			try {
				Transaction tx = s.beginTransaction();
				tx.begin();
				Query q = s.createQuery("UPDATE Client c SET c.isLoggedIn=:isLoggedIn, c.dateLastLogout=:lastLogout WHERE c.clientId=:clientId");
				q.setBoolean("isLoggedIn", false);
				q.setString("clientId", clientId);
				q.setDate("lastLogout", new Date());
				tx.commit();
				result = true;
			} catch(Exception e) {
				log.error("Error hibernating client " + clientId + ", user " + userId.toString(), e);
				result = false;
			} finally {
				s.close();
			}
		}
		
		return result;
	}
	
	// TODO: Implement
	public void renameClient(String thePreviousClient, String clientId) {
		System.out.println("TODO: Implement renameClient");
	}

	public Boolean upgradeClient(String clientId, String deviceId, String deviceType, String userId) throws Exception {
		Boolean result = false;
		Boolean theClientIsValid = findClientByClientIdUserId(clientId, userId);
		String thePreviousClientId = null;
		if (deviceId != null && deviceId.length() > 0) {
			Set<String> previousClientIds = findClientByUserIdDeviceId(userId, deviceId);
			if (previousClientIds != null && !previousClientIds.isEmpty()) {
				thePreviousClientId = previousClientIds.iterator().next();
			}
//			thePreviousClientId = previousClientIds.findClientByUserIdDeviceId(userId, deviceId);
		}
		
		if (!theClientIsValid && StringUtils.hasText(thePreviousClientId))
			clientId = thePreviousClientId;

		if (StringUtils.hasText(clientId)) {
			Session s = sessionFactorySync.openSession();
			
			try {
				if (StringUtils.hasText(thePreviousClientId)) {
					// If the previous client exists, then update previous client's clientId.
					Client thePreviousClient = (Client) SyncHibernateUtils.cleanObject(s.get(Client.class, thePreviousClientId), s);
					thePreviousClient.setClientId(clientId);
					thePreviousClient.setDeviceType(deviceType);
					thePreviousClient.setIsLoggedIn(true);

					Transaction tx = null;
					if (!clientId.equals(thePreviousClient.getClientId())) {
						tx = s.beginTransaction();
						tx.begin();
					
						// Delete the Client, it was apparently only temporary.
						// But, first update all UpdateJournals and DeleteJournals.
						String updateJournalSql = "UPDATE UpdateJournal SET client=:previousClient WHERE client.clientId=:clientId";
						Query updateQuery = s.createQuery(updateJournalSql);
						updateQuery.setEntity("previousClient", thePreviousClient);
						updateQuery.setString("clientId", clientId);
						updateQuery.executeUpdate();
						
						String deleteJournalSql = "UPDATE DeleteJournal SET client=:previousClient WHERE client.clientId=:clientId";
						updateQuery = s.createQuery(deleteJournalSql);
						updateQuery.setEntity("previousClient", thePreviousClient);
						updateQuery.setString("clientId", clientId);
						updateQuery.executeUpdate();
						
						// Now delete theClient.
						String deleteClient = "DELETE Client c WHERE c.clientId=:clientId";
						updateQuery = s.createQuery(deleteClient);
						updateQuery.setString("clientId", clientId);
						updateQuery.executeUpdate();;
						tx.commit();
					}

					// Now save previousClient (can't save before deleting theClient due to unique constraint on clientId).
					tx = s.beginTransaction();
					tx.begin();
					s.merge(thePreviousClient);
					tx.commit();

					result = true;
				}
				else {
					// If the Previous client is null, then upgrade the Client.
					Client theClient = (Client) SyncHibernateUtils.cleanObject(s.get(Client.class, clientId), s);
					theClient.setDeviceType(deviceType);
					Transaction tx = s.beginTransaction();
					tx.begin();
					s.merge(theClient);
					tx.commit();
					result = true;
				}

			} catch(Exception e) {
				log.error("Error upgrading client " + clientId + ", user " + userId.toString(), e);
				result = false;
			} finally {
				s.close();
			}
		}
		
		return result;
	}
	
	public Boolean saveClient(Client client) throws Exception {
		Boolean result = false;

		if (client != null) {
			Session s = sessionFactorySync.openSession();
			
			try {
				Transaction tx = null;
				tx = s.beginTransaction();
				tx.begin();
				s.merge(client);
				tx.commit();
				
				result = true;
			} catch(Exception e) {
				log.error("Error saving client " + client.getID(), e);
				result = false;
			} finally {
				s.close();
			}
		}
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#logoutClient(java.lang.String)
	 */
	public void logoutClient(String clientId, Boolean pleaseDestroyClient) throws Exception {
		Session s = sessionFactorySync.openSession();
		
		try {
			Client foundClient = null;
			Query q = s.createQuery("FROM Client c WHERE c.clientId=:clientId");
			q.setString("clientId", clientId);
			foundClient = (Client) q.uniqueResult();

			//Client queryClient = new Client();
			//queryClient.setClientId(clientId);
			//Criteria criteriaClient = s.createCriteria(Client.class);
			//Example exampleClient = Example.create(queryClient);
			//criteriaClient.add(exampleClient);
			//foundClient = (Client) criteriaClient.uniqueResult();
			
			if (foundClient != null) {
				if (foundClient.getDeviceType() != null && foundClient.getDeviceType().equalsIgnoreCase(Client.PERSISTENT_TYPE)) {
					// Persistent client, so set lastLogout date and isLoggedIn.
					foundClient.setDateLastLogout(new Date());
					foundClient.setIsLoggedIn(false);
					foundClient.setClientId(null);
					Transaction tx = s.beginTransaction();
					tx.begin();
					s.merge(foundClient);
					tx.commit();
				} else {
					// Default is some sort of Non-Persistent client, so delete Client and ALL UpdateJournals.
					Transaction tx = s.beginTransaction();
					tx.begin();
					String deleteTransJournalSql = "DELETE FROM TransactionJournal WHERE client=:client";
					Query deleteQuery = s.createQuery(deleteTransJournalSql);
					deleteQuery.setEntity("client", foundClient);
					deleteQuery.executeUpdate();
					
					String deleteUpdateJournalSql = "DELETE FROM UpdateJournal WHERE client=:client";
					deleteQuery = s.createQuery(deleteUpdateJournalSql);
					deleteQuery.setEntity("client", foundClient);
					deleteQuery.executeUpdate();
					
					String deleteDeleteJournalSql = "DELETE FROM DeleteJournal WHERE client=:client";
					deleteQuery = s.createQuery(deleteDeleteJournalSql);
					deleteQuery.setEntity("client", foundClient);
					deleteQuery.executeUpdate();
					
					// Also delete client from Client table.
					s.delete(foundClient);
					tx.commit();
				}
			}
		} catch(Exception e) {
			log.error("Error logging out client " + clientId, e);
		} finally {
			s.close();
		}
	}
	
	// TODO: Implement getLoggedInUsers
	public Collection<String> getLoggedInUsers() {
		System.out.println("TODO: Implement getLoggedInUsers");
		return null;
	}
	
	// TODO: Implement 
	public void removeAccessJournalsByObject(ClassIDPair classIdPair) {
		System.out.println("TODO: Implement removeAccessJournalsByObject");
		//redisDataStore.deleteKey(RedisKeyUtils.accessJournal(className, classId));
	}

	// TODO: Implement 
	public void removeUpdateJournalsByObject(ClassIDPair classIdPair) throws Exception {
		System.out.println("TODO: Implement removeUpdateJournalsByObject");
		//redisDataStore.removeKeysValue(RedisKeyUtils.updateJournal("*"), RedisKeyUtils.classIdPair(className, classId));
	}

	// TODO: Implement 
	public void removeObjectModJournalsByObject(ClassIDPair classIdPair) {
		System.out.println("TODO: Implement removeObjectModJournalsByObject");
		//redisDataStore.deleteKey(RedisKeyUtils.objectModJournal(className, classId));
	}
	
	// TODO: Implement 
	public void removeHistoricalObjectsByObject(ClassIDPair classIdPair) {
		System.out.println("TODO: Implement removeHistoricalObjectsByObject");
		//redisDataStore.deleteKey(RedisKeyUtils.historicalObject(className, classId));
	}

	// TODO: Implement isNonPersistentClient
	public Boolean isNonPersistentClient(String clientId) {
		System.out.println("TODO: Implement isNonPersistentClient");
		return false;
		//return redisDataStore.getSetIsMember(RedisKeyUtils.clientsNonPersistent(), clientId);
	}
	
	// TODO: Implement deleteUpdateJournal
	public Long deleteUpdateJournal(String clientId, String className, String classId) {
		System.out.println("TODO: deleteUpdateJournal");
		return null;
		//return redisDataStore.removeSetValue(RedisKeyUtils.updateJournal(clientId), RedisKeyUtils.classIdPair(className, classId));
	}
	
	// TODO: deleteUpdateJournals
	public void deleteUpdateJournals(String clientId, ClassIDPair[] objects) {
		System.out.println("TODO: deleteUpdateJournals");
		//for(ClassIDPair nextObject : objects) {
		//	redisDataStore.removeSetValue(RedisKeyUtils.updateJournal(clientId), RedisKeyUtils.classIdPair(nextObject.getClass().getCanonicalName(), nextObject.getID()));
		//}
	}
	
	// TODO: Implement deleteDeleteJournal
	public Long deleteDeleteJournal(String clientId, String className, String classId) {
		System.out.println("TODO: deleteDeleteJournal");
		return null;
		//return redisDataStore.removeSetValue(RedisKeyUtils.updateJournal(clientId), RedisKeyUtils.classIdPair(className, classId));
	}
	
	// TODO: deleteDeleteJournals
	public void deleteDeleteJournals(String clientId, ClassIDPair[] objects) {
		System.out.println("TODO: deleteDeleteJournals");
		//for(ClassIDPair nextObject : objects) {
		//	redisDataStore.removeSetValue(RedisKeyUtils.updateJournal(clientId), RedisKeyUtils.classIdPair(nextObject.getClass().getCanonicalName(), nextObject.getID()));
		//}
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Collection<Object> saveUpdateJournal(ClassIDPair pair, Collection<String> listUserIds, Boolean guaranteeDelivery, String pusherClientId, Boolean sendToPusher) throws Exception {
		Collection<Object> result = new ArrayList<Object>();
		
		if (listUserIds != null && listUserIds.size() > 0) {
			Session s = sessionFactorySync.openSession();
			
			try {
				Date currentDate = new Date();

				// TODO: Since non-persistent, logged out clients are removed from the Client table, shouldn't
				//	this function simply query based on belongsToUserId and not need the additional
				//	(c.isLoggedIn OR s.deviceType) query?
				String queryClientCountSql = "SELECT COUNT(c) AS clientCount FROM Client c WHERE c.belongsToUserId IN (:userIds)";
				Query clientCountQuery = s.createQuery(queryClientCountSql);
				clientCountQuery.setParameterList("userIds", listUserIds);

				Long countClientsResult = (Long)clientCountQuery.uniqueResult();
				
				if (((Long)countClientsResult).longValue() > 0) {
					
					List<UpdateJournal> clientUpdateJournalList = null;
					if (guaranteeDelivery) {
						String updateJournalSelectClientsSql = "SELECT u FROM UpdateJournal u WHERE u.client.ID IN (SELECT c.ID FROM Client c WHERE c.belongsToUserId IN (:userIds)) AND "
								+ "u.classID=:classID AND u.className=:className";
						Query updateJournalSelectClientsQuery = s.createQuery(updateJournalSelectClientsSql);
						updateJournalSelectClientsQuery.setParameterList("userIds", listUserIds);
						updateJournalSelectClientsQuery.setString("classID", pair.getID());
						updateJournalSelectClientsQuery.setString("className", pair.getClassName());
						clientUpdateJournalList = updateJournalSelectClientsQuery.list();
					} else {
						clientUpdateJournalList = new ArrayList<UpdateJournal>();
					}
	
					if (clientUpdateJournalList != null && clientUpdateJournalList.size() > 0) {
						// Update the dateModified in a batch Update command for better performance.
						String updateUpdateJournalSql = "UPDATE UpdateJournal u SET u.dateModified=:dateModified " +
								"WHERE u.client.ID IN (SELECT c.ID FROM Client c WHERE c.belongsToUserId IN (:userIds)) AND "
							+ "u.classID=:classID AND u.className=:className";
						Query updateUpdateJournalQuery = s.createQuery(updateUpdateJournalSql);
						updateUpdateJournalQuery.setDate("dateModified", currentDate);
						updateUpdateJournalQuery.setParameterList("userIds", listUserIds);
						updateUpdateJournalQuery.setString("classID", pair.getID());
						updateUpdateJournalQuery.setString("className", pair.getClassName());
						updateUpdateJournalQuery.executeUpdate();
	
						for(UpdateJournal nextUpdateJournal : clientUpdateJournalList) {
							// Evict the record first so the update does not trigger a database update.
							//	Note: The update has already been done in the batch (see comment above).
							s.evict(nextUpdateJournal);
							nextUpdateJournal.setDateModified(currentDate);
							result.add(nextUpdateJournal);
						}
					}
					
					String queryClientSql = "SELECT c FROM Client c WHERE c.belongsToUserId IN (:userIds) AND c.ID NOT IN (SELECT u.client.ID FROM UpdateJournal u " +
							"WHERE u.classID=:classID AND u.className=:className)";
					Query clientQuery = s.createQuery(queryClientSql);
					clientQuery.setParameterList("userIds", listUserIds);
					clientQuery.setString("classID", pair.getID());
					clientQuery.setString("className", pair.getClassName());
	
					List<Client> listClients = clientQuery.list();
					
					Transaction tx = s.beginTransaction();
					
					for(Client nextClient : listClients) {
						if (!sendToPusher && StringUtils.hasText(pusherClientId)) {
							// Don't send to pushing Client
							if (nextClient.getID().equals(pusherClientId)) {
								continue;
							}
						}
						
						UpdateJournal updateJournal = new UpdateJournal();
						// TODO: Use a class id map instead of class name for faster lookup.
						//	Use hash of full class name to generate ID?
						updateJournal.setDateCreated(currentDate);
						updateJournal.setDateModified(currentDate);
						updateJournal.setClassID(pair.getID());
						updateJournal.setClassName(pair.getClassName());
						updateJournal.setClient(nextClient);
						
						if (guaranteeDelivery || (nextClient.getDeviceType() != null && nextClient.getDeviceType().equalsIgnoreCase(Client.PERSISTENT_TYPE))) {
							try {
								s.saveOrUpdate(updateJournal);
							} catch(Exception e) {
								log.warn("Error saving UpdateJournal: " + updateJournal.toString(), e);
							}
						}
						
						result.add(updateJournal);
					}
					
					tx.commit();
					
					result = (List) SyncHibernateUtils.cleanObject(result, s);
				}
			} catch(Exception e) {
				log.error("Error saving UpdateJournal for " + pair.toString(), e);
			} finally {
				if (s != null && s.isOpen())
					s.close();
			}
		}
		
		return result;
	}
	
	public void saveUpdateJournalClients(ClassIDPair pair, Collection<String> listClients, Boolean guaranteeDelivery, String pusherClientId, Boolean sendToPusher) throws Exception {
		if (listClients != null && listClients.size() > 0) {
			Session s = sessionFactorySync.openSession();
			
			try {
				Date currentDate = new Date();
				
				Query selectUpdateJournal = s.createQuery("FROM UpdateJournal u WHERE u.client=:client AND "
						+ "u.classID=:classID AND u.className=:className");
				selectUpdateJournal.setString("classID", pair.getID());
				selectUpdateJournal.setString("className", pair.getClassName());

				Transaction tx = s.beginTransaction();

				int count = 0;
				for(String nextClientId : listClients) {
					if (!sendToPusher && StringUtils.hasText(pusherClientId)) {
						// Don't send to pushing Client
						if (nextClientId.equals(pusherClientId)) {
							continue;
						}
					}
					
					Client nextClient = (Client) SyncHibernateUtils.loadObject(s.get(Client.class, nextClientId));

					selectUpdateJournal.setParameter("client", nextClient);
					
					UpdateJournal uj = (UpdateJournal) selectUpdateJournal.uniqueResult();
					
					if (uj != null)
					{
						uj.setDateModified(currentDate);
						s.update(uj);
						s.evict(uj);
						uj.setClient(nextClient);	// Make sure Client is fully loaded.
					}
					else
					{
						uj = new UpdateJournal();
						// TODO: Use a class id map instead of class name for faster lookup.
						//	Use hash of full class name to generate ID?
						uj.setDateCreated(currentDate);
						uj.setDateModified(currentDate);
						uj.setClassID(pair.getID());
						uj.setClassName(pair.getClassName());
						uj.setClient(nextClient);
						
						if (guaranteeDelivery || (nextClient.getDeviceType() != null && nextClient.getDeviceType().equalsIgnoreCase(Client.PERSISTENT_TYPE)))
							s.save(uj);
						
					}
					
					if (count > 0 && count % 1000 == 0) {
						s.flush();
						s.clear();
					}
					count++;
				}
				
				tx.commit();

			} catch(Exception e) {
				log.error("Error saving UpdateJournal for " + pair.toString(), e);
			} finally {
				s.close();
			}
		}
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Collection<Object> saveDeleteJournal(ClassIDPair pair, Collection<String> listUserIds, Boolean guaranteeDelivery, String pusherClient, Boolean sendToPusher) throws Exception {
		Collection<Object> result = new HashSet<Object>();
		
		if (listUserIds != null && listUserIds.size() > 0) {
			Session s = sessionFactorySync.openSession();
			
			try {
				Date currentDate = new Date();

				// TODO: Since non-persistent, logged out clients are removed from the Client table, shouldn't
				//	this function simply query based on belongsToUserId and not need the additional
				//	(c.isLoggedIn OR s.deviceType) query?
				String queryClientSql = "SELECT c FROM Client c WHERE c.belongsToUserId IN (:userIds) AND "
					+ "(c.isLoggedIn=:isLoggedIn OR c.deviceType=:deviceType)";
				Query clientQuery = s.createQuery(queryClientSql);
				clientQuery.setParameterList("userIds", listUserIds);
				clientQuery.setBoolean("isLoggedIn", true);
				clientQuery.setString("deviceType", Client.PERSISTENT_TYPE);

				List<Client> listClients = clientQuery.list();

				if (listClients != null && listClients.size() > 0) {
					List<DeleteJournal> clientDeleteJournalList = null;
					
					if (guaranteeDelivery) {
						String deleteJournalSelectClientsSql = "SELECT d FROM DeleteJournal d WHERE d.client IN (:clients) AND "
							+ "d.classID=:classID AND d.className=:className";
						Query deleteJournalSelectClientsQuery = s.createQuery(deleteJournalSelectClientsSql);
						deleteJournalSelectClientsQuery.setParameterList("clients", listClients);
						deleteJournalSelectClientsQuery.setString("classID", pair.getID());
						deleteJournalSelectClientsQuery.setString("className", pair.getClassName());
						
						clientDeleteJournalList = deleteJournalSelectClientsQuery.list();
					} else {
						clientDeleteJournalList = new ArrayList<DeleteJournal>();
					}
	
					Transaction tx = s.beginTransaction();
					tx.begin();
					if (clientDeleteJournalList != null && clientDeleteJournalList.size() > 0) {
						// Update the dateModified in a batch Delete command for better performance.
						String deleteUpdateJournalSql = "UPDATE DeleteJournal d SET d.dateModified=:dateModified WHERE d IN (:deleteJournals)";
						Query deleteUpdateJournalQuery = s.createQuery(deleteUpdateJournalSql);
						deleteUpdateJournalQuery.setDate("dateModified", currentDate);
						deleteUpdateJournalQuery.setParameterList("deleteJournals", clientDeleteJournalList);
						deleteUpdateJournalQuery.executeUpdate();
	
						for(DeleteJournal nextDeleteJournal : clientDeleteJournalList) {
							// Evict the record first so the update does not trigger a database update.
							//	Note: The delete has already been done in the batch (see comment above).
							s.evict(nextDeleteJournal);
							nextDeleteJournal.setDateModified(currentDate);
							
							result.add(nextDeleteJournal.getClient().getClientId());

							if ( listClients.contains(nextDeleteJournal.getClient()) ) {
								listClients.remove(nextDeleteJournal.getClient());
							}
						}
					}
					
					for(Client nextClient : listClients) {
						// Only store updates for LoggedIn and/or Persistent Clients.
						if (nextClient.getIsLoggedIn() || (nextClient.getDeviceType() != null && nextClient.getDeviceType().equalsIgnoreCase(Client.PERSISTENT_TYPE))) {
							if (!sendToPusher && pusherClient != null) {
								// Don't send to pushing Client
								if (nextClient.getID().equals(pusherClient)) {
									continue;
								}
							}
							
							DeleteJournal deleteJournal = new DeleteJournal();
							// TODO: Use a class id map instead of class name for faster lookup.
							//	Use hash of full class name to generate ID?
							deleteJournal.setDateCreated(currentDate);
							deleteJournal.setDateModified(currentDate);
							deleteJournal.setClassID(pair.getID());
							deleteJournal.setClassName(pair.getClassName());
							deleteJournal.setClient(nextClient);
							
							if (guaranteeDelivery || (nextClient.getDeviceType() != null && nextClient.getDeviceType().equalsIgnoreCase(Client.PERSISTENT_TYPE)))
								s.save(deleteJournal);
							
							result.add(nextClient.getClientId());
						}
					}
					tx.commit();
					
					result = (List) SyncHibernateUtils.cleanObject(result, s);
				}
			} catch(Exception e) {
				log.error("Error saving DeleteJournal for " + pair.toString(), e);
			} finally {
				s.close();
			}
		}
		
		return result;
	}
	
	public boolean saveAccessJournal(List<ClassIDPair> classIdPairs, String userId) throws Exception {
		
		boolean saveAccessJournalFailure = false;
		for(ClassIDPair nextObject : classIdPairs) {
			if (!saveAccessJournal(nextObject, userId))
				saveAccessJournalFailure = true;
		}
		
		return !saveAccessJournalFailure;
	}
			
	@SuppressWarnings("rawtypes")
	public boolean saveAccessJournal(ClassIDPair pair, String userId) throws Exception {
		
		Session s = sessionFactorySync.openSession();
		
		try {
			AccessJournal accessJournal = new AccessJournal();
			// TODO: Use a class id map instead of class name for faster lookup.
			//	Use hash of full class name to generate ID?
			accessJournal.setClassName(pair.getClassName());
			accessJournal.setClassID(pair.getID());
			accessJournal.setUserID(userId);
			
			Criteria criteria = s.createCriteria(AccessJournal.class);
			AssociationExample example = AssociationExample.create(accessJournal);
			BaseDataObjectPropertySelector propertySelector = new BaseDataObjectPropertySelector(null);
			example.setPropertySelector(propertySelector);
			criteria.add(example);

			List result = criteria.list();
			
			if (result == null || result.isEmpty()) {
				// Attempt direct insert. Don't care if duplicate index error. Just care that record is in database.
				Transaction tx = s.beginTransaction();
				tx.begin();
				s.save(accessJournal);
				tx.commit();
			}
			
			return true;
		} catch(Exception e) {
			// Unable to INSERT AccessJournal. Must already exist.
			return true;
		} finally {
			s.close();
		}
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#getObjectAccessJournals(java.lang.String, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	public List<String> getObjectAccessJournals(String className, String classId) throws Exception {
		List<String> result = new ArrayList<String>();
		Session s = sessionFactorySync.openSession();
		
		try {
			// TODO: Use a class id map instead of class name for faster lookup.
			//	Use hash of full class name to generate ID?
			String selectAccessJournalSql = "SELECT DISTINCT(aj.userID) FROM AccessJournal aj WHERE className=:className "
				+ "AND (classID='0' OR classID=:classID)";
			Query selectAccessJournalQuery = s.createQuery(selectAccessJournalSql);
			selectAccessJournalQuery.setString("className", className);
			selectAccessJournalQuery.setString("classID", classId);

			List<String> userIdList = selectAccessJournalQuery.list();
			result = checkUserListAccessRights(userIdList, className, classId);
		} catch(Exception e) {
			log.error("Error getting accessJournal for Object " + className + "(" + classId + ")", e);
		} finally {
			s.close();
		}
		
		return result;
	}

	@Override
	public Set<String> getClassAccessJournalIDs(String className) {
		return null;
	}

	@Override
	public long getNumClientsInterestedInWholeClass(String className) {
		return 0;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Collection<String>> getClientAccessess(Collection<ClassIDPair> classIdPairs) throws Exception {
		Map<String, Collection<String>> result = new HashMap<String, Collection<String>>();
		Session s = sessionFactorySync.openSession();

		// Return here if no classIdPairs have actually been supplied.
		if (classIdPairs.size() == 0)
			return result;
		
		try {
			//	Use hash of full class name to generate ID?
			String selectAccessJournalSql = "SELECT c FROM Client c WHERE c.belongsToUserId IN (SELECT DISTINCT(aj.userID) FROM AccessJournal aj WHERE";
			Iterator<ClassIDPair> itrClassIdPairs = classIdPairs.iterator();
			int i = 0;
			while(itrClassIdPairs.hasNext()) {
				ClassIDPair nextObject = itrClassIdPairs.next();
				if (i > 0)
					selectAccessJournalSql += " OR";
				selectAccessJournalSql += " (aj.className='" + nextObject.getClassName() + "' "
						+ "AND (aj.classID='0' OR aj.classID='" + nextObject.getID() + "'))";
				
				i++;
			}
			selectAccessJournalSql += ") ORDER BY c.belongsToUserId";
			Query selectAccessJournalQuery = s.createQuery(selectAccessJournalSql);
			
			List<Client> clientsList = selectAccessJournalQuery.list();
			for(Client nextClient : clientsList) {
				Collection<String> nextClientList = result.get(nextClient.getBelongsToUserId());
				if (nextClientList == null) {
					nextClientList = new ArrayList<String>();
					result.put(nextClient.getBelongsToUserId(), nextClientList);
				}
				nextClientList.add(nextClient.getClientId());
			}
			
			//result = checkUserListAccessRights(userIdList, className, classId);
		} catch(Exception e) {
			log.error("Error getting client accesses", e);
		} finally {
			s.close();
		}
		
		return result;
	}
	
	public List<String> checkUserListAccessRights(List<String> userIdList, String className, String classId) throws Exception {
		List<String> result = new ArrayList<String>();
		Session appSession = appSessionFactory.openSession();
		
		try {
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(className);
			Query query = null;

			boolean isValidReadQuery = false;
			if (mappedClass != null) {
				if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())) {
					isValidReadQuery = true;
					query = appSession.createQuery(mappedClass.getReadQuery().getQuery());
				}
			}

			for(String nextUserId : userIdList) {
				try {
					boolean hasAccess = true;
					// If the Read Query/Filter uses the ID, then we need to check against each ID here.
					if (isValidReadQuery) {
						mappedClass.getReadQuery().setQueryParameters(query.getQueryString(), classId, nextUserId);
						Number readFilterResult = (Number) query.uniqueResult();
						if (readFilterResult == null || readFilterResult.intValue() <= 0)
							hasAccess = false;
					}
					
					if (hasAccess)
						result.add(nextUserId.toString());
				} catch(Exception e) {
					log.warn("Error getting Object Access Journals", e);
				}
			}
		} catch(Exception e) {
			log.error("Error checking User AccessRights for " + className + "(" + classId + ")", e);
		} finally {
			if (appSession != null && appSession.isOpen())
				appSession.close();
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#getUserAccessJournals(java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	public List<AccessJournal> getUserAccessJournals(String userId) throws Exception {
		List<AccessJournal> result = null;
		Session s = sessionFactorySync.openSession();
		
		try {
			// TODO: Use a class id map instead of class name for faster lookup.
			//	Use hash of full class name to generate ID?
			String accessJournalSql = "SELECT aj FROM AccessJournal aj WHERE userID=:userID";
			Query accessJournalQuery = s.createQuery(accessJournalSql);
			accessJournalQuery.setInteger("userID", Integer.parseInt(userId));
			result = accessJournalQuery.list();
		} catch(Exception e) {
			log.error("Error getting accessJournal for user " + userId, e);
		} finally {
			s.close();
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#getClientUpdateJournals(java.lang.String)
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Collection<String> getClientUpdateJournals(String clientId, Boolean loggedInOnly) throws Exception {
		Collection<String> result = new ArrayList<String>();
		Session s = sessionFactorySync.openSession();
		
		try {
			String updateJournalSelect = "SELECT u FROM UpdateJournal u WHERE u.client.clientId=:clientId";// IN (SELECT c FROM Client c WHERE c.clientId=:clientId)";
			Query updateJournalQuery = s.createQuery(updateJournalSelect);
			updateJournalQuery.setString("clientId", clientId);
			
			List updateJournalList = (List) SyncHibernateUtils.cleanObject(updateJournalQuery.list(), s);
			
			if (loggedInOnly) {
				Iterator itrResult = updateJournalList.iterator();
				while(itrResult.hasNext()) {
					UpdateJournal uj = (UpdateJournal) itrResult.next();
					if (!uj.getClient().getIsLoggedIn()) {
						itrResult.remove();
					}
				}
			}
		} catch(Exception e) {
			log.error("Error getting updateJournals for client " + clientId, e);
		} finally {
			s.close();
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#getClientDeleteJournals(java.lang.String)
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Collection<String> getClientDeleteJournals(String clientId, Boolean loggedInOnly) throws Exception {
		List<String> result = new ArrayList<String>();
		Session s = sessionFactorySync.openSession();
		
		try {
			String deleteJournalSelect = "SELECT d FROM DeleteJournal d WHERE d.client IN (SELECT c FROM Client c WHERE c.clientId=:clientId";
			if (loggedInOnly) {
				deleteJournalSelect += " AND c.isLoggedIn=:isLoggedIn";
			}
			deleteJournalSelect += ")";
			Query deleteJournalQuery = s.createQuery(deleteJournalSelect);
			deleteJournalQuery.setString("clientId", clientId);
			if (loggedInOnly) {
				deleteJournalQuery.setBoolean("isLoggedIn", true);
			}
			
			result = (List) SyncHibernateUtils.cleanObject(deleteJournalQuery.list(), s);
		} catch(Exception e) {
			log.error("Error getting deleteJournals for client " + clientId, e);
		} finally {
			s.close();
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#removeUpdateJournals(java.util.List)
	 */
	public void removeUpdateJournals(List<UpdateJournal> updateJournalsToRemove) throws Exception {
		Session s = sessionFactorySync.openSession();
		
		try {
			Transaction tx = s.beginTransaction();
			tx.begin();
			String deleteUpdateJournalSql = "DELETE FROM UpdateJournal uj WHERE uj IN (:updateJournals)";
			Query deleteUpdateJournalQuery = s.createQuery(deleteUpdateJournalSql);
			deleteUpdateJournalQuery.setParameterList("updateJournals", updateJournalsToRemove);
			deleteUpdateJournalQuery.executeUpdate();
			tx.commit();
		} catch(Exception e) {
			log.error("Error removing updateJournals", e);
		} finally {
			s.close();
		}
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#removeUpdateJournalsByObject(IPerceroObject)
	 */
	public void removeUpdateJournalsByObject(IPerceroObject object) throws Exception {
		Session s = sessionFactorySync.openSession();
		
		try {
			Transaction tx = s.beginTransaction();
			tx.begin();
			String deleteUpdateJournalSql = "DELETE FROM UpdateJournal uj WHERE uj.className=:className AND uj.classID=:classId";
			Query deleteUpdateJournalQuery = s.createQuery(deleteUpdateJournalSql);
			deleteUpdateJournalQuery.setString("className", object.getClass().getName());
			deleteUpdateJournalQuery.setString("classId", object.getID());
			deleteUpdateJournalQuery.executeUpdate();
			tx.commit();
		} catch(Exception e) {
			log.error("Error removing updateJournals", e);
		} finally {
			s.close();
		}
	}

	public void addWatcherField(ClassIDPair classIdPair, String fieldName,
			Collection<String> collection) {
		// TODO Auto-generated method stub
		
	}

//	public Collection<Object> getWatcherFields(ClassIDPair classIdPair,
//			String fieldName) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public void addWatcherClient(ClassIDPair classIdPair,
			String fieldName, String clientId) {
		// TODO Auto-generated method stub
		
	}

	public void updateWatcherFields(ClassIDPair classIdPair,
			String fieldName, Collection<String> fieldsToWatch) {
		// TODO Auto-generated method stub
		
	}

	public void saveChangeWatcherResult(ClassIDPair classIdPaird,
			String fieldName, Object result) {
		// TODO Auto-generated method stub
		
	}

	public Object getChangeWatcherResult(ClassIDPair classIdPair,
			String fieldName) {
		// TODO Auto-generated method stub
		return null;
	}

	public void checkChangeWatchers(ClassIDPair classIdPair) {
		// TODO Auto-generated method stub
		
	}

	public void checkChangeWatchers(ClassIDPair classIdPair,
			String fieldName) {
		// TODO Auto-generated method stub
		
	}

	public Boolean getChangeWatcherResultExists(ClassIDPair classIdPair, String fieldName) {
		// TODO Auto-generated method stub
		return false;
	}

	public Boolean getChangeWatcherResultExists(ClassIDPair classIdPair, String fieldName, String[] params) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getChangeWatcherResult(ClassIDPair classIdPair, String fieldName, String[] params) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addWatcherClient(ClassIDPair classIdPair,
			String fieldName, String clientId, String[] params) {
		// TODO Auto-generated method stub
		
	}

	public void updateWatcherFields(ClassIDPair classIdPair,
			String fieldName, Collection<String> fieldsToWatch, String[] params) {
		// TODO Auto-generated method stub
		
	}

	public void saveChangeWatcherResult(ClassIDPair classIdPair,
			String fieldName, Object result, String[] params) {
		// TODO Auto-generated method stub
		
	}

	public void addWatcherField(ClassIDPair classIdPair, String fieldName,
			Collection<String> collection, String[] params) {
		// TODO Auto-generated method stub
		
	}

	public void checkChangeWatchers(ClassIDPair classIdPair,
			String fieldName, String[] params, IPerceroObject oldValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeChangeWatchersByObject(ClassIDPair classIdPair) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void checkChangeWatchers(ClassIDPair classIdPair,
			String[] fieldNames, String[] params, IPerceroObject oldValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void recalculateChangeWatcher(String changeWatcherId, IPerceroObject oldValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Long getChangeWatcherResultTimestamp(ClassIDPair classIdPair,
			String fieldName, String[] params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveDeleteJournalClients(ClassIDPair pair,
			Collection<String> clientIds, Boolean guaranteeDelivery,
			String pusherClient, Boolean sendToPusher) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean saveAccessJournal(ClassIDPair classIdPair, String userId,
			String clientId) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean saveAccessJournal(List<ClassIDPair> classIdPairs,
			String userId, String clientId) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void destroyClient(String clientId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> validateClientsIncludeFromDeviceHistory(Map<String, String> clientDevices)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> validateClients(Collection<String> clientIds)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String validateAndRetrieveCurrentClientId(String clientId,
			String deviceId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateWatcherFields(String category, String subCategory,
			String fieldName, Collection<String> fieldsToWatch, String[] params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addWatcherField(String category, String subCategory,
			String fieldName, Collection<String> collection, String[] params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addWatcherField(String category, String subCategory,
			String fieldName, Collection<String> collection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateWatcherFields(String category, String subCategory,
			String fieldName, Collection<String> fieldsToWatch) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.com.percero.agents.sync.services.IAccessManager#removeDeleteJournals(java.util.List)
	 *
	public boolean removeDeleteJournals(List<DeleteJournal> deleteJournalsToRemove) throws Exception {
		boolean result = false;
		Session s = sessionFactorySync.openSession();
		
		try {
			Transaction tx = s.beginTransaction();
			tx.begin();
			String deleteDeleteJournalSql = "DELETE FROM DeleteJournal dj WHERE dj IN (:deleteJournals)";
			Query deleteDeleteJournalQuery = s.createQuery(deleteDeleteJournalSql);
			deleteDeleteJournalQuery.setParameterList("deleteJournals", deleteJournalsToRemove);
			deleteDeleteJournalQuery.executeUpdate();
			tx.commit();
			
			result = true;
		} catch(Exception e) {
			log.error("Error removing deleteJournals", e);
		} finally {
			s.close();
		}
		
		return result;
	}*/
}

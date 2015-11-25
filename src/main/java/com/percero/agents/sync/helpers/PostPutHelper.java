package com.percero.agents.sync.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.percero.agents.sync.cw.CheckChangeWatcherMessage;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.services.IDataProvider;
import com.percero.agents.sync.services.IDataProviderManager;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.PushUpdateResponse;

@Component
public class PostPutHelper {

	private static final Logger log = Logger.getLogger(PostPutHelper.class);

	@Autowired
	IAccessManager accessManager;
	public void setAccessManager(IAccessManager value) {
		accessManager = value;
	}

	@Autowired
	IDataProviderManager dataProviderManager;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	IPushSyncHelper pushSyncHelper;
	public void setPushSyncHelper(IPushSyncHelper value) {
		pushSyncHelper = value;
	}

	@Autowired
	AmqpTemplate template;
	
	@Autowired
	Boolean guaranteeUpdateDelivery = true;
	public void setGuaranteeUpdateDelivery(Boolean value) {
		guaranteeUpdateDelivery = value;
	}

	public void postPutObject(ClassIDPair pair, String pusherUserId, String pusherClientId, boolean pushToUser, Map<ClassIDPair, Collection<MappedField>> changedFields) throws Exception {
		
		Collection<String> clientIds = accessManager.getObjectAccessJournals(pair.getClassName(), pair.getID());
		/*Collection<Object> clientIds = */accessManager.saveUpdateJournalClients(pair, clientIds, guaranteeUpdateDelivery, pusherClientId, pushToUser);
		
		Collection<MappedField> pairChangedFields = null;
		if (changedFields != null) {
			Iterator<Map.Entry<ClassIDPair, Collection<MappedField>>> itrChangedFieldEntryset = changedFields.entrySet().iterator();
			while (itrChangedFieldEntryset.hasNext()) {
				Map.Entry<ClassIDPair, Collection<MappedField>> nextEntry = itrChangedFieldEntryset.next();
				ClassIDPair nextPair = nextEntry.getKey();
				
				if (nextPair != null && nextPair.equals(pair)) {
					pairChangedFields = nextEntry.getValue();
					break;
				}
			}
//			Iterator<ClassIDPair> itrChangedFieldsKeys = changedFields.keySet().iterator();
//			while (itrChangedFieldsKeys.hasNext()) {
//				ClassIDPair nextPair = itrChangedFieldsKeys.next();
//				if (nextPair.equals(pair)) {
//					pairChangedFields = changedFields.get(nextPair);
//					break;
//				}
//			}
		}
		pushObjectUpdateJournals(clientIds, pair, pairChangedFields);
		
		// Now run past the ChangeWatcher.
		if (changedFields == null || changedFields.isEmpty()) {
			enqueueCheckChangeWatcher(pair, null, null);
//			accessManager.checkChangeWatchers(pair, null, null);
		}
		else {
			// TODO: Need to somehow aggregate changes per client/object.
			Iterator<Map.Entry<ClassIDPair, Collection<MappedField>>> itrChangedFieldEntryset = changedFields.entrySet().iterator();
			while (itrChangedFieldEntryset.hasNext()) {
				Map.Entry<ClassIDPair, Collection<MappedField>> nextEntry = itrChangedFieldEntryset.next();
				ClassIDPair thePair = nextEntry.getKey();
				if (thePair == null) {
					continue;
				}

				Collection<MappedField> changedMappedFields = nextEntry.getValue();
				
				// If thePair is NOT the object being updated, then need to run the postPutHelper for the Pair object as well.
				if (!thePair.equals(pair)) {
					Map<ClassIDPair, Collection<MappedField>> thePairChangedFields = new HashMap<ClassIDPair, Collection<MappedField>>(1);
					thePairChangedFields.put(thePair, changedMappedFields);
					
					// This will also run thePair through the ChangeWatcher check below.
					this.postPutObject(thePair, pusherUserId, pusherClientId, pushToUser, thePairChangedFields);
				}
				else {
					Iterator<MappedField> itrChangedFields = changedMappedFields.iterator();
					String[] fieldNames = new String[changedMappedFields.size()];
					int i = 0;
					while (itrChangedFields.hasNext()) {
						MappedField nextChangedField = itrChangedFields.next();
						fieldNames[i] = nextChangedField.getField().getName();
						i++;
					}

					// Swap out inline processing for a worker queue
					enqueueCheckChangeWatcher(thePair, fieldNames, null);
//					accessManager.checkChangeWatchers(thePair, fieldNames, null);
				}
			}
//			Iterator<ClassIDPair> itrChangedFieldKeyset = changedFields.keySet().iterator();
//			while (itrChangedFieldKeyset.hasNext()) {
//				ClassIDPair thePair = itrChangedFieldKeyset.next();
//				Collection<MappedField> changedMappedFields = changedFields.get(thePair);
//				Iterator<MappedField> itrChangedFields = changedMappedFields.iterator();
//				String[] fieldNames = new String[changedMappedFields.size()];
//				int i = 0;
//				while (itrChangedFields.hasNext()) {
//					MappedField nextChangedField = itrChangedFields.next();
//					fieldNames[i] = nextChangedField.getField().getName();
//					i++;
//				}
//				accessManager.checkChangeWatchers(thePair, fieldNames, null);
//			}
		}
	}

	private void enqueueCheckChangeWatcher(ClassIDPair classIDPair, String[] fieldNames, String[] params){
		CheckChangeWatcherMessage message = new CheckChangeWatcherMessage();
		message.classIDPair = classIDPair;
		message.fieldNames = fieldNames;
		message.params = params;
		template.convertAndSend("checkChangeWatcher", message);
	}


	public void pushObjectUpdateJournals(Collection<String> clientIds, ClassIDPair classIdPair, Collection<MappedField> changedFields) {
		if (classIdPair != null && clientIds != null && !clientIds.isEmpty()) {
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mc = mcm.getMappedClassByClassName(classIdPair.getClassName());
			IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mc.dataProviderName);
		
			// If the object's Class has no READ access rights on itself or any related object, then no need to get a distinct object for each user.
			if (!mc.getNeedsReadCleaning()) {
				PushUpdateResponse pushUpdateResponse = new PushUpdateResponse();
				pushUpdateResponse.setObjectList(new ArrayList<BaseDataObject>());
				
				BaseDataObject object = (BaseDataObject) dataProvider.findById(classIdPair, null);
				if (object != null) {
					pushUpdateResponse.getObjectList().add(object);

					// Add changed fields names
					if (changedFields != null) {
						Iterator<MappedField> itrChangedFields = changedFields.iterator();
						while (itrChangedFields.hasNext()) {
							MappedField nextChangedField = itrChangedFields.next();
							if (nextChangedField != null) {
								pushUpdateResponse.addUpdatedField(nextChangedField.getField().getName());
							}
						}
					}

					pushSyncHelper.pushSyncResponseToClients(pushUpdateResponse, clientIds);
				}
			}
			else {
				// Map Client ID's to UserID.
				Map<String, Collection<String>> userClientsMap = new HashMap<String, Collection<String>>();
				Iterator<String> itrClientIds = clientIds.iterator();
				while (itrClientIds.hasNext()) {
					String nextClientId = itrClientIds.next();
					String userId = accessManager.getClientUserId(nextClientId);
					
					if (userId != null) {
						Collection<String> userClients = userClientsMap.get(userId);
						if (userClients == null) {
							userClients = new ArrayList<String>();
							userClientsMap.put(userId, userClients);
						}
						userClients.add(nextClientId);
					}
				}

				// Setup the PushUpdateResponse fields that are User agnostic.
				PushUpdateResponse pushUpdateResponse = new PushUpdateResponse();
				pushUpdateResponse.setObjectList(new ArrayList<BaseDataObject>());
				// Add changed fields names
				if (changedFields != null) {
					Iterator<MappedField> itrChangedFields = changedFields.iterator();
					while (itrChangedFields.hasNext()) {
						MappedField nextChangedField = itrChangedFields.next();
						if (nextChangedField != null) {
							pushUpdateResponse.addUpdatedField(nextChangedField.getField().getName());
						}
					}
				}
				
				// Send the object update to each the list of Clients by User.
				Iterator<Map.Entry<String, Collection<String>>> itrUserClientsEntrySet = userClientsMap.entrySet().iterator();
				while (itrUserClientsEntrySet.hasNext()) {
					Map.Entry<String, Collection<String>> nextEntry = itrUserClientsEntrySet.next();
					String nextUserId = nextEntry.getKey();
					Collection<String> userClients = nextEntry.getValue();
					
					// Get the version of the object for this user.
					BaseDataObject object = (BaseDataObject) dataProvider.findById(classIdPair, nextUserId);
					if (object != null) {
						pushUpdateResponse.getObjectList().add(object);
						pushSyncHelper.pushSyncResponseToClients(pushUpdateResponse, userClients);
					}

//					try {
//						PushUpdateResponse pushUpdateResponse = new PushUpdateResponse();
//						pushUpdateResponse.setObjectList(new ArrayList<BaseDataObject>());
//						
//						pushUpdateResponse.setClientId(nextClientId);
//						
//						// Need to get version of this object appropriate for this client.
//						Object object = dataProvider.findById(classIdPair, userId);
//						
//						if (object != null) {
//							pushUpdateResponse.getObjectList().add((BaseDataObject) object);
//
//							// Add changed fields names
//							if (changedFields != null) {
//								Iterator<MappedField> itrChangedFields = changedFields.iterator();
//								while (itrChangedFields.hasNext()) {
//									MappedField nextChangedField = itrChangedFields.next();
//									pushUpdateResponse.addUpdatedField(nextChangedField.getField().getName());
//								}
//							}
//
//							pushSyncHelper.pushSyncResponseToClient(pushUpdateResponse, nextClientId);
//						}
//					} catch(Exception e) {
//						log.error("Error pushing Object Update Journal to clients", e);
//					}
				}
			}
		}
	}
	
//	public void pushClientUpdateJournals(Map<String, Set<ClassIDPair>> clientUpdates) {
//
//		Iterator<Map.Entry<String, Set<ClassIDPair>>> itrClientUpdatesEntrySet = clientUpdates.entrySet().iterator();
//		while (itrClientUpdatesEntrySet.hasNext()) {
//			Map.Entry<String, Set<ClassIDPair>> nextEntry = itrClientUpdatesEntrySet.next();
//			String nextClientId = nextEntry.getKey();
//			Set<ClassIDPair> pairs = nextEntry.getValue();
//			
//			String userId = accessManager.getClientUserId(nextClientId);
//			List<BaseDataObject> objectList = new ArrayList<BaseDataObject>();
//
//			PushUpdateResponse pushUpdateResponse = new PushUpdateResponse();
//			pushUpdateResponse.setObjectList(objectList);
//			pushUpdateResponse.setClientId(nextClientId);
//			
//			Iterator<ClassIDPair> itrPairs = pairs.iterator();
//			while (itrPairs.hasNext()) {
//				try {
//					ClassIDPair pair = itrPairs.next();
//	
//					IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
//					MappedClass mc = mcm.getMappedClassByClassName(pair.getClassName());
//					IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mc.dataProviderName);
//					BaseDataObject perceroObject = (BaseDataObject) dataProvider.findById(pair, userId);
//					
//					if (perceroObject != null) {
//						objectList.add(perceroObject);
//					}
//				} catch(Exception e) {
//					log.error("Error pushing Object Update Journal to client " + nextClientId, e);
//				}
//			}
//			
//			if (!objectList.isEmpty()) {
//				pushSyncHelper.pushSyncResponseToClient(pushUpdateResponse, nextClientId);
//			}
//		}
//	}
}

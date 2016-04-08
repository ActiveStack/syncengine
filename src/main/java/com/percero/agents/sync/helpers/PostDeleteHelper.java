package com.percero.agents.sync.helpers;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.services.ISyncAgentService;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.PushDeleteResponse;
import com.percero.agents.sync.vo.RemovedClassIDPair;
import com.percero.framework.vo.IPerceroObject;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Component
public class PostDeleteHelper {

	private static final Logger log = Logger.getLogger(PostDeleteHelper.class);

	@Autowired
	PostPutHelper postPutHelper;
	public void setPostPutHelper(PostPutHelper value) {
		postPutHelper = value;
	}

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	IAccessManager accessManager;
	public void setAccessManager(IAccessManager value) {
		accessManager = value;
	}

	@Autowired
	ISyncAgentService syncAgentService;
	public void setSyncAgentService(ISyncAgentService value) {
		syncAgentService = value;
	}

	@Autowired
	IPushSyncHelper pushSyncHelper;
	public void setPushSyncHelper(IPushSyncHelper value) {
		pushSyncHelper = value;
	}
	
	@Autowired
	Boolean guaranteeUpdateDelivery = true;
	public void setGuaranteeUpdateDelivery(Boolean value) {
		guaranteeUpdateDelivery = value;
	}

	// TODO: Implement postDeleteObject.
	//	Should handle cascade delete.
	//	Should send RemovedClassIDPair object to all interested parties.
	// TODO: Delete all references to this object from the AccesJournal and UpdateJournal tables.
	public void postDeleteObject(IPerceroObject theObject, String pusherUserId, String pusherClientId, boolean pushToUser) throws Exception {
		log.debug("PostDeleteHelper for " + theObject.toString() + " from clientId " + (pusherClientId == null ? "NULL" : pusherClientId));

		ClassIDPair pair = new ClassIDPair(theObject.getID(), theObject.getClass().getCanonicalName());

		// Remove all UpdateJournals for the objects.
		accessManager.removeUpdateJournalsByObject(pair);

		// Notify interested users that this object has been deleted.
		RemovedClassIDPair removedPair = new RemovedClassIDPair();
		removedPair.setClassName(pair.getClassName());
		removedPair.setID(pair.getID());

		List<String> clientIds = accessManager.getObjectAccessJournals(pair.getClassName(), pair.getID());

		// Now remove the AccessJournals for this Object.
		accessManager.removeAccessJournalsByObject(pair);
		// Now remove the ObjectModJournals for this Object.
		accessManager.removeObjectModJournalsByObject(pair);
		// Now remove the HistoricalObjects for this Object.
		accessManager.removeHistoricalObjectsByObject(pair);

		// Now run past the ChangeWatcher.
		accessManager.checkAndRemoveChangeWatchers(pair, null, null, null);

		/*Collection<Object> deleteJournals = */accessManager.saveDeleteJournalClients(pair, clientIds, guaranteeUpdateDelivery, pusherClientId, pushToUser);
		pushObjectDeleteJournals(clientIds, pair.getClassName(), pair.getID());

		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(pair.getClassName());
		if (mappedClass != null) {
			// Since this object has been deleted, ALL fields have changed.
			Iterator<MappedField> itrChangedFields = mappedClass.externalizableFields.iterator();
			String[] fieldNames = new String[mappedClass.externalizableFields.size()];
			int i = 0;
			while (itrChangedFields.hasNext()) {
				MappedField nextChangedField = itrChangedFields.next();
				fieldNames[i] = nextChangedField.getField().getName();
				i++;
			}
			pushSyncHelper.enqueueCheckChangeWatcher(pair, fieldNames, null, theObject);
			
			Map<ClassIDPair, MappedField> objectsToUpdate = mappedClass.getRelatedClassIdPairMappedFieldMap(theObject, false);
			Iterator<Entry<ClassIDPair, MappedField>> itrObjectsToUpdate = objectsToUpdate.entrySet().iterator();
			while (itrObjectsToUpdate.hasNext()) {
				Entry<ClassIDPair, MappedField> nextObjectToUpdate = itrObjectsToUpdate.next();
				Map<ClassIDPair, Collection<MappedField>> changedFields = new HashMap<ClassIDPair, Collection<MappedField>>();
				Collection<MappedField> changedMappedFields = new ArrayList<MappedField>(1);
				changedMappedFields.add(nextObjectToUpdate.getValue());
				changedFields.put(nextObjectToUpdate.getKey(), changedMappedFields);
				postPutHelper.postPutObject(nextObjectToUpdate.getKey(), pusherUserId, pusherClientId, true, changedFields);
			}
		}
	}

	protected void pushObjectDeleteJournals(Collection<String> clientIds, String className, String classId) {
		if (clientIds != null && !clientIds.isEmpty()) {
			try {
				RemovedClassIDPair removedPair = new RemovedClassIDPair();
				removedPair.setClassName(className);
				removedPair.setID(classId);
				
				// Optimization: create the JSON string of the object.
				String objectJson = removedPair.toEmbeddedJson();
				
				PushDeleteResponse pushDeleteResponse = null;
				pushDeleteResponse = new PushDeleteResponse();
				pushDeleteResponse.setObjectList(new ArrayList<RemovedClassIDPair>());
				
				pushDeleteResponse.getObjectList().add(removedPair);

				pushDeleteResponse.setObjectJson(objectJson);
				
				pushSyncHelper.pushSyncResponseToClients(pushDeleteResponse, clientIds);
//
//				for(String nextClientId : clientIds) {
//					PushDeleteResponse pushDeleteResponse = null;
//					pushDeleteResponse = new PushDeleteResponse();
//					pushDeleteResponse.setObjectList(new ArrayList<RemovedClassIDPair>());
//					
//					pushDeleteResponse.setClientId(nextClientId);
//					pushDeleteResponse.getObjectList().add(removedPair);
//
//					pushDeleteResponse.setObjectJson(objectJson);
//					pushSyncHelper.pushSyncResponseToClient(pushDeleteResponse, nextClientId);
//					//pushSyncHelper.pushJsonToRouting(pushDeleteResponse.toJson(objectJson, objectMapper), PushDeleteResponse.class, (String) nextClient);
//				}
			} catch(Exception e) {
				log.error("Error sending delete message to set of clients", e);
			}
		}
	}
}

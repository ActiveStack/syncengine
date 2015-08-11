package com.percero.agents.sync.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.services.ISyncAgentService;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.PushDeleteResponse;
import com.percero.agents.sync.vo.RemovedClassIDPair;
import com.percero.framework.vo.IPerceroObject;

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
		removedPair.setClassName(theObject.getClass().getName());
		removedPair.setID(theObject.getID());

		List<String> clientIds = accessManager.getObjectAccessJournals(theObject
				.getClass().getName(), theObject.getID());
		
		// Now remove the AccessJournals for this Object.
		accessManager.removeAccessJournalsByObject(pair);
		// Now remove the ObjectModJournals for this Object.
		accessManager.removeObjectModJournalsByObject(pair);
		// Now remove the HistoricalObjects for this Object.
		accessManager.removeHistoricalObjectsByObject(pair);
		
		// Now run past the ChangeWatcher.
		accessManager.checkChangeWatchers(pair, null, null);
		// Remove ChangeWatchers associated with this object.
		accessManager.removeChangeWatchersByObject(pair);

		/*Collection<Object> deleteJournals = */accessManager.saveDeleteJournalClients(pair, clientIds, guaranteeUpdateDelivery, pusherClientId, pushToUser);
		pushObjectDeleteJournals(clientIds, pair.getClassName(), pair.getID());
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
				
//				pushDeleteResponse.setClientId(nextClientId);
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

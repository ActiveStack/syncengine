package com.percero.agents.sync.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.metadata.MappedFieldPerceroObject;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.accessor.IAccessorService;
import com.percero.framework.vo.IPerceroObject;

@Component
public class PostCreateHelper {
	
	private static final Logger log = Logger.getLogger(PostCreateHelper.class);
	
	@Autowired
	IAccessManager accessManager;
	public void setAccessManager(IAccessManager value) {
		accessManager = value;
	}

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	PostPutHelper postPutHelper;
	public void setPostPutHelper(PostPutHelper value) {
		postPutHelper = value;
	}

	@Autowired
	IPushSyncHelper pushSyncHelper;
	public void setPushSyncHelper(IPushSyncHelper value) {
		pushSyncHelper = value;
	}
	
	@Autowired
	IAccessorService accessorService;
	public void setAccessorService(IAccessorService value) {
		accessorService = value;
	}
	
	@Autowired
	Boolean guaranteeUpdateDelivery = true;
	public void setGuaranteeUpdateDelivery(Boolean value) {
		guaranteeUpdateDelivery = value;
	}

	/**
	 * This handles pushing out the appropriate notifications to all interested clients so that
	 * 	they are able to consume this newly created object.
	 * NOTE: percerObject has already been "deep cleaned" at this point, meaning all its related
	 * 	objects are full objects and therefore don't need to be cleaned.
	 * 
	 * @param perceroObject
	 * @param pusherUserId
	 * @param pusherClientId
	 * @param pushToUser
	 * @throws Exception
	 */
	public void postCreateObject(IPerceroObject perceroObject, String pusherUserId, String pusherClientId, boolean pushToUser) throws Exception {

		ClassIDPair pair = BaseDataObject.toClassIdPair(perceroObject);
		
		// Register the creator of this object in the AccessJournal.
		if (pusherUserId != null && pusherUserId.length() > 0)
			accessManager.saveAccessJournal(pair, pusherUserId, pusherClientId);
		
		// For each association, update push the associated objects.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(pair.getClassName());
		
		// Sending an empty List as the ChangedFields parameter will cause the PostPutHelper to NOT check access 
		//	managers for each field since it is impossible for change watchers to already exist for this new
		//	object.

		// Since this object has been deleted, ALL fields have changed.
		Iterator<MappedField> itrChangedFields = mappedClass.externalizableFields.iterator();
		Map<ClassIDPair, Collection<MappedField>> changedFields = new HashMap<ClassIDPair, Collection<MappedField>>(1);
		Collection<MappedField> changedMappedFields = new ArrayList<MappedField>(mappedClass.externalizableFields.size());
		while (itrChangedFields.hasNext()) {
			MappedField nextChangedField = itrChangedFields.next();
			changedMappedFields.add(nextChangedField);
		}

		postPutHelper.postPutObject(pair, pusherUserId, pusherClientId, pushToUser, changedFields);

		Map<ClassIDPair, Collection<MappedField>> changedObjects = new HashMap<ClassIDPair, Collection<MappedField>>();
		
		// The only relationship fields that could have changed are source relationship fields 
		for(MappedFieldPerceroObject nextMappedField : mappedClass.getSourceMappedFields()) {
			try {
				// We only care about two-way relationships.
				if (nextMappedField.getReverseMappedField() != null) {
					IPerceroObject fieldValue = (IPerceroObject) nextMappedField.getValue(perceroObject);
					if (fieldValue != null) {
						ClassIDPair fieldPair = BaseDataObject.toClassIdPair(fieldValue);

						Collection<MappedField> classChangedFields = changedObjects.get(fieldPair);
						if (classChangedFields == null) {
							classChangedFields = new HashSet<MappedField>();
							changedObjects.put(fieldPair, classChangedFields);
						}
						MappedField reverseMappedField = nextMappedField.getReverseMappedField();
						classChangedFields.add(reverseMappedField);
					}
				}
			} catch(Exception e) {
				log.error("Error in postCreateObject " + mappedClass.className + "." + nextMappedField.getField().getName(), e);
			}
		}
		
		// Consolidated post push down per classIdPair.
		/**
		postPutHelper.postPutObjects(changedObjects.keySet(), pusherUserId, pusherClientId, pushToUser, changedObjects);
		 */
		Iterator<Map.Entry<ClassIDPair, Collection<MappedField>>> itrChangedObjectEntries = changedObjects.entrySet().iterator();
		while (itrChangedObjectEntries.hasNext()) {
			Map.Entry<ClassIDPair, Collection<MappedField>> nextEntry = itrChangedObjectEntries.next();
			ClassIDPair nextPair = nextEntry.getKey();
			Collection<MappedField> changedMappedFieds = nextEntry.getValue();

			Map<ClassIDPair, Collection<MappedField>> changedFieldsMap = new HashMap<ClassIDPair, Collection<MappedField>>();
			changedFieldsMap.put(nextPair, changedMappedFieds);
			postPutHelper.postPutObject(nextPair, pusherUserId, pusherClientId, pushToUser, changedFieldsMap);
		}
//		Iterator<ClassIDPair> itrChangedObjects = changedObjects.keySet().iterator();
//		while (itrChangedObjects.hasNext()) {
//			ClassIDPair nextPair = itrChangedObjects.next();
//			Collection<MappedField> changedMappedFieds = changedObjects.get(nextPair);
//			Map<ClassIDPair, Collection<MappedField>> changedFieldsMap = new HashMap<ClassIDPair, Collection<MappedField>>();
//			changedFieldsMap.put(nextPair, changedMappedFieds);
//			postPutHelper.postPutObject(nextPair, pusherUserId, pusherClientId, pushToUser, changedFieldsMap);
//		}
	}
}

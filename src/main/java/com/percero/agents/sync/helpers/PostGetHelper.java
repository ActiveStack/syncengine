package com.percero.agents.sync.helpers;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.access.RedisAccessManager;
import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.cw.ChangeWatcherReporting;
import com.percero.agents.sync.cw.IChangeWatcherHelper;
import com.percero.agents.sync.cw.IChangeWatcherHelperFactory;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.vo.IPerceroObject;

@Component
public class PostGetHelper {

	private static Logger log = Logger.getLogger(PostGetHelper.class);

	
	public static final String CATEGORY = "POST_GET";

	@Autowired
	IAccessManager accessManager;
	public void setAccessManager(IAccessManager value) {
		accessManager = value;
	}
	
	@Autowired
	IChangeWatcherHelperFactory changeWatcherHelperFactory;
	public void setChangeWatcherHelperFactory(IChangeWatcherHelperFactory value) {
		this.changeWatcherHelperFactory = value;
	}
	

	public void postGetObject(IPerceroObject perceroObject, String userId, String clientId) throws Exception {
		ClassIDPair pair = new ClassIDPair(perceroObject.getID(), perceroObject.getClass().getCanonicalName());
		accessManager.saveAccessJournal(pair, userId, clientId);
		
		postGet(pair);
	}
	
	public void postGetObject(List<IPerceroObject> perceroObjects, String userId, String clientId) throws Exception {
		List<ClassIDPair> classIdPairs = new ArrayList<ClassIDPair>();
		Iterator<IPerceroObject> itrPerceroObjects = perceroObjects.iterator();
		while (itrPerceroObjects.hasNext()) {
			IPerceroObject nextPerceroObject = itrPerceroObjects.next();
			classIdPairs.add(new ClassIDPair(nextPerceroObject.getID(), nextPerceroObject.getClass().getCanonicalName()));
		}
		accessManager.saveAccessJournal(classIdPairs, userId, clientId);
	}
	
	protected void postGet(ClassIDPair classIdPair) {
		IChangeWatcherHelper cwh = changeWatcherHelperFactory.getHelper(CATEGORY);
		if (cwh != null) {
			String changeWatcherId = "cw:cf:" + CATEGORY + ":" + classIdPair.getClassName() + ":" + classIdPair.getID();
			setupRecalculateChangeWatcher(changeWatcherId);
		}
	}
	
	protected void setupRecalculateChangeWatcher(String changeWatcherId) {
		
		ChangeWatcherReporting.internalRequestsCounter++;
//		if (useChangeWatcherQueue && pushSyncHelper != null) {
//			pushSyncHelper.pushStringToRoute( (new StringBuilder(changeWatcherId).append(":TS:").append(System.currentTimeMillis())).toString(), changeWatcherRouteName);
//		}
//		else {
			recalculateChangeWatcher(changeWatcherId);
//		}
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
				IChangeWatcherHelper cwh = changeWatcherHelperFactory.getHelper(category);
				cwh.reprocess(category, subCategory, fieldName, null, otherParams, requestTimestamp);

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
}

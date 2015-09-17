package com.percero.agents.sync.cache;

import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.datastore.ICacheDataStore;
import com.percero.agents.sync.hibernate.SyncHibernateUtils;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.metadata.MappedFieldPerceroObject;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.agents.sync.services.IDataProvider;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.vo.IPerceroObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.misc.Cache;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created by jonnysamps on 9/5/15.
 */
@Component
public class CacheManager {

    private static Logger logger = Logger.getLogger(CacheManager.class);

    @Autowired
    ICacheDataStore cacheDataStore;

    @Autowired
    Long cacheTimeout = Long.valueOf(60 * 60 * 24 * 14);	// Two weeks

    public void updateCachedObject(IPerceroObject perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields){
        // TODO: Field-level updates could be REALLY useful here.  Would avoid A TON of UNNECESSARY work...
        try {
			if (cacheTimeout > 0) {
				// TODO: Also need to update the caches of anything object that is related to this object.
				String key = RedisKeyUtils.classIdPair(perceroObject.getClass().getCanonicalName(), perceroObject.getID());
				if (cacheDataStore.hasKey(key)) {
					cacheDataStore.setValue(key, ((BaseDataObject)perceroObject).toJson());
				}
				
				// Iterate through each changed object and reset the cache for that object.
				if (changedFields != null) {
//					Iterator<Map.Entry<ClassIDPair, Collection<MappedField>>> itrChangedFieldEntrySet = changedFields.entrySet().iterator();
//					Set<String> keysToDelete = new HashSet<String>();
//					while (itrChangedFieldEntrySet.hasNext()) {
//						Map.Entry<ClassIDPair, Collection<MappedField>> nextEntry = itrChangedFieldEntrySet.next();
//						ClassIDPair thePair = nextEntry.getKey();
//						if (!thePair.comparePerceroObject(perceroObject)) {
//							String nextKey = RedisKeyUtils.classIdPair(thePair.getClassName(), thePair.getID());
//							keysToDelete.add(nextKey);
//						}
//					}
					Iterator<ClassIDPair> itrChangedFieldKeyset = changedFields.keySet().iterator();
					Set<String> keysToDelete = new HashSet<String>();
					while (itrChangedFieldKeyset.hasNext()) {
						ClassIDPair thePair = itrChangedFieldKeyset.next();
						if (!thePair.comparePerceroObject(perceroObject)) {
							String nextKey = RedisKeyUtils.classIdPair(thePair.getClassName(), thePair.getID());
							keysToDelete.add(nextKey);
						}
					}
					
					if (!keysToDelete.isEmpty()) {
						cacheDataStore.deleteKeys(keysToDelete);
						// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
						//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
					}
				}
				else {
					// No changedFields?  We should never get here?
					IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
					MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
					Iterator<MappedField> itrToManyFields = mappedClass.toManyFields.iterator();
					while(itrToManyFields.hasNext()) {
						MappedField nextMappedField = itrToManyFields.next();
						Object fieldObject = nextMappedField.getGetter().invoke(perceroObject);
						if (fieldObject != null) {
							if (fieldObject instanceof IPerceroObject) {
								String nextKey = RedisKeyUtils.classIdPair(fieldObject.getClass().getCanonicalName(), ((IPerceroObject)fieldObject).getID());
								if (cacheDataStore.hasKey(nextKey)) {
									cacheDataStore.deleteKey(nextKey);
									// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
									//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
								}
							}
							else if (fieldObject instanceof Collection) {
								Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
								while(itrFieldObject.hasNext()) {
									Object nextListObject = itrFieldObject.next();
									if (nextListObject instanceof IPerceroObject) {
										String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject)nextListObject).getID());
										if (cacheDataStore.hasKey(nextKey)) {
											cacheDataStore.deleteKey(nextKey);
											// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
											//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
										}
									}
								}
							}
						}
					}
					Iterator<MappedFieldPerceroObject> itrToOneFields = mappedClass.toOneFields.iterator();
					while(itrToOneFields.hasNext()) {
						MappedFieldPerceroObject nextMappedField = itrToOneFields.next();
						Object fieldObject = nextMappedField.getGetter().invoke(perceroObject);
						if (fieldObject != null) {
							if (fieldObject instanceof IPerceroObject) {
								String nextKey = RedisKeyUtils.classIdPair(fieldObject.getClass().getCanonicalName(), ((IPerceroObject)fieldObject).getID());
								if (cacheDataStore.hasKey(nextKey)) {
									cacheDataStore.deleteKey(nextKey);
									// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
									//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
								}
							}
							else if (fieldObject instanceof Collection) {
								Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
								while(itrFieldObject.hasNext()) {
									Object nextListObject = itrFieldObject.next();
									if (nextListObject instanceof IPerceroObject) {
										String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject)nextListObject).getID());
										if (cacheDataStore.hasKey(nextKey)) {
											cacheDataStore.deleteKey(nextKey);
											// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
											//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
										}
									}
								}
							}
						}
					}
				}
			}
        } catch (Exception e){
            logger.warn(e.getMessage(), e);
        }
    }
    
    public void handleDeletedObject(IPerceroObject perceroObject, String className, Boolean isShellObject) {
		if (cacheTimeout > 0) {
			try {
				Set<String> keysToDelete = new HashSet<String>();
	
				String key = RedisKeyUtils.classIdPair(className, perceroObject.getID());
				keysToDelete.add(key);
				
				Set<ClassIDPair> relatedClassIdPairs = getRelatedClassIdPairs(perceroObject, className, isShellObject);
				Iterator<ClassIDPair> itrRelatedClassIdPairs = relatedClassIdPairs.iterator();
				while (itrRelatedClassIdPairs.hasNext()) {
					ClassIDPair nextRelatedClassIdPair = itrRelatedClassIdPairs.next();
					String nextKey = RedisKeyUtils.classIdPair(nextRelatedClassIdPair.getClassName(), nextRelatedClassIdPair.getID());
					keysToDelete.add(nextKey);
				}
	
				if (!keysToDelete.isEmpty()) {
					cacheDataStore.deleteKeys(keysToDelete);
					// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
					//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
    }
    
    public void deleteObjectFromCache(ClassIDPair classIdPair) {
		String key = RedisKeyUtils.classIdPair(classIdPair.getClassName(), classIdPair.getID());
		cacheDataStore.deleteKey(key);
    }
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public Set<ClassIDPair> getRelatedClassIdPairs(IPerceroObject perceroObject, String className, Boolean isShellObject) throws Exception {
    	Set<ClassIDPair> results = new HashSet<ClassIDPair>();
    	
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(className);

		Iterator<MappedField> itrToManyFields = mappedClass.toManyFields.iterator();
		while(itrToManyFields.hasNext()) {
			MappedField nextMappedField = itrToManyFields.next();
			
			// If this is a SHELL object, then we need to get ALL objects of this MappedField type.
			if (isShellObject) {
				// If NO reverse mapped field, then nothing to update.
				if (nextMappedField.getReverseMappedField() != null) {
					MappedClass reverseMappedClass = nextMappedField.getReverseMappedField().getMappedClass();
					IDataProvider reverseDataProvider = DataProviderManager.getInstance().getDataProviderByName(reverseMappedClass.className);
					Set<ClassIDPair> allClassIdPairs = reverseDataProvider.getAllClassIdPairsByName(reverseMappedClass.className);
					Iterator<ClassIDPair> itrAllClassIdPairs = allClassIdPairs.iterator();
					while (itrAllClassIdPairs.hasNext()) {
						ClassIDPair nextAllClassIdPair = itrAllClassIdPairs.next();
						results.add(nextAllClassIdPair);
					}
				}
			}
			else {
				Object fieldObject = nextMappedField.getGetter().invoke(perceroObject);
				if (fieldObject != null) {
					if (fieldObject instanceof IPerceroObject) {
						results.add(new ClassIDPair(((IPerceroObject)fieldObject).getID(), fieldObject.getClass().getCanonicalName()));
					}
					else if (fieldObject instanceof Collection) {
						Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
						while(itrFieldObject.hasNext()) {
							Object nextListObject = itrFieldObject.next();
							if (nextListObject instanceof IPerceroObject) {
								results.add(new ClassIDPair(((IPerceroObject)nextListObject).getID(), nextListObject.getClass().getCanonicalName()));
							}
						}
					}
				}
			}
		}
		Iterator<MappedFieldPerceroObject> itrToOneFields = mappedClass.toOneFields.iterator();
		while(itrToOneFields.hasNext()) {
			MappedFieldPerceroObject nextMappedField = itrToOneFields.next();

			// If this is a SHELL object, then we need to get ALL objects of this MappedField type.
			if (isShellObject) {
				// If NO reverse mapped field, then nothing to update.
				if (nextMappedField.getReverseMappedField() != null) {
					MappedClass reverseMappedClass = nextMappedField.getReverseMappedField().getMappedClass();
					IDataProvider reverseDataProvider = DataProviderManager.getInstance().getDataProviderByName(reverseMappedClass.className);
					Set<ClassIDPair> allClassIdPairs = reverseDataProvider.getAllClassIdPairsByName(reverseMappedClass.className);
					Iterator<ClassIDPair> itrAllClassIdPairs = allClassIdPairs.iterator();
					while (itrAllClassIdPairs.hasNext()) {
						ClassIDPair nextAllClassIdPair = itrAllClassIdPairs.next();
						results.add(nextAllClassIdPair);
					}
				}
			}
			else {
				Object fieldObject = nextMappedField.getGetter().invoke(perceroObject);
				if (fieldObject != null) {
					if (fieldObject instanceof IPerceroObject) {
						results.add(new ClassIDPair(((IPerceroObject)fieldObject).getID(), fieldObject.getClass().getCanonicalName()));
					}
					else if (fieldObject instanceof Collection) {
						Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
						while(itrFieldObject.hasNext()) {
							Object nextListObject = itrFieldObject.next();
							if (nextListObject instanceof IPerceroObject) {
								results.add(new ClassIDPair(((IPerceroObject)nextListObject).getID(), nextListObject.getClass().getCanonicalName()));
							}
						}
					}
				}
			}
		}
    	
    	return results;
    }
}

package com.percero.agents.sync.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.datastore.ICacheDataStore;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.metadata.MappedFieldPerceroObject;
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.agents.sync.services.IDataProvider;
import com.percero.agents.sync.services.SyncAgentService;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.vo.IPerceroObject;

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

				List<ClassIDPair> pairsToDelete = new ArrayList<ClassIDPair>();
				
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
					while (itrChangedFieldKeyset.hasNext()) {
						ClassIDPair thePair = itrChangedFieldKeyset.next();
						if (thePair != null && StringUtils.hasText(thePair.getID()) && !thePair.comparePerceroObject(perceroObject)) {
							pairsToDelete.add(thePair);
//							String nextKey = RedisKeyUtils.classIdPair(thePair.getClassName(), thePair.getID());
						}
					}
				}
				else {
					// No changedFields -> VERY INEFFICIENT.
					// This is typically only reached when using UpdateTables
					// that have update records that are NOT in the cache.
					IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
					MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
					Iterator<MappedField> itrToManyFields = mappedClass.toManyFields.iterator();
					while(itrToManyFields.hasNext()) {
						MappedField nextMappedField = itrToManyFields.next();
						Object fieldObject = nextMappedField.getGetter().invoke(perceroObject);
						if (fieldObject != null) {
							if (fieldObject instanceof IPerceroObject) {
								pairsToDelete.add(BaseDataObject.toClassIdPair((IPerceroObject) fieldObject));
//								String nextKey = RedisKeyUtils.classIdPair(fieldObject.getClass().getCanonicalName(), ((IPerceroObject)fieldObject).getID());
//								if (cacheDataStore.hasKey(nextKey)) {
//									cacheDataStore.deleteKey(nextKey);
//									// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
//									//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
//								}
							}
							else if (fieldObject instanceof Collection) {
								Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
								while(itrFieldObject.hasNext()) {
									Object nextListObject = itrFieldObject.next();
									if (nextListObject instanceof IPerceroObject) {
										pairsToDelete.add(BaseDataObject.toClassIdPair((IPerceroObject) nextListObject));
//										String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject)nextListObject).getID());
//										if (cacheDataStore.hasKey(nextKey)) {
//											cacheDataStore.deleteKey(nextKey);
//											// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
//											//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
//										}
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
								pairsToDelete.add(BaseDataObject.toClassIdPair((IPerceroObject) fieldObject));
//								String nextKey = RedisKeyUtils.classIdPair(fieldObject.getClass().getCanonicalName(), ((IPerceroObject)fieldObject).getID());
//								if (cacheDataStore.hasKey(nextKey)) {
//									cacheDataStore.deleteKey(nextKey);
//									// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
//									//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
//								}
							}
							else if (fieldObject instanceof Collection) {
								Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
								while(itrFieldObject.hasNext()) {
									Object nextListObject = itrFieldObject.next();
									if (nextListObject instanceof IPerceroObject) {
										pairsToDelete.add(BaseDataObject.toClassIdPair((IPerceroObject) nextListObject));
//										String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject)nextListObject).getID());
//										if (cacheDataStore.hasKey(nextKey)) {
//											cacheDataStore.deleteKey(nextKey);
//											// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
//											//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
//										}
									}
								}
							}
						}
					}
				}
				
				if (!pairsToDelete.isEmpty()) {
					deleteObjectsFromRedisCache(pairsToDelete);
//					cacheDataStore.deleteKeys(pairsToDelete);
					// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
					//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
				}
			}
        } catch (Exception e){
            logger.warn(e.getMessage(), e);
        }
    }
    
    public void handleDeletedObject(IPerceroObject perceroObject, String className, Boolean isShellObject) {
		if (cacheTimeout > 0) {
			try {
				List<ClassIDPair> pairsToDelete = new ArrayList<ClassIDPair>();
	
//				String key = RedisKeyUtils.classIdPair(className, perceroObject.getID());
				pairsToDelete.add(BaseDataObject.toClassIdPair(perceroObject));
				
				Set<ClassIDPair> relatedClassIdPairs = getRelatedCachedClassIdPairs(perceroObject, className, isShellObject);
				Iterator<ClassIDPair> itrRelatedClassIdPairs = relatedClassIdPairs.iterator();
				while (itrRelatedClassIdPairs.hasNext()) {
					ClassIDPair nextRelatedClassIdPair = itrRelatedClassIdPairs.next();
					pairsToDelete.add(nextRelatedClassIdPair);
//					String nextKey = RedisKeyUtils.classIdPair(nextRelatedClassIdPair.getClassName(), nextRelatedClassIdPair.getID());
				}
	
				if (!pairsToDelete.isEmpty()) {
					deleteObjectsFromRedisCache(pairsToDelete);
//					cacheDataStore.deleteKeys(pairsToDelete);
//					// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
//					//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
    }
    
//    public void deleteObjectFromCache(ClassIDPair classIdPair) {
//		String key = RedisKeyUtils.classIdPair(classIdPair.getClassName(), classIdPair.getID());
//		cacheDataStore.deleteKey(key);
//		
//		String classKey = RedisKeyUtils.classIds(classIdPair.getClassName());
//		cacheDataStore.removeSetValue(classKey, classIdPair.getID());
//    }
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private Set<ClassIDPair> getRelatedCachedClassIdPairs(IPerceroObject perceroObject, String className, Boolean isShellObject) throws Exception {
    	Set<ClassIDPair> results = new HashSet<ClassIDPair>();
    	
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(className);

		Iterator<MappedField> itrToManyFields = mappedClass.toManyFields.iterator();
		while(itrToManyFields.hasNext()) {
			MappedField nextMappedField = itrToManyFields.next();
			
//			// If this is a SHELL object, then we need to get ALL objects of this MappedField type.
			// We only care about Cached values, so if this is a shell object, then only retrieve object keys that are in the cache
			if (isShellObject) {
				// If NO reverse mapped field, then nothing to update.
				if (nextMappedField.getReverseMappedField() != null) {
					MappedClass reverseMappedClass = nextMappedField.getReverseMappedField().getMappedClass();
//					IDataProvider reverseDataProvider = DataProviderManager.getInstance().getDataProviderByName(reverseMappedClass.className);

					MappedClass nextReverseMappedClass = reverseMappedClass;
					while (nextReverseMappedClass != null) {
						String nextClassName = nextReverseMappedClass.className;
						String key = RedisKeyUtils.classIds(nextClassName);
						Set<String> classIds = (Set<String>) cacheDataStore.getSetValue(key);
						for(String nextClassId : classIds) {
							results.add(new ClassIDPair(nextClassId, nextClassName));
						}
						nextReverseMappedClass = nextReverseMappedClass.parentMappedClass;
					}
//					Set<ClassIDPair> allClassIdPairs = reverseDataProvider.getAllClassIdPairsByName(reverseMappedClass.className);
//					Iterator<ClassIDPair> itrAllClassIdPairs = allClassIdPairs.iterator();
//					while (itrAllClassIdPairs.hasNext()) {
//						ClassIDPair nextAllClassIdPair = itrAllClassIdPairs.next();
//						results.add(nextAllClassIdPair);
//					}
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
					MappedClass nextReverseMappedClass = reverseMappedClass;
					while (nextReverseMappedClass != null) {
						String nextClassName = nextReverseMappedClass.className;
						String key = RedisKeyUtils.classIds(nextClassName);
						Set<String> classIds = (Set<String>) cacheDataStore.getSetValue(key);
						for(String nextClassId : classIds) {
							results.add(new ClassIDPair(nextClassId, nextClassName));
						}
						nextReverseMappedClass = nextReverseMappedClass.parentMappedClass;
					}
//					IDataProvider reverseDataProvider = DataProviderManager.getInstance().getDataProviderByName(reverseMappedClass.className);
//					Set<ClassIDPair> allClassIdPairs = reverseDataProvider.getAllClassIdPairsByName(reverseMappedClass.className);
//					Iterator<ClassIDPair> itrAllClassIdPairs = allClassIdPairs.iterator();
//					while (itrAllClassIdPairs.hasNext()) {
//						ClassIDPair nextAllClassIdPair = itrAllClassIdPairs.next();
//						results.add(nextAllClassIdPair);
//					}
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

    public void deleteObjectFromCache(ClassIDPair pair) {
    	// Now put the object in the cache.
    	if (cacheTimeout > 0 && pair != null) {
    		String key = RedisKeyUtils.classIdPair(pair.getClassName(), pair.getID());
    		cacheDataStore.deleteKey(key);
    		
    		String classKey = RedisKeyUtils.classIds(pair.getClassName());
    		cacheDataStore.removeSetValue(classKey, pair.getID());
    	}
    }
    
    public void deleteObjectsFromRedisCache(List<ClassIDPair> results) {
    	if (cacheTimeout > 0) {
    		Set<String> objectStrings = new HashSet<String>(results.size());
    		Map<String, Set<String>> mapJsonClassIdStrings = new HashMap<String, Set<String>>();
    		Iterator<ClassIDPair> itrDatabaseObjects = results.iterator();
    		while (itrDatabaseObjects.hasNext()) {
    			ClassIDPair nextDatabaseObject = itrDatabaseObjects.next();
    			String nextCacheKey = RedisKeyUtils.classIdPair(nextDatabaseObject.getClassName(), nextDatabaseObject.getID());
    			objectStrings.add(nextCacheKey);
    			
    			Set<String> classIdList = mapJsonClassIdStrings.get(RedisKeyUtils.classIds(nextDatabaseObject.getClassName()));
    			if (classIdList == null) {
    				classIdList = new HashSet<String>();
    				mapJsonClassIdStrings.put(RedisKeyUtils.classIds(nextDatabaseObject.getClassName()), classIdList);
    			}
    			classIdList.add(nextDatabaseObject.getID());
    		}
    		
    		// Store the objects in redis.
    		cacheDataStore.deleteKeys(objectStrings);
    		// Store the class Id's list in redis.
    		cacheDataStore.removeSetsValues(mapJsonClassIdStrings);
    	}
    }
}

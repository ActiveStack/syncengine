package com.percero.agents.sync.cache;

import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.datastore.RedisDataStore;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
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
    RedisDataStore redisDataStore;

    @Autowired
    Long cacheTimeout = Long.valueOf(60 * 60 * 24 * 14);	// Two weeks

    public void updateCachedObject(IPerceroObject perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields){
        // TODO: Field-level updates could be REALLY useful here.  Would avoid A TON of UNNECESSARY work...
        try {
            if (cacheTimeout > 0) {
                // TODO: Also need to update the caches of anything object that is related to this object.
                String key = RedisKeyUtils.classIdPair(perceroObject.getClass().getCanonicalName(), perceroObject.getID());
                if (redisDataStore.hasKey(key)) {
                    redisDataStore.setValue(key, ((BaseDataObject) perceroObject).toJson());
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
                        redisDataStore.deleteKeys(keysToDelete);
                        // TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
                        //redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
                    }
                } else {
                    // No changedFields?  We should never get here?
                    IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
                    MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
                    Iterator<MappedField> itrToManyFields = mappedClass.toManyFields.iterator();
                    while (itrToManyFields.hasNext()) {
                        MappedField nextMappedField = itrToManyFields.next();
                        Object fieldObject = nextMappedField.getGetter().invoke(perceroObject);
                        if (fieldObject != null) {
                            if (fieldObject instanceof IPerceroObject) {
                                String nextKey = RedisKeyUtils.classIdPair(fieldObject.getClass().getCanonicalName(), ((IPerceroObject) fieldObject).getID());
                                if (redisDataStore.hasKey(nextKey)) {
                                    redisDataStore.deleteKey(nextKey);
                                    // TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
                                    //redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
                                }
                            } else if (fieldObject instanceof Collection) {
                                Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
                                while (itrFieldObject.hasNext()) {
                                    Object nextListObject = itrFieldObject.next();
                                    if (nextListObject instanceof IPerceroObject) {
                                        String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject) nextListObject).getID());
                                        if (redisDataStore.hasKey(nextKey)) {
                                            redisDataStore.deleteKey(nextKey);
                                            // TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
                                            //redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Iterator<MappedField> itrToOneFields = mappedClass.toOneFields.iterator();
                    while (itrToOneFields.hasNext()) {
                        MappedField nextMappedField = itrToOneFields.next();
                        Object fieldObject = nextMappedField.getGetter().invoke(perceroObject);
                        if (fieldObject != null) {
                            if (fieldObject instanceof IPerceroObject) {
                                String nextKey = RedisKeyUtils.classIdPair(fieldObject.getClass().getCanonicalName(), ((IPerceroObject) fieldObject).getID());
                                if (redisDataStore.hasKey(nextKey)) {
                                    redisDataStore.deleteKey(nextKey);
                                    // TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
                                    //redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
                                }
                            } else if (fieldObject instanceof Collection) {
                                Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
                                while (itrFieldObject.hasNext()) {
                                    Object nextListObject = itrFieldObject.next();
                                    if (nextListObject instanceof IPerceroObject) {
                                        String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject) nextListObject).getID());
                                        if (redisDataStore.hasKey(nextKey)) {
                                            redisDataStore.deleteKey(nextKey);
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
}

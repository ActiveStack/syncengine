package com.percero.agents.sync.services;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.PropertyValueException;
import org.hibernate.Query;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.dao.DAORegistry;
import com.percero.agents.sync.dao.IDataAccessObject;
import com.percero.agents.sync.datastore.ICacheDataStore;
import com.percero.agents.sync.exceptions.SyncDataException;
import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.metadata.MappedFieldList;
import com.percero.agents.sync.metadata.MappedFieldPerceroObject;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.ClassIDPairs;
import com.percero.agents.sync.vo.IJsonObject;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;
import com.percero.serial.JsonUtils;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

@Component
public class DAODataProvider implements IDataProvider {

    private static final Logger log = Logger.getLogger(DAODataProvider.class);

    private static DAODataProvider instance = null;

    public static DAODataProvider getInstance() {
        return instance;
    }

    public DAODataProvider() {
        instance = this;
    }

    public void initialize(){
        // Do nothing
    }

    public String getName() {
        return "daoDataProvider";
    }

    @Autowired
    IDataProviderManager dataProviderManager;
    public void setDataProviderManager(IDataProviderManager value) {
        dataProviderManager = value;
    }

    @Autowired
    ICacheDataStore cacheDataStore;

    @Autowired
    Long cacheTimeout = Long.valueOf(60 * 60 * 24 * 14);	// Two weeks

    @Autowired
    ObjectMapper safeObjectMapper;



    @SuppressWarnings({ "unchecked" })
    public PerceroList<IPerceroObject> getAllByName(String className, Integer pageNumber, Integer pageSize, Boolean returnTotal, String userId) throws Exception {
        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(className);
        PerceroList<IPerceroObject> results = dao.getAll(pageNumber, pageSize, returnTotal, userId, false);
        List<IPerceroObject> resultsToCache = new ArrayList<IPerceroObject>();

        if (results != null && !results.isEmpty()) {
            Iterator<? extends IPerceroObject> itrResults = results.iterator();
            while (itrResults.hasNext()) {
                IPerceroObject nextResult = itrResults.next();
                
                // If the object is in the cache, then we can use that instead of querying the database AGAIN for related objects.
                IPerceroObject cachedResult = retrieveCachedObject(BaseDataObject.toClassIdPair(nextResult));
                if (cachedResult != null) {
                	results.set(results.indexOf(nextResult), cachedResult);
                	nextResult = cachedResult;
                    setObjectExpiration(nextResult);
                }
                else {
	                try {
	                    populateToManyRelationships(nextResult, true, null);
	                    populateToOneRelationships(nextResult, true, null);
	                    resultsToCache.add(nextResult);
	                } catch (IllegalArgumentException e) {
	                    throw new SyncDataException(e);
	                } catch (IllegalAccessException e) {
	                    throw new SyncDataException(e);
	                } catch (InvocationTargetException e) {
	                    throw new SyncDataException(e);
	                }
                }
            }
        }

        // We only need to put non-cached results into the cache.
        if (!resultsToCache.isEmpty()) {
        	putObjectsInRedisCache(resultsToCache);
        }

        // Now clean the objects for the user.
        List<IPerceroObject> cleanedObjects = cleanObject(results, userId);
        results.clear();
        results.addAll(cleanedObjects);
        return results;
    }

    @SuppressWarnings("unchecked")
    public Set<ClassIDPair> getAllClassIdPairsByName(String className) throws Exception {
        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(className);
        PerceroList<IPerceroObject> allObjects = dao.getAll(null, null, false, null, true);

        Set<ClassIDPair> results = new HashSet<ClassIDPair>(allObjects == null ? 0 : allObjects.size());

        if (allObjects != null && !allObjects.isEmpty()) {
            Iterator<? extends IPerceroObject> itrResults = allObjects.iterator();
            while (itrResults.hasNext()) {
                IPerceroObject nextResult = itrResults.next();
                results.add(BaseDataObject.toClassIdPair(nextResult));
            }
        }

        return results;
    }

    @SuppressWarnings({ "unchecked" })
    public Integer countAllByName(String className, String userId) throws Exception {
        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(className);
        Integer result = dao.countAll(userId);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Object> runQuery(MappedClass mappedClass, String queryName, Object[] queryArguments, String userId) throws SyncException {
        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(mappedClass.className);
        List<Object> result = dao.runQuery(queryName, queryArguments, userId);
        return result;
    }

    // TODO: This method has not been tested and is most likely broken.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static List<Object> processQueryResults(String resultClassName, Query updateFilter, List updateFilterResult) throws Exception {
        String[] returnAliases = updateFilter.getReturnAliases();
        Type[] returnTypes = updateFilter.getReturnTypes();

        String[] fieldNames = new String[returnAliases.length];
        String[] getMethodNames = new String[returnAliases.length];
        String[] setMethodNames = new String[returnAliases.length];
        Class[] fieldClasses = new Class[returnAliases.length];

        Class clazz = null;
        ClassPool pool = null;
        CtClass evalClass = null;

        if (returnAliases.length > 1) {
            try {
                clazz = MappedClass.forName(resultClassName);
            } catch(Exception e) {
                // Class must not yet exist, so let's create it.
            }

            if (clazz == null) {
                pool = ClassPool.getDefault();
                evalClass = pool.makeClass(resultClassName);
            }

            // Create a new Class based on the result set.
            for(int i = 0; i < returnAliases.length; i++) {
                Type nextType = returnTypes[i];
                String nextTypeCanonicalName = nextType.getReturnedClass().getCanonicalName();
                String nextFieldName = returnAliases[i];
                try {
                    Integer.parseInt(nextFieldName);
                    nextFieldName = "field" + i;
                } catch(NumberFormatException nfe) {
                    // Do nothing. Simply means the field name is not a Number.
                }
                String nextUpperFieldName = nextFieldName.substring(0, 1).toUpperCase() + nextFieldName.substring(1);

                fieldNames[i] = nextFieldName;
                getMethodNames[i] = "get" + nextUpperFieldName;
                setMethodNames[i] = "set" + nextUpperFieldName;
                fieldClasses[i] = nextType.getReturnedClass();

                if (evalClass != null) {
                    evalClass.addField(CtField.make("private " + fieldClasses[i].getCanonicalName() + " " + nextFieldName + ";", evalClass));
                    evalClass.addMethod(CtMethod.make("public void " + setMethodNames[i] + "(" + fieldClasses[i].getCanonicalName() + " value) {this." + nextFieldName + " = value;}", evalClass));
                    evalClass.addMethod(CtMethod.make("public " + nextTypeCanonicalName +" " + getMethodNames[i] + "() {return this." + nextFieldName + ";}", evalClass));
                }
            }

            if (clazz == null && evalClass != null) {
                clazz = evalClass.toClass();
            }
        }

        List results = new ArrayList();

        // Now populate the newly created objects.
        for(Object nextResult : (List<Object[]>)updateFilterResult) {

            if (nextResult instanceof Object[]) {
                Object nextObject = clazz.newInstance();
                for(int i = 0; i < returnAliases.length; i++) {
                    Class[] formalParams = new Class[] { fieldClasses[i] };
                    Method setMethod = clazz.getDeclaredMethod(setMethodNames[i], formalParams);
                    setMethod.invoke(nextObject, ((Object[])nextResult)[i]);
                }

                results.add(nextObject);
            } else
                results.add(nextResult);
        }

        return results;
    }

    public IPerceroObject findById(ClassIDPair classIdPair, String userId) {
        return findById(classIdPair, userId, false);
    }
    public IPerceroObject findById(ClassIDPair classIdPair, String userId, Boolean ignoreCache) {
    	return findById(classIdPair, userId, false, false);
    }
    @SuppressWarnings("unchecked")
    public IPerceroObject findById(ClassIDPair classIdPair, String userId, Boolean ignoreCache, Boolean shellOnly) {

        try {
            IPerceroObject result = null;
            if (!ignoreCache) {
                result = retrieveFromRedisCache(classIdPair, shellOnly);
            }

            if (result == null) {

                IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(classIdPair.getClassName());
                // Retrieve results BEFORE applying access rules so that our cached value represents the full object.
                result = dao.retrieveObject(classIdPair, null, shellOnly);

                // Now put the object in the cache.
                if (result != null) {
                	if (!shellOnly) {
                		// Now need to populate relationships when only a shell object.
	                	populateToManyRelationships(result, true, null);
	                	populateToOneRelationships(result, true, null);
	                	// We don't want to put a shell object in the cache.
	                    putObjectInRedisCache(result, false);
                	}
                }
                else {
                    // Not necessarily a problem but could be helpful when debugging.
                    log.debug("Unable to retrieve object from database: " + classIdPair.toJson());
                }
            }
            else {
                // (Re)Set the expiration.
                setObjectExpiration(result);
            }

            if (!shellOnly) {
            	result = cleanObject(result, userId);
            }

            return result;
        } catch(Exception e) {
            log.error("Unable to findById: "+classIdPair.toJson(), e);
        }

        return null;
    }


    private void putObjectInRedisCache(IPerceroObject perceroObject, boolean onlyIfExists) {
        // Now put the object in the cache.
        if (cacheTimeout > 0 && perceroObject != null) {
            String key = RedisKeyUtils.classIdPair(perceroObject.getClass().getCanonicalName(), perceroObject.getID());
            
            if (!onlyIfExists || cacheDataStore.hasKey(key)) {
	            cacheDataStore.setValue(key, ((BaseDataObject)perceroObject).toJson());
	            setObjectExpiration(key);
	            
	            String classKey = RedisKeyUtils.classIds(perceroObject.getClass().getCanonicalName());
	            cacheDataStore.setSetValue(classKey, perceroObject.getID());
            }
        }
    }

    private void putObjectsInRedisCache(List<? extends IPerceroObject> results) {
        if (cacheTimeout > 0) {
            Map<String, String> mapJsonObjectStrings = new HashMap<String, String>(results.size());
            Map<String, Set<String>> mapJsonClassIdStrings = new HashMap<String, Set<String>>();
            Iterator<? extends IPerceroObject> itrDatabaseObjects = results.iterator();
            while (itrDatabaseObjects.hasNext()) {
                IPerceroObject nextDatabaseObject = itrDatabaseObjects.next();
                String nextCacheKey = RedisKeyUtils.classIdPair(nextDatabaseObject.getClass().getCanonicalName(), nextDatabaseObject.getID());
                
                Set<String> classIdList = mapJsonClassIdStrings.get(RedisKeyUtils.classIds(nextDatabaseObject.getClass().getCanonicalName()));
                if (classIdList == null) {
                	classIdList = new HashSet<String>();
                	mapJsonClassIdStrings.put(RedisKeyUtils.classIds(nextDatabaseObject.getClass().getCanonicalName()), classIdList);
                }
                classIdList.add(nextDatabaseObject.getID());

                mapJsonObjectStrings.put(nextCacheKey, ((BaseDataObject)nextDatabaseObject).toJson());
            }

            // Store the objects in redis.
            cacheDataStore.setValues(mapJsonObjectStrings);
            // Store the class Id's list in redis.
            cacheDataStore.setSetsValues(mapJsonClassIdStrings);
            // (Re)Set the expiration.
            cacheDataStore.expire(mapJsonObjectStrings.keySet(), cacheTimeout, TimeUnit.SECONDS);
        }
    }

    private void deleteObjectFromRedisCache(ClassIDPair pair) {
    	// Now put the object in the cache.
    	if (cacheTimeout > 0 && pair != null) {
    		String key = RedisKeyUtils.classIdPair(pair.getClassName(), pair.getID());
    		cacheDataStore.deleteKey(key);
    		
    		String classKey = RedisKeyUtils.classIds(pair.getClassName());
    		cacheDataStore.removeSetValue(classKey, pair.getID());
    	}
    }
    
    private void deleteObjectsFromRedisCache(List<ClassIDPair> results) {
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
    
    private void setObjectExpiration(IPerceroObject perceroObject) {
    	setObjectExpiration(RedisKeyUtils.classIdPair(perceroObject.getClass().getCanonicalName(), perceroObject.getID()));
    }

    private void setObjectExpiration(String key) {
        // (Re)Set the expiration.
        if (cacheTimeout > 0 && key != null) {
            cacheDataStore.expire(key, cacheTimeout, TimeUnit.SECONDS);
        }
    }

    @Override
    public IPerceroObject retrieveCachedObject(ClassIDPair classIdPair) throws Exception {
    	return retrieveFromRedisCache(classIdPair, false);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private IPerceroObject retrieveFromRedisCache(ClassIDPair classIdPair, boolean shellOnly) throws Exception {
        IPerceroObject result = null;
        if (cacheTimeout > 0) {
            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            Class theClass = MappedClass.forName(classIdPair.getClassName());
            MappedClass mc = mcm.getMappedClassByClassName(classIdPair.getClassName());

            String key = RedisKeyUtils.classIdPair(classIdPair.getClassName(), classIdPair.getID());
            // If we are only retrieving the shell object, then we really only care if the key exists.
            if (shellOnly) {
            	if (cacheDataStore.hasKey(key)) {
                    result = (IPerceroObject) theClass.newInstance();
                    result.setID(classIdPair.getID());
            	}
	            else {
	                // Check MappedClass' child classes.
	                Iterator<MappedClass> itrChildMappedClasses = mc.childMappedClasses.iterator();
	                while (itrChildMappedClasses.hasNext()) {
	                    MappedClass nextChildMc = itrChildMappedClasses.next();
	                    key = RedisKeyUtils.classIdPair(nextChildMc.className, classIdPair.getID());
	                    if (cacheDataStore.hasKey(key)) {
	                    	result = (IPerceroObject) nextChildMc.clazz.newInstance();
	                    	result.setID(classIdPair.getID());
	                        break;
	                    }
	                }
	            }
            }
            else {
	            String jsonObjectString = (String) cacheDataStore.getValue(key);
	            if (jsonObjectString != null) {
	            	result = createFromJson(jsonObjectString, theClass);
	            }
	            else {
	                // Check MappedClass' child classes.
	                Iterator<MappedClass> itrChildMappedClasses = mc.childMappedClasses.iterator();
	                while (itrChildMappedClasses.hasNext()) {
	                    MappedClass nextChildMc = itrChildMappedClasses.next();
	                    key = RedisKeyUtils.classIdPair(nextChildMc.className, classIdPair.getID());
	                    jsonObjectString = (String) cacheDataStore.getValue(key);
	                    if (jsonObjectString != null) {
	                    	result = createFromJson(jsonObjectString, nextChildMc.clazz);
	                        break;
	                    }
	                }
	            }
            }
        }

        if (result instanceof BaseDataObject) {
            ((BaseDataObject) result).setDataSource(BaseDataObject.DATA_SOURCE_CACHE);
        }
        return result;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	protected IPerceroObject createFromJson(String jsonObjectString, Class clazz) throws InstantiationException, IllegalAccessException, JsonParseException, JsonMappingException, IOException {
        if (JsonUtils.isClassAssignableFromIJsonObject(clazz)) {
            IJsonObject jsonObject = (IJsonObject) clazz.newInstance();
            jsonObject.fromJson(jsonObjectString);
            return (IPerceroObject) jsonObject;
        }
        else {
            return (IPerceroObject) safeObjectMapper.readValue(jsonObjectString, clazz);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map<String, IPerceroObject> retrieveFromRedisCache(ClassIDPairs classIdPairs, Boolean pleaseSetTimeout) throws Exception {
        Map<String, IPerceroObject> result = new HashMap<String, IPerceroObject>(classIdPairs.getIds().size());

        if (cacheTimeout > 0) {
            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            MappedClass mc = mcm.getMappedClassByClassName(classIdPairs.getClassName());

            Map<String, Class> keys = new HashMap<String, Class>(classIdPairs.getIds().size());
            Iterator<String> itrIds = classIdPairs.getIds().iterator();
            while (itrIds.hasNext()) {
                String nextId = itrIds.next();
                String nextKey = RedisKeyUtils.classIdPair(classIdPairs.getClassName(), nextId);
                keys.put(nextKey, mc.clazz);

                // Check MappedClass' child classes.
                Iterator<MappedClass> itrChildMappedClasses = mc.childMappedClasses.iterator();
                while (itrChildMappedClasses.hasNext()) {
                    MappedClass nextChildMc = itrChildMappedClasses.next();
                    if (nextChildMc.clazz == BaseDataObject.class) {
                        // Reached the top level, so break.
                        break;
                    }
                    nextKey = RedisKeyUtils.classIdPair(nextChildMc.className, nextId);
                    keys.put(nextKey, nextChildMc.clazz);
                }
            }
            
            for(Entry<String, Class> nextKeyClass : keys.entrySet()) {
                String jsonObjectString = (String) cacheDataStore.getValue(nextKeyClass.getKey());
                if (jsonObjectString != null) {
                	IPerceroObject nextPerceroObject = createFromJson(jsonObjectString, nextKeyClass.getValue());
                	((BaseDataObject) nextPerceroObject).setDataSource(BaseDataObject.DATA_SOURCE_CACHE);
                	result.put(nextPerceroObject.getID(), nextPerceroObject);
                }
            }

            if (pleaseSetTimeout) {
                cacheDataStore.expire(keys.keySet(), cacheTimeout, TimeUnit.SECONDS);
            }
        }

        return result;
    }

    @SuppressWarnings({ })
    public Boolean getReadAccess(ClassIDPair classIdPair, String userId) {
        IDataAccessObject<? extends IPerceroObject> dao = DAORegistry.getInstance().getDataAccessObject(classIdPair.getClassName());
        return dao.hasReadAccess(classIdPair, userId);
    }

    @SuppressWarnings({ })
    public Boolean getDeleteAccess(ClassIDPair classIdPair, String userId) {
        IDataAccessObject<? extends IPerceroObject> dao = DAORegistry.getInstance().getDataAccessObject(classIdPair.getClassName());
        return dao.hasDeleteAccess(classIdPair, userId);
    }

    public List<IPerceroObject> findByIds(ClassIDPairs classIdPairs, String userId) {
        return findByIds(classIdPairs, userId, false);
    }
    @SuppressWarnings({ })
    public List<IPerceroObject> findByIds(ClassIDPairs classIdPairs, String userId, Boolean ignoreCache) {
        IDataAccessObject<? extends IPerceroObject> dao = DAORegistry.getInstance().getDataAccessObject(classIdPairs.getClassName());
        List<IPerceroObject> results = new ArrayList<IPerceroObject>();

        try {
            // Copy the ClassIDPairs to find object since we remove any of the
            // ID's from the list that we find in the cache.
            ClassIDPairs classIdPairsCopy = new ClassIDPairs();
            classIdPairsCopy.setClassName(classIdPairs.getClassName());
            List<String> idsToFind = new ArrayList<String>(classIdPairs.getIds().size());
            idsToFind.addAll(classIdPairs.getIds());
            classIdPairsCopy.setIds(idsToFind);

            Map<String, IPerceroObject> cachedResults = null;
            if (!ignoreCache) {
                cachedResults = retrieveFromRedisCache(classIdPairs, true);
                if (cachedResults != null &&!cachedResults.isEmpty()) {
                    // Add the cached results

                    Iterator<IPerceroObject> itrCachedResults = cachedResults.values().iterator();
                    while (itrCachedResults.hasNext()) {
                        IPerceroObject nextCachedResult = itrCachedResults.next();
                        if (nextCachedResult != null) {
                            idsToFind.remove(nextCachedResult.getID());
                            results.add(nextCachedResult);
                            setObjectExpiration(nextCachedResult);
                        }
                    }
                }
            }

            List<? extends IPerceroObject> daoObjects = null;
            if (classIdPairsCopy.getIds() != null && !classIdPairsCopy.getIds().isEmpty()) {
                daoObjects = dao.retrieveObjects(classIdPairsCopy, userId, false);

                for(IPerceroObject nextResult : daoObjects) {
                    populateToManyRelationships(nextResult, true, null);
                    populateToOneRelationships(nextResult, true, null);
                }

                putObjectsInRedisCache(daoObjects);	// Only need to put objects in cache that were not already found in cache.
                results.addAll(daoObjects);
            }

            // Now clean the objects for the user.
            results = cleanObject(results, userId);

        } catch(Exception e) {
            log.error(e);
        }

        return results;
    }


    @SuppressWarnings("unchecked")
    public List<IPerceroObject> findByExample(IPerceroObject theQueryObject, List<String> excludeProperties, String userId, Boolean shellOnly) throws SyncException {
        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(theQueryObject.getClass().getCanonicalName());
        List<IPerceroObject> results = dao.findByExample(theQueryObject, excludeProperties, userId, shellOnly);
        List<IPerceroObject> resultsToCache = new ArrayList<IPerceroObject>();

        if (results != null && !results.isEmpty()) {
            Iterator<? extends IPerceroObject> itrResults = results.iterator();
            while (itrResults.hasNext()) {
                IPerceroObject nextResult = itrResults.next();

            	if (!shellOnly) {
            		try {
		                // If the object is in the cache, then we can use that instead of querying the database AGAIN for related objects.
		                IPerceroObject cachedResult = retrieveCachedObject(BaseDataObject.toClassIdPair(nextResult));
		                if (cachedResult != null) {
		                	results.set(results.indexOf(nextResult), cachedResult);
		                	nextResult = cachedResult;
		                    setObjectExpiration(nextResult);
		                }
		                else {
		                    populateToManyRelationships(nextResult, true, null);
		                    populateToOneRelationships(nextResult, true, null);
		                    
		                    resultsToCache.add(nextResult);
		                }
            		} catch (IllegalArgumentException e) {
            			throw new SyncDataException(e);
            		} catch (IllegalAccessException e) {
            			throw new SyncDataException(e);
            		} catch (InvocationTargetException e) {
            			throw new SyncDataException(e);
            		} catch (Exception e) {
            			throw new SyncDataException(e);
					}
            	}
            }
        }

        if (!shellOnly) {
        	if (!resultsToCache.isEmpty()) {
        		putObjectsInRedisCache(resultsToCache);
        	}
        	
        	// Now clean the objects for the user.
        	results = cleanObject(results, userId);
        }

        return results;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends IPerceroObject> T createObject(T perceroObject, String userId) throws SyncException {

        try {
            IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(perceroObject.getClass().getCanonicalName());
            // Make sure object has an ID.
            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
            if (!mappedClass.hasGeneratedId && !StringUtils.hasText(perceroObject.getID()))
                perceroObject.setID(UUID.randomUUID().toString());
            else {
                // Check to see if item already exists.
            	try {
					// We want to see if this object already exists, we will
					// check the cache first, then the database.
					// We search without a UserID in the case that the object
					// exists, but the user does NOT have access to
					// it. Then we clean the object after retrieval (if it has
					// been found)
	                IPerceroObject existingObject = findById(BaseDataObject.toClassIdPair(perceroObject), null);
	                if (existingObject != null) {
	                	return (T) cleanObject(existingObject, userId);
	                }
            	} catch( Exception e) {
            		log.debug("Error retrieving object on create", e);
            	}
            }
            
			// The incoming perceroObject will have all of it's source
			// relationships filled (by definition, it can't have any target
			// relationships yet since it is a new object).

            IPerceroObject createdPerceroObject = (T) dao.createObject(perceroObject, userId);
            if (createdPerceroObject == null) {
            	// User must not have permission to create the object.
                return null;
            }
            
            // Reset all the relationships in the case that dao.createObject removed them from the object.
            overwriteToManyRelationships(createdPerceroObject, perceroObject);
            overwriteToOneRelationships(createdPerceroObject, perceroObject);

            // Now update the cache.
            if (cacheTimeout > 0) {
            	putObjectInRedisCache(createdPerceroObject, false);
            	
				// For each source related object, we need to handle the update
				// by updating the cache. We only care about source mapped fields
            	// because those are the only ones that are possible to be present
            	// here since this is a new object.
            	for(MappedFieldPerceroObject nextSourceMappedField : mappedClass.getSourceMappedFields()) {
            		if (nextSourceMappedField.getReverseMappedField() != null) {
            			IPerceroObject relatedObject = (IPerceroObject) nextSourceMappedField.getValue(createdPerceroObject);
            			if (relatedObject != null) {
            				Collection<MappedField> reverseMappedFields = new ArrayList<MappedField>(1);
            				reverseMappedFields.add(nextSourceMappedField.getReverseMappedField());
            				handleUpdatedClassIdPair(createdPerceroObject, BaseDataObject.toClassIdPair(relatedObject), reverseMappedFields, userId);
            			}
            		}
            	}
            }

            return (T) cleanObject(perceroObject, userId);
        }
        catch(PropertyValueException pve) {
            log.error("Error creating object", pve);

            SyncDataException sde = new SyncDataException(SyncDataException.MISSING_REQUIRED_FIELD, SyncDataException.MISSING_REQUIRED_FIELD_CODE, "Missing required field " + pve.getPropertyName());
            sde.fieldName = pve.getPropertyName();
            throw sde;
        }
        catch(Exception e) {
            log.error("Error creating object", e);

            SyncDataException sde = new SyncDataException(SyncDataException.CREATE_OBJECT_ERROR, SyncDataException.CREATE_OBJECT_ERROR_CODE);
            throw sde;
        }
    }


    ////////////////////////////////////////////////////
    //	PUT
    ////////////////////////////////////////////////////
    @SuppressWarnings("unchecked")
    public <T extends IPerceroObject> T putObject(T perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields, String userId) throws SyncException {
        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(perceroObject.getClass().getCanonicalName());
        IPerceroObject updateResultObject = (T) dao.updateObject(perceroObject, changedFields, userId);
        
        if (updateResultObject == null) {
        	// The update failed, so we can return here.
        	return null;
        }

        try {
        	// Overwrite the related objects.  No need to go to the database since these could NOT have changed in this update.
        	overwriteToManyRelationships(updateResultObject, perceroObject);
        	overwriteToOneRelationships(updateResultObject, perceroObject);
        } catch (IllegalArgumentException e) {
            throw new SyncDataException(SyncDataException.UPDATE_OBJECT_ERROR, SyncDataException.UPDATE_OBJECT_ERROR_CODE, e);
        } catch (IllegalAccessException e) {
            throw new SyncException(SyncDataException.UPDATE_OBJECT_ERROR, SyncDataException.UPDATE_OBJECT_ERROR_CODE, e);
        } catch (InvocationTargetException e) {
            throw new SyncException(SyncDataException.UPDATE_OBJECT_ERROR, SyncDataException.UPDATE_OBJECT_ERROR_CODE, e);
        }
        // Now update the cache.
        if (cacheTimeout > 0) {
            // TODO: Also need to update the caches of anything object that is related to this object.
        	putObjectInRedisCache(updateResultObject, true);

            List<ClassIDPair> cachePairsToUpdate = new ArrayList<ClassIDPair>();
            // Iterate through each changed object and reset the cache for that object.
            if (changedFields != null) {
            	Iterator<Entry<ClassIDPair, Collection<MappedField>>> itrChangedFieldEntrySet = changedFields.entrySet().iterator();
            	while (itrChangedFieldEntrySet.hasNext()) {
            		Entry<ClassIDPair, Collection<MappedField>> nextEntry = itrChangedFieldEntrySet.next();
        			ClassIDPair thePair = nextEntry.getKey();
        			Collection<MappedField> mappedFields = nextEntry.getValue();
					handleUpdatedClassIdPair(updateResultObject, thePair, mappedFields, userId);
            	}
            }
            else {
            	log.error("No Changed fields when updating object " + perceroObject.getClass().getCanonicalName() + "::" + perceroObject.getID());
                // No changedFields?  We should never get here?
                IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
                MappedClass mappedClass = mcm.getMappedClassByClassName(updateResultObject.getClass().getName());
                Iterator<MappedField> itrToManyFields = mappedClass.toManyFields.iterator();
                while(itrToManyFields.hasNext()) {
                    MappedField nextMappedField = itrToManyFields.next();
                    Object fieldObject = null;
                    try {
                        fieldObject = nextMappedField.getGetter().invoke(updateResultObject);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    if (fieldObject != null) {
                        if (fieldObject instanceof IPerceroObject) {
                        	cachePairsToUpdate.add(BaseDataObject.toClassIdPair((IPerceroObject) fieldObject));
//                            String nextKey = RedisKeyUtils.classIdPair(fieldObject.getClass().getCanonicalName(), ((IPerceroObject)fieldObject).getID());
//                            if (cacheDataStore.hasKey(nextKey)) {
//                                cacheDataStore.deleteKey(nextKey);
//                                // TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
//                                //redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
//                            }
                        }
                        else if (fieldObject instanceof Collection) {
                            Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
                            while(itrFieldObject.hasNext()) {
                                Object nextListObject = itrFieldObject.next();
                                if (nextListObject instanceof IPerceroObject) {
                                	cachePairsToUpdate.add(BaseDataObject.toClassIdPair((IPerceroObject) nextListObject));
//                                    String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject)nextListObject).getID());
//                                    if (cacheDataStore.hasKey(nextKey)) {
//                                        cacheDataStore.deleteKey(nextKey);
//                                        // TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
//                                        //redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
//                                    }
                                }
                            }
                        }
                    }
                }
                Iterator<MappedFieldPerceroObject> itrToOneFields = mappedClass.toOneFields.iterator();
                while(itrToOneFields.hasNext()) {
                    MappedFieldPerceroObject nextMappedField = itrToOneFields.next();
                    Object fieldObject = null;
                    try {
                        fieldObject = nextMappedField.getGetter().invoke(updateResultObject);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    if (fieldObject != null) {
                        if (fieldObject instanceof IPerceroObject) {
                        	cachePairsToUpdate.add(BaseDataObject.toClassIdPair((IPerceroObject) fieldObject));
//                            String nextKey = RedisKeyUtils.classIdPair(fieldObject.getClass().getCanonicalName(), ((IPerceroObject)fieldObject).getID());
//                            if (cacheDataStore.hasKey(nextKey)) {
//                                cacheDataStore.deleteKey(nextKey);
//                                // TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
//                                //redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
//                            }
                        }
                        else if (fieldObject instanceof Collection) {
                            Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
                            while(itrFieldObject.hasNext()) {
                                Object nextListObject = itrFieldObject.next();
                                if (nextListObject instanceof IPerceroObject) {
                                	cachePairsToUpdate.add(BaseDataObject.toClassIdPair((IPerceroObject) nextListObject));
//                                    String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject)nextListObject).getID());
//                                    if (cacheDataStore.hasKey(nextKey)) {
//                                        cacheDataStore.deleteKey(nextKey);
//                                        // TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
//                                        //redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
//                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!cachePairsToUpdate.isEmpty()) {
                // TODO: Re-fetch the object here and update the key
            	for(ClassIDPair classIdPair : cachePairsToUpdate) {
            		IPerceroObject nextUpdatedCachedObject;
					try {
						nextUpdatedCachedObject = findById(classIdPair, null, true);
						if (nextUpdatedCachedObject != null) {
							String nextKey = RedisKeyUtils.classIdPair(nextUpdatedCachedObject.getClass().getCanonicalName(), nextUpdatedCachedObject.getID());
							// TODO: Actually update the cached object.
							cacheDataStore.setValue(nextKey, ((BaseDataObject)nextUpdatedCachedObject).toJson());
						}
					} catch (Exception e) {
						log.error("Error retrieving " + classIdPair.toString() + " from the cache", e);
					}
            	}
            }
        }

        return (T) cleanObject(updateResultObject, userId);
    }

	/**
	 * @param perceroObject
	 * @param originalUpdatedObject
	 * @param perceroIdPair
	 * @param theRelatedObjectClassIdPair
	 * @param mappedFields
	 * @param userId
	 */
	protected <T extends IPerceroObject> void handleUpdatedClassIdPair(T originalUpdatedObject, ClassIDPair theRelatedObjectClassIdPair,
			Collection<MappedField> mappedFields, String userId) {

		// Make sure that theRelatedClassIdPair is not the same as the original object
		if (!theRelatedObjectClassIdPair.comparePerceroObject(originalUpdatedObject)) {
			// This should contain at least one field, otherwise nothing as changed (most likely a one-way relationship).
			if (mappedFields != null && !mappedFields.isEmpty()) {
				IPerceroObject nextUpdatedCachedObject;
				try {
					nextUpdatedCachedObject = retrieveCachedObject(theRelatedObjectClassIdPair);
					boolean cachedObjectUpdated = false;

					// If the related object is NOT in the cache, then we having nothing to do.
					if (nextUpdatedCachedObject != null) {
						for(MappedField nextMappedField : mappedFields) {
				        	try {
				        		if (nextMappedField.getMappedClass().toManyFields.contains(nextMappedField)) {
									if (handleUpdatedClassIdPair_ToManyMappedField(
											originalUpdatedObject, nextUpdatedCachedObject, nextMappedField, userId)) {
										cachedObjectUpdated = true;
									}
				        		}
				        		else if (nextMappedField.getMappedClass().toOneFields.contains(nextMappedField)) {
									if (handleUpdatedClassIdPair_ToOneMappedField(
											originalUpdatedObject, nextUpdatedCachedObject, nextMappedField, userId)) {
										cachedObjectUpdated = true;
									}
				        		}
				        	} catch(Exception e) {
				        		log.error("Unable to retrieve related objects for " + originalUpdatedObject.getClass().getCanonicalName() + "::" + nextMappedField.getField().getName());
				        	}
						}
						
						if (cachedObjectUpdated) {
							// Update the cached object.
							putObjectInRedisCache(nextUpdatedCachedObject, false);
						}
					}
				} catch (Exception e) {
					log.error("Error updating related object " + theRelatedObjectClassIdPair.toJson(), e);
				}
			}
		}
	}

	/**
	 * @param originalUpdatedObject
	 * @param currentRelatedObject
	 * @param relatedMappedField
	 * @param userId
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws SyncException
	 */
	@SuppressWarnings("unchecked")
	private <T extends IPerceroObject> boolean handleUpdatedClassIdPair_ToManyMappedField(T originalUpdatedObject,
			IPerceroObject currentRelatedObject, MappedField relatedMappedField, String userId)
					throws IllegalAccessException, InvocationTargetException, SyncException {
		boolean cachedObjectUpdated = false;

		IPerceroObject newRelatedObject = (IPerceroObject) (relatedMappedField.getReverseMappedField() != null ? relatedMappedField.getReverseMappedField().getGetter().invoke(originalUpdatedObject) : null);
		if (newRelatedObject != null) {
			List<IPerceroObject> updatedList = (List<IPerceroObject>) relatedMappedField.getGetter().invoke(currentRelatedObject);
			if (updatedList == null) {
				updatedList = new ArrayList<IPerceroObject>();
				relatedMappedField.getSetter().invoke(currentRelatedObject, updatedList);
			}

			// Find the index of the original object in this related mapped list, if it exists.
			int collectionIndex = findPerceroObjectInList(originalUpdatedObject,
					updatedList);
			
			if (BaseDataObject.toClassIdPair(currentRelatedObject).comparePerceroObject(newRelatedObject)) {
				// The perceroObject has been ADDED to the list.
				if (collectionIndex < 0) {
					// Only add the perceroObject to the Collection if it is NOT already there.
		            updatedList.add(originalUpdatedObject);
		            cachedObjectUpdated = true;
				}
			}
			else {
				// The perceroObject has been REMOVED from the list.
				if (collectionIndex >= 0){
					updatedList.remove(collectionIndex);
					cachedObjectUpdated = true;
				}
			}
		}
		else {
			// We are unable to get a hold of the reverse mapped field, so we default back to the underlying data store.
		    List<IPerceroObject> allRelatedObjects = findAllRelatedObjects(currentRelatedObject, relatedMappedField, true, userId);
		    relatedMappedField.getSetter().invoke(currentRelatedObject, allRelatedObjects);
		    cachedObjectUpdated = true;
		}
		return cachedObjectUpdated;
	}

	/**
	 * @param originalUpdatedObject
	 * @param currentRelatedObject
	 * @param relatedMappedField
	 * @param userId
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws SyncException
	 */
	private <T extends IPerceroObject> boolean handleUpdatedClassIdPair_ToOneMappedField(T originalUpdatedObject,
			IPerceroObject currentRelatedObject, MappedField relatedMappedField, String userId)
					throws IllegalAccessException, InvocationTargetException, SyncException {

		boolean cachedObjectUpdated = false;

		IPerceroObject newRelatedObject = (IPerceroObject) (relatedMappedField.getReverseMappedField() != null ? relatedMappedField.getReverseMappedField().getGetter().invoke(originalUpdatedObject) : null);
		if (newRelatedObject != null) {
			IPerceroObject oldUpdatedObject = (IPerceroObject) relatedMappedField.getGetter().invoke(currentRelatedObject);

			if (BaseDataObject.toClassIdPair(currentRelatedObject).comparePerceroObject(newRelatedObject)) {
				// If the newRelatedPerceroObject is the same as the
				// currentRelatedPerceroObject, then we know that this is the NEW
				// related object.
				
				// Only add the perceroObject to the Collection if it is NOT already there.
				if (!BaseDataObject.toClassIdPair(originalUpdatedObject).comparePerceroObject(oldUpdatedObject)) {
//					oldRelatedObject.add(originalUpdatedObject);
					relatedMappedField.getSetter().invoke(currentRelatedObject, originalUpdatedObject);
					cachedObjectUpdated = true;
				}
			}
			else {
				// Else, we know that this is the OLD related object.
				// The perceroObject has been REMOVED from the list.
				if (oldUpdatedObject != null) {
//					oldRelatedObject.remove(collectionIndex);
					boolean isAccessible = relatedMappedField.getField().isAccessible();
					if (!isAccessible) {
						relatedMappedField.getField().setAccessible(true);
					}
					relatedMappedField.getField().set(currentRelatedObject, null);
					if (!isAccessible) {
						relatedMappedField.getField().setAccessible(false);
					}
					cachedObjectUpdated = true;
				}
			}
		}
		else {
			// We are unable to get a hold of the reverse mapped field, so we default back to the underlying data store.
			// We need to go to the dataProvider for the related object's class and ask for it.
			// Though this returns a List, we expect there to be only one result in the list.
			List<IPerceroObject> allRelatedObjects = findAllRelatedObjects(currentRelatedObject, relatedMappedField, true, userId);
			IPerceroObject relatedPerceroObject = null;
			if (allRelatedObjects != null && !allRelatedObjects.isEmpty()) {
			    relatedPerceroObject = allRelatedObjects.get(0);
			}
			relatedMappedField.getSetter().invoke(currentRelatedObject, relatedPerceroObject);
			cachedObjectUpdated = true;
		}
		return cachedObjectUpdated;
//		
//		boolean cachedObjectUpdated;
//		// We need to go to the dataProvider for the related object's class and ask for it.
//		// Though this returns a List, we expect there to be only one result in the list.
//		List<IPerceroObject> allRelatedObjects = findAllRelatedObjects(currentRelatedObject, relatedMappedField, true, userId);
//		IPerceroObject relatedPerceroObject = null;
//		if (allRelatedObjects != null && !allRelatedObjects.isEmpty()) {
//		    relatedPerceroObject = allRelatedObjects.get(0);
//		}
//		relatedMappedField.getSetter().invoke(currentRelatedObject, relatedPerceroObject);
//		cachedObjectUpdated = true;
//		return cachedObjectUpdated;
	}

	/**
	 * @param originalUpdatedObject
	 * @param updatedList
	 * @return
	 */
	protected <T extends IPerceroObject> int findPerceroObjectInList(T originalUpdatedObject,
			List<IPerceroObject> updatedList) {
		int index = 0;
		Iterator<IPerceroObject> itrUpdatedCollection = updatedList.iterator();
		ClassIDPair perceroIdPair = BaseDataObject.toClassIdPair(originalUpdatedObject);

		while (itrUpdatedCollection.hasNext()) {
			IPerceroObject nextCollectionObject = itrUpdatedCollection.next();
			if (perceroIdPair.comparePerceroObject(nextCollectionObject)) {
				return index;
			}
			index++;
		}
		
		return -1;
	}


    ////////////////////////////////////////////////////
    //	DELETE
    ////////////////////////////////////////////////////
    @SuppressWarnings({ "unchecked" })
    public Boolean deleteObject(ClassIDPair theClassIdPair, String userId) throws SyncException {
    	
    	if (theClassIdPair == null || !StringUtils.hasText(theClassIdPair.getID())) {
    		// Invalid object.
    		return false;
    	}

        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(theClassIdPair.getClassName());
        IPerceroObject originalDeletedObject = dao.retrieveObject(theClassIdPair, null, false);	// Retrieve the full object so we can update the cache if the delete is successful.
        
        if (originalDeletedObject == null) {
        	// If the object already does NOT exist, then there is nothing to do.
        	return true;
        }
        
        Boolean result = dao.deleteObject(theClassIdPair, userId);

        try {
            MappedClass mappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(originalDeletedObject.getClass().getCanonicalName());
            if (mappedClass == null) {
                log.warn("Missing MappedClass for " + originalDeletedObject.getClass().getCanonicalName());
                throw new SyncException(SyncException.MISSING_MAPPED_CLASS_ERROR, SyncException.MISSING_MAPPED_CLASS_ERROR_CODE);
            }

            // Now delete from cache.
            // Now update the cache.
            // TODO: Field-level updates could be REALLY useful here.  Would avoid A TON of UNNECESSARY work...
            if (result && cacheTimeout > 0) {
                Iterator<MappedField> itrToManyFields = mappedClass.toManyFields.iterator();
                while(itrToManyFields.hasNext()) {
                	handleDeletedClassIDPair_MappedField(originalDeletedObject, itrToManyFields.next());
                }
                Iterator<MappedFieldPerceroObject> itrToOneFields = mappedClass.toOneFields.iterator();
                while(itrToOneFields.hasNext()) {
                	handleDeletedClassIDPair_MappedField(originalDeletedObject, itrToOneFields.next());
                }

                deleteObjectFromRedisCache(BaseDataObject.toClassIdPair(originalDeletedObject));
            }

        } catch(Exception e) {
            throw new SyncDataException(SyncDataException.DELETE_OBJECT_ERROR, SyncDataException.DELETE_OBJECT_ERROR_CODE, e);
        }

        return result;
    }

	/**
	 * @param originalDeletedObject
	 * @param itrToOneFields
	 * @return
	 * @throws Exception 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean handleDeletedClassIDPair_MappedField(IPerceroObject originalDeletedObject,
			MappedField mappedField)
					throws Exception {
		
		if (mappedField == null) {
			return false;
		}
		if (mappedField.getReverseMappedField() == null) {
			// Nothing to do since the related object does not know about the relationship.
			return true;
		}
		
		Object fieldObject = mappedField.getGetter().invoke(originalDeletedObject);

		// We only need to update this related object(s) if it is set.
		if (fieldObject != null) {
		    if (fieldObject instanceof IPerceroObject) {
		    	fieldObject = retrieveCachedObject(BaseDataObject.toClassIdPair((IPerceroObject) fieldObject));
				Object theRelatedObject = mappedField.getReverseMappedField().getGetter().invoke(fieldObject);
				if (theRelatedObject != null) {
					if (theRelatedObject instanceof IPerceroObject) {
						// Since the value is set, un-set it and save back to to the data store.
						mappedField.getReverseMappedField().setToNull((IPerceroObject) fieldObject);
						putObjectInRedisCache((IPerceroObject) fieldObject, true);
					}
					else if (theRelatedObject instanceof List) {
						int i = 0;
						for(IPerceroObject nextRelatedObject : (List<IPerceroObject>) theRelatedObject) {
							if (BaseDataObject.toClassIdPair(originalDeletedObject).comparePerceroObject(nextRelatedObject)) {
								((List<IPerceroObject>) theRelatedObject).remove(i);
								break;
							}
							i++;
						}
						putObjectInRedisCache((IPerceroObject) fieldObject, true);
					}
				}
		    }
		    else if (fieldObject instanceof Collection) {
		        Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
		        while(itrFieldObject.hasNext()) {
		            Object nextListObject = itrFieldObject.next();
		            if (nextListObject instanceof IPerceroObject) {
				    	nextListObject = retrieveCachedObject(BaseDataObject.toClassIdPair((IPerceroObject) nextListObject));
		        		IPerceroObject theRelatedObject = (IPerceroObject) mappedField.getReverseMappedField().getGetter().invoke(nextListObject);
		        		if (theRelatedObject != null) {
		        			// Since the value is set, un-set it and save back to to the data store.
		        			mappedField.getReverseMappedField().setToNull((IPerceroObject) nextListObject);
							putObjectInRedisCache((IPerceroObject) nextListObject, true);
		        		}
		            }
		        }
		    }
		}
		return true;
	}


    
    ////////////////////////////////////////////////////
    //	CLEAN
    ////////////////////////////////////////////////////
    @SuppressWarnings("unchecked")
    public IPerceroObject cleanObject(IPerceroObject perceroObject, String userId) throws SyncException {
        if(perceroObject == null) return null;
        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(perceroObject.getClass().getCanonicalName());
        perceroObject = dao.cleanObjectForUser(perceroObject, userId);
        return perceroObject;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<IPerceroObject> cleanObject(List<IPerceroObject> perceroObjects, String userId) throws SyncException {
        try {
            List<IPerceroObject> results = new ArrayList<IPerceroObject>(perceroObjects.size());

            Map<Class, List<IPerceroObject>> classPairs = new HashMap<Class, List<IPerceroObject>>();

            Iterator<? extends IPerceroObject> itrPerceroObjects = perceroObjects.iterator();
            while (itrPerceroObjects.hasNext()) {
                IPerceroObject nextPerceroObject = itrPerceroObjects.next();
                if ( ((BaseDataObject)nextPerceroObject).getIsClean()) {
                    results.add(nextPerceroObject);
                }
                else {
                    List<IPerceroObject> classObjects = classPairs.get(nextPerceroObject.getClass());
                    if (classObjects == null) {
                        classObjects = new ArrayList<IPerceroObject>();
                        classPairs.put(nextPerceroObject.getClass(), classObjects);
                    }
                    classObjects.add(nextPerceroObject);
                }
            }

            Iterator<Entry<Class, List<IPerceroObject>>> itrClassPairs = classPairs.entrySet().iterator();
            while (itrClassPairs.hasNext()) {
                Entry<Class, List<IPerceroObject>> nextEntrySet = itrClassPairs.next();
                IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(nextEntrySet.getKey().getCanonicalName());

                List<IPerceroObject> nextClassObjects = nextEntrySet.getValue();
                if (nextClassObjects != null && !nextClassObjects.isEmpty()) {
                    Iterator<IPerceroObject> itrClassObjects = nextClassObjects.iterator();
                    while (itrClassObjects.hasNext()) {
                        IPerceroObject nextObject = itrClassObjects.next();
                        IPerceroObject nextResult = dao.cleanObjectForUser(nextObject, userId);
                        if (nextResult != null) {
                            results.add(nextResult);
                        }
                    }
                }
            }

            return results;
        } catch(Exception e) {
            throw new SyncException(e);
        }
    }

    public Map<ClassIDPair, Collection<MappedField>> getChangedMappedFields(IPerceroObject newObject) {
    	return getChangedMappedFields(newObject, false);
    }

    public Map<ClassIDPair, Collection<MappedField>> getChangedMappedFields(IPerceroObject newObject, boolean ignoreCache) {
        String className = newObject.getClass().getCanonicalName();
        IPerceroObject oldObject = findById(new ClassIDPair(newObject.getID(), className), null, ignoreCache);

        return getChangedMappedFields(oldObject, newObject, ignoreCache);
    }
    
    @SuppressWarnings({ "unchecked" })
    public Map<ClassIDPair, Collection<MappedField>> getChangedMappedFields(IPerceroObject oldObject, IPerceroObject newObject, boolean ignoreCache) {
        Map<ClassIDPair, Collection<MappedField>> result = new HashMap<ClassIDPair, Collection<MappedField>>();
        Collection<MappedField> baseObjectResult = null;
        ClassIDPair basePair = new ClassIDPair(newObject.getID(), newObject.getClass().getCanonicalName());

        String className = newObject.getClass().getCanonicalName();
        IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
        MappedClass mc = mcm.getMappedClassByClassName(className);

        Iterator<MappedField> itrMappedFields = mc.externalizableFields.iterator();
        while (itrMappedFields.hasNext()) {
            MappedField nextMappedField = itrMappedFields.next();
            try {
                Boolean fieldIsEqual = nextMappedField.compareObjects(oldObject, newObject);
                if (!fieldIsEqual) {
                    if (baseObjectResult == null) {
                        baseObjectResult = new HashSet<MappedField>();
                        result.put(basePair, baseObjectResult);
                    }
                    baseObjectResult.add(nextMappedField);

                    // If this is a relationship field, then need to grab the old and new values.
                    if (nextMappedField.getReverseMappedField() != null) {
                        if (nextMappedField instanceof MappedFieldPerceroObject) {
                            MappedFieldPerceroObject nextMappedFieldPerceroObject = (MappedFieldPerceroObject) nextMappedField;

                            IPerceroObject oldReversePerceroObject = (IPerceroObject) nextMappedFieldPerceroObject.getGetter().invoke(oldObject);
                            if (oldReversePerceroObject != null && StringUtils.hasText(oldReversePerceroObject.getID())) {
                                ClassIDPair oldReversePair = new ClassIDPair(oldReversePerceroObject.getID(), oldReversePerceroObject.getClass().getCanonicalName());
                                Collection<MappedField> oldReverseChangedFields = result.get(oldReversePair);
                                if (oldReverseChangedFields == null) {
                                    oldReverseChangedFields = new HashSet<MappedField>();
                                    result.put(oldReversePair, oldReverseChangedFields);
                                }
                                oldReverseChangedFields.add(nextMappedField.getReverseMappedField());
                            }

                            IPerceroObject newReversePerceroObject = (IPerceroObject) nextMappedFieldPerceroObject.getGetter().invoke(newObject);
                            if (newReversePerceroObject != null && StringUtils.hasText(newReversePerceroObject.getID())) {
                                ClassIDPair newReversePair = new ClassIDPair(newReversePerceroObject.getID(), newReversePerceroObject.getClass().getCanonicalName());
                                Collection<MappedField> changedFields = result.get(newReversePair);
                                if (changedFields == null) {
                                    changedFields = new HashSet<MappedField>();
                                    result.put(newReversePair, changedFields);
                                }
                                changedFields.add(nextMappedField.getReverseMappedField());
                            }
                        }
                        else if (nextMappedField instanceof MappedFieldList) {
                            MappedFieldList nextMappedFieldList = (MappedFieldList) nextMappedField;

                            List<IPerceroObject> oldReverseList = (List<IPerceroObject>) nextMappedFieldList.getGetter().invoke(oldObject);
                            if (oldReverseList == null)
                                oldReverseList = new ArrayList<IPerceroObject>();

                            List<IPerceroObject> newReverseList = (List<IPerceroObject>) nextMappedFieldList.getGetter().invoke(newObject);
                            if (newReverseList == null)
                                newReverseList = new ArrayList<IPerceroObject>();

                            // Compare each item in the lists.
                            Collection<ClassIDPair> oldChangedList = retrieveObjectsNotInCollection(oldReverseList, newReverseList);
                            Iterator<ClassIDPair> itrOldChangedList = oldChangedList.iterator();
                            while (itrOldChangedList.hasNext()) {
                            	ClassIDPair nextOldReversePair = itrOldChangedList.next();

                                // Old object is not in new list, so add to list of changed fields.
                                Collection<MappedField> changedFields = result.get(nextOldReversePair);
                                if (changedFields == null) {
                                    changedFields = new HashSet<MappedField>();
                                    result.put(nextOldReversePair, changedFields);
                                }
                                changedFields.add(nextMappedField.getReverseMappedField());
                            }

                            Collection<ClassIDPair> newChangedList = retrieveObjectsNotInCollection(newReverseList, oldReverseList);
                            Iterator<ClassIDPair> itrNewChangedList = newChangedList.iterator();
                            while (itrNewChangedList.hasNext()) {
                            	ClassIDPair nextNewReversePair = itrNewChangedList.next();

                                // Old object is not in new list, so add to list of changed fields.
                                Collection<MappedField> changedFields = result.get(nextNewReversePair);
                                if (changedFields == null) {
                                    changedFields = new HashSet<MappedField>();
                                    result.put(nextNewReversePair, changedFields);
                                }
                                changedFields.add(nextMappedField.getReverseMappedField());
                            }
                        }
                    }
                }
            } catch(Exception e) {
                log.warn("Error getting changed field: " + nextMappedField.getField().getName(), e);
                baseObjectResult.add(nextMappedField);
            }
        }

        return result;
    }

    private Collection<ClassIDPair> retrieveObjectsNotInCollection(Collection<IPerceroObject> baseList, Collection<IPerceroObject> compareToList) {
        Collection<ClassIDPair> result = new HashSet<ClassIDPair>();
        Iterator<IPerceroObject> itrBaseList = baseList.iterator();
        Iterator<IPerceroObject> itrCompareToList = null;
        boolean matchFound = false;

        while (itrBaseList.hasNext()) {
            IPerceroObject nextBasePerceroObject = itrBaseList.next();
            if (nextBasePerceroObject == null || !StringUtils.hasText(nextBasePerceroObject.getID())) {
            	continue;
            }

            itrCompareToList = compareToList.iterator();
            matchFound = false;
            while (itrCompareToList.hasNext()) {
                IPerceroObject nextCompareToPerceroObject = itrCompareToList.next();

				if (nextCompareToPerceroObject != null
						&& nextBasePerceroObject != null
						&& nextBasePerceroObject.getClass() == nextCompareToPerceroObject
								.getClass()
						&& nextBasePerceroObject.getID().equalsIgnoreCase(
								nextCompareToPerceroObject.getID())) {
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) {
            	ClassIDPair nextBasePair = BaseDataObject.toClassIdPair(nextBasePerceroObject);
                result.add(nextBasePair);
            }
        }

        return result;
    }

	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.services.IDataProvider#findAllRelatedObjects(com.percero.framework.vo.IPerceroObject, com.percero.agents.sync.metadata.MappedField, java.lang.Boolean, java.lang.String)
	 */

    //	@Override
    @SuppressWarnings("unchecked")
    public List<IPerceroObject> findAllRelatedObjects(IPerceroObject perceroObject, MappedField mappedField, Boolean shellOnly, String userId) throws SyncException {
        List<IPerceroObject> result = new ArrayList<IPerceroObject>();

        if (!StringUtils.hasText(perceroObject.getID())) {
            // No valid ID on the object, so can't search for it.
            return result;
        }

        if (mappedField.getMappedClass().getSourceMappedFields().contains(mappedField)) {
            // This object is the source.
            IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(mappedField.getMappedClass().className);
            IPerceroObject thisObject = dao.retrieveObject(BaseDataObject.toClassIdPair(perceroObject), userId, false);
            IPerceroObject relatedObject;
            try {
                relatedObject = (IPerceroObject) mappedField.getGetter().invoke(thisObject);
            } catch (Exception e) {
                throw new SyncException(e);
            }
            if (relatedObject != null) {
                if (!shellOnly) {
                    MappedClass relatedMappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(relatedObject.getClass().getCanonicalName());
                    relatedObject = relatedMappedClass.getDataProvider().findById(BaseDataObject.toClassIdPair(relatedObject), userId);
                }
                result.add(relatedObject);
            }
            result.add(relatedObject);
        }
        else {
            // This object is the target.
            // The reverse mapped field should be the MappedField on the target object, the one that this MUST be the data provider for.
            MappedField reverseMappedField = mappedField.getReverseMappedField();
            if (reverseMappedField == null) {
                // No reverse mapped field, meaning there is nothing to do.
                return result;
            }

            IDataProvider dataProvider = reverseMappedField.getMappedClass().getDataProvider();
            result = dataProvider.getAllByRelationship(reverseMappedField, BaseDataObject.toClassIdPair(perceroObject), shellOnly, userId);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<IPerceroObject> getAllByRelationship(MappedField mappedField, ClassIDPair targetClassIdPair, Boolean shellOnly, String userId) throws SyncException {
        if (mappedField.getMappedClass().getSourceMappedFields().contains(mappedField)) {
            // This object is the source.
            IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(mappedField.getMappedClass().className);
            return dao.retrieveAllByRelationship(mappedField, targetClassIdPair, shellOnly, userId);
        }
        else {
            // This object is the target.
            if (mappedField.getReverseMappedField() != null) {
                IDataProvider dataProvider = mappedField.getReverseMappedField().getMappedClass().getDataProvider();
                try {
                    IPerceroObject targetObject = (IPerceroObject) Class.forName(targetClassIdPair.getClassName()).newInstance();
                    return dataProvider.findAllRelatedObjects(targetObject, mappedField.getReverseMappedField(), shellOnly, userId);
                } catch(Exception e) {
                    throw new SyncException(e);
                }
            }
            else {
                return new ArrayList<IPerceroObject>(0);
            }
        }


    }


    protected boolean isMappedFieldInChangedFields(IPerceroObject perceroObject, MappedField nextMappedField, Map<ClassIDPair, Collection<MappedField>> changedFields) {
        Iterator<Entry<ClassIDPair, Collection<MappedField>>> itrChangedFields = changedFields.entrySet().iterator();
        while (itrChangedFields.hasNext()) {
            Entry<ClassIDPair, Collection<MappedField>> nextChangedFieldEntry = itrChangedFields.next();

            if (!nextChangedFieldEntry.getKey().comparePerceroObject(perceroObject)) {
                // Not the same object.
                continue;
            }

            for(MappedField nextChangedField : nextChangedFieldEntry.getValue()) {
                if ( StringUtils.hasText(nextMappedField.getColumnName()) && nextMappedField.getColumnName().equalsIgnoreCase(nextChangedField.getColumnName()) ) {
                    // We have found our field.
                    return true;
                }
                else if ( StringUtils.hasText(nextMappedField.getJoinColumnName()) && nextMappedField.getJoinColumnName().equalsIgnoreCase(nextChangedField.getJoinColumnName()) ) {
                    // We have found our field.
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Populates all the *-TO-MANY relationships on the specified object.  All TO-MANY relationships have their
     * relationship data stored on the other side of the relationship, so we have to go those dataProvider and
     * ask for the objects.
     *
     * @param perceroObject
     * @param userId
     * @throws SyncException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void populateToManyRelationships(IPerceroObject perceroObject, Boolean shellOnly,
                                            String userId) throws SyncException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        if (perceroObject == null || !StringUtils.hasText(perceroObject.getID())) {
            // Invalid object.
            log.warn("Invalid object in populateToManyRelationships");
            return;
        }

        MappedClass mappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(perceroObject.getClass().getCanonicalName());
        for(MappedField nextToManyMappedField : mappedClass.toManyFields) {

            // TODO: Take into account Access Rights.
        	try {
	            List<IPerceroObject> allRelatedObjects = findAllRelatedObjects(perceroObject, nextToManyMappedField, shellOnly, userId);
	            nextToManyMappedField.getSetter().invoke(perceroObject, allRelatedObjects);
        	} catch(Exception e) {
        		log.error("Unable to retrieve related objects for " + perceroObject.getClass().getCanonicalName() + "::" + nextToManyMappedField.getField().getName());
        	}
        }
    }
    
    protected void overwriteToManyRelationships(IPerceroObject perceroObject, IPerceroObject sourceObject) {
        if (perceroObject == null || sourceObject == null) {
            // Invalid object.
            log.warn("Invalid object in overwriteToManyRelationships");
            return;
        }

        MappedClass mappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(perceroObject.getClass().getCanonicalName());
        for(MappedField nextToManyMappedField : mappedClass.toManyFields) {

        	try {
	            nextToManyMappedField.getSetter().invoke(perceroObject, nextToManyMappedField.getGetter().invoke(sourceObject));
        	} catch(Exception e) {
        		log.error("Unable to retrieve related objects for " + perceroObject.getClass().getCanonicalName() + "::" + nextToManyMappedField.getField().getName());
        	}
        }
    }

    public void populateToOneRelationships(IPerceroObject perceroObject, Boolean shellOnly,
                                           String userId) throws SyncException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        if (perceroObject == null || !StringUtils.hasText(perceroObject.getID())) {
            // Invalid object.
            log.warn("Invalid object in populateToOneRelationships");
            return;
        }

        MappedClass mappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(perceroObject.getClass().getCanonicalName());
        for(MappedFieldPerceroObject nextToOneMappedField : mappedClass.toOneFields) {
            // If perceroObject is the "owner" of this relationship, then we have all the data necessary here.
            if (nextToOneMappedField.isSourceEntity()) {
//				if (!shellOnly || true) {
//					// We want more than a Shell object, so ask the dataProvider of the mappedField for that object.
//					IDataProvider dataProvider = nextToOneMappedField.getReverseMappedField().getMappedClass().getDataProvider();
//					IPerceroObject relatedPerceroObject = nextToOneMappedField.getPerceroObjectValue(perceroObject);
//					relatedPerceroObject = dataProvider.systemGetById(BaseDataObject.toClassIdPair(relatedPerceroObject));
//					nextToOneMappedField.getSetter().invoke(perceroObject, relatedPerceroObject);
//					
//					if (!((BaseDataObject)perceroObject).getIsClean()) {
//						((BaseDataObject)perceroObject).setIsClean(true);
//						dataProvider.populateToManyRelationships(relatedPerceroObject, false, userId);
//						dataProvider.populateToOneRelationships(relatedPerceroObject, false, userId);
//					}
//					
//					// Make sure the reverse relationship is mapped.
//					if (nextToOneMappedField.getReverseMappedField() != null) {
//						if (nextToOneMappedField.getReverseMappedField() instanceof MappedFieldPerceroObject) {
//							// OneToOne, so just set reverse value.
//							nextToOneMappedField.getReverseMappedField().getSetter().invoke(relatedPerceroObject, perceroObject);
//						}
//						else {
//							// OneToMany/ManyToOne, so need to swap out of list.
//							List<IPerceroObject> reverseList = (List<IPerceroObject>) nextToOneMappedField.getReverseMappedField().getValue(relatedPerceroObject);
//							if (reverseList == null) {
//								reverseList = new ArrayList<IPerceroObject>();
//								nextToOneMappedField.getReverseMappedField().getSetter().invoke(relatedPerceroObject, reverseList);
//								reverseList.add(perceroObject);
//							}
//							else {
//								boolean objectFound = false;
//								for(int i=0; i<reverseList.size(); i++) {
//									IPerceroObject nextListObject = reverseList.get(i);
//									if (perceroObject.getID().equalsIgnoreCase(nextListObject.getID()) && perceroObject.getClass().getCanonicalName().equals(nextListObject.getClass().getCanonicalName())) {
//										// We found our object, now swap it out.
//										reverseList.remove(i);
//										reverseList.add(i, perceroObject);
//										
//										objectFound = true;
//										break;
//									}
//								}
//								
//								if (!objectFound) {
//									reverseList.add(perceroObject);
//								}
//							}
//						}
//					}
//				}
            }
            else {
                // We need to go to the dataProvider for the related object's class and ask for it.
                // Though this returns a List, we expect there to be only one result in the list.
                List<IPerceroObject> allRelatedObjects = findAllRelatedObjects(perceroObject, nextToOneMappedField, shellOnly, userId);
                IPerceroObject relatedPerceroObject = null;
                if (allRelatedObjects != null && !allRelatedObjects.isEmpty()) {
                    relatedPerceroObject = allRelatedObjects.get(0);
                }
                nextToOneMappedField.getSetter().invoke(perceroObject, relatedPerceroObject);
            }
        }
    }
    
    protected void overwriteToOneRelationships(IPerceroObject perceroObject, IPerceroObject sourceObject) throws SyncException, IllegalArgumentException,
    IllegalAccessException, InvocationTargetException {
    	if (perceroObject == null || sourceObject == null) {
    		// Invalid object.
    		log.warn("Invalid object in overwriteToOneRelationships");
    		return;
    	}
    	
    	MappedClass mappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(perceroObject.getClass().getCanonicalName());
    	for(MappedFieldPerceroObject nextToOneMappedField : mappedClass.toOneFields) {
    		// If perceroObject is the "owner" of this relationship, then we have all the data necessary here.
    		if (!nextToOneMappedField.isSourceEntity()) {
    			nextToOneMappedField.getSetter().invoke(perceroObject, nextToOneMappedField.getGetter().invoke(sourceObject));
    		}
    	}
    }

}

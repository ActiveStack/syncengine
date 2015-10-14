package com.percero.agents.sync.services;

import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.dao.DAORegistry;
import com.percero.agents.sync.dao.IDataAccessObject;
import com.percero.agents.sync.datastore.ICacheDataStore;
import com.percero.agents.sync.exceptions.SyncDataException;
import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.metadata.*;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.ClassIDPairs;
import com.percero.agents.sync.vo.IJsonObject;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.PropertyValueException;
import org.hibernate.Query;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

@Component
public class DAODataProvider implements IDataProvider {

    // TODO: Better manage Hibernate Sessions (opening and closing).

    private static final Logger log = Logger.getLogger(DAODataProvider.class);

    private static DAODataProvider instance = null;


    public static DAODataProvider getInstance() {
        return instance;
    }

    public DAODataProvider() {
        instance = this;
    }

    @PostConstruct
    public void init()
    {
        dataProviderManager.addDataProvider(this);
        ((DataProviderManager)dataProviderManager).setDefaultDataProvider(this);
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
    // TODO: @Transactional(readOnly=true)
    public PerceroList<IPerceroObject> getAllByName(String className, Integer pageNumber, Integer pageSize, Boolean returnTotal, String userId) throws Exception {
        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(className);
        PerceroList<IPerceroObject> results = dao.getAll(pageNumber, pageSize, returnTotal, userId, false);

        if (results != null && !results.isEmpty()) {
            Iterator<? extends IPerceroObject> itrResults = results.iterator();
            while (itrResults.hasNext()) {
                IPerceroObject nextResult = itrResults.next();
                try {
                    populateToManyRelationships(nextResult, true, null);
                    populateToOneRelationships(nextResult, true, null);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    throw new SyncDataException(e);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    throw new SyncDataException(e);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    throw new SyncDataException(e);
                }
            }
        }

        putObjectsInRedisCache(results);

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
    // TODO: @Transactional(readOnly=true)
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
    @SuppressWarnings("unchecked")
    public IPerceroObject findById(ClassIDPair classIdPair, String userId, Boolean ignoreCache) {

        try {
            IPerceroObject result = null;
            if (!ignoreCache) {
                result = retrieveFromRedisCache(classIdPair);
            }

            if (result == null) {

                IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(classIdPair.getClassName());
                // Retrieve results BEFORE applying access rules so that our cached value represents the full object.
                result = dao.retrieveObject(classIdPair, null, false);

                // Now put the object in the cache.
                if (result != null) {
                	populateToManyRelationships(result, true, null);
                	populateToOneRelationships(result, true, null);
                    putObjectInRedisCache(result);
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

            result = cleanObject(result, userId);

            return result;
        } catch(Exception e) {
            log.error("Unable to findById: "+classIdPair.toJson(), e);
        }

        return null;
    }


    private void putObjectInRedisCache(IPerceroObject perceroObject) {
        // Now put the object in the cache.
        if (cacheTimeout > 0 && perceroObject != null) {
            String key = RedisKeyUtils.classIdPair(perceroObject.getClass().getCanonicalName(), perceroObject.getID());
            cacheDataStore.setValue(key, ((BaseDataObject)perceroObject).toJson());
            setObjectExpiration(key);
        }
    }

    private void putObjectsInRedisCache(List<? extends IPerceroObject> results) {
        if (cacheTimeout > 0) {
            Map<String, String> mapJsonObjectStrings = new HashMap<String, String>(results.size());
            Iterator<? extends IPerceroObject> itrDatabaseObjects = results.iterator();
            while (itrDatabaseObjects.hasNext()) {
                IPerceroObject nextDatabaseObject = itrDatabaseObjects.next();
                String nextCacheKey = RedisKeyUtils.classIdPair(nextDatabaseObject.getClass().getCanonicalName(), nextDatabaseObject.getID());

                mapJsonObjectStrings.put(nextCacheKey, ((BaseDataObject)nextDatabaseObject).toJson());
            }

            // Store the objects in redis.
            cacheDataStore.setValues(mapJsonObjectStrings);
            // (Re)Set the expiration.
            cacheDataStore.expire(mapJsonObjectStrings.keySet(), cacheTimeout, TimeUnit.SECONDS);
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
    	return retrieveFromRedisCache(classIdPair);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private IPerceroObject retrieveFromRedisCache(ClassIDPair classIdPair) throws Exception {
        IPerceroObject result = null;
        if (cacheTimeout > 0) {
            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            Class theClass = MappedClass.forName(classIdPair.getClassName());
            MappedClass mc = mcm.getMappedClassByClassName(classIdPair.getClassName());

            String key = RedisKeyUtils.classIdPair(classIdPair.getClassName(), classIdPair.getID());
            String jsonObjectString = (String) cacheDataStore.getValue(key);
            if (jsonObjectString != null) {
                if (IJsonObject.class.isAssignableFrom(theClass)) {
                    IJsonObject jsonObject = (IJsonObject) theClass.newInstance();
                    jsonObject.fromJson(jsonObjectString);
                    result = (IPerceroObject) jsonObject;
                }
                else {
                    result = (IPerceroObject) safeObjectMapper.readValue(jsonObjectString, theClass);
                }
            }
            else {
                // Check MappedClass' child classes.
                Iterator<MappedClass> itrChildMappedClasses = mc.childMappedClasses.iterator();
                while (itrChildMappedClasses.hasNext()) {
                    MappedClass nextChildMc = itrChildMappedClasses.next();
                    key = RedisKeyUtils.classIdPair(nextChildMc.className, classIdPair.getID());
                    jsonObjectString = (String) cacheDataStore.getValue(key);
                    if (jsonObjectString != null) {
                        result = (IPerceroObject) safeObjectMapper.readValue(jsonObjectString, theClass);
                        return result;
                    }
                }
            }
        }

        return result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map<String, IPerceroObject> retrieveFromRedisCache(ClassIDPairs classIdPairs, Boolean pleaseSetTimeout) throws Exception {
        Map<String, IPerceroObject> result = new HashMap<String, IPerceroObject>(classIdPairs.getIds().size());

        if (cacheTimeout > 0) {
            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            Class theClass = MappedClass.forName(classIdPairs.getClassName());
            MappedClass mc = mcm.getMappedClassByClassName(classIdPairs.getClassName());

            Set<String> keys = new HashSet<String>(classIdPairs.getIds().size());
            Iterator<String> itrIds = classIdPairs.getIds().iterator();
            while (itrIds.hasNext()) {
                String nextId = itrIds.next();
                String nextKey = RedisKeyUtils.classIdPair(classIdPairs.getClassName(), nextId);
                keys.add(nextKey);

                // Check MappedClass' child classes.
                Iterator<MappedClass> itrChildMappedClasses = mc.childMappedClasses.iterator();
                while (itrChildMappedClasses.hasNext()) {
                    MappedClass nextChildMc = itrChildMappedClasses.next();
                    if (nextChildMc.clazz == BaseDataObject.class) {
                        // Reached the top level, so break.
                        break;
                    }
                    nextKey = RedisKeyUtils.classIdPair(nextChildMc.className, nextId);
                    keys.add(nextKey);
                }
            }

//			String key = RedisKeyUtils.classIdPair(classIdPair.getClassName(), classIdPair.getID());
            List<Object> jsonObjectStrings = cacheDataStore.getValues(keys);
            Iterator<Object> itrJsonObjectStrings = jsonObjectStrings.iterator();
            while (itrJsonObjectStrings.hasNext()) {
                String jsonObjectString = (String) itrJsonObjectStrings.next();
                if (jsonObjectString != null) {
                    if (IJsonObject.class.isAssignableFrom(theClass)) {
                        IJsonObject jsonObject = (IJsonObject) theClass.newInstance();
                        jsonObject.fromJson(jsonObjectString);
                        result.put( (((IPerceroObject) jsonObject).getID()), (IPerceroObject) jsonObject );
                    }
                    else {
                        IPerceroObject nextPerceroObject = (IPerceroObject) safeObjectMapper.readValue(jsonObjectString, theClass);
                        result.put( nextPerceroObject.getID(), nextPerceroObject);
                    }

                }
//				else {
//					// Check MappedClass' child classes.
//					Iterator<MappedClass> itrChildMappedClasses = mc.childMappedClasses.iterator();
//					while (itrChildMappedClasses.hasNext()) {
//						MappedClass nextChildMc = itrChildMappedClasses.next();
//						key = RedisKeyUtils.classIdPair(nextChildMc.className, classIdPair.getID());
//						jsonObjectString = (String) redisDataStore.getValue(key);
//						if (jsonObjectString != null) {
//							result = (IPerceroObject) safeObjectMapper.readValue(jsonObjectString, theClass);
//							return result;
//						}
//					}
//				}
            }

            if (pleaseSetTimeout) {
                cacheDataStore.expire(keys, cacheTimeout, TimeUnit.SECONDS);
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
            e.printStackTrace();
        }

        return results;
    }


    @SuppressWarnings("unchecked")
    public List<IPerceroObject> findByExample(IPerceroObject theQueryObject, List<String> excludeProperties, String userId, Boolean shellOnly) throws SyncException {
        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(theQueryObject.getClass().getCanonicalName());
        List<IPerceroObject> results = dao.findByExample(theQueryObject, excludeProperties, userId, shellOnly);

        if (results != null && !results.isEmpty()) {
            Iterator<? extends IPerceroObject> itrResults = results.iterator();
            while (itrResults.hasNext()) {
                IPerceroObject nextResult = itrResults.next();
                try {
                    populateToManyRelationships(nextResult, true, null);
                    populateToOneRelationships(nextResult, true, null);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    throw new SyncDataException(e);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    throw new SyncDataException(e);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    throw new SyncDataException(e);
                }
            }
        }

        putObjectsInRedisCache(results);

        // Now clean the objects for the user.
        results = cleanObject(results, userId);

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
                IPerceroObject existingObject = dao.retrieveObject(BaseDataObject.toClassIdPair(perceroObject), null, false);
                if (existingObject != null)
                {
                    populateToManyRelationships(perceroObject, true, null);
                    populateToOneRelationships(perceroObject, true, null);
                    return (T) cleanObject(perceroObject, userId);
                }
            }

            perceroObject = (T) dao.createObject(perceroObject, userId);
            if (perceroObject == null) {
                return perceroObject;
            }
            populateToManyRelationships(perceroObject, true, null);
            populateToOneRelationships(perceroObject, true, null);

            // Now update the cache.
            // TODO: Field-level updates could be REALLY useful here.  Would avoid A TON of UNNECESSARY work...
            if (cacheTimeout > 0) {
                String key = RedisKeyUtils.classIdPair(perceroObject.getClass().getCanonicalName(), perceroObject.getID());
                cacheDataStore.setValue(key, ((BaseDataObject)perceroObject).toJson());
                cacheDataStore.expire(key, cacheTimeout, TimeUnit.SECONDS);

                Set<String> keysToDelete = new HashSet<String>();
                MappedClass nextMappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
                Iterator<MappedField> itrToManyFields = nextMappedClass.toManyFields.iterator();
                while(itrToManyFields.hasNext()) {
                    MappedField nextMappedField = itrToManyFields.next();
                    Object fieldObject = nextMappedField.getGetter().invoke(perceroObject);
                    if (fieldObject != null) {
                        if (fieldObject instanceof IPerceroObject) {
                            String nextKey = RedisKeyUtils.classIdPair(fieldObject.getClass().getCanonicalName(), ((IPerceroObject)fieldObject).getID());
                            keysToDelete.add(nextKey);
                        }
                        else if (fieldObject instanceof Collection) {
                            Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
                            while(itrFieldObject.hasNext()) {
                                Object nextListObject = itrFieldObject.next();
                                if (nextListObject instanceof IPerceroObject) {
                                    String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject)nextListObject).getID());
                                    keysToDelete.add(nextKey);
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
                            keysToDelete.add(nextKey);
                        }
                        else if (fieldObject instanceof Collection) {
                            Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
                            while(itrFieldObject.hasNext()) {
                                Object nextListObject = itrFieldObject.next();
                                if (nextListObject instanceof IPerceroObject) {
                                    String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject)nextListObject).getID());
                                    keysToDelete.add(nextKey);
                                }
                            }
                        }
                    }
                }

                if (!keysToDelete.isEmpty()) {
                    cacheDataStore.deleteKeys(keysToDelete);
                    // TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
                    //redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
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
        perceroObject = (T) dao.updateObject(perceroObject, changedFields, userId);

        try {
            populateToManyRelationships(perceroObject, true, null);
            populateToOneRelationships(perceroObject, true, null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new SyncException(e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new SyncException(e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new SyncException(e);
        }
        // Now update the cache.
        if (cacheTimeout > 0) {
            // TODO: Also need to update the caches of anything object that is related to this object.
            String key = RedisKeyUtils.classIdPair(perceroObject.getClass().getCanonicalName(), perceroObject.getID());
            if (cacheDataStore.hasKey(key)) {
                cacheDataStore.setValue(key, ((BaseDataObject)perceroObject).toJson());
            }

            // Iterate through each changed object and reset the cache for that object.
            if (changedFields != null) {
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
                    Object fieldObject = null;
                    try {
                        fieldObject = nextMappedField.getGetter().invoke(perceroObject);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
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
                    Object fieldObject = null;
                    try {
                        fieldObject = nextMappedField.getGetter().invoke(perceroObject);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
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

        return (T) cleanObject(perceroObject, userId);
    }


    ////////////////////////////////////////////////////
    //	DELETE
    ////////////////////////////////////////////////////
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Boolean deleteObject(ClassIDPair theClassIdPair, String userId) throws SyncException {

        IDataAccessObject<IPerceroObject> dao = (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(theClassIdPair.getClassName());
        IPerceroObject perceroObject = dao.retrieveObject(theClassIdPair, null, false);	// Retrieve the full object so we can update the cache if the delete is successful.
        Boolean result = dao.deleteObject(theClassIdPair, userId);

        try {
            MappedClass mappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(perceroObject.getClass().getCanonicalName());
            if (mappedClass == null) {
                log.warn("Missing MappedClass for " + perceroObject.getClass().getCanonicalName());
                throw new SyncException(SyncException.MISSING_MAPPED_CLASS_ERROR, SyncException.MISSING_MAPPED_CLASS_ERROR_CODE );
            }

            // Now delete from cache.
            // Now update the cache.
            // TODO: Field-level updates could be REALLY useful here.  Would avoid A TON of UNNECESSARY work...
            if (result && cacheTimeout > 0) {
                Set<String> keysToDelete = new HashSet<String>();

                String key = RedisKeyUtils.classIdPair(perceroObject.getClass().getCanonicalName(), perceroObject.getID());
                keysToDelete.add(key);

                Iterator<MappedField> itrToManyFields = mappedClass.toManyFields.iterator();
                while(itrToManyFields.hasNext()) {
                    MappedField nextMappedField = itrToManyFields.next();
                    Object fieldObject = nextMappedField.getGetter().invoke(perceroObject);
                    if (fieldObject != null) {
                        if (fieldObject instanceof IPerceroObject) {
                            String nextKey = RedisKeyUtils.classIdPair(fieldObject.getClass().getCanonicalName(), ((IPerceroObject)fieldObject).getID());
                            keysToDelete.add(nextKey);
                        }
                        else if (fieldObject instanceof Collection) {
                            Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
                            while(itrFieldObject.hasNext()) {
                                Object nextListObject = itrFieldObject.next();
                                if (nextListObject instanceof IPerceroObject) {
                                    String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject)nextListObject).getID());
                                    keysToDelete.add(nextKey);
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
                            keysToDelete.add(nextKey);
                        }
                        else if (fieldObject instanceof Collection) {
                            Iterator<Object> itrFieldObject = ((Collection) fieldObject).iterator();
                            while(itrFieldObject.hasNext()) {
                                Object nextListObject = itrFieldObject.next();
                                if (nextListObject instanceof IPerceroObject) {
                                    String nextKey = RedisKeyUtils.classIdPair(nextListObject.getClass().getCanonicalName(), ((IPerceroObject)nextListObject).getID());
                                    keysToDelete.add(nextKey);
                                }
                            }
                        }
                    }
                }

                if (!keysToDelete.isEmpty()) {
                    cacheDataStore.deleteKeys(keysToDelete);
                }
            }

        } catch(Exception e) {
            if (perceroObject != null) {
                log.error("Unable to delete record from database: " + perceroObject.getClass().getCanonicalName() + ":" + perceroObject.getID(), e);
            }
            else {
                log.error("Unable to delete record from database: NULL Object", e);
            }
            throw new SyncDataException(SyncDataException.DELETE_OBJECT_ERROR, SyncDataException.DELETE_OBJECT_ERROR_CODE);
        }

        return result;
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

        return getChangedMappedFields(oldObject, newObject);
    }
    
    @SuppressWarnings("rawtypes")
    public Map<ClassIDPair, Collection<MappedField>> getChangedMappedFields(IPerceroObject oldObject, IPerceroObject newObject) {
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
                            if (oldReversePerceroObject != null) {
                                ClassIDPair oldReversePair = new ClassIDPair(oldReversePerceroObject.getID(), oldReversePerceroObject.getClass().getCanonicalName());
                                Collection<MappedField> oldReverseChangedFields = result.get(oldReversePair);
                                if (oldReverseChangedFields == null) {
                                    oldReverseChangedFields = new HashSet<MappedField>();
                                    result.put(oldReversePair, oldReverseChangedFields);
                                }
                                oldReverseChangedFields.add(nextMappedField.getReverseMappedField());
                            }

                            IPerceroObject newReversePerceroObject = (IPerceroObject) nextMappedFieldPerceroObject.getGetter().invoke(newObject);
                            if (newReversePerceroObject != null) {
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

                            List oldReverseList = (List) nextMappedFieldList.getGetter().invoke(oldObject);
                            if (oldReverseList == null)
                                oldReverseList = new ArrayList();

                            List newReverseList = (List) nextMappedFieldList.getGetter().invoke(newObject);
                            if (newReverseList == null)
                                newReverseList = new ArrayList();

                            // Compare each item in the lists.
                            Collection oldChangedList = retrieveObjectsNotInCollection(oldReverseList, newReverseList);
                            Iterator itrOldChangedList = oldChangedList.iterator();
                            while (itrOldChangedList.hasNext()) {
                                BaseDataObject nextOldChangedObject = (BaseDataObject) itrOldChangedList.next();
                                ClassIDPair nextOldReversePair = BaseDataObject.toClassIdPair(nextOldChangedObject);

                                // Old object is not in new list, so add to list of changed fields.
                                Collection<MappedField> changedFields = result.get(nextOldReversePair);
                                if (changedFields == null) {
                                    changedFields = new HashSet<MappedField>();
                                    result.put(nextOldReversePair, changedFields);
                                }
                                changedFields.add(nextMappedField.getReverseMappedField());
                            }

                            Collection newChangedList = retrieveObjectsNotInCollection(newReverseList, oldReverseList);
                            Iterator itrNewChangedList = newChangedList.iterator();
                            while (itrNewChangedList.hasNext()) {
                                BaseDataObject nextNewChangedObject = (BaseDataObject) itrNewChangedList.next();
                                ClassIDPair nextNewReversePair = BaseDataObject.toClassIdPair(nextNewChangedObject);

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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Collection retrieveObjectsNotInCollection(Collection baseList, Collection compareToList) {
        Collection result = new HashSet();
        Iterator itrBaseList = baseList.iterator();
        Iterator itrCompareToList = null;
        boolean matchFound = false;

        while (itrBaseList.hasNext()) {
            BaseDataObject nextBasePerceroObject = (BaseDataObject) itrBaseList.next();
            ClassIDPair nextBasePair = BaseDataObject.toClassIdPair(nextBasePerceroObject);
            nextBasePerceroObject = (BaseDataObject) findById(nextBasePair, null);

            itrCompareToList = compareToList.iterator();
            matchFound = false;
            while (itrCompareToList.hasNext()) {
                BaseDataObject nextCompareToPerceroObject = (BaseDataObject) itrCompareToList.next();
                nextCompareToPerceroObject = (BaseDataObject) findById(BaseDataObject.toClassIdPair(nextCompareToPerceroObject), null);

                if (nextBasePerceroObject.getClass() == nextCompareToPerceroObject.getClass() && nextBasePerceroObject.getID().equalsIgnoreCase(nextCompareToPerceroObject.getID())) {
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) {
                result.add(nextBasePerceroObject);
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
            List<IPerceroObject> allRelatedObjects = findAllRelatedObjects(perceroObject, nextToManyMappedField, shellOnly, userId);
            nextToManyMappedField.getSetter().invoke(perceroObject, allRelatedObjects);
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

    protected void addObjectToCache(IPerceroObject nextPerceroObject) {
        String key = RedisKeyUtils.classIdPair(nextPerceroObject.getClass().getCanonicalName(), nextPerceroObject.getID());
        if (cacheTimeout > 0)
            cacheDataStore.setValue(key, ((BaseDataObject)nextPerceroObject).toJson());

        // (Re)Set the expiration.
        if (cacheTimeout > 0 && key != null) {
            cacheDataStore.expire(key, cacheTimeout, TimeUnit.SECONDS);
        }
    }

}

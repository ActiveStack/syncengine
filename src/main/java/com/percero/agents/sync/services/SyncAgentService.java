package com.percero.agents.sync.services;

//import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.cw.IChangeWatcherHelperFactory;
import com.percero.agents.sync.cw.IChangeWatcherValueHelper;
import com.percero.agents.sync.datastore.ICacheDataStore;
import com.percero.agents.sync.events.SyncEvent;
import com.percero.agents.sync.exceptions.ClientException;
import com.percero.agents.sync.exceptions.SyncDataException;
import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.helpers.PostCreateHelper;
import com.percero.agents.sync.helpers.PostCreateTask;
import com.percero.agents.sync.helpers.PostDeleteHelper;
import com.percero.agents.sync.helpers.PostDeleteTask;
import com.percero.agents.sync.helpers.PostGetHelper;
import com.percero.agents.sync.helpers.PostPutHelper;
import com.percero.agents.sync.helpers.PostPutTask;
import com.percero.agents.sync.helpers.ProcessHelper;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.metadata.MappedFieldList;
import com.percero.agents.sync.metadata.MappedFieldPerceroObject;
import com.percero.agents.sync.rr.IRequestHandler;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.ClassIDPairs;
import com.percero.agents.sync.vo.HistoricalObject;
import com.percero.agents.sync.vo.IHistoryObject;
import com.percero.agents.sync.vo.ObjectModJournal;
import com.percero.agents.sync.vo.PushDeleteResponse;
import com.percero.agents.sync.vo.PushUpdateResponse;
import com.percero.agents.sync.vo.RemovedClassIDPair;
import com.percero.agents.sync.vo.ServerRequest;
import com.percero.agents.sync.vo.ServerResponse;
import com.percero.framework.bl.IManifest;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;
import com.percero.serial.map.SafeObjectMapper;

@Component
public class SyncAgentService implements ISyncAgentService, ApplicationEventPublisherAware {

	// TODO: Define conflict management algorithm (for syncing).
	
	private static final Logger log = Logger.getLogger(SyncAgentService.class);
	
	protected static final String VERSION = "0.1";

	// TODO: Read this in as a config parameter.
	public static final boolean GUARANTEE_UPDATE_DELIVERY = true;

	public SyncAgentService() {
	}
	
	@Autowired
	ObjectMapper safeObjectMapper;
	public void setSafeObjectMapper(SafeObjectMapper value) {
		safeObjectMapper = value;
	}

	@Autowired
	ICacheDataStore cacheDataStore;
	public void setCacheDataStore(ICacheDataStore value) {
		cacheDataStore = value;
	}
	
	@Autowired
	IChangeWatcherHelperFactory changeWatcherHelperFactory;
	public void setChangeWatcherHelperFactory(IChangeWatcherHelperFactory value) {
		changeWatcherHelperFactory = value;
	}
	
	@Autowired
	IDataProviderManager dataProviderManager;
	public void setDataProviderManager(IDataProviderManager value) {
		dataProviderManager = value;
	}
	
	@Autowired
	IAccessManager accessManager;
	public void setAccessManager(IAccessManager value) {
		accessManager = value;
	}
	
	@Autowired
	TaskExecutor taskExecutor;
	public void setTaskExecutor(TaskExecutor value) {
		taskExecutor = value;
	}

	@Autowired
	PostGetHelper postGetHelper;
	public void setPostGetHelper(PostGetHelper value) {
		postGetHelper = value;
	}
	@Autowired
	PostPutHelper postPutHelper;
	public void setPostPutHelper(PostPutHelper value) {
		postPutHelper = value;
	}
	@Autowired
	PostCreateHelper postCreateHelper;
	public void setPostCreateHelper(PostCreateHelper value) {
		postCreateHelper = value;
	}
	@Autowired
	PostDeleteHelper postDeleteHelper;
	public void setPostDeleteHelper(PostDeleteHelper value) {
		postDeleteHelper = value;
	}

	@Autowired
	ProcessHelper processHelper;
	public void setProcessHelper(ProcessHelper value) {
		processHelper = value;
	}
	
	@Autowired
	IRequestHandler requestHandler;
	public void setRequestHandler(IRequestHandler value) {
		requestHandler = value;
	}

	@Autowired
	IPushSyncHelper pushSyncHelper;
	public void setPushSyncHelper(IPushSyncHelper value) {
		pushSyncHelper = value;
	}
	
	@Autowired
	Boolean storeHistory = false;

	@Autowired
	IManifest manifest;
	
	@PostConstruct
	public void setup() {
		processManifest();
		initialized = true;
		
		SyncEvent se = new SyncEvent(this, SyncEvent.SYNC_AGENT_INITIALIZED);
		eventPublisher.publishEvent(se);
	}
	
	//@Autowired
	ApplicationEventPublisher eventPublisher;
	@Override
	public void setApplicationEventPublisher(
			ApplicationEventPublisher applicationEventPublisher) {
		eventPublisher = applicationEventPublisher;
	}
	
	private Boolean initialized = false;
	public Boolean isInitialized() {
		return initialized;
	}

	public Object testCall(String aParam) {
		log.debug("Received testCall: " + aParam);

		return aParam;
	}
	
	public Date getServerDate() {
		return new Date();
	}

	public String getVersion() {
		return VERSION;
	}
	
	public void processManifest() {
		MappedClass.processManifest(manifest);
		/**
		try {
			Set<MappedClass> mappedClasses = new HashSet<MappedClass>();
			ManifestHelper.setManifest(manifest);
			Iterator<String> itrUuidMap = manifest.getUuidMap().keySet().iterator();
			while(itrUuidMap.hasNext()) {
				String nextUuid = itrUuidMap.next();
				Class nextClass = manifest.getUuidMap().get(nextUuid);
				
				IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
				MappedClass mc = mcm.getMappedClassByClassName(nextClass.getCanonicalName());
				mappedClasses.add(mc);
			}

			// Initialize the Fields
			Iterator<MappedClass> itrMappedClasses = mappedClasses.iterator();
			while (itrMappedClasses.hasNext()) {
				MappedClass mc = itrMappedClasses.next();
				mc.initializeFields();
			}
			// Initialize the Queries
			itrMappedClasses = mappedClasses.iterator();
			while (itrMappedClasses.hasNext()) {
				MappedClass mc = itrMappedClasses.next();
				mc.initializeQueries();
			}
			// Initialize the Relationships
			itrMappedClasses = mappedClasses.iterator();
			while (itrMappedClasses.hasNext()) {
				MappedClass mc = itrMappedClasses.next();
				mc.initializeRelationships();
			}
			
			MappedClass.allMappedClassesInitialized = true;

		} catch(Exception e) {
			log.error("Unable to process manifest.", e);
		}
		*/
	}

	
	
	/* (non-Javadoc)
	 * The dataId defines the ID of the current data set.  Whenever a client connects, they must
	 * verify that they are using the same data set.
	 * @see com.com.percero.agents.sync.services.ISyncAgentService#getDataId(java.lang.String)
	 */
	public String getDataId(String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);

		String dataId = (String) cacheDataStore.listIndex(RedisKeyUtils.dataRecord());

		if (dataId == null) {
			dataId = UUID.randomUUID().toString();
			cacheDataStore.lpushListValue(RedisKeyUtils.dataRecord(), dataId);
		}
		
		return dataId;
	}
	
	
	public PerceroList<IPerceroObject> getAllByName(Object aName, Boolean returnTotal, String clientId) throws Exception {
		return getAllByName(aName, null, null, returnTotal, clientId);
	}

	@SuppressWarnings("rawtypes")
	public PerceroList<IPerceroObject> getAllByName(Object aName, Integer pageNumber, Integer pageSize, Boolean returnTotal, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		String aClassName = aName.toString();
		Class theClass = MappedClass.forName(aClassName);

		// Get the MappedClass and determine which DataProvider provides data for this object.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(aName.toString());
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		String userId = accessManager.getClientUserId(clientId);
		PerceroList<IPerceroObject> result = dataProvider.getAllByName(aName, pageNumber, pageSize, returnTotal, userId);
		
		if (result != null) {
			// Register an Zero ID object to indicate that this user wants updates to ALL objects of this type.
			Object newInstance = theClass.newInstance();
			if (newInstance instanceof IPerceroObject) {
				((IPerceroObject) newInstance).setID("0");
				postGetHelper.postGetObject((IPerceroObject) newInstance, userId, clientId);
			}
		}
		
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unused" })
	public Map<String, Integer> countAllByName(Collection<String> classNames, String clientId) throws Exception {
		Map<String, Integer> result = new HashMap<String, Integer>();
		
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);

		Iterator<String> itrClassNames = classNames.iterator();
		while(itrClassNames.hasNext()) {
			String nextClassName = itrClassNames.next();
			Class theClass = MappedClass.forName(nextClassName);
			
			// Get the MappedClass and determine which DataProvider provides data for this object.
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(nextClassName);
			
			IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
			String userId = accessManager.getClientUserId(clientId);
			Integer count = dataProvider.countAllByName(nextClassName, userId);

			result.put(nextClassName, count);
		}
		return result;
	}

	public List<Object> runQuery(String className, String queryName, Object[] queryArguments, String clientId) throws Exception {
		List<Object> result = null;
		
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		try {
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(className);
			if (mappedClass != null) {
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				result = dataProvider.runQuery(mappedClass, queryName, queryArguments, clientId);
			}
		} catch (Exception e) {
			log.error("Unable to runQuery", e);
		}

		return result;
	}
	
	public Object runProcess(String processName, Object[] queryArguments, String clientId) throws Exception {
		Object result = null;
		
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		if (processHelper != null)
		{
			try {
				Object args[] = new Object[2];
				args[0] = clientId;
				args[1] = queryArguments;
				Method method = processHelper.getClass().getDeclaredMethod(processName, new Class[]{String.class,Object[].class});
				result = method.invoke(processHelper, args);
			} catch(Exception e) {
				if (e instanceof InvocationTargetException) {
					InvocationTargetException ite = (InvocationTargetException) e;
					if (ite.getTargetException() instanceof SyncException) {
						throw (SyncException) ite.getTargetException();
					}
					else {
						log.error("Unable to run Process", ite.getTargetException());
						throw new SyncException("Error Running Process", -101, "Unable to run process " + processName + ":\n" + ite.getTargetException().getMessage());
					}
				}
				else {
					log.error("Unable to run Process", e);
					throw new SyncException("Error Running Process", -101, "Unable to run process " + processName + ":\n" + e.getMessage());
				}
			}
		}
		
		return result;
	}

	public Object getChangeWatcherValue(ClassIDPair classIdPair, String fieldName, String[] params, String clientId) throws Exception {
		Object result = null;
		
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		// Only ChangeWatcherValueHelper's have the "get" function.
		IChangeWatcherValueHelper cwh = (IChangeWatcherValueHelper) changeWatcherHelperFactory.getHelper(classIdPair.getClassName());
		result = cwh.get(fieldName, classIdPair, params, clientId);
		
		return result;
	}
	
	public Object findByExample(Object theQueryObject, String clientId) throws Exception {
		return findByExample(theQueryObject, null, clientId);
	}

	
	//@Transactional
	public Object findByExample(Object theQueryObject,
			List<String> excludeProperties, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		if (theQueryObject instanceof IPerceroObject) {
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(theQueryObject.getClass().getCanonicalName());
			if (mappedClass != null) {
				String userId = accessManager.getClientUserId(clientId);
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				return dataProvider.findByExample((IPerceroObject) theQueryObject, excludeProperties, userId);
			}
		}
		
		return null;
	}

	public List<IPerceroObject> systemFindByExample(Object theQueryObject, List<String> excludeProperties) {
		if (theQueryObject instanceof IPerceroObject) {
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(theQueryObject.getClass().getCanonicalName());
			if (mappedClass != null) {
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				return dataProvider.systemFindByExample((IPerceroObject) theQueryObject, excludeProperties);
			}
		}
		
		return null;
	}
	
	@SuppressWarnings({ "rawtypes" })
	public Object findUnique(Object theQueryObject, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		Class objectClass = theQueryObject.getClass();
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(objectClass.getName());
		if (mappedClass != null) {
			String userId = accessManager.getClientUserId(clientId);
			
			IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
			IPerceroObject result = dataProvider.findUnique((IPerceroObject) theQueryObject, userId);
			
			if (result != null) {
				postGetHelper.postGetObject(result, userId, clientId);
			}
			
			return result;
		}
		
		return null;
	}

	public Object searchByExample(Object theQueryObject, String clientId) throws Exception {
		return searchByExample(theQueryObject, null, clientId);
	}

	// TODO: Add permissions check
	public Object searchByExample(Object theQueryObject,
			List<String> excludeProperties, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);

		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(theQueryObject.getClass().getCanonicalName());
		if (mappedClass != null) {
			String userId = accessManager.getClientUserId(clientId);
			
			IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
			List<IPerceroObject> result = dataProvider.searchByExample((IPerceroObject) theQueryObject, excludeProperties, userId);
			
			if (result != null && result.size() > 0)
				postGetHelper.postGetObject(result, userId, clientId);
			
			return result;
		}
		
		return null;
	}

	public Object findById(ClassIDPair classIdPair, String clientId) throws Exception {
		return findById(classIdPair.getClassName(), classIdPair.getID(), clientId);
	}

	
	public Object findById(String aClassName, String anId, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		// Get the MappedClass and determine which DataProvider provides data for this object.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(aClassName);
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		String userId = accessManager.getClientUserId(clientId);
		IPerceroObject result = dataProvider.findById(new ClassIDPair(anId, aClassName), userId);
		
		if (result != null) {
			postGetHelper.postGetObject(result, userId, clientId);
		}
		
		return result;
	}

	public IPerceroObject systemGetById(String aClassName, String anId) {//throws Exception {
		// Get the MappedClass and determine which DataProvider provides data for this object.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(aClassName);
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		IPerceroObject result = dataProvider.systemGetById(new ClassIDPair(anId, aClassName));
		
		return result;
	}
	
	public IPerceroObject systemGetById(ClassIDPair cip) { //throws Exception {
		// Get the MappedClass and determine which DataProvider provides data for this object.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(cip.getClassName());
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		IPerceroObject result = dataProvider.systemGetById(cip);
		
		return result;
	}
	
	public <T extends IPerceroObject> T systemGetByObject(IPerceroObject perceroObject) {//throws Exception {
		// Get the MappedClass and determine which DataProvider provides data for this object.
		if (perceroObject == null || !StringUtils.hasText(perceroObject.getID())) {
			return null;
		}
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		String className = perceroObject.getClass().getCanonicalName();
		MappedClass mappedClass = mcm.getMappedClassByClassName(className);
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		T result = dataProvider.systemGetById(new ClassIDPair(perceroObject.getID(), className));
		
		return result;
	}
	
	
	/**
	 * getHistory attempts to return the history of the specified object. The result is a List of
	 * HistoricalObject's.  If the object does is not setup as a IHistoryObject, then the result is
	 * NULL.
	 * 
	 * @param classIdPair
	 * @param clientId
	 * @return List<HistoricalObject>
	 * @throws Exception
	 */
	public List<Object> getHistory(ClassIDPair classIdPair, String clientId) throws Exception {
		return getHistory(classIdPair.getClassName(), classIdPair.getID(), clientId);
	}
	
	
	/**
	 * getHistory attempts to return the history of the specified object. The result is a List of
	 * HistoricalObject's.  If the object does is not setup as a IHistoryObject, then the result is
	 * NULL.
	 * 
	 * @param aClassName
	 * @param anId
	 * @param clientId
	 * @return List<HistoricalObject>
	 * @throws Exception
	 */
	public List<Object> getHistory(String aClassName, String anId, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		// Get the MappedClass and determine which DataProvider provides data for this object.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(aClassName);
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		String userId = accessManager.getClientUserId(clientId);
		Boolean hasReadAccess = dataProvider.getReadAccess(new ClassIDPair(anId, aClassName), userId);
		
		if (hasReadAccess) {
			// Get the Historical Objects.
			return cacheDataStore.listAll(RedisKeyUtils.historicalObject(aClassName, anId));
		}
		
		return null;
	}
	
	public List<IPerceroObject> findByIds(List<ClassIDPairs> classIdList, String clientId) throws Exception {
		List<IPerceroObject> result = new ArrayList<IPerceroObject>();

		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		String userId = accessManager.getClientUserId(clientId);
		
		// Get the MappedClass and determine which DataProvider provides data for this object.
		for(ClassIDPairs nextClassIDPairs : classIdList) {
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(nextClassIDPairs.getClassName());
			
			IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
			List<IPerceroObject> classResult = dataProvider.findByIds(nextClassIDPairs, userId);
			if (classResult != null)
				result.addAll(classResult);
		}
		
		postGetHelper.postGetObject(result, userId, clientId);
		
		return result;
	}

	public ServerResponse putObject(IPerceroObject perceroObject, String transactionId, Date updateDate, String clientId) throws Exception {
		return putObject(perceroObject, transactionId, updateDate, clientId, false);
	}
	public ServerResponse putObject(IPerceroObject perceroObject, String transactionId, Date updateDate, String clientId, Boolean pushToClient) throws Exception {
		ServerResponse response = new ServerResponse();
		ClassIDPair classIdPair = new ClassIDPair(((IPerceroObject)perceroObject).getID(), perceroObject.getClass().getName());
		response.setResultObject(classIdPair);
		
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);

		// Get the MappedClass and determine which DataProvider provides data for this object.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
		IPerceroObject result = null;
		
		String userId = accessManager.getClientUserId(clientId);
		
		// Check to see if object has a history.
		ObjectModJournal objectModJournal = null;
		Map<ClassIDPair, Collection<MappedField>> changedFields = null;
		try {
			// Get the most recent ObjectModJournal, only if the UpdateDate is set.
			if (updateDate != null) {
				try {
					objectModJournal = (ObjectModJournal) cacheDataStore.listIndex(RedisKeyUtils.objectModJournal(perceroObject.getClass().getName(), perceroObject.getID()));
				} catch(Exception e) {
					log.warn("Unable to retrieve most recent objectModJournal", e);
				}
			}
			
			boolean isOkToUpdate = true;

			Date currentDate = new Date();
			if (updateDate != null && updateDate.getTime() > currentDate.getTime()) {
				updateDate.setTime(currentDate.getTime());
			}
			
			if (updateDate == null)
				updateDate = new Date();
			
			// Compare Modification dates.
			if (objectModJournal != null && objectModJournal.getDateModified() != null && updateDate != null) {
				if (objectModJournal.getDateModified().compareTo(updateDate) >= 0) {
					// The ObjectModJournal is newer or the exact same date, therefore reject this update.
					// TODO: Is this the correct way to handle this? Should we really reject if date modified is 
					//	exactly the same?  Or should the key (className, classId, dateModified) be changed to
					//	non-unique instead?
					log.warn("Rejecting update (DB Update " + updateDate.toString() + " vs. ObjectModDate " + objectModJournal.getDateModified().toString() + ") - ID: " + perceroObject.getID() + " / Class: " + perceroObject.getClass().getName() + " / UserID: " + userId);
					isOkToUpdate = false;
				}
			}
			
			if (isOkToUpdate) {
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				changedFields = dataProvider.getChangedMappedFields(perceroObject);
				if (changedFields == null || changedFields.size() > 0) {
					// Something has changed.
					result = dataProvider.putObject(perceroObject, changedFields, userId);
					
					if (result != null) {
						// Now record the updated object.
						ObjectModJournal newModJournal = new ObjectModJournal();
						newModJournal.setDateModified(updateDate);
						newModJournal.setUserId(userId);
						
						if (transactionId == null || transactionId.length() == 0)
							transactionId = UUID.randomUUID().toString();
						newModJournal.setTransactionId(transactionId);
						newModJournal.setClassID(result.getID());
						newModJournal.setClassName(result.getClass().getName());
						
						cacheDataStore.lpushListValue(RedisKeyUtils.objectModJournal(perceroObject.getClass().getCanonicalName(), perceroObject.getID()), newModJournal);
						
						// Also store historical record, if necessary.
						// Get the Current object if this is a BaseHistoryObject.
						if (storeHistory && (result instanceof IHistoryObject))
						{
							HistoricalObject historyObject = new HistoricalObject();
							historyObject.setObjectVersion(result.classVersion());
							historyObject.setID(UUID.randomUUID().toString());
							historyObject.setObjectChangeDate(updateDate);
							historyObject.setObjectClassName(result.getClass().getName());
							historyObject.setObjectId(result.getID());
							historyObject.setObjectChangerId(userId);
							historyObject.setObjectData(safeObjectMapper.writeValueAsString(result));
							
							cacheDataStore.lpushListValue(RedisKeyUtils.historicalObject(result.getClass().getCanonicalName(), result.getID()), historyObject);
						}

						if (taskExecutor != null && false) {
							taskExecutor.execute(new PostPutTask(postPutHelper, BaseDataObject.toClassIdPair((BaseDataObject) result), userId, clientId, pushToClient, changedFields));
						} else {
							postPutHelper.postPutObject(BaseDataObject.toClassIdPair((BaseDataObject) result), userId, clientId, pushToClient, changedFields);
						}
					}
				}
				else {
					log.info("Unnecessary PutObject: " + perceroObject.getClass().getCanonicalName() + " (" + perceroObject.getID() + ")");
					result = perceroObject;
				}
			}
		
		} catch(Exception e) {
			log.error("Error PUTting object: " + perceroObject.toString(), e);
		}
		
		if (result != null) {
			response.setIsSuccessful(true);
			/**
			if (taskExecutor != null && false) {
				taskExecutor.execute(new PostPutTask(postPutHelper, result, userId, clientId, pushToClient, changedFields));
			} else {
				postPutHelper.postPutObject(result, userId, clientId, pushToClient, changedFields);
			}*/
		}
		else {
			response.setIsSuccessful(false);
		}

		return response;
	}

	public boolean systemPutObject(IPerceroObject perceroObject, String transactionId, Date updateDate, String userId, boolean pushToUser) {
		// Get the MappedClass and determine which DataProvider provides data for this object.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
		IPerceroObject result = null;
		Map<ClassIDPair, Collection<MappedField>> changedFields = null;

		// Check to see if object has a history.
		ObjectModJournal objectModJournal = null;
		try {
			// Get the most recent ObjectModJournal.
			try {
				objectModJournal = (ObjectModJournal) cacheDataStore.listIndex(RedisKeyUtils.objectModJournal(perceroObject.getClass().getName(), perceroObject.getID()));
			} catch(Exception e) {
				log.warn("Unable to retrieve most recent objectModJournal", e);
			}
			
			boolean isOkToUpdate = true;

			Date currentDate = new Date();
			if (updateDate != null && updateDate.getTime() > currentDate.getTime()) {
				updateDate.setTime(currentDate.getTime());
			}

			// Compare Modification dates.
			if (objectModJournal != null && objectModJournal.getDateModified() != null && updateDate != null) {
				if (objectModJournal.getDateModified().compareTo(updateDate) >= 0) {
					// The ObjectModJournal is newer, therefore reject this update.
					isOkToUpdate = false;
				}
			}
			
			if (updateDate == null)
				updateDate = new Date();
			
			if (isOkToUpdate) {
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				changedFields = dataProvider.getChangedMappedFields(perceroObject);
				if (changedFields == null || changedFields.size() > 0) {
					result = dataProvider.systemPutObject(perceroObject, changedFields);
					
					if (result != null) {
						// Now record the updated object.
						ObjectModJournal newModJournal = new ObjectModJournal();
						newModJournal.setID(UUID.randomUUID().toString());
						if (transactionId == null || transactionId.length() == 0)
							transactionId = UUID.randomUUID().toString();
						newModJournal.setTransactionId(transactionId);
						newModJournal.setClassID(result.getID());
						newModJournal.setClassName(result.getClass().getName());
						newModJournal.setDateModified(updateDate);
						
						cacheDataStore.lpushListValue(RedisKeyUtils.objectModJournal(result.getClass().getCanonicalName(), result.getID()), newModJournal);
						
						// Also store historical record, if necessary.
						// Get the Current object if this is a BaseHistoryObject.
						if (storeHistory && (result instanceof IHistoryObject))
						{
							HistoricalObject historyObject = new HistoricalObject();
							historyObject.setObjectVersion(result.classVersion());
							historyObject.setID(UUID.randomUUID().toString());
							historyObject.setObjectChangeDate(updateDate);
							historyObject.setObjectClassName(result.getClass().getName());
							historyObject.setObjectId(result.getID());
							historyObject.setObjectChangerId(userId);
							historyObject.setObjectData(safeObjectMapper.writeValueAsString(result));
							
							cacheDataStore.lpushListValue(RedisKeyUtils.historicalObject(result.getClass().getCanonicalName(), result.getID()), historyObject);
	
							//syncSession.save(historyObject);
						}
						
						if (taskExecutor != null && false) {
							taskExecutor.execute(new PostPutTask(postPutHelper, BaseDataObject.toClassIdPair((BaseDataObject) result), userId, null, pushToUser, changedFields));
						} else {
							postPutHelper.postPutObject(BaseDataObject.toClassIdPair((BaseDataObject) result), userId, null, pushToUser, changedFields);
						}
					}
				}
				else {
					log.info("Unnecessary PutObject: " + perceroObject.getClass().getCanonicalName() + " (" + perceroObject.getID() + ")");
					result = perceroObject;
				}
			}
		
		} catch(Exception e) {
			log.error("Error getting ObjectModJournal", e);
		}
		
		return (result != null);
	}

	public ServerResponse createObject(IPerceroObject perceroObject, String clientId) throws Exception {
		return createObject(perceroObject, clientId, false);
	}
	
	public ServerResponse createObject(IPerceroObject perceroObject, String clientId, Boolean pushToClient) throws Exception {
		ServerResponse response = new ServerResponse();
		
		log.debug("Create: " + perceroObject.getClass().getName() + ": " + perceroObject.getID());
		
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
			
		if (perceroObject != null) {
			String userId = accessManager.getClientUserId(clientId);
			
			try {
				IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
				MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				IPerceroObject result = dataProvider.createObject(perceroObject, userId);

				if (result != null) {
					Date updateDate = new Date();
					String transactionId = "";
					
					// Now record the updated object.
					ObjectModJournal newModJournal = new ObjectModJournal();
					newModJournal.setDateModified(updateDate);
					newModJournal.setUserId(userId);
					
					if (!StringUtils.hasText(transactionId))
						transactionId = UUID.randomUUID().toString();
					newModJournal.setTransactionId(transactionId);
					newModJournal.setClassID(result.getID());
					newModJournal.setClassName(result.getClass().getName());
					
					try {
						cacheDataStore.lpushListValue(RedisKeyUtils.objectModJournal(result.getClass().getCanonicalName(), result.getID()), newModJournal);
					} catch(Exception e) {
						log.error("Unable to save mod journal to redis cache", e);
					}
					
					// Also store historical record, if necessary.
					// Get the Current object if this is a BaseHistoryObject.
					if (storeHistory && (result instanceof IHistoryObject))
					{
						HistoricalObject historyObject = new HistoricalObject();
						historyObject.setObjectVersion(result.classVersion());
						historyObject.setID(UUID.randomUUID().toString());
						historyObject.setObjectChangeDate(updateDate);
						historyObject.setObjectClassName(result.getClass().getName());
						historyObject.setObjectId(result.getID());
						historyObject.setObjectChangerId(userId);
						historyObject.setObjectData(safeObjectMapper.writeValueAsString(result));
						
						try {
							cacheDataStore.lpushListValue(RedisKeyUtils.historicalObject(result.getClass().getCanonicalName(), result.getID()), historyObject);
						} catch(Exception e) {
							log.error("Unable to save history object to redis cache", e);
						}
					}
					
					if (taskExecutor != null && false) {
						taskExecutor.execute(new PostCreateTask(postCreateHelper, result, userId, clientId, pushToClient));
					} else {
						postCreateHelper.postCreateObject(result, userId, clientId, pushToClient);
					}
					
					response.setResultObject(result);
					response.setIsSuccessful(true);
				}
				else {
					//ClassIDPair classIdPair = new ClassIDPair(((IPerceroObject)perceroObject).getID(), perceroObject.getClass().getName());
					log.warn("No access to create object " + perceroObject.getClass().getCanonicalName() + "/" + perceroObject.getID());
					response.setResultObject(perceroObject);
					response.setIsSuccessful(false);
				}

				return response;
			} catch (SyncException se) {
				throw se;
			} catch (Exception e) {
				log.error("Error creating object", e);
				response.setResultObject(perceroObject);
				response.setIsSuccessful(false);
			}
		}
		// TODO: Return an exception object with the unchanged Object so client can undo changes.
		return response;
	}
	
	public <T extends IPerceroObject> T systemCreateObject(IPerceroObject perceroObject, String userId) throws SyncException {
		
		log.debug("Create: " + perceroObject.getClass().getName() + ": " + perceroObject.getID());
		
		if (perceroObject != null) {
			try {
				IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
				MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				T result = dataProvider.systemCreateObject(perceroObject);
				
				if (result != null) {
					Date updateDate = new Date();
					String transactionId = "";
					
					// Now record the updated object.
					ObjectModJournal newModJournal = new ObjectModJournal();
					newModJournal.setDateModified(updateDate);
					newModJournal.setUserId(userId);
					
					if (!StringUtils.hasText(transactionId))
						transactionId = UUID.randomUUID().toString();
					newModJournal.setTransactionId(transactionId);
					newModJournal.setClassID(result.getID());
					newModJournal.setClassName(result.getClass().getName());
					
					try {
						cacheDataStore.lpushListValue(RedisKeyUtils.objectModJournal(result.getClass().getCanonicalName(), result.getID()), newModJournal);
					} catch(Exception e) {
						log.error("Unable to save mod journal to redis cache", e);
					}
					
					// Also store historical record, if necessary.
					// Get the Current object if this is a BaseHistoryObject.
					if (storeHistory && (result instanceof IHistoryObject))
					{
						HistoricalObject historyObject = new HistoricalObject();
						historyObject.setObjectVersion(result.classVersion());
						historyObject.setID(UUID.randomUUID().toString());
						historyObject.setObjectChangeDate(updateDate);
						historyObject.setObjectClassName(result.getClass().getName());
						historyObject.setObjectId(result.getID());
						historyObject.setObjectChangerId(userId);
						historyObject.setObjectData(safeObjectMapper.writeValueAsString(result));
						
						try {
							cacheDataStore.lpushListValue(RedisKeyUtils.historicalObject(result.getClass().getCanonicalName(), result.getID()), historyObject);
						} catch(Exception e) {
							log.error("Unable to save history object to redis cache", e);
						}
					}
					
					if (taskExecutor != null && false) {
						taskExecutor.execute(new PostCreateTask(postCreateHelper, result, userId, null, true));
					} else {
						postCreateHelper.postCreateObject(result, userId, null, true);
					}
					
					return result;
				}
				else {
					//ClassIDPair classIdPair = new ClassIDPair(((IPerceroObject)perceroObject).getID(), perceroObject.getClass().getName());
					log.warn("No access to create object " + perceroObject.getClass().getCanonicalName() + "/" + perceroObject.getID());
					return null;
				}
				
			} catch (SyncException e) {
				throw e;
			} catch (Exception e) {
				log.error("Error creating object", e);
				SyncDataException sde = new SyncDataException(SyncDataException.CREATE_OBJECT_ERROR, SyncDataException.CREATE_OBJECT_ERROR_CODE, e.getMessage());
				throw sde;
			}
		}
		else {
			return null;
		}
	}

	public ServerResponse deleteObjectById(String theClassName, String theId, String clientId) throws Exception {
		return deleteObjectById(theClassName, theId, theClassName, false);
	}
	public ServerResponse deleteObjectById(String theClassName, String theId, String clientId, Boolean pushToClient) throws Exception {
		return deleteObject(new ClassIDPair(theId, theClassName), clientId, pushToClient);
	}
	public ServerResponse deleteObject(ClassIDPair theClassIdPair, String clientId) throws Exception {
		return deleteObject(theClassIdPair, clientId, false);
	}
	public ServerResponse deleteObject(ClassIDPair theClassIdPair, String clientId, Boolean pushToClient) throws Exception {
		ServerResponse response = new ServerResponse();
		response.setResultObject(theClassIdPair);
		
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		try {
			
			String userId = accessManager.getClientUserId(clientId);
			
			boolean hasAccess = true;
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(theClassIdPair.getClassName());
			if (mappedClass != null) {
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				hasAccess = dataProvider.getDeleteAccess(theClassIdPair, userId);

				if (hasAccess) {
					IPerceroObject perceroObject = dataProvider.systemGetById(theClassIdPair);
					if (perceroObject == null) {
						response.setIsSuccessful(true);
						return response;
					}
					
					if (systemDeleteObject(perceroObject, clientId, pushToClient, new HashSet<IPerceroObject>())) {
						response.setIsSuccessful(true);
					}
					else {
						response.setIsSuccessful(false);
					}
				}
				else {
					log.warn("No access to delete object " + theClassIdPair.toString());
				}
			}
		} catch (Exception e) {
			log.error("Error deleting object", e);
			response.setIsSuccessful(false);
		}

		return response;
	}

	public boolean systemDeleteObject(IPerceroObject perceroObject, String clientId, boolean pushToUser) throws Exception {
		return systemDeleteObject(perceroObject, clientId, pushToUser, new HashSet<IPerceroObject>());
	}
	public boolean systemDeleteObject(IPerceroObject perceroObject, String clientId, boolean pushToUser, Collection<IPerceroObject> deletedObjects) throws Exception {
		boolean result = true;
		if(perceroObject == null)
			return true;
		if (deletedObjects.contains(perceroObject))
			return true;
		else
			deletedObjects.add(perceroObject);
		
		String userId = accessManager.getClientUserId(clientId);
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
		if (mappedClass != null) {
			IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
			
			perceroObject = (IPerceroObject) dataProvider.systemGetById(new ClassIDPair(perceroObject.getID(), perceroObject.getClass().getCanonicalName()));
			
			if (perceroObject == null) {
				return true;
			}

			Iterator<Map.Entry<MappedField, MappedField>> itrCascadeRemoveFieldReferencesEntrySet = mappedClass.cascadeRemoveFieldReferences.entrySet().iterator();
			while (itrCascadeRemoveFieldReferencesEntrySet.hasNext()) {
				Map.Entry<MappedField, MappedField> nextEntry = itrCascadeRemoveFieldReferencesEntrySet.next();
				MappedField nextRemoveMappedFieldRef = nextEntry.getKey();
				try {
					MappedField nextMappedField = nextEntry.getValue();
//			for(MappedField nextRemoveMappedFieldRef : mappedClass.cascadeRemoveFieldReferences.keySet()) {
//				try {
//					// nextRemoveMappedFieldRef points to mappedClass, so need to find all objects of that type that point to this particular
//					//	instance perceroObejct of mappedClass.
//					MappedField nextMappedField = mappedClass.cascadeRemoveFieldReferences.get(nextRemoveMappedFieldRef);
					if (nextMappedField == null) {
						// There is no direct link from mappedClass, so need to get all by example.
						IPerceroObject tempObject = (IPerceroObject) nextRemoveMappedFieldRef.getMappedClass().clazz.newInstance();
						nextRemoveMappedFieldRef.getSetter().invoke(tempObject, perceroObject);
						IDataProvider dataProviderRef = dataProviderManager.getDataProviderByName(nextRemoveMappedFieldRef.getMappedClass().dataProviderName);
						List<IPerceroObject> referencingObjects = dataProviderRef.systemFindByExample(tempObject, null);
						Iterator<IPerceroObject> itrReferencingObjects = referencingObjects.iterator();
						while (itrReferencingObjects.hasNext()) {
							IPerceroObject nextReferencingObject = itrReferencingObjects.next();
							systemDeleteObject(nextReferencingObject, clientId, true, deletedObjects);
						}
					}
					else {
						// We have the reverse lookup right here.
						IPerceroObject tempObject = (IPerceroObject) nextRemoveMappedFieldRef.getMappedClass().clazz.newInstance();
						nextRemoveMappedFieldRef.getSetter().invoke(tempObject, perceroObject);
						IDataProvider dataProviderRef = dataProviderManager.getDataProviderByName(nextRemoveMappedFieldRef.getMappedClass().dataProviderName);
						List<IPerceroObject> referencingObjects = dataProviderRef.systemFindByExample(tempObject, null);
						Iterator<IPerceroObject> itrReferencingObjects = referencingObjects.iterator();
						while (itrReferencingObjects.hasNext()) {
							IPerceroObject nextReferencingObject = itrReferencingObjects.next();
							systemDeleteObject(nextReferencingObject, clientId, true, deletedObjects);
						}
					}
				} catch(Exception e) {
					log.error("Unable to remove referenced object", e);
					result = false;
				}
			}

			Object[] nullObject = new Object[1];
			nullObject[0] = null;
			Iterator<Map.Entry<MappedField, MappedField>> itrNulledOnRemoveFieldReferencesEntrySet = mappedClass.nulledOnRemoveFieldReferences.entrySet().iterator();
			while (itrNulledOnRemoveFieldReferencesEntrySet.hasNext()) {
				Map.Entry<MappedField, MappedField> nextEntry = itrNulledOnRemoveFieldReferencesEntrySet.next();
				MappedField nextToNullMappedFieldRef = nextEntry.getKey();
				try {
					MappedField nextMappedField = nextEntry.getValue();
//			for(MappedField nextToNullMappedFieldRef : mappedClass.nulledOnRemoveFieldReferences.keySet()) {
//				try {
//					// nextRemoveMappedFieldRef points to mappedClass, so need to find all objects of that type that point to this particular
//					//	instance perceroObejct of mappedClass.
//					MappedField nextMappedField = mappedClass.nulledOnRemoveFieldReferences.get(nextToNullMappedFieldRef);
					if (nextMappedField == null) {
						// There is no direct link from mappedClass, so need to get all by example.
						IPerceroObject tempObject = (IPerceroObject) nextToNullMappedFieldRef.getMappedClass().clazz.newInstance();
						nextToNullMappedFieldRef.getSetter().invoke(tempObject, perceroObject);
						IDataProvider dataProviderRef = dataProviderManager.getDataProviderByName(nextToNullMappedFieldRef.getMappedClass().dataProviderName);
						List<IPerceroObject> referencingObjects = dataProviderRef.systemFindByExample(tempObject, null);
						Iterator<IPerceroObject> itrReferencingObjects = referencingObjects.iterator();
						while (itrReferencingObjects.hasNext()) {
							IPerceroObject nextReferencingObject = itrReferencingObjects.next();
							nextToNullMappedFieldRef.getSetter().invoke(nextReferencingObject, nullObject);
							systemPutObject((IPerceroObject) nextReferencingObject, null, new Date(), userId, true);
						}
					}
					else {
						// We have the reverse lookup right here.
						IPerceroObject tempObject = (IPerceroObject) nextToNullMappedFieldRef.getMappedClass().clazz.newInstance();
						nextToNullMappedFieldRef.getSetter().invoke(tempObject, perceroObject);
						IDataProvider dataProviderRef = dataProviderManager.getDataProviderByName(nextToNullMappedFieldRef.getMappedClass().dataProviderName);
						List<IPerceroObject> referencingObjects = dataProviderRef.systemFindByExample(tempObject, null);
						Iterator<IPerceroObject> itrReferencingObjects = referencingObjects.iterator();
						while (itrReferencingObjects.hasNext()) {
							IPerceroObject nextReferencingObject = itrReferencingObjects.next();
							nextToNullMappedFieldRef.getSetter().invoke(nextReferencingObject, nullObject);
							systemPutObject((IPerceroObject) nextReferencingObject, null, new Date(), userId, true);
						}
					}
				} catch(Exception e) {
					log.error("Unable to remove referenced object", e);
					result = false;
				}
			}
			
			Iterator<MappedField> itrToOneFieldsToUpdate = mappedClass.toOneFields.iterator();
			while (itrToOneFieldsToUpdate.hasNext()) {
				MappedField nextToOneField = itrToOneFieldsToUpdate.next();
				if (nextToOneField instanceof MappedFieldPerceroObject) {
					MappedFieldPerceroObject nextPerceroObjectField = (MappedFieldPerceroObject) nextToOneField;
					IPerceroObject toOneObject = (IPerceroObject) nextPerceroObjectField.getGetter().invoke(perceroObject);
					if (toOneObject != null) {
						// Remove this object from the cache.
						// TODO: ?
					}
				}
			}
							
			Iterator<MappedField> itrToManyFieldsToUpdate = mappedClass.toManyFields.iterator();
			while (itrToManyFieldsToUpdate.hasNext()) {
				MappedField nextToManyField = itrToManyFieldsToUpdate.next();
				if (nextToManyField instanceof MappedFieldPerceroObject) {
					MappedFieldPerceroObject nextPerceroObjectField = (MappedFieldPerceroObject) nextToManyField;
					// TODO: ?
				}
				else if (nextToManyField instanceof MappedFieldList) {
					MappedFieldList nextListField = (MappedFieldList) nextToManyField;
					// TODO: ?
				}
			}
			
			// If the result has been set to false, it means that deletion/update of one of the related objects failed.
			if (result && dataProvider.systemDeleteObject(perceroObject)) {
				// Also store historical record, if necessary.
				if (storeHistory && (perceroObject instanceof IHistoryObject))
				{
					try {
						HistoricalObject historyObject = new HistoricalObject();
						historyObject.setObjectVersion(perceroObject.classVersion());
						historyObject.setID(UUID.randomUUID().toString());
						historyObject.setObjectChangeDate(new Date());
						historyObject.setObjectClassName(perceroObject.getClass().getName());
						historyObject.setObjectId(perceroObject.getID());
						historyObject.setObjectChangerId(userId);
						historyObject.setObjectData(safeObjectMapper.writeValueAsString(perceroObject));
						
						cacheDataStore.lpushListValue(RedisKeyUtils.historicalObject(perceroObject.getClass().getCanonicalName(), perceroObject.getID()), historyObject);
					} catch(Exception e) {
						log.warn("Unable to save HistoricalObject in deleteObject", e);
					}
				}
				
				if (taskExecutor != null && false) {
					taskExecutor.execute(new PostDeleteTask(postDeleteHelper, perceroObject, userId, clientId, pushToUser));
				} else {
					postDeleteHelper.postDeleteObject(perceroObject, userId, clientId, pushToUser);
				}
				
				result = true;
			}
			else {
				result = false;
			}
		}

		return result;
	}

	public void updatesReceived(ClassIDPair[] theObjects, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);

		// Remove UpdateJournals for this client.
		accessManager.deleteUpdateJournals(clientId, theObjects);
	}

	public void deletesReceived(ClassIDPair[] theObjects, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE);
		
		// Remove DeleteJournals for this client.
		accessManager.deleteDeleteJournals(clientId, theObjects);
	}

	
	public ServerResponse request(ServerRequest aRequest, String userId) throws Exception {
		if (StringUtils.hasText(userId)) {
			try {
				ServerResponse response = requestHandler.handleRequest(aRequest);
				return response;
			} catch(Exception e) {
				log.error("Error processing request " + aRequest.toString(), e);
			}
		}

		return null;
	}
	
	public ServerResponse processTransaction(List<IPerceroObject> objectsToSave, List<IPerceroObject> objectsToRemove, String transactionId, Date transDate, String clientId) throws Exception {
		throw new Exception("Transactions are not currently supported.");
	}
	
	public Boolean pushClientUpdateJournals(String clientId, Collection<String> listUpdateJournals) {
		if (listUpdateJournals != null && listUpdateJournals.size() > 0) {
			try {
				if (listUpdateJournals.size() > 0) {
					try {
						// Optimization: create the JSON string of the object.
						String userId = accessManager.getClientUserId(clientId);
						String objectJson = "";
						int counter = 0;

						PushUpdateResponse pushUpdateResponse = null;
						IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
						for(String nextUpdateJournal : listUpdateJournals) {

							if (nextUpdateJournal == null || nextUpdateJournal.toString().length() <= 0)
								continue;
							String[] classValues = nextUpdateJournal.toString().split(":");
							if (classValues.length != 2)
								continue;
							
							String className = classValues[0];
							String classId = classValues[1];
							
							if (pushUpdateResponse == null) {
								pushUpdateResponse = new PushUpdateResponse();
								pushUpdateResponse.setObjectList(new ArrayList<BaseDataObject>());
							}
							
							pushUpdateResponse.setClientId(clientId);
							
							MappedClass mc = mcm.getMappedClassByClassName(className);
							IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mc.dataProviderName);
							IPerceroObject anObject = dataProvider.findById(new ClassIDPair(classId, className), userId);
							
							if (anObject != null) {
								pushUpdateResponse.getObjectList().add((BaseDataObject) anObject);
								if (anObject instanceof IPerceroObject) {
									if (counter> 0)
										objectJson += ",";
									objectJson += ((BaseDataObject)anObject).toJson();
									counter++;
								}
							}
						}

						if (pushUpdateResponse != null && counter > 0) {
							//pushObjectToRabbit(pushUpdateResponse, clientId);
							pushUpdateResponse.setObjectJson(objectJson);
							pushSyncHelper.pushSyncResponseToClient(pushUpdateResponse, clientId);
							//pushSyncHelper.pushJsonToRouting(pushUpdateResponse.toJson(objectJson, safeObjectMapper), PushUpdateResponse.class, clientId);
						}
						
					} catch(Exception e) {
						log.error("Error sending UpdateJournals to client" + clientId, e);
						return false;
					}
				}
			} catch(Exception  e) {
				log.error("Error pushing message", e);
				return false;
			}
		}
		
		return true;
	}
	
	public Boolean pushClientDeleteJournals(String clientId, Collection<String> listDeleteJournals) {
		if (listDeleteJournals != null && listDeleteJournals.size() > 0) {
			try {
				if (listDeleteJournals.size() > 0) {
					try {
						// Optimization: create the JSON string of the object.
						String objectJson = "";
						int counter = 0;

						PushDeleteResponse pushDeleteResponse = null;
						for(String nextUpdateJournal : listDeleteJournals) {

							if (nextUpdateJournal == null || nextUpdateJournal.toString().length() <= 0)
								continue;
							String[] classValues = nextUpdateJournal.toString().split(":");
							if (classValues.length != 2)
								continue;
							
							String className = classValues[0];
							String classId = classValues[1];
							
							if (pushDeleteResponse == null) {
								pushDeleteResponse = new PushDeleteResponse();
								pushDeleteResponse.setObjectList(new ArrayList<RemovedClassIDPair>());
							}
							
							pushDeleteResponse.setClientId(clientId);
							
							RemovedClassIDPair classIdPair = new RemovedClassIDPair();
							classIdPair.setClassName(className);
							classIdPair.setID(classId);
							
							if (classIdPair != null) {
								pushDeleteResponse.getObjectList().add(classIdPair);
								if (counter> 0)
									objectJson += ",";
								objectJson += classIdPair.toJson();
								counter++;
							}
						}

						if (pushDeleteResponse != null) {
							//pushObjectToRabbit(pushDeleteResponse, clientId);
							pushDeleteResponse.setObjectJson(objectJson);
							pushSyncHelper.pushSyncResponseToClient(pushDeleteResponse, clientId);
							//pushSyncHelper.pushJsonToRouting(pushDeleteResponse.toJson(objectJson, safeObjectMapper), PushDeleteResponse.class, clientId);
						}
						
					} catch(Exception e) {
						log.error("Error sending DeleteJournals to client" + clientId, e);
						return false;
					}
				}
			} catch(Exception  e) {
				log.error("Error pushing message", e);
				return false;
			}
		}
		
		return true;
	}

}

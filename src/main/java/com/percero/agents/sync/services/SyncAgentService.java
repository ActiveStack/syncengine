package com.percero.agents.sync.services;

import java.io.IOException;

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
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.connectors.ILogicConnector;
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
	List<ILogicConnector> connectorFactories;

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
	
	@Autowired @Qualifier("executorWithCallerRunsPolicy")
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
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);

		String dataId = (String) cacheDataStore.listIndex(RedisKeyUtils.dataRecord());

		if (dataId == null) {
			dataId = UUID.randomUUID().toString();
			cacheDataStore.lpushListValue(RedisKeyUtils.dataRecord(), dataId);
		}
		
		return dataId;
	}
	
	
	public PerceroList<IPerceroObject> getAllByName(String className, Boolean returnTotal, String clientId) throws Exception {
		return getAllByName(className, null, null, returnTotal, clientId);
	}

	public PerceroList<IPerceroObject> getAllByName(String className, Integer pageNumber, Integer pageSize, Boolean returnTotal, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
		
		// Get the MappedClass and determine which DataProvider provides data for this object.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(className);
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		String userId = accessManager.getClientUserId(clientId);
		PerceroList<IPerceroObject> result = dataProvider.getAllByName(className, pageNumber, pageSize, returnTotal, userId);
		
		if (result != null) {
			// Register an Zero ID object to indicate that this user wants updates to ALL objects of this type.
			IPerceroObject newInstance = mappedClass.newPerceroObject();
			newInstance.setID("0");
			postGetHelper.postGetObject((IPerceroObject) newInstance, userId, clientId);
		}
		
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unused" })
	public Map<String, Integer> countAllByName(Collection<String> classNames, String clientId) throws Exception {
		Map<String, Integer> result = new HashMap<String, Integer>();
		
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);

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
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
		
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
	
	public Object runProcess(String processName, Object queryArguments, String clientId) throws Exception {
		Object result = null;
		
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
		
		if (!StringUtils.hasText(processName))
			throw new SyncException(SyncException.INVALID_PROCESS_ERROR, SyncException.INVALID_PROCESS_ERROR_CODE);
		
		// Check to see if this has a prefix, which would route it to a ConnectorFactory.
		int prefixLocation = processName.indexOf(':');
		if (prefixLocation > 0) {
			String prefix = processName.substring(0, prefixLocation);
			String operation = processName.substring(prefixLocation+1);
			
			if (StringUtils.hasText(prefix) && StringUtils.hasText(operation)) {
				Iterator<ILogicConnector> itrConnectorFactories = connectorFactories.iterator();
				while (itrConnectorFactories.hasNext()) {
					ILogicConnector nextConnectorFactory = itrConnectorFactories.next();
					if (nextConnectorFactory.getConnectorPrefix().equalsIgnoreCase(prefix)) {
						// We have found our Connector Factory.
						if (queryArguments instanceof Object[] && ((Object[]) queryArguments).length > 0) {
							return nextConnectorFactory.runOperation(operation, clientId, ((Object[]) queryArguments)[0]);
						}
						else {
							return nextConnectorFactory.runOperation(operation, clientId, queryArguments);
						}
					}
				}
			}
		}
		
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
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
		
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
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
		
		if (theQueryObject instanceof IPerceroObject) {
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(theQueryObject.getClass().getCanonicalName());
			if (mappedClass != null) {
				String userId = accessManager.getClientUserId(clientId);
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				List<IPerceroObject> result = dataProvider.findByExample((IPerceroObject) theQueryObject, excludeProperties, userId, false);

				if (result != null && !result.isEmpty()) {
					postGetHelper.postGetObject(result, userId, clientId);
				}
				
				return result;
			}
		}
		
		return null;
	}

	public List<IPerceroObject> systemFindByExample(Object theQueryObject, List<String> excludeProperties) throws SyncException {
		if (theQueryObject instanceof IPerceroObject) {
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(theQueryObject.getClass().getCanonicalName());
			if (mappedClass != null) {
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				 return dataProvider.findByExample((IPerceroObject) theQueryObject, excludeProperties, null, false);
			}
		}
		
		return null;
	}
	
	@SuppressWarnings({ "rawtypes" })
	public Object findUnique(Object theQueryObject, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
		
		Class objectClass = theQueryObject.getClass();
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(objectClass.getName());
		if (mappedClass != null) {
			String userId = accessManager.getClientUserId(clientId);
			
			IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
			List<IPerceroObject> results = dataProvider.findByExample((IPerceroObject) theQueryObject, null, userId, false);
			IPerceroObject result = null;
			if (results != null && !results.isEmpty()) {
				result = results.get(0);
			}
			
			if (result != null) {
				IPerceroObject postGetResult = postGetHelper.postGetObject(result, userId, clientId);
				if (postGetResult != null) {
					result = postGetResult;
				}
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
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);

		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(theQueryObject.getClass().getCanonicalName());
		if (mappedClass != null) {
			String userId = accessManager.getClientUserId(clientId);
			
			IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
			List<IPerceroObject> result = dataProvider.findByExample((IPerceroObject) theQueryObject, excludeProperties, userId, false);
			
			if (result != null && !result.isEmpty()) {
				postGetHelper.postGetObject(result, userId, clientId);
			}
			
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
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
		
		// Get the MappedClass and determine which DataProvider provides data for this object.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(aClassName);
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		String userId = accessManager.getClientUserId(clientId);
		IPerceroObject result = dataProvider.findById(new ClassIDPair(anId, aClassName), userId);
		
		if (result != null) {
			IPerceroObject postGetResult = postGetHelper.postGetObject(result, userId, clientId);
			if (postGetResult != null) {
				result = postGetResult;
			}
		}
		
		return result;
	}

	public IPerceroObject systemGetById(String aClassName, String anId) {//throws Exception {
		// Get the MappedClass and determine which DataProvider provides data for this object.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(aClassName);
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		IPerceroObject result = dataProvider.findById(new ClassIDPair(anId, aClassName), null);
//		
//		if (result != null) {
//			try {
//				postGetHelper.postGetObject(result, null, null);
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
//		}

		return result;
	}
	
	public IPerceroObject systemGetById(ClassIDPair cip) { //throws Exception {
		// Get the MappedClass and determine which DataProvider provides data for this object.
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(cip.getClassName());
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		IPerceroObject result = dataProvider.findById(cip, null);
//		
//		if (result != null) {
//			try {
//				postGetHelper.postGetObject(result, null, null);
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
//		}

		return result;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends IPerceroObject> T systemGetByObject(T perceroObject) {//throws Exception {
		// Get the MappedClass and determine which DataProvider provides data for this object.
		if (perceroObject == null || !StringUtils.hasText(perceroObject.getID())) {
			return null;
		}
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		String className = perceroObject.getClass().getCanonicalName();
		MappedClass mappedClass = mcm.getMappedClassByClassName(className);
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		T result = (T) dataProvider.findById(new ClassIDPair(perceroObject.getID(), className), null);
//
//		if (result != null) {
//			try {
//				postGetHelper.postGetObject(result, null, null);
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
//		}
		
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
	public List<? extends Object> getHistory(ClassIDPair classIdPair, String clientId) throws Exception {
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
	public List<? extends Object> getHistory(String aClassName, String anId, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
		
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
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
		
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
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);

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

//						if (taskExecutor != null && false) {
//							taskExecutor.execute(new PostPutTask(postPutHelper, BaseDataObject.toClassIdPair((BaseDataObject) result), userId, clientId, pushToClient, changedFields));
//						} else {
							postPutHelper.postPutObject(BaseDataObject.toClassIdPair((BaseDataObject) result), userId, clientId, pushToClient, changedFields);
//						}
						
						// For each changed field, we look at the reverse mapped
						// relationship (if one exists) and update that object
						// (this was already done in the cache by the
						// cacheManager).
						if (changedFields != null && !changedFields.isEmpty()) {
							Iterator<Map.Entry<ClassIDPair, Collection<MappedField>>> itrChangedFieldEntryset = changedFields.entrySet().iterator();
							while (itrChangedFieldEntryset.hasNext()) {
								Map.Entry<ClassIDPair, Collection<MappedField>> nextEntry = itrChangedFieldEntryset.next();
								ClassIDPair thePair = nextEntry.getKey();
								Collection<MappedField> changedMappedFields = nextEntry.getValue();
								
								// If thePair is NOT the object being updated, then need to run the postPutHelper for the Pair object as well.
								if (!thePair.equals(classIdPair)) {
									Map<ClassIDPair, Collection<MappedField>> thePairChangedFields = new HashMap<ClassIDPair, Collection<MappedField>>(1);
									thePairChangedFields.put(thePair, changedMappedFields);
									
									// This will also run thePair through the ChangeWatcher check below.
//									if (taskExecutor != null && false) {
//										taskExecutor.execute(new PostPutTask(postPutHelper, thePair, userId, clientId, pushToClient, thePairChangedFields));
//									} else {
										postPutHelper.postPutObject(thePair, userId, clientId, pushToClient, thePairChangedFields);
//									}
								}
								else {
									Iterator<MappedField> itrChangedFields = changedMappedFields.iterator();
									String[] fieldNames = new String[changedMappedFields.size()];
									int i = 0;
									while (itrChangedFields.hasNext()) {
										MappedField nextChangedField = itrChangedFields.next();
										fieldNames[i] = nextChangedField.getField().getName();
										i++;
									}
									accessManager.checkChangeWatchers(thePair, fieldNames, null, null);
								}
							}
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
					result = handleUpdateObject_ChangedFields(perceroObject, transactionId, updateDate, userId,
							pushToUser, changedFields, dataProvider);
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

	/**
	 * @param perceroObject
	 * @param transactionId
	 * @param updateDate
	 * @param userId
	 * @param pushToUser
	 * @param changedFields
	 * @param dataProvider
	 * @return
	 * @throws SyncException
	 * @throws IOException
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws Exception
	 */
	private IPerceroObject handleUpdateObject_ChangedFields(IPerceroObject perceroObject, String transactionId,
			Date updateDate, String userId, boolean pushToUser, Map<ClassIDPair, Collection<MappedField>> changedFields,
			IDataProvider dataProvider)
					throws SyncException, IOException, JsonGenerationException, JsonMappingException, Exception {
		IPerceroObject result;
		result = dataProvider.putObject(perceroObject, changedFields, null);
		
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
			if (storeHistory && (result instanceof IHistoryObject)) {
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
				taskExecutor.execute(new PostPutTask(postPutHelper, BaseDataObject.toClassIdPair((BaseDataObject) result), userId, null, pushToUser, changedFields));
			} else {
				postPutHelper.postPutObject(BaseDataObject.toClassIdPair((BaseDataObject) result), userId, null, pushToUser, changedFields);
			}
		}
		return result;
	}

	public ServerResponse createObject(IPerceroObject perceroObject, String clientId) throws Exception {
		return createObject(perceroObject, clientId, false);
	}
	
	public ServerResponse createObject(IPerceroObject perceroObject, String clientId, Boolean pushToClient) throws Exception {
		ServerResponse response = new ServerResponse();
		
		log.debug("Create: " + perceroObject.getClass().getName() + ": " + perceroObject.getID());
		
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
			
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
	
	public <T extends IPerceroObject> T systemCreateObject(T perceroObject, String userId) throws SyncException {
		
		log.debug("Create: " + perceroObject.getClass().getName() + ": " + perceroObject.getID());
		
		if (perceroObject != null) {
			try {
				IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
				MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				T result = dataProvider.createObject(perceroObject, null);
				
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
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
		
		try {
			
			String userId = accessManager.getClientUserId(clientId);
			
			boolean hasAccess = true;
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(theClassIdPair.getClassName());
			if (mappedClass != null) {
				IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
				hasAccess = dataProvider.getDeleteAccess(theClassIdPair, userId);

				if (hasAccess) {
					if (systemDeleteObject(theClassIdPair, clientId, pushToClient)) {
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

	public boolean systemDeleteObject(ClassIDPair classIdPair, String clientId, boolean pushToUser) throws Exception {
		return systemDeleteObject(classIdPair, clientId, pushToUser, new HashSet<ClassIDPair>());
	}

	/**
	 *	- The pushes out notifications to ALL related objects.
	 *	- The DataProvider should handle the updated target objects by updating the cache (IFF they are in the cache)
	 *	- The updated objects source objects are handled by the cascadeRemoveFieldReferences and nulledOnRemoveFieldReferences
	 *		since these objects need to be deleted/updated in the data store as well.
	 * @param classIdPair
	 * @param clientId
	 * @param pushToUser
	 * @param deletedObjects
	 * @return
	 * @throws Exception
	 */
	/**
	 * @param classIdPair
	 * @param clientId
	 * @param pushToUser
	 * @param deletedObjects
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public boolean systemDeleteObject(ClassIDPair classIdPair, String clientId, boolean pushToUser, Collection<ClassIDPair> deletedObjects) throws Exception {

		if(classIdPair == null)
			return true;
		if (deletedObjects.contains(classIdPair))
			return true;
		else
			deletedObjects.add(classIdPair);
		
		String userId = accessManager.getClientUserId(clientId);
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(classIdPair.getClassName());
		if (mappedClass == null) {
			return false;
		}
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
		
		IPerceroObject perceroObject = (IPerceroObject) dataProvider.findById(classIdPair, null);
		
		if (perceroObject == null) {
			return true;
		}

		handleDeleteObject_CascadeRemove(perceroObject, mappedClass, deletedObjects, clientId);

		handleDeleteObject_CascadeNull(perceroObject, mappedClass, userId);
		
		// This is a list of objects that need to be notified that they have
		// somehow changed. That somehow is directly linked to these objects
		// relationship with the deleted object.
		Map<ClassIDPair, MappedField> objectsToUpdate = mappedClass.getRelatedClassIdPairMappedFieldMap(perceroObject, false);
		
		if (dataProvider.deleteObject(BaseDataObject.toClassIdPair(perceroObject), userId)) {
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
			
			Iterator<Entry<ClassIDPair, MappedField>> itrObjectsToUpdate = objectsToUpdate.entrySet().iterator();
			while (itrObjectsToUpdate.hasNext()) {
				Entry<ClassIDPair, MappedField> nextObjectToUpdate = itrObjectsToUpdate.next();
				Map<ClassIDPair, Collection<MappedField>> changedFields = new HashMap<ClassIDPair, Collection<MappedField>>();
				Collection<MappedField> changedMappedFields = new ArrayList<MappedField>(1);
				changedMappedFields.add(nextObjectToUpdate.getValue());
				changedFields.put(nextObjectToUpdate.getKey(), changedMappedFields);
				postPutHelper.postPutObject(nextObjectToUpdate.getKey(), userId, clientId, true, changedFields);
			}
			
			return true;
		}

		return false;
	}

	/**
	 * Given the perceroObject, goes through each relationship where the related
	 * object relationship must be set to NULL before perceroObject can be
	 * removed -> cascade update.
	 * 
	 * @param perceroObject
	 * @param mappedClass
	 * @param userId
	 * @throws SyncDataException
	 */
	private void handleDeleteObject_CascadeNull(IPerceroObject perceroObject, MappedClass mappedClass, String userId)
			throws SyncDataException {
		Iterator<Map.Entry<MappedField, MappedField>> itrNulledOnRemoveFieldReferencesEntrySet = mappedClass.getNulledOnRemoveFieldReferences().entrySet().iterator();
		while (itrNulledOnRemoveFieldReferencesEntrySet.hasNext()) {
			Map.Entry<MappedField, MappedField> nextEntry = itrNulledOnRemoveFieldReferencesEntrySet.next();
			MappedField nextToNullMappedFieldRef = nextEntry.getKey();
			try {
				MappedField nextMappedField = nextEntry.getValue();
				if (nextMappedField == null) {
					// There is no direct link from mappedClass, so need to get all by example.
					IPerceroObject tempObject = (IPerceroObject) nextToNullMappedFieldRef.getMappedClass().newPerceroObject();
					nextToNullMappedFieldRef.getSetter().invoke(tempObject, perceroObject);
					IDataProvider dataProviderRef = dataProviderManager.getDataProviderByName(nextToNullMappedFieldRef.getMappedClass().dataProviderName);
					List<IPerceroObject> referencingObjects = dataProviderRef.findByExample(tempObject, null, null, false);
					Iterator<IPerceroObject> itrReferencingObjects = referencingObjects.iterator();
					while (itrReferencingObjects.hasNext()) {
						IPerceroObject nextReferencingObject = itrReferencingObjects.next();
						nextToNullMappedFieldRef.setToNull(nextReferencingObject);
//							systemPutObject((IPerceroObject) nextReferencingObject, null, new Date(), userId, true);
//
//							TODO: Is this better?
						Map<ClassIDPair, Collection<MappedField>> changedFields = new HashMap<ClassIDPair, Collection<MappedField>>(1);
						Collection<MappedField> mappedFields = new ArrayList<MappedField>(1);
						mappedFields.add(nextToNullMappedFieldRef);
						changedFields.put(BaseDataObject.toClassIdPair(nextReferencingObject), mappedFields);
						handleUpdateObject_ChangedFields((IPerceroObject) nextReferencingObject, null, new Date(), userId, true, changedFields, dataProviderManager.getDataProviderByName(nextToNullMappedFieldRef.getMappedClass().dataProviderName));
					}
				}
				else {
					// Since perceroObject is fully loaded, we have the list hanging off of perceroObject
					if (nextMappedField instanceof MappedFieldList) {
						List<IPerceroObject> referencingObjects = (List<IPerceroObject>) nextMappedField.getGetter().invoke(perceroObject);
						if (referencingObjects != null && !referencingObjects.isEmpty()) {
							for(IPerceroObject nextReferencingObject : referencingObjects) {
								nextToNullMappedFieldRef.setToNull(nextReferencingObject);
//									systemPutObject((IPerceroObject) nextReferencingObject, null, new Date(), userId, true);
								Map<ClassIDPair, Collection<MappedField>> changedFields = new HashMap<ClassIDPair, Collection<MappedField>>(1);
								Collection<MappedField> mappedFields = new ArrayList<MappedField>(1);
								mappedFields.add(nextToNullMappedFieldRef);
								changedFields.put(BaseDataObject.toClassIdPair(nextReferencingObject), mappedFields);
								handleUpdateObject_ChangedFields((IPerceroObject) nextReferencingObject, null, new Date(), userId, true, changedFields, dataProviderManager.getDataProviderByName(nextToNullMappedFieldRef.getMappedClass().dataProviderName));
							}
						}
					}
					else if (nextMappedField instanceof MappedFieldPerceroObject) {
						IPerceroObject referencingObject = (IPerceroObject) nextMappedField.getGetter().invoke(perceroObject);
						if (referencingObject != null) {
							nextToNullMappedFieldRef.setToNull(referencingObject);
//								systemPutObject((IPerceroObject) referencingObject, null, new Date(), userId, true);
							Map<ClassIDPair, Collection<MappedField>> changedFields = new HashMap<ClassIDPair, Collection<MappedField>>(1);
							Collection<MappedField> mappedFields = new ArrayList<MappedField>(1);
							mappedFields.add(nextToNullMappedFieldRef);
							changedFields.put(BaseDataObject.toClassIdPair(referencingObject), mappedFields);
							handleUpdateObject_ChangedFields((IPerceroObject) referencingObject, null, new Date(), userId, true, changedFields, dataProviderManager.getDataProviderByName(nextToNullMappedFieldRef.getMappedClass().dataProviderName));
						}
					}
					else {
						// Fall back case if we don't have a special way of handling this type of MappedField
						IPerceroObject tempObject = (IPerceroObject) nextToNullMappedFieldRef.getMappedClass().clazz.newInstance();
						nextToNullMappedFieldRef.getSetter().invoke(tempObject, perceroObject);
						IDataProvider dataProviderRef = dataProviderManager.getDataProviderByName(nextToNullMappedFieldRef.getMappedClass().dataProviderName);
						List<IPerceroObject> referencingObjects = dataProviderRef.findByExample(tempObject, null, null, false);
						Iterator<IPerceroObject> itrReferencingObjects = referencingObjects.iterator();
						while (itrReferencingObjects.hasNext()) {
							IPerceroObject nextReferencingObject = itrReferencingObjects.next();
							nextToNullMappedFieldRef.setToNull(nextReferencingObject);
//								systemPutObject((IPerceroObject) nextReferencingObject, null, new Date(), userId, true);
							Map<ClassIDPair, Collection<MappedField>> changedFields = new HashMap<ClassIDPair, Collection<MappedField>>(1);
							Collection<MappedField> mappedFields = new ArrayList<MappedField>(1);
							mappedFields.add(nextToNullMappedFieldRef);
							changedFields.put(BaseDataObject.toClassIdPair(nextReferencingObject), mappedFields);
							handleUpdateObject_ChangedFields((IPerceroObject) nextReferencingObject, null, new Date(), userId, true, changedFields, dataProviderManager.getDataProviderByName(nextToNullMappedFieldRef.getMappedClass().dataProviderName));
						}
					}
				}
			} catch(Exception e) {
				throw new SyncDataException(SyncDataException.DELETE_OBJECT_ERROR, SyncDataException.DELETE_OBJECT_ERROR_CODE, e);
			}
		}
	}

	/**
	 * Given the perceroObject, goes through each relationship where the related
	 * object must be removed before perceroObject can be removed -> cascade
	 * remove.
	 * 
	 * @param perceroObject
	 * @param mappedClass
	 * @param deletedObjects
	 * @param clientId
	 */
	private void handleDeleteObject_CascadeRemove(IPerceroObject perceroObject, 
			MappedClass mappedClass,
			Collection<ClassIDPair> deletedObjects,
			String clientId) throws SyncDataException {

		Iterator<Map.Entry<MappedField, MappedField>> itrCascadeRemoveFieldReferencesEntrySet = mappedClass.getCascadeRemoveFieldReferences().entrySet().iterator();
		while (itrCascadeRemoveFieldReferencesEntrySet.hasNext()) {
			Map.Entry<MappedField, MappedField> nextEntry = itrCascadeRemoveFieldReferencesEntrySet.next();
			MappedField nextRemoveMappedFieldRef = nextEntry.getKey();
			try {
				MappedField nextMappedField = nextEntry.getValue();
				System.out.println(nextMappedField);
				if (nextMappedField == null) {
					// There is no direct link from mappedClass, so need to get all by example.
					IPerceroObject tempObject = (IPerceroObject) nextRemoveMappedFieldRef.getMappedClass().newPerceroObject();
					nextRemoveMappedFieldRef.getSetter().invoke(tempObject, perceroObject);
					IDataProvider dataProviderRef = dataProviderManager.getDataProviderByName(nextRemoveMappedFieldRef.getMappedClass().dataProviderName);
					List<IPerceroObject> referencingObjects = dataProviderRef.findByExample(tempObject, null, null, false);
					Iterator<IPerceroObject> itrReferencingObjects = referencingObjects.iterator();
					while (itrReferencingObjects.hasNext()) {
						IPerceroObject nextReferencingObject = itrReferencingObjects.next();
						systemDeleteObject(BaseDataObject.toClassIdPair(nextReferencingObject), clientId, true, deletedObjects);
					}
				}
				else {
					// Since perceroObject is fully loaded, we have the list hanging off of perceroObject
					if (nextMappedField instanceof MappedFieldList) {
						List<IPerceroObject> referencingObjects = (List<IPerceroObject>) nextMappedField.getGetter().invoke(perceroObject);
						if (referencingObjects != null && !referencingObjects.isEmpty()) {
							for(IPerceroObject nextReferencingObject : referencingObjects) {
								systemDeleteObject(BaseDataObject.toClassIdPair(nextReferencingObject), clientId, true, deletedObjects);
							}
						}
					}
					else if (nextMappedField instanceof MappedFieldPerceroObject) {
						IPerceroObject referencingObject = (IPerceroObject) nextMappedField.getGetter().invoke(perceroObject);
						if (referencingObject != null) {
							systemDeleteObject(BaseDataObject.toClassIdPair(referencingObject), clientId, true, deletedObjects);
						}
					}
					else {
						// Fall back case if we don't have a special way of handling this type of MappedField
						IPerceroObject tempObject = (IPerceroObject) nextRemoveMappedFieldRef.getMappedClass().clazz.newInstance();
						nextRemoveMappedFieldRef.getSetter().invoke(tempObject, perceroObject);
						IDataProvider dataProviderRef = dataProviderManager.getDataProviderByName(nextRemoveMappedFieldRef.getMappedClass().dataProviderName);
						List<IPerceroObject> referencingObjects = dataProviderRef.findByExample(tempObject, null, null, false);
						Iterator<IPerceroObject> itrReferencingObjects = referencingObjects.iterator();
						while (itrReferencingObjects.hasNext()) {
							IPerceroObject nextReferencingObject = itrReferencingObjects.next();
							systemDeleteObject(BaseDataObject.toClassIdPair(nextReferencingObject), clientId, true, deletedObjects);
						}
					}
				}
			} catch(Exception e) {
				throw new SyncDataException(SyncDataException.DELETE_OBJECT_ERROR, SyncDataException.DELETE_OBJECT_ERROR_CODE, e);
			}
		}
	}
	

	public void updatesReceived(ClassIDPair[] theObjects, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);

		// Remove UpdateJournals for this client.
		accessManager.deleteUpdateJournals(clientId, theObjects);
	}

	public void deletesReceived(ClassIDPair[] theObjects, String clientId) throws Exception {
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
		
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
						StringBuilder objectJson = new StringBuilder();
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
										objectJson.append(',');
									objectJson.append(((BaseDataObject)anObject).toJson());
									counter++;
								}
							}
						}

						if (pushUpdateResponse != null && counter > 0) {
							//pushObjectToRabbit(pushUpdateResponse, clientId);
							pushUpdateResponse.setObjectJson(objectJson.toString());
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
						StringBuilder objectJson = new StringBuilder();
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
									objectJson.append(',');
								objectJson.append(classIdPair.toJson());
								counter++;
							}
						}

						if (pushDeleteResponse != null) {
							//pushObjectToRabbit(pushDeleteResponse, clientId);
							pushDeleteResponse.setObjectJson(objectJson.toString());
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

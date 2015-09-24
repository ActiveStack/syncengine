package com.percero.agents.sync.services;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.ClassIDPairs;
import com.percero.agents.sync.vo.ServerRequest;
import com.percero.agents.sync.vo.ServerResponse;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;

public interface ISyncAgentService {

	public String getVersion();
	public void processManifest();
	
	public Boolean isInitialized();
	
	public String getDataId(String clientId) throws Exception;

	public ServerResponse createObject(IPerceroObject perceroObject, String clientId) throws Exception;
	public ServerResponse createObject(IPerceroObject perceroObject, String clientId, Boolean pushToClient) throws Exception;
	public <T extends IPerceroObject> T systemCreateObject(T perceroObject, String userId) throws SyncException;
	public ServerResponse deleteObject(ClassIDPair theClassIdPair, String clientId) throws Exception;
	public ServerResponse deleteObject(ClassIDPair theClassIdPair, String clientId, Boolean pushToClient) throws Exception;
	public ServerResponse deleteObjectById(String theClassName, String theId, String clientId) throws Exception;
	public ServerResponse deleteObjectById(String theClassName, String theId, String clientId, Boolean pushToClient) throws Exception;
	public boolean systemDeleteObject(IPerceroObject perceroObject, String clientId, boolean pushToUser) throws Exception;
	public boolean systemDeleteObject(IPerceroObject perceroObject, String clientId, boolean pushToUser, Collection<IPerceroObject> deletedObjects) throws Exception;

	public ServerResponse putObject(IPerceroObject perceroObject, String transactionId, Date updateDate, String clientId) throws Exception;
	public ServerResponse putObject(IPerceroObject perceroObject, String transactionId, Date updateDate, String clientId, Boolean pushToClient) throws Exception;
	public boolean systemPutObject(IPerceroObject perceroObject, String transactionId, Date updateDate, String userId, boolean pushToUser);

	public ServerResponse processTransaction(List<IPerceroObject> objectsToSave, List<IPerceroObject> objectsToRemove, String transactionId, Date transDate, String clientId) throws Exception;
	
	public Map<String, Integer> countAllByName(Collection<String> classNames, String clientId) throws Exception;
	public PerceroList<?> getAllByName(String className, Boolean returnTotal, String clientId) throws Exception;
	public PerceroList<?> getAllByName(String className, Integer pageNumber, Integer pageSize, Boolean returnTotal, String clientId) throws Exception;
	public Object findById(ClassIDPair classIdPair, String clientId) throws Exception;
	public Object findById(String aClassName, String anId, String clientId) throws Exception;
	public <T extends IPerceroObject> T systemGetByObject(T perceroObject);
	public IPerceroObject systemGetById(ClassIDPair cip);
	public IPerceroObject systemGetById(String aClassName, String anId);
	public List<IPerceroObject> systemFindByExample(Object theQueryObject, List<String> excludeProperties) throws SyncException;
	public List<IPerceroObject> findByIds(List<ClassIDPairs> classIdList, String clientId) throws Exception;
	public Object findByExample(Object theQueryObject, List<String> excludeProperties, String clientId) throws Exception;
	public Object findUnique(Object theQueryObject, String clientId) throws Exception;
	public Object runQuery(String className, String queryName, Object[] queryArguments, String clientId) throws Exception;
	public Object runProcess(String processName, Object queryArguments, String clientId) throws Exception;
	public Object getChangeWatcherValue(ClassIDPair classIdPair, String fieldName, String[] params, String clientId) throws Exception;
	public List<? extends Object> getHistory(String aClassName, String anId, String clientId) throws Exception;

	public Object searchByExample(Object theQueryObject, String clientId) throws Exception;
	public Object searchByExample(Object theQueryObject, List<String> excludeProperties, String clientId) throws Exception;

	public void updatesReceived(ClassIDPair[] theObjects, String clientId) throws Exception;
	public Boolean pushClientUpdateJournals(String clientId, Collection<String> listUpdateJournals);
	public void deletesReceived(ClassIDPair[] theObjects, String clientId) throws Exception;
	public Boolean pushClientDeleteJournals(String clientId, Collection<String> listDeleteJournals);

	public ServerResponse request(ServerRequest aRequest, String clientId) throws Exception;

}

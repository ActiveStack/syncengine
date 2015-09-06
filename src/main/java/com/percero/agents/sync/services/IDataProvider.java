package com.percero.agents.sync.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.ClassIDPairs;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;

public interface IDataProvider {

	public void initialize();
	
	public String getName();
	
	public Integer countAllByName(String className, String userId) throws Exception;
	public PerceroList<IPerceroObject> getAllByName(Object aName, Integer pageNumber, Integer pageSize, Boolean returnTotal, String userId) throws Exception;
	public List<Object> runQuery(MappedClass mappedClass, String queryName, Object[] queryArguments, String clientId);
	public IPerceroObject findById(ClassIDPair classIdPair, String userId);
	public <T extends IPerceroObject> T systemGetById(ClassIDPair classIdPair);
	public <T extends IPerceroObject> T systemGetById(ClassIDPair classIdPair, boolean ignoreCache);
	public List<IPerceroObject> findByIds(ClassIDPairs classIdPairs, String userId);
	public IPerceroObject findUnique(IPerceroObject theQueryObject, String userId);
	public List<IPerceroObject> findByExample(IPerceroObject theQueryObject, List<String> excludeProperties, String userId);
	public List<IPerceroObject> systemFindByExample(IPerceroObject theQueryObject, List<String> excludeProperties);
	public List<IPerceroObject> searchByExample(IPerceroObject theQueryObject, List<String> excludeProperties, String userId);
	public Boolean getReadAccess(ClassIDPair classIdPair, String userId);
	public Boolean getDeleteAccess(ClassIDPair classIdPair, String userId);

	public <T extends IPerceroObject> T systemCreateObject(IPerceroObject perceroObject) throws SyncException;
	public IPerceroObject createObject(IPerceroObject perceroObject, String userId) throws SyncException;
	public IPerceroObject putObject(IPerceroObject perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields, String userId) throws SyncException;
	public IPerceroObject systemPutObject(IPerceroObject perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields) throws SyncException;
	public Boolean deleteObject(ClassIDPair theClassIdPair, String userId) throws SyncException;
	public Boolean systemDeleteObject(IPerceroObject perceroObject) throws SyncException;
	
	//public Object cleanObject(Object object, String userId);
	public Object cleanObject(Object object, Session s, String userId);

	/**
	 * Returns a map of changed fields for the base IPerceroObject as well as all associated objects that will
	 * have changed due to the nature of the changes to newObject.
	 * @param newObject
	 * @return
	 */
	public Map<ClassIDPair, Collection<MappedField>> getChangedMappedFields(IPerceroObject newObject);
}

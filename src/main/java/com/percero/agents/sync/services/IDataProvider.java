package com.percero.agents.sync.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	public PerceroList<IPerceroObject> getAllByName(String className, Integer pageNumber, Integer pageSize, Boolean returnTotal, String userId) throws Exception;
	public Set<ClassIDPair> getAllClassIdPairsByName(String className) throws Exception;
	public List<Object> runQuery(MappedClass mappedClass, String queryName, Object[] queryArguments, String clientId) throws SyncException;
	public IPerceroObject findById(ClassIDPair classIdPair, String userId);
	public IPerceroObject findById(ClassIDPair classIdPair, String userId, Boolean ignoreCache);
	public IPerceroObject retrieveCachedObject(ClassIDPair classIdPair) throws Exception;
	public List<IPerceroObject> findByIds(ClassIDPairs classIdPairs, String userId);
	public List<IPerceroObject> findByIds(ClassIDPairs classIdPairs, String userId, Boolean ignoreCache);
//	public IPerceroObject findUnique(IPerceroObject theQueryObject, String userId);
	public List<IPerceroObject> findByExample(IPerceroObject theQueryObject, List<String> excludeProperties, String userId, Boolean shellOnly) throws SyncException;
//	public List<IPerceroObject> systemFindByExample(IPerceroObject theQueryObject, List<String> excludeProperties);
//	public List<IPerceroObject> searchByExample(IPerceroObject theQueryObject, List<String> excludeProperties, String userId);
	public Boolean getReadAccess(ClassIDPair classIdPair, String userId);
	public Boolean getDeleteAccess(ClassIDPair classIdPair, String userId);

//	public <T extends IPerceroObject> T systemCreateObject(IPerceroObject perceroObject) throws SyncException;
	public <T extends IPerceroObject> T createObject(T perceroObject, String userId) throws SyncException;
	public <T extends IPerceroObject> T putObject(T perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields, String userId) throws SyncException;
//	public IPerceroObject systemPutObject(IPerceroObject perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields) throws SyncException;
	public Boolean deleteObject(ClassIDPair theClassIdPair, String userId) throws SyncException;
//	public Boolean systemDeleteObject(IPerceroObject perceroObject) throws SyncException;
	
	//public Object cleanObject(Object object, String userId);
//	public Object cleanObject(Object object, Session s, String userId);

	/**
	 * Returns a map of changed fields for the base IPerceroObject as well as all associated objects that will
	 * have changed due to the nature of the changes to newObject.
	 * @param newObject
	 * @return
	 */
	public Map<ClassIDPair, Collection<MappedField>> getChangedMappedFields(IPerceroObject newObject);
	public Map<ClassIDPair, Collection<MappedField>> getChangedMappedFields(IPerceroObject newObject, boolean ignoreCache);
	public Map<ClassIDPair, Collection<MappedField>> getChangedMappedFields(IPerceroObject oldObject, IPerceroObject compareObject);
	
	/**
	 * Given the mappedField, returns ALL objects in the relationship described by the mappedField.
	 * If shellOnly is TRUE, then only a shell object will be returned, which is an Object with only it's ID set.
	 * Will enforce access rights based on the userId.
	 * Returns a cleaned object, meaning all its relationships are filled in with shell objects.
	 * 
	 * @param perceroObject
	 * @param mappedField
	 * @param shellOnly
	 * @param userId
	 * @return List<IPerceroObject>
	 * @throws SyncException 
	 */
	public List<IPerceroObject> findAllRelatedObjects(IPerceroObject perceroObject, MappedField mappedField, Boolean shellOnly, String userId) throws SyncException;

	public List<IPerceroObject> getAllByRelationship(MappedField mappedField, ClassIDPair targetClassIdPair, Boolean shellOnly, String userId) throws SyncException;

	IPerceroObject cleanObject(IPerceroObject perceroObject, String userId)
			throws SyncException;

	List<IPerceroObject> cleanObject(List<IPerceroObject> perceroObjects,
			String userId) throws SyncException;
}

package com.percero.agents.sync.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.ClassIDPairs;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;

public interface IDataAccessObject<T extends IPerceroObject> {
	
	Boolean hasCreateAccess(ClassIDPair classIdPair, String userId);
	Boolean hasReadAccess(ClassIDPair classIdPair, String userId);
	Boolean hasUpdateAccess(ClassIDPair classIdPair, String userId);
	Boolean hasDeleteAccess(ClassIDPair classIdPair, String userId);

	PerceroList<T> getAll(Integer pageNumber, Integer pageSize, Boolean returnTotal, String userId, Boolean shellOnly) throws Exception;
	Integer countAll(String userId) throws SyncException;

	List<T> retrieveAllByRelationship(MappedField mappedField, ClassIDPair targetClassIdPair, Boolean shellOnly, String userId) throws SyncException;

	List<T> findByExample(T theQueryObject, List<String> excludeProperties, String userId, Boolean shellOnly) throws SyncException;

	T retrieveObject(ClassIDPair classIdPair, String userId, Boolean shellOnly) throws SyncException;
	
	List<T> retrieveObjects(ClassIDPairs classIdPairs, String userId, Boolean shellOnly) throws SyncException;
	
	T createObject(T percero, String userIdObject) throws SyncException;
	
	T updateObject(T perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields, String userId) throws SyncException;
	
	Boolean deleteObject(ClassIDPair classIdPair, String userId) throws SyncException;

	List<Object> runQuery(String queryName, Object[] queryArguments, String userId) throws SyncException;
	T cleanObjectForUser(T perceroObject,
			String userId);
}

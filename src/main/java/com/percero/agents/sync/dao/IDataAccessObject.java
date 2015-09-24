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

/**
 * Defines the interface between ActiveStack and a data store. These are the
 * methods required by ActiveStack to successfully interact with object of
 * type<T>.
 * 
 * @author Collin Brown
 * 
 * @param <T>
 */
public interface IDataAccessObject<T extends IPerceroObject> {
	
	/**
	 * Given the classIdPair, does the user have permissions to create the
	 * corresponding object.
	 * 
	 * @param classIdPair
	 * @param userId
	 * @return
	 */
	Boolean hasCreateAccess(ClassIDPair classIdPair, String userId);

	/**
	 * Given the classIdPair, does the user have permissions to read the
	 * corresponding object.
	 * 
	 * @param classIdPair
	 * @param userId
	 * @return
	 */
	Boolean hasReadAccess(ClassIDPair classIdPair, String userId);
	
	/**
	 * Given the classIdPair, does the user have permissions to update the
	 * corresponding object.
	 * 
	 * @param classIdPair
	 * @param userId
	 * @return
	 */
	Boolean hasUpdateAccess(ClassIDPair classIdPair, String userId);

	/**
	 * Given the classIdPair, does the user have permissions to delete the
	 * corresponding object.
	 * 
	 * @param classIdPair
	 * @param userId
	 * @return
	 */
	Boolean hasDeleteAccess(ClassIDPair classIdPair, String userId);

	/**
	 * Returns a list of all objects of this type, given the pageNumber (offset)
	 * and pageSize (limit). If shellOnly is TRUE, then only the object.ID's
	 * need to be filled in (this is typically used when populating related
	 * objects. If returnTotal is TRUE, then the List should also include the
	 * total number of records of this type<T>. This is typically used when
	 * accessing the first page of results so that the caller can know how many
	 * pages/objects there are in total.
	 * 
	 * @param pageNumber
	 * @param pageSize
	 * @param returnTotal
	 * @param userId
	 * @param shellOnly
	 * @return
	 * @throws Exception
	 */
	PerceroList<T> getAll(Integer pageNumber, Integer pageSize, Boolean returnTotal, String userId, Boolean shellOnly) throws Exception;

	/**
	 * Returns the number of records of this type<T>.
	 * @param userId
	 * @return
	 * @throws SyncException
	 */
	Integer countAll(String userId) throws SyncException;

	/**
	 * Returns a list of all objects that point to the targetClassIdPair in the
	 * specificied mappedField. This is typically used when filling a list on
	 * the target object.
	 * 
	 * @param mappedField
	 * @param targetClassIdPair
	 * @param shellOnly
	 * @param userId
	 * @return
	 * @throws SyncException
	 */
	List<T> retrieveAllByRelationship(MappedField mappedField, ClassIDPair targetClassIdPair, Boolean shellOnly, String userId) throws SyncException;

	/**
	 * Given the query object, returns a list of all matching objects.
	 * NULL/empty fields, as well as excludeProperties, are ignored in the
	 * search. If shellOnly is TRUE, then only the ID's are filled in on the
	 * resulting objects.
	 * 
	 * @param theQueryObject
	 * @param excludeProperties
	 * @param userId
	 * @param shellOnly
	 * @return
	 * @throws SyncException
	 */
	List<T> findByExample(T theQueryObject, List<String> excludeProperties, String userId, Boolean shellOnly) throws SyncException;

	/**
	 * Retrieves an object identified by the classIdPair. If shellOnly is TRUE,
	 * then only the ID's are filled in on the resulting objects.
	 * 
	 * @param classIdPair
	 * @param userId
	 * @param shellOnly
	 * @return
	 * @throws SyncException
	 */
	T retrieveObject(ClassIDPair classIdPair, String userId, Boolean shellOnly) throws SyncException;
	
	/**
	 * Retrieves the list of object identified by the classIdPairs. If shellOnly
	 * is TRUE, then only the ID's are filled in on the resulting objects.
	 * 
	 * @param classIdPairs
	 * @param userId
	 * @param shellOnly
	 * @return
	 * @throws SyncException
	 */
	List<T> retrieveObjects(ClassIDPairs classIdPairs, String userId, Boolean shellOnly) throws SyncException;
	
	/**
	 * Inserts perceroObject into the data store.
	 * 
	 * @param perceroObject
	 * @param userId
	 * @return
	 * @throws SyncException
	 */
	T createObject(T perceroObject, String userId) throws SyncException;
	
	/**
	 * Updates perceroObject in the data store. If supplied, the list of
	 * changedFields is used to optimize the update.
	 * 
	 * @param perceroObject
	 * @param changedFields
	 * @param userId
	 * @return
	 * @throws SyncException
	 */
	T updateObject(T perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields, String userId) throws SyncException;
	
	/**
	 * Deletes the object identified by classIdPair from the data store.
	 * 
	 * @param classIdPair
	 * @param userId
	 * @return
	 * @throws SyncException
	 */
	Boolean deleteObject(ClassIDPair classIdPair, String userId) throws SyncException;

	/**
	 * Runs the query against the data store. This may not be supported by all
	 * IDataAccessObjects.
	 * 
	 * @param queryName
	 * @param queryArguments
	 * @param userId
	 * @return
	 * @throws SyncException
	 */
	List<Object> runQuery(String queryName, Object[] queryArguments, String userId) throws SyncException;

	/**
	 * Given the perceroObject, returns ONLY the portions of that perceroObject
	 * that userId has permissions to READ (which may be the entire object).
	 * This should also filter any lists off of perceroObject.
	 * 
	 * @param perceroObject
	 * @param userId
	 * @return
	 */
	T cleanObjectForUser(T perceroObject,
			String userId);
}

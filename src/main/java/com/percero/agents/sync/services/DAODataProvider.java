package com.percero.agents.sync.services;

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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.PropertyValueException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.impl.SessionImpl;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.dao.IDataAccessObject;
import com.percero.agents.sync.datastore.ICacheDataStore;
import com.percero.agents.sync.exceptions.SyncDataException;
import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.hibernate.SyncHibernateUtils;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.JpqlQuery;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.metadata.MappedFieldList;
import com.percero.agents.sync.metadata.MappedFieldPerceroObject;
import com.percero.agents.sync.metadata.MappedFieldString;
import com.percero.agents.sync.metadata.SqlQuery;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.ClassIDPairs;
import com.percero.agents.sync.vo.IJsonObject;
import com.percero.framework.metadata.IMappedQuery;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;

@Component
public class DAODataProvider implements IDataProvider {

	// TODO: Better manage Hibernate Sessions (opening and closing).

	private static final Logger log = Logger.getLogger(DAODataProvider.class);
	
	public void initialize()
	{
		// Do nothing.
	}
	
	public String getName() {
		return "syncAgent";
	}
	
	@Autowired
	IDataProviderManager dataProviderManager;
	public void setDataProviderManager(IDataProviderManager value) {
		dataProviderManager = value;
	}
	
	@Autowired
	DAORegistry daoRegistry;
	public void setDaoRegistry(DAORegistry value) {
		daoRegistry = value;
	}
	
	@Autowired
	ICacheDataStore cacheDataStore;
	
	@Autowired
	Long cacheTimeout = Long.valueOf(60 * 60 * 24 * 14);	// Two weeks

	@Autowired
	ObjectMapper safeObjectMapper;

	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	// TODO: @Transactional(readOnly=true)
	public PerceroList<IPerceroObject> getAllByName(String className, Integer pageNumber, Integer pageSize, Boolean returnTotal, String userId) throws Exception {
		IDataAccessObject<IPerceroObject> dao = daoRegistry.getDataAccessObject(className);
		return PerceroList<IPerceroObject> result = dao.getAll(pageNumber, pageSize, returnTotal, userId);
	}

	@SuppressWarnings({ "rawtypes" })
	// TODO: @Transactional(readOnly=true)
	public Integer countAllByName(String aClassName, String userId) throws Exception {
		Session s = appSessionFactory.openSession();
		try {
			Query countQuery = null;
			Class theClass = MappedClass.forName(aClassName);
			
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(aClassName);
			boolean isValidReadQuery = false;
			if (mappedClass != null) {
				if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())) {
					isValidReadQuery = true;
				}
			}
			
			if(mappedClass.getReadAllQuery() != null && mappedClass.getReadAllQuery().getQuery() != null && !mappedClass.getReadAllQuery().getQuery().isEmpty()){
				if((mappedClass.getReadAllQuery() instanceof JpqlQuery)){
					throw new IllegalArgumentException("Illegal query type on:"+aClassName+". readAllQueries must be plain SQL. JPQL, HQL not supported yet. ");
				}
				
				String selectQueryString = mappedClass.getReadAllQuery().getQuery();
				
				String countQueryString = "select count(*) from ("+selectQueryString+") as U";
				
				countQuery = s.createSQLQuery(countQueryString);
				countQuery.setParameter("userId", userId);
			}
			else if (theClass != null) {
				String countQueryString = "SELECT COUNT(getAllResult.ID) FROM " + aClassName + " getAllResult";
				
				// If the Read Query/Filter uses the ID, then we need to check against each ID here.
				if (isValidReadQuery) {
					String queryFilterString = mappedClass.getReadQuery().getQuery();
					if (mappedClass.getReadQuery().getUseId()) {
						// Need to replace :id with 
						queryFilterString = queryFilterString.replaceAll(":id", "getAllResult.ID");
						queryFilterString = queryFilterString.replaceAll(":Id", "getAllResult.ID");
						queryFilterString = queryFilterString.replaceAll(":iD", "getAllResult.ID");
						queryFilterString = queryFilterString.replaceAll(":ID", "getAllResult.ID");
					}
					String countQueryFilterString = queryFilterString;
					countQueryFilterString = countQueryString + " WHERE (" + countQueryFilterString + ") > 0";
					
					countQuery = s.createQuery(countQueryFilterString);
					mappedClass.getReadQuery().setQueryParameters(countQuery, null, userId);
				}
				else {
					countQueryString += " ORDER BY ID";
					countQuery = s.createQuery(countQueryString);
				}
			}
			
			if (countQuery != null) {
//				log.debug(countQuery.toString());
				Integer result = ((Number)countQuery.uniqueResult()).intValue();
				return result;
			}
			else {
				return null;
			}
			
		} catch (Exception e) {
			log.error("Unable to countAllByName", e);
		} finally {
			s.close();
		}
		
		return 0;
	}

	public List<Object> runQuery(MappedClass mappedClass, String queryName, Object[] queryArguments, String userId) {
		Session s = appSessionFactory.openSession();
		try {
			if (mappedClass != null) {
				for(IMappedQuery nextQuery : mappedClass.queries)
				{
					if (queryName.equalsIgnoreCase(nextQuery.getQueryName()))
					{
						Query readFilter = s.createQuery(nextQuery.getQuery());
						nextQuery.setQueryParameters(readFilter, queryArguments, userId, queryArguments, (SessionImpl)s);
						return processQueryResults(mappedClass.className + "_" + queryName, readFilter, readFilter.list());
					}
				}
			}
		} catch (Exception e) {
			log.error("Unable to runQuery", e);
		} finally {
			if (s != null && s.isOpen())
				s.close();
		}

		return null;
	}
	
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
	
	//@SuppressWarnings({ "rawtypes", "unchecked" })
	public IPerceroObject findById(ClassIDPair classIdPair, String userId) {
		boolean hasReadQuery = false;
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(classIdPair.getClassName());
		if (mappedClass != null) {
			if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())) {
				hasReadQuery = true;
			}
		}

		Session s = null;
		try {
			boolean hasAccess = true;
			IPerceroObject result = systemGetById(classIdPair);
			
			if (result != null) {
				if (hasReadQuery) {
					if (s == null)
						s = appSessionFactory.openSession();
					Query readFilter = s.createQuery(mappedClass.getReadQuery().getQuery());
					mappedClass.getReadQuery().setQueryParameters(readFilter, result, userId);
					Number readFilterResult = (Number) readFilter.uniqueResult();
					if (readFilterResult == null || readFilterResult.intValue() <= 0)
						hasAccess = false;
				}

				if (hasAccess) {
					if (s == null)
						s = appSessionFactory.openSession();
//					((BaseDataObject)result).setIsClean(false);
					result = (IPerceroObject) SyncHibernateUtils.cleanObject(result, s, userId);
					return result;
				}
				else {
					return null;
				}
			}
			else {
				return null;
			}
		} catch (Exception e) {
			log.error("Unable to findById", e);
		} finally {
			if (s != null && s.isOpen())
				s.close();
		}
		return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public IPerceroObject systemGetById(ClassIDPair classIdPair) {
		Session s = null;
		try {
			Class theClass = MappedClass.forName(classIdPair.getClassName().toString());
			
			if (theClass != null) {
				IPerceroObject result = retrieveFromRedisCache(classIdPair);
				String key = null;

				if (result == null) {

					result = readObjectFromDatabase(classIdPair, false);
					populateToManyRelationships(result, true, null);
					populateToOneRelationships(result, true, null);
					/**
					 * CRB: Going direct to database instead.
					s = appSessionFactory.openSession();
					result = (IPerceroObject) s.get(theClass, classIdPair.getID());
					 */
					
					// Now put the object in the cache.
					if (result != null) {
						key = RedisKeyUtils.classIdPair(result.getClass().getCanonicalName(), result.getID());
//						result = (IPerceroObject) SyncHibernateUtils.cleanObject(result, s);
						if (cacheTimeout > 0)
							cacheDataStore.setValue(key, ((BaseDataObject)result).toJson());
					}
					else {
						// Not necessarily a problem but could be helpful when debugging.
						log.debug("Unable to retrieve object from database: " + classIdPair.toJson());
					}
				}
				else {
					key = RedisKeyUtils.classIdPair(result.getClass().getCanonicalName(), result.getID());
				}

				// (Re)Set the expiration.
				if (cacheTimeout > 0 && key != null) {
					cacheDataStore.expire(key, cacheTimeout, TimeUnit.SECONDS);
				}
				
				return result;
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error("Unable to systemGetById: "+classIdPair.toJson(), e);
		} finally {
			if (s != null && s.isOpen())
				s.close();
		}
		return null;
	}

//	@SuppressWarnings({ "rawtypes" })
//	public IPerceroObject systemGetByIdWithClassAndSession(ClassIDPair classIdPair, Class theClass, Session s) {
//		Boolean sessionAlreadyOpen = false;
//		try {
//			if (theClass != null) {
//				IPerceroObject result = retrieveFromRedisCache(classIdPair);
//				String key = null;
//				
//				if (result == null) {
//					if (s == null || !s.isOpen()) {
//						s = appSessionFactory.openSession();
//					}
//					else {
//						sessionAlreadyOpen = true;
//					}
//					result = (IPerceroObject) s.get(theClass, classIdPair.getID());
//					
//					// Now put the object in the cache.
//					if (result != null) {
//						key = RedisKeyUtils.classIdPair(result.getClass().getCanonicalName(), result.getID());
//						result = (IPerceroObject) SyncHibernateUtils.cleanObject(result, s);
//						if (cacheTimeout > 0)
//							redisDataStore.setValue(key, ((BaseDataObject)result).toJson());
//					}
//					else {
//						log.warn("Unable to retrieve object from database: " + classIdPair.toString());
//					}
//				}
//				else {
//					key = RedisKeyUtils.classIdPair(result.getClass().getCanonicalName(), result.getID());
//				}
//				
//				// (Re)Set the expiration.
//				if (cacheTimeout > 0 && key != null) {
//					redisDataStore.expire(key, cacheTimeout, TimeUnit.SECONDS);
//				}
//				
//				return result;
//			} else {
//				return null;
//			}
//		} catch (Exception e) {
//			log.error("Unable to systemGetById: "+classIdPair.toJson(), e);
//		} finally {
//			// Only close the session if it wasn't already open.
//			if (!sessionAlreadyOpen) {
//				if (s != null && s.isOpen()) {
//					s.close();
//				}
//			}
//		}
//		return null;
//	}
	
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
	
	@SuppressWarnings({ "rawtypes" })
	public Boolean getReadAccess(ClassIDPair classIdPair, String userId) {
		Session s = appSessionFactory.openSession();
		try {
			Class theClass = MappedClass.forName(classIdPair.getClassName().toString());
			
			if (theClass != null) {
				IPerceroObject parent = (IPerceroObject) s.get(theClass, classIdPair.getID());
				
				boolean hasAccess = (parent != null);
				
				if (hasAccess) {
					IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
					MappedClass mappedClass = mcm.getMappedClassByClassName(classIdPair.getClassName());
					if (mappedClass != null) {
						if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())){
							Query readFilter = s.createQuery(mappedClass.getReadQuery().getQuery());
							mappedClass.getReadQuery().setQueryParameters(readFilter, parent, userId);
							Number readFilterResult = (Number) readFilter.uniqueResult();
							if (readFilterResult == null || readFilterResult.intValue() <= 0)
								hasAccess = false;
						}
					}
				}
				
				return hasAccess;
			}
		} catch (Exception e) {
			log.error("Unable to getReadAccess", e);
		} finally {
			s.close();
		}
		return false;
	}
	
	@SuppressWarnings({ "rawtypes" })
	public Boolean getDeleteAccess(ClassIDPair classIdPair, String userId) {
		Session s = appSessionFactory.openSession();
		try {
			Class theClass = MappedClass.forName(classIdPair.getClassName().toString());
			
			if (theClass != null) {
				IPerceroObject parent = (IPerceroObject) s.get(theClass, classIdPair.getID());
				
				if (parent == null)
					return true;
				
				boolean hasAccess = true;
				IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
				MappedClass mappedClass = mcm.getMappedClassByClassName(classIdPair.getClassName());
				if (mappedClass != null) {
					if (mappedClass.getDeleteQuery() != null && StringUtils.hasText(mappedClass.getDeleteQuery().getQuery())){
						Query deleteFilter = s.createQuery(mappedClass.getDeleteQuery().getQuery());
						mappedClass.getDeleteQuery().setQueryParameters(deleteFilter, parent, userId);
						Number deleteFilterResult = (Number) deleteFilter.uniqueResult();
						if (deleteFilterResult == null || deleteFilterResult.intValue() <= 0)
							hasAccess = false;
					}
				}
				
				return hasAccess;
			}
		} catch (Exception e) {
			log.error("Unable to getDeleteAccess", e);
		} finally {
			s.close();
		}
		return false;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<IPerceroObject> findByIds(ClassIDPairs classIdPairs, String userId) {
		List<IPerceroObject> result = null;
		
		boolean hasAccess = true;
		Class theClass = null;
		try {
			theClass = MappedClass.forName(classIdPairs.getClassName());
		} catch (ClassNotFoundException e2) {
			log.error("Unable to get Class from class name " + classIdPairs.getClassName(), e2);
			return result;
		}
		
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(classIdPairs.getClassName());
		boolean isValidReadQuery = false;
		if (mappedClass != null) {
			if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())) {
				isValidReadQuery = true;
			}

		}

		if (hasAccess) {
			if (theClass != null) {
				Set<String> idsSet = new HashSet<String>(classIdPairs.getIds().size());
				idsSet.addAll(classIdPairs.getIds());

				try {
					// Attempt to get as many from the cache as possible...
					Map<String, IPerceroObject> cachedObjects = retrieveFromRedisCache(classIdPairs, true);
					
					if (!cachedObjects.isEmpty()) {
						result = new ArrayList<IPerceroObject>(cachedObjects.size());
						List<IPerceroObject> cachedResults = new ArrayList<IPerceroObject>(cachedObjects.size());
						cachedResults.addAll(cachedObjects.values());
						idsSet.removeAll(cachedObjects.keySet());
						
						// If there is a read query, we need to check each object through that query.
						if (isValidReadQuery) {
							Session s = null;
							try {
								// Need to clean each result for the user.
								// TODO: Need to figure out how to NOT open the Session if we don't have to.  The problem lies in the SyncHibernateUtils.cleanObject function
								s = appSessionFactory.openSession();
								
								Iterator<IPerceroObject> itrCachedResults = cachedResults.iterator();
								while (itrCachedResults.hasNext()) {
									IPerceroObject nextCachedResult = itrCachedResults.next();
									if (isValidReadQuery) {
										Query readFilter = s.createQuery(mappedClass.getReadQuery().getQuery());
										mappedClass.getReadQuery().setQueryParameters(readFilter, nextCachedResult, userId);
										Number readFilterResult = (Number) readFilter.uniqueResult();
										if (readFilterResult == null || readFilterResult.intValue() <= 0)
											hasAccess = false;
									}
			
									if (hasAccess) {
										result.add( (IPerceroObject) SyncHibernateUtils.cleanObject(nextCachedResult, s, userId) );
									}
								}
							} catch (Exception e) {
								log.error("Error cleaning object for user in findByIds", e);
							} finally {
								if (s != null && s.isOpen()) {
									s.close();
								}
							}
						}
						else {
							// This class has relationship objects that need to be cleaned.
							if (!mappedClass.getReadAccessRightsFieldReferences().isEmpty()) {
								Session s = null;
								try {
									// Need to clean each result for the user.
									// TODO: Need to figure out how to NOT open the Session if we don't have to.  The problem lies in the SyncHibernateUtils.cleanObject function
									// TODO: Maybe only clean relationships that are in mappedClass.readAccessRightsFieldReferences?
									s = appSessionFactory.openSession();
									
									Iterator<IPerceroObject> itrCachedResults = cachedResults.iterator();
									while (itrCachedResults.hasNext()) {
										IPerceroObject nextCachedResult = itrCachedResults.next();
										result.add( (IPerceroObject) SyncHibernateUtils.cleanObject(nextCachedResult, s, userId) );
									}
								} catch (Exception e) {
									log.error("Error cleaning object for user in findByIds", e);
								} finally {
									if (s != null && s.isOpen()) {
										s.close();
									}
								}
							}
							else {
								// This class has NO read access query AND has NO relationships that require cleaning,
								//	since this object is completely retrieved from redis, we can send as-is.
								// Though this may seem rare, this is probably a very common path.
								result.addAll(cachedResults);
							}
						}
					}
				} catch (Exception e1) {
					// We errored out here, but we can still try and retrieve from the database.
					log.error("Error retrieving objects from redis cache, attempting to retrieve from database", e1);
				}
				
				// Now get the rest from the database.
				if (!idsSet.isEmpty()) {
					
					
					/**
					 * CRB: Going direct to database instead of Hibernate
					String queryString = "SELECT findByIdsResult FROM " + classIdPairs.getClassName() + " findByIdsResult WHERE findByIdsResult.ID IN (:idsSet)";
					 */

					// Open the database session.
					Session s = null;
					try {
						s = appSessionFactory.openSession();
						/**
						 * CRB: Going direct to database instead of Hibernate
						Query query = null;

						// If the Read Query/Filter uses the ID, then we need to check against each ID here.
						if (isValidReadQuery) {
							String queryFilterString = mappedClass.getReadQuery().getQuery();
							if (mappedClass.getReadQuery().getUseId()) {
								// Need to replace :id with 
								queryFilterString = queryFilterString.replaceAll(":id", "findByIdsResult.ID");
								queryFilterString = queryFilterString.replaceAll(":Id", "findByIdsResult.ID");
								queryFilterString = queryFilterString.replaceAll(":iD", "findByIdsResult.ID");
								queryFilterString = queryFilterString.replaceAll(":ID", "findByIdsResult.ID");
							}
							queryFilterString = queryString + " AND (" + queryFilterString + ") > 0";
	
							query = s.createQuery(queryFilterString);
							mappedClass.getReadQuery().setQueryParameters(query, null, userId);
						}
						else {
							query = s.createQuery(queryString);
						}
						
						query.setParameterList("idsSet", idsSet);
						List<Object> queryResult = query.list();
						*/
						List<IPerceroObject> queryResult = readObjectsFromDatabase(classIdPairs, userId);
						
						for(IPerceroObject nextResult : queryResult) {
							populateToManyRelationships(nextResult, true, userId);
							populateToOneRelationships(nextResult, true, userId);
						}
						List<IPerceroObject> cleanedDatabaseObjects = queryResult;
						// 
//						List<IPerceroObject> cleanedDatabaseObjects = (List<IPerceroObject>) SyncHibernateUtils.cleanObject(queryResult, s);
						
						// Need to put results into cache.
						if (cacheTimeout > 0) {
							Map<String, String> mapJsonObjectStrings = new HashMap<String, String>(cleanedDatabaseObjects.size());
							Iterator<IPerceroObject> itrDatabaseObjects = cleanedDatabaseObjects.iterator();
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

						// Clean the objects for the selected User.
						cleanedDatabaseObjects = (List<IPerceroObject>) SyncHibernateUtils.cleanObject(cleanedDatabaseObjects, s, userId);
						if (result == null) {
							result = cleanedDatabaseObjects;
						}
						else {
							result.addAll(cleanedDatabaseObjects);
						}
					} catch (QueryException qe) {
						log.error("Unable to findByIds", qe);
					} catch (Exception e) {
						log.error("Unable to findByIds", e);
					} finally {
						if (s != null && s.isOpen()) {
							s.close();
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * CRB: This is the same as findByExample, filtering out the first result.
	@SuppressWarnings({ "rawtypes", })
	public IPerceroObject findUnique(IPerceroObject theQueryObject, String userId) throws SyncException {

		MappedClass mappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(theQueryObject.getClass().getCanonicalName());
		if (mappedClass == null) {
			log.warn("Missing MappedClass for " + theQueryObject.getClass().getCanonicalName());
			throw new SyncException(SyncException.MISSING_MAPPED_CLASS_ERROR, SyncException.MISSING_MAPPED_CLASS_ERROR_CODE );
		}
		
		Session s = appSessionFactory.openSession();
		try {
			Class objectClass = theQueryObject.getClass();
			
			Iterator<List<MappedField>> itr = mappedClass.uniqueConstraints.iterator();
			while(itr.hasNext()) {
				Criteria criteria = null;
				List<MappedField> nextConstraint = itr.next();

//				if (nextConstraint instanceof MappedField) {
//					MappedField nextConstraintField = (MappedField) nextConstraint;
//					Object nextConstraintFieldValue = nextConstraintField.getValue(theQueryObject);
//					if (nextConstraintFieldValue != null) {
//						if (criteria == null)
//							criteria = s.createCriteria(objectClass);
//						Criterion uniqueFieldValue = Restrictions.eq(nextConstraintField.getField().getName(), nextConstraintFieldValue);
//						criteria.add(uniqueFieldValue);
//					}
//				}
//				else if (nextConstraint instanceof List) {
					
				List<MappedField> listMappedFields = (List<MappedField>) nextConstraint;
				Iterator<MappedField> itrMappedFields = listMappedFields.iterator();
				while(itrMappedFields.hasNext()) {
					MappedField nextConstraintField = itrMappedFields.next();
					
					if (nextConstraintField.isValueSetForQuery(theQueryObject)) {
						
					}
					
					Object nextConstraintFieldValue = nextConstraintField.getValue(theQueryObject);
					if (nextConstraintFieldValue != null) {
						if (criteria == null)
							criteria = s.createCriteria(objectClass);
						Criterion uniqueFieldValue = Restrictions.eq(nextConstraintField.getField().getName(), nextConstraintFieldValue);
						criteria.add(uniqueFieldValue);
					}
				}

				if (criteria != null) {
					criteria.setMaxResults(1);
					Object result = criteria.uniqueResult();
					if (result != null) {
						// Make sure user has access.
						boolean hasAccess = true;
						if (mappedClass != null) {
							if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())){
								Query readFilter = s.createQuery(mappedClass.getReadQuery().getQuery());
								mappedClass.getReadQuery().setQueryParameters(readFilter, result, userId);
								Number readFilterResult = (Number) readFilter.uniqueResult();
								if (readFilterResult == null || readFilterResult.intValue() <= 0)
									hasAccess = false;
							}
						}

						if (hasAccess) {
							result = SyncHibernateUtils.cleanObject(result, s, userId);
							return (IPerceroObject) result;
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Unable to findUnique", e);
		} finally {
			s.close();
		}

		return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public IPerceroObject findUnique_OLD(IPerceroObject theQueryObject, String userId) {
		Session s = appSessionFactory.openSession();
		try {
			Class objectClass = theQueryObject.getClass();
			
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(objectClass.getName());
			if (mappedClass != null) {
				Iterator itr = mappedClass.uniqueConstraints.iterator();
				while(itr.hasNext()) {
					Criteria criteria = null;
					Object nextConstraint = itr.next();
					if (nextConstraint instanceof MappedField) {
						MappedField nextConstraintField = (MappedField) nextConstraint;
						Object nextConstraintFieldValue = nextConstraintField.getValue(theQueryObject);
						if (nextConstraintFieldValue != null) {
							if (criteria == null)
								criteria = s.createCriteria(objectClass);
							Criterion uniqueFieldValue = Restrictions.eq(nextConstraintField.getField().getName(), nextConstraintFieldValue);
							criteria.add(uniqueFieldValue);
						}
					}
					else if (nextConstraint instanceof List) {
						List<MappedField> listMappedFields = (List<MappedField>) nextConstraint;
						Iterator<MappedField> itrMappedFields = listMappedFields.iterator();
						while(itrMappedFields.hasNext()) {
							MappedField nextConstraintField = itrMappedFields.next();
							Object nextConstraintFieldValue = nextConstraintField.getValue(theQueryObject);
							if (nextConstraintFieldValue != null) {
								if (criteria == null)
									criteria = s.createCriteria(objectClass);
								Criterion uniqueFieldValue = Restrictions.eq(nextConstraintField.getField().getName(), nextConstraintFieldValue);
								criteria.add(uniqueFieldValue);
							}
						}
					}
					
					if (criteria != null) {
						criteria.setMaxResults(1);
						Object result = criteria.uniqueResult();
						if (result != null) {
							// Make sure user has access.
							boolean hasAccess = true;
							if (mappedClass != null) {
								if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())){
									Query readFilter = s.createQuery(mappedClass.getReadQuery().getQuery());
									mappedClass.getReadQuery().setQueryParameters(readFilter, result, userId);
									Number readFilterResult = (Number) readFilter.uniqueResult();
									if (readFilterResult == null || readFilterResult.intValue() <= 0)
										hasAccess = false;
								}
							}
							
							if (hasAccess) {
								result = SyncHibernateUtils.cleanObject(result, s, userId);
								return (IPerceroObject) result;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Unable to findUnique", e);
		} finally {
			s.close();
		}
		
		return null;
	}
	*/

	// TODO: Add permissions check.
	@SuppressWarnings("unchecked")
	public List<IPerceroObject> findByExample(IPerceroObject theQueryObject, List<String> excludeProperties, String userId) throws SyncException {
		List<IPerceroObject> result = systemFindByExample(theQueryObject, excludeProperties);
		if (result != null) {
			// Make sure user has access.
			boolean hasAccess = true;

			MappedClass mappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(theQueryObject.getClass().getCanonicalName());
			Session s = appSessionFactory.openSession();
			try {
				if (mappedClass != null) {
					if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())){
						Query readFilter = mappedClass.getReadQuery().createQuery(result, userId, null, (SessionImpl)s);
//						Query readFilter = s.createQuery(mappedClass.getReadQuery().getQuery());
//						mappedClass.getReadQuery().setQueryParameters(readFilter, result, userId);
						Number readFilterResult = (Number) readFilter.uniqueResult();
						if (readFilterResult == null || readFilterResult.intValue() <= 0) {
							hasAccess = false;
						}
					}
				}
	
				if (hasAccess) {
					result = (List<IPerceroObject>) SyncHibernateUtils.cleanObject(result, s, userId);
				}
			} catch (Exception e) {
				log.error("Unable to findByExample", e);
				throw new SyncDataException(e);
			} finally {
				s.close();
			}
		}
		
		return result;

//		Session s = appSessionFactory.openSession();
//		try {
//			Criteria criteria = s.createCriteria(theQueryObject.getClass());
//			AssociationExample example = AssociationExample.create(theQueryObject);
//			BaseDataObjectPropertySelector propertySelector = new BaseDataObjectPropertySelector(excludeProperties);
//			example.setPropertySelector(propertySelector);
//			criteria.add(example);
//
//			List<IPerceroObject> result = criteria.list();
//			result = (List<IPerceroObject>) SyncHibernateUtils.cleanObject(result, s, userId);
//
//			return (List<IPerceroObject>) result;
//		} catch (Exception e) {
//			log.error("Unable to findByExample", e);
//		} finally {
//			s.close();
//		}
//
//		return null;
	}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<IPerceroObject> systemFindByExample(IPerceroObject theQueryObject, List<String> excludeProperties) throws SyncException {
		// TODO: Use StringBuilder or something more efficient here.

		List<IPerceroObject> result = new ArrayList<IPerceroObject>();
		
		MappedClass mappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(theQueryObject.getClass().getCanonicalName());
		if (mappedClass == null) {
			log.warn("Missing MappedClass for " + theQueryObject.getClass().getCanonicalName());
			throw new SyncException(SyncException.MISSING_MAPPED_CLASS_ERROR, SyncException.MISSING_MAPPED_CLASS_ERROR_CODE );
		}
		
		String selectQueryString = "SELECT ";
		String whereString = "WHERE ";
		List<MappedField> wherePropertyFields = new ArrayList<MappedField>();
		List<MappedFieldPerceroObject> whereToOneFields = new ArrayList<MappedFieldPerceroObject>();
		
//		if (excludeProperties == null) {
//			excludeProperties = new ArrayList<String>(1);
//		}
//		if (!excludeProperties.contains(mappedClass.idMappedField)) {
//			excludeProperties.add(mappedClass.idMappedField.);
//		}
		
		int selectCounter = 0;
		int whereCounter = 0;
		
		// Iterate over PropertyFields.  These are simple fields with their value being stored here.
		for(MappedField nextMappedField : mappedClass.propertyFields) {
			if (selectCounter > 0) {
				selectQueryString += ", ";
			}
			
			selectQueryString += "P." + nextMappedField.getColumnName();
			selectCounter++;
			
			// Now check the QueryObject to see if this field is a filter.
			try {
				if (nextMappedField != mappedClass.idMappedField && nextMappedField.isValueSetForQuery(theQueryObject)) {
					if (whereCounter > 0) {
						whereString += " AND ";
					}
					if (nextMappedField instanceof MappedFieldString) {
						whereString += "P." + nextMappedField.getColumnName() + " LIKE(:" + nextMappedField.getColumnName() + ")";
					}
					else {
						whereString += "P." + nextMappedField.getColumnName() + "=:" + nextMappedField.getColumnName();
					}
					wherePropertyFields.add(nextMappedField);
					whereCounter++;
				}
			} catch (IllegalArgumentException e) {
				// Don't necessarily care about this here.
			} catch (IllegalAccessException e) {
				// Don't necessarily care about this here.
			} catch (InvocationTargetException e) {
				// Don't necessarily care about this here.
			}
		}
		
		// Iterate over ToOneFields, meaning fields where the ID of the relationship is stored on this side of the relationship.
		for(MappedFieldPerceroObject nextMappedField : mappedClass.toOneFields) {
			
			// If this mapped field is the source, then we include it in our query.
			if (nextMappedField.isSourceEntity()) {
				if (selectCounter > 0) {
					selectQueryString += ", ";
				}
	
				selectQueryString += "P." + nextMappedField.getJoinColumnName();
				selectCounter++;
				
				// Now check the QueryObject to see if this field is a filter.
				try {
					if (nextMappedField != mappedClass.idMappedField && nextMappedField.isValueSetForQuery(theQueryObject)) {
						if (whereCounter > 0) {
							whereString += " AND ";
						}
						whereString += "P." + nextMappedField.getJoinColumnName() + "=:" + nextMappedField.getJoinColumnName();
						whereToOneFields.add(nextMappedField);
						whereCounter++;
					}
				} catch (IllegalArgumentException e) {
					// Don't necessarily care about this here.
				} catch (IllegalAccessException e) {
					// Don't necessarily care about this here.
				} catch (InvocationTargetException e) {
					// Don't necessarily care about this here.
				}
			}
			else {
				System.out.println("No Join Column");
			}
		}
		
		// If there are no Query Fields set, then we simply return an empty list.
		if ( (selectCounter + whereCounter) == 0) {
			return result;
		}
		
		String fullQueryString = selectQueryString + " FROM " + (StringUtils.hasText(mappedClass.tableSchema) ?  mappedClass.tableSchema + "." : "") + mappedClass.tableName + " P " + whereString;

		Session s = appSessionFactory.openSession();
		try {
			Query query = s.createSQLQuery(fullQueryString);
			
			for(MappedField nextMappedField : wherePropertyFields) {
				// We know this field has a valid value here.
				if (nextMappedField instanceof MappedFieldString) {
					query.setParameter(nextMappedField.getColumnName(), "%" + nextMappedField.getValue(theQueryObject) + "%");
				}
				else {
					query.setParameter(nextMappedField.getColumnName(), nextMappedField.getValue(theQueryObject));
				}
			}
			for(MappedField nextMappedField : whereToOneFields) {
				// Because we know this field has a value set, we know it is an IPerceroObject with valid ID.
				query.setParameter(nextMappedField.getJoinColumnName(), ((IPerceroObject) nextMappedField.getValue(theQueryObject)).getID() );
			}
			
			List queryResult = query.list();
			
			Iterator itrQueryResults = queryResult.iterator();
			while (itrQueryResults.hasNext()) {
				
				IPerceroObject nextPerceroObject = (IPerceroObject) mappedClass.clazz.newInstance();

				Object[] nextResult = (Object[]) itrQueryResults.next();
				
				int j = 0;
				for(MappedField nextMappedField : mappedClass.propertyFields) {
					Object nextValue = nextResult[j];
					nextMappedField.getSetter().invoke(nextPerceroObject, nextValue);
					j++;
				}
				for(MappedFieldPerceroObject nextMappedField : mappedClass.toOneFields) {
					if (nextMappedField.isSourceEntity()) {
						Object nextValue = nextResult[j];
						
						IPerceroObject nextToOnePerceroObject = (IPerceroObject) nextMappedField.getField().getType().newInstance();
						nextToOnePerceroObject.setID( (String) nextValue );
	
						if (!nextMappedField.getUseLazyLoading()) {
							IDataProvider dataProvider = nextMappedField.getMappedClass().getDataProvider();
							nextToOnePerceroObject = dataProvider.systemGetById(BaseDataObject.toClassIdPair(nextToOnePerceroObject));
						}
						
						nextMappedField.getSetter().invoke(nextPerceroObject, nextToOnePerceroObject);
					}
				}
				
				nextPerceroObject = (IPerceroObject) SyncHibernateUtils.cleanObject(nextPerceroObject, s);
				addObjectToCache(nextPerceroObject);
				
				result.add(nextPerceroObject);
			}
		} catch(Exception e) {
			log.error("Unable to systemFindByExample", e);
			// TODO: Throw a SyncException here.
		} finally {
			s.close();
		}
		
		// Now clean the results.
		result = (List<IPerceroObject>) SyncHibernateUtils.cleanObject(result, s, null);
		
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<IPerceroObject> searchByExample(IPerceroObject theQueryObject,
			List<String> excludeProperties, String userId) throws SyncException {
		return findByExample(theQueryObject, excludeProperties, userId);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public IPerceroObject systemCreateObject(IPerceroObject perceroObject)
			throws SyncException {
//		Session s = appSessionFactory.openSession();
		
		try {
			// Make sure object has an ID.
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
			if (!mappedClass.hasGeneratedId && !StringUtils.hasText(perceroObject.getID()))
				perceroObject.setID(UUID.randomUUID().toString());
			else {
				// Check to see if item already exists.
				IPerceroObject existingObject = readObjectFromDatabase(BaseDataObject.toClassIdPair(perceroObject), false);
				if (existingObject != null)
				{
					populateToManyRelationships(perceroObject, true, null);
					populateToOneRelationships(perceroObject, true, null);
					return perceroObject;
				}
			}
			
			insertObjectIntoDatabase(perceroObject);
			populateToManyRelationships(perceroObject, true, null);
			populateToOneRelationships(perceroObject, true, null);
	
			// Now update the cache.
			// TODO: Field-level updates could be REALLY useful here.  Would avoid A TON of UNNECESSARY work...
			if (cacheTimeout > 0) {
				String key = RedisKeyUtils.classIdPair(perceroObject.getClass().getCanonicalName(), perceroObject.getID());
				if (cacheTimeout > 0) {
					cacheDataStore.setValue(key, ((BaseDataObject)perceroObject).toJson());
					cacheDataStore.expire(key, cacheTimeout, TimeUnit.SECONDS);
				}

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
			
			return perceroObject;
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
	
//	private void deepCleanObject(IPerceroObject perceroObject, MappedClass mappedClass, Session s, String userId) {
//		if (!(perceroObject instanceof IRootObject)) {
//			for(MappedField nextMappedField : mappedClass.externalizableFields) {
//				try {
//					if (nextMappedField instanceof MappedFieldPerceroObject) {
//						Object fieldValue = nextMappedField.getValue(perceroObject);
//						if (fieldValue != null) {
//							fieldValue = SyncHibernateUtils.cleanObject(fieldValue, s, userId);
//							nextMappedField.getSetter().invoke(perceroObject, fieldValue);
//						}
//					}
//					else if (nextMappedField instanceof MappedFieldList) {
//						Object fieldValue = nextMappedField.getValue(perceroObject);
//						if (fieldValue != null) {
//							fieldValue = SyncHibernateUtils.cleanObject(fieldValue, s, userId);
//							nextMappedField.getSetter().invoke(perceroObject, fieldValue);
//						}
//					}
//					else if (nextMappedField instanceof MappedFieldMap) {
//						Object fieldValue = nextMappedField.getValue(perceroObject);
//						if (fieldValue != null) {
//							fieldValue = SyncHibernateUtils.cleanObject(fieldValue, s, userId);
//							nextMappedField.getSetter().invoke(perceroObject, fieldValue);
//						}
//					}
//				} catch(Exception e) {
//					log.error("Error in postCreateObject " + mappedClass.className + "." + nextMappedField.getField().getName(), e);
//				}
//			}
//		}
//	}

	public IPerceroObject createObject(IPerceroObject perceroObject, String userId) throws SyncException {
		boolean hasAccess = true;
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
		if (mappedClass != null) {
			if (mappedClass.getCreateQuery() != null && StringUtils.hasText(mappedClass.getCreateQuery().getQuery())){
				Session s = appSessionFactory.openSession();
				try {
					Query createFilter = s.createQuery(mappedClass.getCreateQuery().getQuery());
					mappedClass.getCreateQuery().setQueryParameters(createFilter, perceroObject, userId);
					Number createFilterResult = (Number) createFilter.uniqueResult();
					if (createFilterResult == null || createFilterResult.intValue() <= 0)
						hasAccess = false;
				} catch (Exception e) {
					log.error("Error getting PUT AccessRights for object " + perceroObject.toString(), e);
					hasAccess = false;
				} finally {
					s.close();
				}
			}
		}

		if (hasAccess) {
			return systemCreateObject(perceroObject);
		} else {
			return null;
		}
	}

	

	////////////////////////////////////////////////////
	//	PUT
	////////////////////////////////////////////////////
	public IPerceroObject putObject(IPerceroObject perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields, String userId) throws SyncException {
		boolean hasAccess = true;
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
		if (mappedClass != null) {
			if (mappedClass.getUpdateQuery() != null && StringUtils.hasText(mappedClass.getUpdateQuery().getQuery())){
				Session s = appSessionFactory.openSession();
				try {
					Query updateFilter = null;
					if (mappedClass.getUpdateQuery() instanceof JpqlQuery) {
						updateFilter = s.createQuery(mappedClass.getUpdateQuery().getQuery());
					}
					else if (mappedClass.getUpdateQuery() instanceof SqlQuery) {
						updateFilter = s.createSQLQuery(mappedClass.getUpdateQuery().getQuery());
					}
					mappedClass.getUpdateQuery().setQueryParameters(updateFilter, perceroObject, userId);
					Number updateFilterResult = (Number) updateFilter.uniqueResult();
					if (updateFilterResult == null || updateFilterResult.intValue() <= 0)
						hasAccess = false;
				} catch (Exception e) {
					log.error("Error getting PUT AccessRights for object " + perceroObject.toString(), e);
					hasAccess = false;
				} finally {
					s.close();
				}
			}
		}

		if (hasAccess) {
			return systemPutObject(perceroObject, changedFields);
		} else {
			return null;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public IPerceroObject systemPutObject(IPerceroObject perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields) throws SyncException {

		try {
			updateObjectInDatabase(perceroObject, changedFields);	// If write fails, an exception will be thrown.
			
			if (changedFields != null && !changedFields.isEmpty()) {
				// Since changedFields was set, it is possible (and likely) that perceroObject is only PARTIALLY filled out, so load the full object.
				// Re-fetch the full object from the database.
				perceroObject = readObjectFromDatabase(BaseDataObject.toClassIdPair(perceroObject), false);
				populateToManyRelationships(perceroObject, true, null);
				populateToOneRelationships(perceroObject, true, null);
			}
			
			// Now update the cache.
			// TODO: Field-level updates could be REALLY useful here.  Would avoid A TON of UNNECESSARY work...
			if (cacheTimeout > 0) {
				// TODO: Also need to update the caches of anything object that is related to this object.
				String key = RedisKeyUtils.classIdPair(perceroObject.getClass().getCanonicalName(), perceroObject.getID());
				if (cacheDataStore.hasKey(key)) {
					cacheDataStore.setValue(key, ((BaseDataObject)perceroObject).toJson());
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
						Object fieldObject = nextMappedField.getGetter().invoke(perceroObject);
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
						Object fieldObject = nextMappedField.getGetter().invoke(perceroObject);
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

			return perceroObject;
		} catch (Exception e) {
			log.error("Error putting object", e);
			
			SyncDataException sde = new SyncDataException(SyncDataException.UPDATE_OBJECT_ERROR, SyncDataException.UPDATE_OBJECT_ERROR_CODE);
			throw sde;
		}
	}

	

	////////////////////////////////////////////////////
	//	DELETE
	////////////////////////////////////////////////////
	public Boolean deleteObject(ClassIDPair theClassIdPair, String userId) throws SyncException {
		Session s = appSessionFactory.openSession();

		try {
			IPerceroObject perceroObject = readObjectFromDatabase(theClassIdPair, true);
//			IPerceroObject perceroObject = (IPerceroObject) s.get(theClassIdPair.getClassName(), theClassIdPair.getID());
			if (perceroObject == null) {
				return true;
			}
			
			boolean hasAccess = true;
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(theClassIdPair.getClassName());
			if (mappedClass != null) {
				if (mappedClass.getDeleteQuery() != null && StringUtils.hasText(mappedClass.getDeleteQuery().getQuery())){
					Query deleteFilter = null;
					if (mappedClass.getDeleteQuery() instanceof JpqlQuery) {
						deleteFilter = s.createQuery(mappedClass.getDeleteQuery().getQuery());
					}
					else if (mappedClass.getDeleteQuery() instanceof SqlQuery) {
						deleteFilter = s.createSQLQuery(mappedClass.getDeleteQuery().getQuery());
					}
					mappedClass.getDeleteQuery().setQueryParameters(deleteFilter, perceroObject, userId);
					Number deleteFilterResult = (Number) deleteFilter.uniqueResult();
					if (deleteFilterResult == null || deleteFilterResult.intValue() <= 0)
						hasAccess = false;
				}
			}

			if (hasAccess) {
				if (systemDeleteObject(perceroObject)) {
					return true;
				} 
				else {
					return true;
				}
			} else {
				log.warn("No access to delete object " + theClassIdPair.toString());
				return false;
			}
		} catch (Exception e) {
			log.error("Error putting object", e);
			return false;
		} finally {
			s.close();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Boolean systemDeleteObject(IPerceroObject perceroObject) throws SyncException {

		Session s = null;

		try {
			MappedClass mappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(perceroObject.getClass().getCanonicalName());
			if (mappedClass == null) {
				log.warn("Missing MappedClass for " + perceroObject.getClass().getCanonicalName());
				throw new SyncException(SyncException.MISSING_MAPPED_CLASS_ERROR, SyncException.MISSING_MAPPED_CLASS_ERROR_CODE );
			}
			
			// TODO: Call the DAO for this.
			String deleteQueryString = "DELETE FROM " + (StringUtils.hasText(mappedClass.tableSchema) ?  mappedClass.tableSchema + "." : "") + mappedClass.tableName + " WHERE ID=:objectId";
			
			s = appSessionFactory.openSession();
			Query query = s.createSQLQuery(deleteQueryString);
			query.setParameter("objectId", perceroObject.getID());
			
			int result = query.executeUpdate();
			
			// Now delete from cache.
			// Now update the cache.
			// TODO: Field-level updates could be REALLY useful here.  Would avoid A TON of UNNECESSARY work...
			if (cacheTimeout > 0) {
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
					// TODO: Do we simply delete the key?  Or do we refetch the object here and update the key?
					//redisDataStore.setValue(nextKey, ((BaseDataObject)perceroObject).toJson());
				}
			}
			
			return true;
			
		} catch(Exception e) {
			if (perceroObject != null) {
				log.error("Unable to delete record from database: " + perceroObject.getClass().getCanonicalName() + ":" + perceroObject.getID(), e);
			}
			else {
				log.error("Unable to delete record from database: NULL Object", e);
			}
			throw new SyncDataException(SyncDataException.DELETE_OBJECT_ERROR, SyncDataException.DELETE_OBJECT_ERROR_CODE);
		} finally {
			if (s != null && s.isOpen()) {
				s.close();
			}
		}
	}
	
	
	////////////////////////////////////////////////////
	//	CLEAN
	////////////////////////////////////////////////////
	@Override
	public IPerceroObject cleanObject(IPerceroObject perceroObject, String userId) throws SyncException {
		try {
			if (((BaseDataObject) perceroObject).getIsClean()) {
				perceroObject = findById(BaseDataObject.toClassIdPair(perceroObject), userId);
				((BaseDataObject) perceroObject).setIsClean(true);
			}
			
		} catch(Exception e) {
			throw new SyncException(e);
		}
		return perceroObject;
		
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public List<IPerceroObject> cleanObject(List<IPerceroObject> perceroObjects, String userId) throws SyncException {
		try {
			List<IPerceroObject> results = new ArrayList<IPerceroObject>(perceroObjects.size());
			
			Map<Class, ClassIDPairs> classPairs = new HashMap<Class, ClassIDPairs>();
			
			Iterator<IPerceroObject> itrPerceroObjects = perceroObjects.iterator();
			while (itrPerceroObjects.hasNext()) {
				IPerceroObject nextPerceroObject = itrPerceroObjects.next();
				if ( ((BaseDataObject)nextPerceroObject).getIsClean()) {
					results.add(nextPerceroObject);
				}
				else {
					ClassIDPairs pairs = classPairs.get(nextPerceroObject.getClass());
					if (pairs == null) {
						pairs = new ClassIDPairs();
						pairs.setClassName(nextPerceroObject.getClass().getCanonicalName());
						classPairs.put(nextPerceroObject.getClass(), pairs);
					}
					pairs.addId(nextPerceroObject.getID());
				}
			}
			
			Iterator<Entry<Class, ClassIDPairs>> itrClassPairs = classPairs.entrySet().iterator();
			while (itrClassPairs.hasNext()) {
				Entry<Class, ClassIDPairs> nextEntrySet = itrClassPairs.next();
				
				MappedClass mappedClass = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(nextEntrySet.getKey().getCanonicalName());
				if (mappedClass != null) {
					results.addAll( mappedClass.getDataProvider().findByIds(nextEntrySet.getValue(), userId) );
				}
				
			}
			
			return results;
		} catch(Exception e) {
			throw new SyncException(e);
		}
	}

	
	@SuppressWarnings("rawtypes")
	public Map<ClassIDPair, Collection<MappedField>> getChangedMappedFields(IPerceroObject newObject) {
		Map<ClassIDPair, Collection<MappedField>> result = new HashMap<ClassIDPair, Collection<MappedField>>();
		Collection<MappedField> baseObjectResult = null;
		ClassIDPair basePair = new ClassIDPair(newObject.getID(), newObject.getClass().getCanonicalName());
		
		String className = newObject.getClass().getCanonicalName();
		IPerceroObject oldObject = systemGetById(new ClassIDPair(newObject.getID(), className));
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
			nextBasePerceroObject = (BaseDataObject) systemGetById(nextBasePair);
			
			itrCompareToList = compareToList.iterator();
			matchFound = false;
			while (itrCompareToList.hasNext()) {
				BaseDataObject nextCompareToPerceroObject = (BaseDataObject) itrCompareToList.next();
				nextCompareToPerceroObject = (BaseDataObject) systemGetById(BaseDataObject.toClassIdPair(nextCompareToPerceroObject));
				
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
	@SuppressWarnings("unchecked")
//	@Override
	public List<IPerceroObject> findAllRelatedObjects(IPerceroObject perceroObject, MappedField mappedField, Boolean shellOnly, String userId) throws SyncException {
		List<IPerceroObject> result = new ArrayList<IPerceroObject>();
		
		if (!StringUtils.hasText(perceroObject.getID())) {
			// No valid ID on the object, so can't search for it.
			return result;
		}
		
		// The reverse mapped field should be the MappedField on the target object, the one that this MUST be the data provider for.
		MappedField reverseMappedField = mappedField.getReverseMappedField();
		if (reverseMappedField == null) {
			// No reverse mapped field, meaning there is nothing to do.
			return result;
		}
		
		IDataProvider dataProvider = dataProviderManager.getDataProviderByName(reverseMappedField.getMappedClass().dataProviderName);
		result = dataProvider.getAllByRelationship(reverseMappedField, BaseDataObject.toClassIdPair(perceroObject), shellOnly, userId);
		
		return result;
	}
		
	public List<IPerceroObject> getAllByRelationship(MappedField mappedField, ClassIDPair targetClassIdPair, Boolean shellOnly, String userId) throws SyncException {
		IDataAccessObject<IPerceroObject> dao = daoRegistry.getDataAccessObject(mappedField.getMappedClass().className);
		return dao.getAllByRelationship(mappedField, targetClassIdPair, shellOnly, userId);
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

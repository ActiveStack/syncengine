package com.percero.agents.sync.services;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.Criteria;
import org.hibernate.PropertyValueException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.impl.SessionImpl;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.datastore.ICacheDataStore;
import com.percero.agents.sync.exceptions.SyncDataException;
import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.hibernate.AssociationExample;
import com.percero.agents.sync.hibernate.BaseDataObjectPropertySelector;
import com.percero.agents.sync.hibernate.SyncHibernateUtils;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.JpqlQuery;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.metadata.MappedFieldList;
import com.percero.agents.sync.metadata.MappedFieldMap;
import com.percero.agents.sync.metadata.MappedFieldPerceroObject;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.ClassIDPairs;
import com.percero.agents.sync.vo.IJsonObject;
import com.percero.agents.sync.vo.IRootObject;
import com.percero.framework.metadata.IMappedQuery;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;

@Component
public class HibernateDataProvider implements IDataProvider {

	// TODO: Better manage Hibernate Sessions (opening and closing).

	private static final Logger log = Logger.getLogger(HibernateDataProvider.class);
	
	public void initialize()
	{
		// Do nothing.
	}
	
	public String getName() {
		return "syncAgent";
	}
	
	@Autowired
	ICacheDataStore cacheDataStore;
	
	@Autowired
	Long cacheTimeout = Long.valueOf(60 * 60 * 24 * 14);	// Two weeks

	@Autowired
	ObjectMapper safeObjectMapper;

	@Autowired
	SessionFactory appSessionFactory;
	public void setAppSessionFactory(SessionFactory value) {
		appSessionFactory = value;
	}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	// TODO: @Transactional(readOnly=true)
	public PerceroList<IPerceroObject> getAllByName(Object aName, Integer pageNumber, Integer pageSize, Boolean returnTotal, String userId) throws Exception {
		Session s = appSessionFactory.openSession();
		try {
			returnTotal = true;
			String aClassName = aName.toString();
			Query countQuery = null;
			Query query = null;
			Class theClass = MappedClass.forName(aName.toString());

			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(aClassName);
			boolean isValidReadQuery = false;
			if (mappedClass != null) {
				if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())) {
					isValidReadQuery = true;
				}
			}

			/**
			 * readAllQuery optimization
			 * You can now define a readAllQuery on a class to imporove it's initial download time
			 * for briefcase mode.
			 * 
			 * Only supports plain SQL for now
			 */
			if(mappedClass.getReadAllQuery() != null && mappedClass.getReadAllQuery().getQuery() != null && !mappedClass.getReadAllQuery().getQuery().isEmpty()){
				if((mappedClass.getReadAllQuery() instanceof JpqlQuery)){
					throw new IllegalArgumentException("Illegal query type on:"+aClassName+". readAllQueries must be plain SQL. JPQL, HQL not supported yet. ");
				}
				
				log.debug("Using readAllQuery: "+aClassName);
				String selectQueryString = mappedClass.getReadAllQuery().getQuery();
				
				String countQueryString = "select count(*) from ("+selectQueryString+") as U";
				
				// Add the limit clause
				if (pageSize != null && pageNumber != null && pageSize.intValue() > 0) {
					int offset = pageSize.intValue() * pageNumber.intValue();
					selectQueryString += " limit "+pageSize+" OFFSET "+offset;
				}
				
				query = s.createSQLQuery(selectQueryString).addEntity(theClass);
				countQuery = s.createSQLQuery(countQueryString);
				
				query.setParameter("userId", userId);
				countQuery.setParameter("userId", userId);
			}
			else 
				if (theClass != null) {
				String countQueryString = "";
				if (returnTotal)
					countQueryString = "SELECT COUNT(getAllResult.ID) FROM " + aClassName + " getAllResult";
				String queryString = "SELECT getAllResult FROM " + aClassName + " getAllResult";

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
					String countQueryFilterString = "";
					if (returnTotal)
						countQueryFilterString = queryFilterString;
					queryFilterString = queryString + " WHERE (" + queryFilterString + ") > 0 ORDER BY getAllResult.ID";

					if (returnTotal) {
						countQueryFilterString = countQueryString + " WHERE (" + countQueryFilterString + ") > 0";
						countQuery = s.createQuery(countQueryFilterString);
					}
					query = s.createQuery(queryFilterString);
					mappedClass.getReadQuery().setQueryParameters(query, null, userId);
					mappedClass.getReadQuery().setQueryParameters(countQuery, null, userId);
				}
				else {
					queryString += " ORDER BY getAllResult.ID";
					if (returnTotal) {
						countQueryString += " ORDER BY ID";
						countQuery = s.createQuery(countQueryString);
					}
					query = s.createQuery(queryString);
				}
				if (pageSize != null && pageNumber != null && pageSize.intValue() > 0) {
					int pageValue = pageSize.intValue() * pageNumber.intValue();
					query.setFirstResult(pageValue);
					query.setMaxResults(pageSize.intValue());
				}
			}

			if (query != null) {
				
				log.debug("Get ALL: "+aClassName);	
				long t1 = new Date().getTime();
				List<?> list = query.list();
				long t2 = new Date().getTime();
				log.debug("Query Time: "+(t2-t1));
				PerceroList<IPerceroObject> result = new PerceroList<IPerceroObject>( (List<IPerceroObject>) SyncHibernateUtils.cleanObject(list, s, userId) );
				long t3 = new Date().getTime();
				log.debug("Clean Time: "+(t3-t2));
				
				result.setPageNumber(pageNumber);
				result.setPageSize(pageSize);
				
				if (returnTotal && pageSize != null && pageNumber != null && pageSize.intValue() > 0){
					result.setTotalLength(((Number)countQuery.uniqueResult()).intValue());
					log.debug("Total Obs: "+result.getTotalLength());
				}
				else
					result.setTotalLength(result.size());
				return result;
			}
			else {
				return null;
			}

		} catch (Exception e) {
			log.error("Unable to getAllByName", e);
		} finally {
			s.close();
		}

		return null;
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
					s = appSessionFactory.openSession();
					result = (IPerceroObject) s.get(theClass, classIdPair.getID());
					
					// Now put the object in the cache.
					if (result != null) {
						key = RedisKeyUtils.classIdPair(result.getClass().getCanonicalName(), result.getID());
						result = (IPerceroObject) SyncHibernateUtils.cleanObject(result, s);
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

	@SuppressWarnings({ "rawtypes" })
	public IPerceroObject systemGetByIdWithClassAndSession(ClassIDPair classIdPair, Class theClass, Session s) {
		Boolean sessionAlreadyOpen = false;
		try {
			if (theClass != null) {
				IPerceroObject result = retrieveFromRedisCache(classIdPair);
				String key = null;
				
				if (result == null) {
					if (s == null || !s.isOpen()) {
						s = appSessionFactory.openSession();
					}
					else {
						sessionAlreadyOpen = true;
					}
					result = (IPerceroObject) s.get(theClass, classIdPair.getID());
					
					// Now put the object in the cache.
					if (result != null) {
						key = RedisKeyUtils.classIdPair(result.getClass().getCanonicalName(), result.getID());
						result = (IPerceroObject) SyncHibernateUtils.cleanObject(result, s);
						if (cacheTimeout > 0)
							cacheDataStore.setValue(key, ((BaseDataObject)result).toJson());
					}
					else {
						log.warn("Unable to retrieve object from database: " + classIdPair.toString());
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
			// Only close the session if it wasn't already open.
			if (!sessionAlreadyOpen) {
				if (s != null && s.isOpen()) {
					s.close();
				}
			}
		}
		return null;
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
					String queryString = "SELECT findByIdsResult FROM " + classIdPairs.getClassName() + " findByIdsResult WHERE findByIdsResult.ID IN (:idsSet)";

					// Open the database session.
					Session s = null;
					try {
						s = appSessionFactory.openSession();
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
						List<IPerceroObject> cleanedDatabaseObjects = (List<IPerceroObject>) SyncHibernateUtils.cleanObject(queryResult, s, userId);
						
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public IPerceroObject findUnique(IPerceroObject theQueryObject, String userId) {
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
			log.error("Unable to findByExample", e);
		} finally {
			s.close();
		}

		return null;
	}

	// TODO: Add permissions check.
	@SuppressWarnings({ "unchecked" })
	public List<IPerceroObject> findByExample(IPerceroObject theQueryObject, List<String> excludeProperties, String userId) {
		Session s = appSessionFactory.openSession();
		try {
			Criteria criteria = s.createCriteria(theQueryObject.getClass());
			AssociationExample example = AssociationExample.create(theQueryObject);
			BaseDataObjectPropertySelector propertySelector = new BaseDataObjectPropertySelector(excludeProperties);
			example.setPropertySelector(propertySelector);
			criteria.add(example);

			List<IPerceroObject> result = criteria.list();
			result = (List<IPerceroObject>) SyncHibernateUtils.cleanObject(result, s, userId);

			return (List<IPerceroObject>) result;
		} catch (Exception e) {
			log.error("Unable to findByExample", e);
		} finally {
			s.close();
		}

		return null;
	}
	
	@SuppressWarnings({ "unchecked" })
	public List<IPerceroObject> systemFindByExample(IPerceroObject theQueryObject, List<String> excludeProperties) {
		Session s = appSessionFactory.openSession();
		try {
			Criteria criteria = s.createCriteria(theQueryObject.getClass());
			AssociationExample example = AssociationExample.create(theQueryObject);
			BaseDataObjectPropertySelector propertySelector = new BaseDataObjectPropertySelector(excludeProperties);
			example.setPropertySelector(propertySelector);
			criteria.add(example);
			
			List<IPerceroObject> result = criteria.list();
			result = (List<IPerceroObject>) SyncHibernateUtils.cleanObject(result, s);
			
			return (List<IPerceroObject>) result;
		} catch (Exception e) {
			log.error("Unable to findByExample", e);
		} finally {
			s.close();
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<IPerceroObject> searchByExample(IPerceroObject theQueryObject,
			List<String> excludeProperties, String userId) {
		Session s = appSessionFactory.openSession();
		try {
			Criteria criteria = s.createCriteria(theQueryObject.getClass());
			AssociationExample example = AssociationExample.create(theQueryObject);
			BaseDataObjectPropertySelector propertySelector = new BaseDataObjectPropertySelector(excludeProperties);
			example.setPropertySelector(propertySelector);
			example.enableLike(MatchMode.ANYWHERE);
			criteria.add(example);

			List<IPerceroObject> result = criteria.list();
			result = (List<IPerceroObject>) SyncHibernateUtils.cleanObject(result, s, userId);
			
			return result;
		} catch (Exception e) {
			log.error("Unable to searchByExample", e);
		} finally {
			s.close();
		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public IPerceroObject systemCreateObject(IPerceroObject perceroObject)
			throws SyncException {
		Session s = appSessionFactory.openSession();
		
		try {
			// Make sure object has an ID.
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
			if (!mappedClass.hasGeneratedId && !StringUtils.hasText(perceroObject.getID()))
				perceroObject.setID(UUID.randomUUID().toString());
			else {
				// Check to see if item already exists.
				Object existingObject = s.get(perceroObject.getClass(), perceroObject.getID());
				if (existingObject != null)
				{
					perceroObject = (IPerceroObject) SyncHibernateUtils.cleanObject(existingObject, s);
					deepCleanObject(perceroObject, mappedClass, s, null);
					return perceroObject;
				}
			}
	
			Transaction tx = s.beginTransaction();
			tx.begin();
			s.save(perceroObject);
			tx.commit();
	
			s.refresh(perceroObject);
	
			perceroObject = (IPerceroObject) SyncHibernateUtils.cleanObject(perceroObject, s);
			deepCleanObject(perceroObject, mappedClass, s, null);

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
				Iterator<MappedField> itrToOneFields = mappedClass.toOneFields.iterator();
				while(itrToOneFields.hasNext()) {
					MappedField nextMappedField = itrToOneFields.next();
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
		finally {
			if (s != null && s.isOpen()) {
				s.close();
			}
		}
	}
	
	private void deepCleanObject(IPerceroObject perceroObject, MappedClass mappedClass, Session s, String userId) {
		if (!(perceroObject instanceof IRootObject)) {
			for(MappedField nextMappedField : mappedClass.externalizableFields) {
				try {
					if (nextMappedField instanceof MappedFieldPerceroObject) {
						Object fieldValue = nextMappedField.getValue(perceroObject);
						if (fieldValue != null) {
							fieldValue = SyncHibernateUtils.cleanObject(fieldValue, s, userId);
							nextMappedField.getSetter().invoke(perceroObject, fieldValue);
						}
					}
					else if (nextMappedField instanceof MappedFieldList) {
						Object fieldValue = nextMappedField.getValue(perceroObject);
						if (fieldValue != null) {
							fieldValue = SyncHibernateUtils.cleanObject(fieldValue, s, userId);
							nextMappedField.getSetter().invoke(perceroObject, fieldValue);
						}
					}
					else if (nextMappedField instanceof MappedFieldMap) {
						Object fieldValue = nextMappedField.getValue(perceroObject);
						if (fieldValue != null) {
							fieldValue = SyncHibernateUtils.cleanObject(fieldValue, s, userId);
							nextMappedField.getSetter().invoke(perceroObject, fieldValue);
						}
					}
				} catch(Exception e) {
					log.error("Error in postCreateObject " + mappedClass.className + "." + nextMappedField.getField().getName(), e);
				}
			}
		}
	}

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
					Query updateFilter = s.createQuery(mappedClass.getUpdateQuery().getQuery());
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
		Session s = null;

		try {
			s = appSessionFactory.openSession();
			Transaction tx = s.beginTransaction();
			tx.begin();
			try {
				//s.evict(perceroObject);
				//perceroObject.setID(perceroObject.getID());
				perceroObject = (IPerceroObject) SyncHibernateUtils.cleanObject(perceroObject, s);
				//s.persist(perceroObject);
				//perceroObject = (IPerceroObject) s.merge(perceroObject);
			} catch(Exception e) {
				e.printStackTrace();
			}
			//s.saveOrUpdate(perceroObject);
			s.update(perceroObject);
			tx.commit();

			s.refresh(perceroObject);

			((BaseDataObject)perceroObject).setIsClean(false);
			perceroObject = (IPerceroObject) SyncHibernateUtils.cleanObject(perceroObject, s);
			
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
					Iterator<MappedField> itrToOneFields = mappedClass.toOneFields.iterator();
					while(itrToOneFields.hasNext()) {
						MappedField nextMappedField = itrToOneFields.next();
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
		} finally {
			s.close();
		}
	}

	

	////////////////////////////////////////////////////
	//	DELETE
	////////////////////////////////////////////////////
	public Boolean deleteObject(ClassIDPair theClassIdPair, String userId) throws SyncException {
		Session s = appSessionFactory.openSession();

		try {
			IPerceroObject perceroObject = (IPerceroObject) s.get(theClassIdPair.getClassName(), theClassIdPair.getID());
			if (perceroObject == null) {
				return true;
			}
			
			boolean hasAccess = true;
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(theClassIdPair.getClassName());
			if (mappedClass != null) {
				if (mappedClass.getDeleteQuery() != null && StringUtils.hasText(mappedClass.getDeleteQuery().getQuery())){
					Query deleteFilter = s.createQuery(mappedClass.getDeleteQuery().getQuery());
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
		Boolean result = false;
		Session s = appSessionFactory.openSession();

		try {
			// Load the object.
			IPerceroObject perceroObjectToDelete = (IPerceroObject) s.load(perceroObject.getClass().getName(), perceroObject.getID());

			// Make sure all associated objects are loaded.
			// Clean the object before deletion because otherwise it will fail.
			perceroObject = (IPerceroObject) SyncHibernateUtils.cleanObject(perceroObjectToDelete, s);
			
			Transaction tx = s.beginTransaction();
			tx.begin();
			s.delete(perceroObjectToDelete);
			tx.commit();

			// Now delete from cache.
			// Now update the cache.
			// TODO: Field-level updates could be REALLY useful here.  Would avoid A TON of UNNECESSARY work...
			if (cacheTimeout > 0) {
				Set<String> keysToDelete = new HashSet<String>();

				String key = RedisKeyUtils.classIdPair(SyncHibernateUtils.getShell(perceroObject).getClass().getCanonicalName(), perceroObject.getID());
				keysToDelete.add(key);

				IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
				MappedClass mappedClass = mcm.getMappedClassByClassName(SyncHibernateUtils.getShell(perceroObject).getClass().getName());
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
				Iterator<MappedField> itrToOneFields = mappedClass.toOneFields.iterator();
				while(itrToOneFields.hasNext()) {
					MappedField nextMappedField = itrToOneFields.next();
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

			result = true;
		} catch (Exception e) {
			log.error("Error deleting object", e);
			
			SyncDataException sde = new SyncDataException(SyncDataException.DELETE_OBJECT_ERROR, SyncDataException.DELETE_OBJECT_ERROR_CODE);
			throw sde;
		} finally {
			s.close();
		}

		return result;
	}

	/*
	public Object cleanObject(Object object) {
		return SyncHibernateUtils.cleanObject(object);
	}*/

	public Object cleanObject(Object object, Session s, String userId) {
		try {
			if (s == null) {
				s = appSessionFactory.openSession();
			}
			return SyncHibernateUtils.cleanObject(object, s, userId);
		} catch(Exception e) {
			log.warn("Error cleaning object", e);
			return null;
		} finally {
			if (s != null && s.isOpen()) {
				s.close();
			}
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
}

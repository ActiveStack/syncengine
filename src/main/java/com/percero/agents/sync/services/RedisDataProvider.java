package com.percero.agents.sync.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.ClassIDPairs;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;

@Component
public class RedisDataProvider implements IDataProvider {

	//private static final Logger log = Logger.getLogger(RedisDataProvider.class);
	
	public void initialize()
	{
		// Do nothing.
	}

	public String getName() {
		return "redis";
	}
	

	public Integer countAllByName(String className, String userId) throws Exception {
		return null;
	}
	
	public PerceroList<IPerceroObject> getAllByName(Object aName, Integer pageNumber, Integer pageSize, Boolean returnTotal, String clientId) throws Exception {
/*		try {
			String aClassName = aName.toString();
			Query query = null;
			Class theClass = Class.forName(aName.toString());

			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(aClassName);
			boolean isValidReadQuery = false;
			if (mappedClass != null) {
				if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())) {
					isValidReadQuery = true;
				}
			}

			if (theClass != null) {
				Set<Object> resultSet = (Set<Object>) JOhm.getAll(theClass);
				List<IPerceroObject> result = new ArrayList(resultSet);
				return result;
				/**
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
					queryFilterString = queryString + " WHERE (" + queryFilterString + ") > 0";

					query = s.createQuery(queryFilterString);
					mappedClass.getReadQuery().setQueryParameters(query, null, client.getBelongsToUserId());
				}
				else {
					queryString += " ORDER BY ID";
					query = s.createQuery(queryString);
					if (pageSize != null && pageNumber != null && pageSize.intValue() > 0) {
						int pageValue = pageSize.intValue() * pageNumber.intValue();
						query.setFirstResult(pageValue);
						query.setMaxResults(pageSize.intValue());
						//queryString += " LIMIT " + pageValue + " " + pageSize.toString();
					}
				}
				**
			}*/

			/**
			if (query != null) {
				List<IPerceroObject> result = (List<IPerceroObject>) query.list();
				result = (List<IPerceroObject>) HibernateUtils.cleanObject(result);
				
				return result;
			}
			else {
				return null;
			}**

		} catch (Exception e) {
			log.error("Unable to getAllByName", e);
		}*/

		return null;
	}

	public Boolean getReadAccess(ClassIDPair classIdPair, String userId) {
		return null;
	}
	
	public IPerceroObject findById(ClassIDPair classIdPair, String userId) {
/*		try {
			Class theClass = Class.forName(classIdPair.getClassName().toString());
			return (IPerceroObject) JOhm.get(theClass, classIdPair.getID());
		} catch (Exception e) {
			log.error("Unable to findById", e);
		}*/
		return null;
/**		Session s = appSessionFactory.openSession();
		try {
			Class theClass = Class.forName(classIdPair.getClassName().toString());

			if (theClass != null) {
				IPerceroObject result = (IPerceroObject) s.get(theClass, classIdPair.getID());

				boolean hasAccess = (result != null);
				
				if (hasAccess) {
 					IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
					MappedClass mappedClass = mcm.getMappedClassByClassName(classIdPair.getClassName());
					if (mappedClass != null) {
						if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())){
							Query readFilter = s.createQuery(mappedClass.getReadQuery().getQuery());
							mappedClass.getReadQuery().setQueryParameters(readFilter, result, client.getBelongsToUserId());
							Number readFilterResult = (Number) readFilter.uniqueResult();
							if (readFilterResult == null || readFilterResult.intValue() <= 0)
								hasAccess = false;
						}
					}
				}

				if (hasAccess) {
					result = (IPerceroObject) HibernateUtils.cleanObject(result);
					return result;

				} else
					return null;
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error("Unable to findById", e);
		} finally {
			s.close();
		}
		return null;**/
	}

	@Override
	public <T extends IPerceroObject> T systemGetById(ClassIDPair classIdPair, boolean ignoreCache) {
		return null;
	}

	public IPerceroObject systemGetById(ClassIDPair classIdPair) {
		return null;
	}

	public List<IPerceroObject> findByIds(ClassIDPairs classIdPairs, String clientId) {
/*		List<IPerceroObject> result = null;

		try {
			Class theClass = Class.forName(classIdPairs.getClassName().toString());
			for(String nextId : classIdPairs.getIds()) {
				IPerceroObject nextObject = JOhm.get(theClass, nextId);
				if (nextObject != null)
					result.add(nextObject);
			}
			return result;
		} catch (Exception e) {
			log.error("Unable to findById", e);
		}*/
		return null;

		/**
		Session s = appSessionFactory.openSession();

		try {
			boolean hasAccess = true;
			Query query = null;
			Class theClass = Class.forName(classIdPairs.getClassName().toString());
			
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(classIdPairs.getClassName());
			boolean isValidReadQuery = false;
			if (mappedClass != null) {
				if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())) {
					isValidReadQuery = true;
				}

			}

			if (hasAccess) {
				if (theClass != null)
				{
					String strIds = "";
					int count = 0;
					for(String nextId : classIdPairs.getIds()) {
						if (count > 0)
							strIds += ",";
						strIds += "'" + nextId + "'";
						count++;
					}
					
					String queryString = "SELECT findByIdsResult FROM " + classIdPairs.getClassName() + " findByIdsResult WHERE findByIdsResult.ID IN (" + strIds + ")";
					
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
						mappedClass.getReadQuery().setQueryParameters(query, null, client.getBelongsToUserId());
					}
					else {
						query = s.createQuery(queryString);
					}
				}
	
				if (query != null) {
					List queryResult = query.list();
					
					if (result == null)
						result = (List<IPerceroObject>) HibernateUtils.cleanObject(queryResult);
					else
						result.addAll( (List<IPerceroObject>) HibernateUtils.cleanObject(queryResult) );
				}
			}
		} catch (Exception e) {
			log.error("Unable to findByIds", e);
		} finally {
			s.close();
		}

		return result;
		**/
	}

	public IPerceroObject findUnique(IPerceroObject theQueryObject, String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<IPerceroObject> findByExample(IPerceroObject theQueryObject,
			List<String> excludeProperties, String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<IPerceroObject> searchByExample(IPerceroObject theQueryObject,
			List<String> excludeProperties, String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public IPerceroObject systemCreateObject(IPerceroObject perceroObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public IPerceroObject createObject(IPerceroObject perceroObject, String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	

	////////////////////////////////////////////////////
	//	PUT
	////////////////////////////////////////////////////
	public IPerceroObject putObject(IPerceroObject perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields, String userId) {
/*		try {
			Object result = JOhm.save(perceroObject);
			return perceroObject;
		} catch (Exception e) {
			log.error("Unable to putObject", e);
		}*/
		return null;
		/**
		boolean hasAccess = true;
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(perceroObject.getClass().getName());
		if (mappedClass != null) {
			if (mappedClass.getUpdateQuery() != null && StringUtils.hasText(mappedClass.getUpdateQuery().getQuery())){
				Session s = appSessionFactory.openSession();
				try {
					Query updateFilter = s.createQuery(mappedClass.getUpdateQuery().getQuery());
					mappedClass.getUpdateQuery().setQueryParameters(updateFilter, perceroObject, client.getBelongsToUserId());
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
			return systemPutObject(perceroObject, client, false);
		} else {
			return null;
		}
		**/
	}

	

	////////////////////////////////////////////////////
	//	DELETE
	////////////////////////////////////////////////////
	public Boolean deleteObject(ClassIDPair theClassIdPair, String userId) {
		// TODO Auto-generated method stub
		return false;
	}

	public List<IPerceroObject> systemFindByExample(
			IPerceroObject theQueryObject, List<String> excludeProperties) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Object> runQuery(MappedClass mappedClass, String queryName,
			Object[] queryArguments, String clientId) {
		// TODO Auto-generated method stub
		return null;
	}

	public Boolean getDeleteAccess(ClassIDPair classIdPair, String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public IPerceroObject systemPutObject(IPerceroObject perceroObject, Map<ClassIDPair, Collection<MappedField>> changedFields) {
		// TODO Auto-generated method stub
		return null;
	}

	public Boolean systemDeleteObject(IPerceroObject perceroObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object cleanObject(Object object, Session s, String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<ClassIDPair, Collection<MappedField>> getChangedMappedFields(IPerceroObject newObject) {
		return null;
	}
}

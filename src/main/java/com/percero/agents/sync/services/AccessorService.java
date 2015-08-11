package com.percero.agents.sync.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.framework.accessor.Accessor;
import com.percero.framework.accessor.IAccessorService;
import com.percero.framework.metadata.IMappedClass;
import com.percero.framework.vo.IPerceroObject;

@Component
public class AccessorService implements IAccessorService {

	private static String currentVersion = "0.1";
	private static Logger log = Logger
			.getLogger(AccessorService.class);
	
	@Autowired
	SessionFactory appSessionFactory;
	public void setAppSessionFactory(SessionFactory value) {
		appSessionFactory = value;
	}
	
	@Autowired
	ISyncAgentService syncAgentService;
	public void setSyncAgentService(ISyncAgentService value) {
		syncAgentService = value;
	}

	public AccessorService() {
	}

	public Object testCall(String aParam) {
		return aParam;
	}

	public Object getVersion() {
		return currentVersion;
	}

	public Object pushMessage(String userId, String userToken, String deviceId, String className, String classId, Object message) {
//		getSessionManager().putObject(theObject, user)
		return false;
	}

	@SuppressWarnings("rawtypes")
	public Accessor getAccessor(String userId, String className, String classId) {
		// TODO: This should not take in the userId. Or else it should not be
		// publicly available.
		// It should take the userId of the current Principal.
		log.debug("Getting AccessorType for user[" + userId + "], " + className
				+ "(" + classId + ")");
		Accessor result = null;

		try {
			Class theClass = MappedClass.forName(className.toString());
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(className);
			
			result = new Accessor();
			Accessor readAccessor = getReadAccessor(userId, theClass, classId, mappedClass);
			result.setCanRead(readAccessor.getCanRead());
			Accessor modifyAccessor = getUpdateAccessor(userId, theClass, classId, mappedClass);
			result.setCanUpdate(modifyAccessor.getCanUpdate());
			Accessor createAccessor = getCreateAccessor(userId, theClass, classId, mappedClass);
			result.setCanCreate(createAccessor.getCanCreate());
			Accessor deleteAccessor = getDeleteAccessor(userId, theClass, classId, mappedClass);
			result.setCanDelete(deleteAccessor.getCanDelete());
			log.debug("Returning AccessorType " + result.toString());
		} catch(Exception e) {
			log.error("Error getting accessor for " + className + "[" + classId + "] for user " + userId, e);
		}

		return result;
	}

	@SuppressWarnings("rawtypes")
	public Accessor getReadAccessor(String userId, String className, String classId) {
		
		try {
			Class theClass = MappedClass.forName(className.toString());
			
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(className);
			
			return getReadAccessor(userId, theClass, classId, mappedClass);
		} catch(Exception e) {
			log.error("Error getting READ accessor for " + className + " [" + classId + "] for user " + userId, e);
		}
		
		return new Accessor();
	}

	@SuppressWarnings("rawtypes")
	public Accessor getReadAccessor(String userId, Class theClass, String classId, IMappedClass mappedClass) throws Exception {
		// TODO: This should not take in the userId. Or else it should not be
		// publicly available.
		// It should take the userId of the current Principal.
		log.debug("Getting AccessorType for user[" + userId + "], " + theClass.getName()
				+ "(" + classId + ")");
		Accessor result = new Accessor();

		Session s = appSessionFactory.openSession();
		try {
			if (theClass != null && mappedClass != null) {
				Object theObject = s.get(theClass, classId);
				boolean hasReadAccess = (theObject != null);
				
				if (hasReadAccess) {
					if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())){
						Query readFilter = s.createQuery(mappedClass.getReadQuery().getQuery());
						mappedClass.getReadQuery().setQueryParameters(readFilter, theObject, userId);
						List readFilterResult = readFilter.list();
						if (readFilterResult == null || readFilterResult.size() == 0)
							hasReadAccess = false;
					}
				}
				result.setCanRead(hasReadAccess);
			}
		} catch(Exception e) {
			log.error("Error getting accessor for " + theClass.getName() + "[" + classId + "] for user " + userId, e);
		} finally {
			s.close();
		}

		log.debug("Returning AccessorType " + result.toString());

		return result;
	}

	@SuppressWarnings("rawtypes")
	public Map<String, Accessor> getReadAccessors(List<String> userIds, Class theClass, String classId, IMappedClass mappedClass) throws Exception {
		// TODO: This should not take in the userId. Or else it should not be
		// publicly available.
		// It should take the userId of the current Principal.
		log.debug("Getting Read Accessors for " + theClass.getName()
				+ "(" + classId + ")");
		Map<String, Accessor> result = new HashMap<String, Accessor>();

		Session s = appSessionFactory.openSession();
		try {
			if (theClass != null && mappedClass != null) {
				Object theObject = s.get(theClass, classId);
				for(String userId : userIds) {
					boolean hasReadAccess = (theObject != null);
					
					if (hasReadAccess) {
						if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())){
							Query readFilter = s.createQuery(mappedClass.getReadQuery().getQuery());
							mappedClass.getReadQuery().setQueryParameters(readFilter, theObject, userId);
							List readFilterResult = readFilter.list();
							if (readFilterResult == null || readFilterResult.size() == 0)
								hasReadAccess = false;
						}
					}
					Accessor accessor = new Accessor();
					accessor.setCanRead(hasReadAccess);
					result.put(userId, accessor);
				}
			}
		} catch(Exception e) {
			log.error("Error getting read accessors for " + theClass.getName() + "[" + classId + "]", e);
		} finally {
			s.close();
		}

		return result;
	}

	@SuppressWarnings("rawtypes")
	public Accessor getUpdateAccessor(String userId, String className, String classId) {
		
		try {
			Class theClass = MappedClass.forName(className.toString());
			
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(className);
			
			return getUpdateAccessor(userId, theClass, classId, mappedClass);
		} catch(Exception e) {
			log.error("Error getting UPDATE accessor for " + className + " [" + classId + "] for user " + userId, e);
		}
		
		return new Accessor();
	}

	@SuppressWarnings("rawtypes")
	public Accessor getUpdateAccessor(String userId, Class theClass, String classId, MappedClass mappedClass) throws Exception {
		// TODO: This should not take in the userId. Or else it should not be
		// publicly available.
		// It should take the userId of the current Principal.
		log.debug("Getting AccessorType for user[" + userId + "], " + theClass.getName()
				+ "(" + classId + ")");
		Accessor result = new Accessor();

		Session s = appSessionFactory.openSession();
		try {
			if (theClass != null && mappedClass != null) {
				Object theObject = s.get(theClass, classId);
				boolean hasUpdateAccess = true;
				if (theObject instanceof IPerceroObject) {
					if (mappedClass != null) {
						if (mappedClass.getUpdateQuery() != null && StringUtils.hasText(mappedClass.getUpdateQuery().getQuery())){
							Query updateFilter = s.createQuery(mappedClass.getUpdateQuery().getQuery());
							mappedClass.getUpdateQuery().setQueryParameters(updateFilter, theObject, userId);
							List updateFilterResult = updateFilter.list();
							if (updateFilterResult == null || updateFilterResult.size() == 0)
								hasUpdateAccess = false;
						}
					}
				}
				result.setCanUpdate(hasUpdateAccess);
			}
		} catch(Exception e) {
			log.error("Error getting UPDATE accessor for " + theClass.getName() + "[" + classId + "] for user " + userId, e);
		} finally {
			s.close();
		}

		log.debug("Returning AccessorType " + result.toString());

		return result;
	}

	@SuppressWarnings("rawtypes")
	public Accessor getCreateAccessor(String userId, String className, String classId) {
		
		try {
			Class theClass = MappedClass.forName(className.toString());
			
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(className);
			
			return getCreateAccessor(userId, theClass, classId, mappedClass);
		} catch(Exception e) {
			log.error("Error getting CREATE accessor for " + className + " [" + classId + "] for user " + userId, e);
		}
		
		return new Accessor();
	}

	@SuppressWarnings("rawtypes")
	public Accessor getCreateAccessor(String userId, Class theClass, String classId, MappedClass mappedClass) throws Exception {
		// TODO: This should not take in the userId. Or else it should not be
		// publicly available.
		// It should take the userId of the current Principal.
		log.debug("Getting AccessorType for user[" + userId + "], " + theClass.getName()
				+ "(" + classId + ")");
		Accessor result = new Accessor();

		Session s = appSessionFactory.openSession();
		try {
			if (theClass != null && mappedClass != null) {
				Object theObject = s.get(theClass, classId);
				boolean hasCreateAccess = true;
				if (theObject instanceof IPerceroObject) {
					if (mappedClass != null) {
						if (mappedClass.getCreateQuery() != null && StringUtils.hasText(mappedClass.getCreateQuery().getQuery())){
							Query createFilter = s.createQuery(mappedClass.getCreateQuery().getQuery());
							mappedClass.getCreateQuery().setQueryParameters(createFilter, theObject, userId);
							List createFilterResult = createFilter.list();
							if (createFilterResult == null || createFilterResult.size() == 0)
								hasCreateAccess = false;
						}
					}
				}
				result.setCanCreate(hasCreateAccess);
			}
		} catch(Exception e) {
			log.error("Error getting CREATE accessor for " + theClass.getName() + "[" + classId + "] for user " + userId, e);
		} finally {
			s.close();
		}

		log.debug("Returning AccessorType " + result.toString());

		return result;
	}

	@SuppressWarnings("rawtypes")
	public Accessor getDeleteAccessor(String userId, String className, String classId) {
		
		try {
			Class theClass = MappedClass.forName(className.toString());
			
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mappedClass = mcm.getMappedClassByClassName(className);
			
			return getDeleteAccessor(userId, theClass, classId, mappedClass);
		} catch(Exception e) {
			log.error("Error getting DELETE accessor for " + className + " [" + classId + "] for user " + userId, e);
		}
		
		return new Accessor();
	}

	@SuppressWarnings("rawtypes")
	public Accessor getDeleteAccessor(String userId, Class theClass, String classId, MappedClass mappedClass) throws Exception {
		// TODO: This should not take in the userId. Or else it should not be
		// publicly available.
		// It should take the userId of the current Principal.
		log.debug("Getting AccessorType for user[" + userId + "], " + theClass.getName()
				+ "(" + classId + ")");
		Accessor result = new Accessor();

		Session s = appSessionFactory.openSession();
		try {
			if (theClass != null && mappedClass != null) {
				Object theObject = s.get(theClass, classId);
				boolean hasDeleteAccess = true;
				if (theObject instanceof IPerceroObject) {
					if (mappedClass != null) {
						if (mappedClass.getDeleteQuery() != null && StringUtils.hasText(mappedClass.getDeleteQuery().getQuery())){
							Query deleteFilter = s.createQuery(mappedClass.getDeleteQuery().getQuery());
							mappedClass.getDeleteQuery().setQueryParameters(deleteFilter, theObject, userId);
							List deleteFilterResult = deleteFilter.list();
							if (deleteFilterResult == null || deleteFilterResult.size() == 0)
								hasDeleteAccess = false;
						}
					}
				}
				result.setCanDelete(hasDeleteAccess);
			}
		} catch(Exception e) {
			log.error("Error getting DELETE accessor for " + theClass.getName() + "[" + classId + "] for user " + userId, e);
		} finally {
			s.close();
		}

		log.debug("Returning AccessorType " + result.toString());

		return result;
	}

	
	
	/****************************************************
	 * HELPER FUNCTIONS *
	 ****************************************************/
}

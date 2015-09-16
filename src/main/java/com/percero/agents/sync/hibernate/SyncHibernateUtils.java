package com.percero.agents.sync.hibernate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.persistence.Entity;

import org.apache.log4j.Logger;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.collection.PersistentBag;
import org.hibernate.collection.PersistentList;
import org.hibernate.collection.PersistentMap;
import org.hibernate.collection.PersistentSet;
import org.hibernate.collection.PersistentSortedSet;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.metadata.MappedFieldPerceroObject;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.framework.vo.IPerceroObject;


// TODO: Utilize MappedClass.readAccessRightsFieldReferences to optimize read access checking.

public class SyncHibernateUtils {

	private static final Logger log = Logger.getLogger(SyncHibernateUtils.class);
	
	public static Object initHibernateProxy(HibernateProxy hp) {
		try {
			return hp.getHibernateLazyInitializer().getImplementation();
		} catch(ObjectNotFoundException onfe) {
			return null;
		} catch(Exception e) {
			return null;
		}
	}
	
	public static Object loadObject(Object object) {
		if (object instanceof HibernateProxy)
			object = initHibernateProxy((HibernateProxy) object);
		
		return object;
	}
	
	public static Boolean isProxy(Object object) {
		return (object instanceof HibernateProxy);
	}
	public static Object cleanObject(Object object, Session s) {
		return cleanObject(object, s, new HashMap<Object, Object>(), null);
	}
	public static Object cleanObject(Object object, Session s, String userId) {
		return cleanObject(object, s, new HashMap<Object, Object>(), userId);
	}
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object cleanObject(Object object, Session s, Map<Object, Object> inProcessObjects, String userId) {
		/**
		 * Used to indicate that we have already cleaned this object in general, and now only
		 * fields that have readAccessRights need to be recalculated.
		 */
		boolean isClean = false;
		if (object == null)
			return null;
		
		if (object instanceof List<?>) {
			List<Object> list = (List<Object>) object;
			for (int i = 0; i < list.size(); i++) {
				Object nextObject = list.get(i);
				if (inProcessObjects.containsKey(nextObject))
					nextObject = inProcessObjects.get(nextObject);
				list.set(i, cleanObject(nextObject, s, inProcessObjects, userId));
			}
			
			return list;
		}
		
		if (object instanceof Map<?, ?>) {
			Map<Object, Object> map = (Map<Object, Object>) object;
			Map<Object, Object> newMap = new HashMap<Object, Object>();
			for(Object key : map.keySet()) {
				Object nextObject = map.get(key);
				if (inProcessObjects.containsKey(nextObject))
					nextObject = inProcessObjects.get(nextObject);
				newMap.put(key, cleanObject(nextObject, s, inProcessObjects, userId));
			}
			
			return newMap;
		}
		
		if (inProcessObjects.containsKey(object))
			return inProcessObjects.get(object);
		
		Entity entity = object.getClass().getAnnotation(Entity.class);
		if (entity == null && !(object instanceof HibernateProxy)) {
			inProcessObjects.put(object, object);
			return object;
		}
		// Check to see if this object is clean.
		else if (((object instanceof BaseDataObject) && ((BaseDataObject)object).getIsClean())) {
			if (!StringUtils.hasText(userId)) {
				// If their is a User, need to check to see if any Fields have ReadAccessRights, if not then
				//	we can just return the object as-is.  Otherwise, we need to recalculate just those fields.
				inProcessObjects.put(object, object);
				return object;
			}
			else {
				isClean = true;
			}
		}
		else if (object instanceof HibernateProxy)
		{
			object = ((HibernateProxy) object).getHibernateLazyInitializer().getImplementation();

			// Check to see if this object is in the inProcessObjects.
			if (inProcessObjects.containsKey(object))
				return inProcessObjects.get(object);
		}

		Class objectClass = HibernateProxyHelper.getClassWithoutInitializingProxy(object);

		Object newObject = null;
		try {
			newObject = objectClass.newInstance();
			inProcessObjects.put(object, newObject);
			IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
			MappedClass mc = mcm.getMappedClassByClassName(objectClass.getName());
			
			for(MappedField theMappedField : mc.toManyFields) {
				Object fieldObject = theMappedField.getGetter().invoke(object);
				if (fieldObject == null)
					theMappedField.getSetter().invoke(newObject, fieldObject);
				else if (fieldObject instanceof PersistentList) {
					handleCollection(object, newObject, mc, theMappedField, (Collection) fieldObject, new ArrayList(), userId, mcm, s, inProcessObjects, isClean);
					//handlePersistentList(object, newObject, mc, theMappedField, (PersistentList) fieldObject, userId, mcm, s, inProcessObjects);
				} else if (fieldObject instanceof PersistentBag) {
					handleCollection(object, newObject, mc, theMappedField, (Collection) fieldObject, new ArrayList(), userId, mcm, s, inProcessObjects, isClean);
					//handlePersistentBag(object, newObject, mc, theMappedField, (PersistentBag) fieldObject, userId, mcm, s, inProcessObjects);
				} else if (fieldObject instanceof PersistentSortedSet) {
					handleCollection(object, newObject, mc, theMappedField, (Collection) fieldObject, new TreeSet(), userId, mcm, s, inProcessObjects, isClean);
					//handlePersistentSortedSet(object, newObject, mc, theMappedField, (PersistentSortedSet) fieldObject, userId, mcm, s, inProcessObjects);
				} else if (fieldObject instanceof PersistentSet) {
					handleCollection(object, newObject, mc, theMappedField, (Collection) fieldObject, new HashSet(), userId, mcm, s, inProcessObjects, isClean);
				} else if (fieldObject instanceof PersistentMap) {
					handleMap(object, newObject, mc, theMappedField, (PersistentMap) fieldObject, new HashMap(), userId, mcm, s, inProcessObjects, isClean);
				} else if (fieldObject instanceof HibernateProxy) {
					handleFieldObject(object, newObject, mc, theMappedField, fieldObject, userId, mcm, s, inProcessObjects, isClean);
					/**
					if (theMappedField.getUseLazyLoading())
						try {
							boolean hasAccess = true;
							if (userId != null && userId.length() > 0) {
								// Need to check read access.
								hasAccess = hasAccess(fieldObject, userId, mcm, s);
							}

							if (hasAccess) {
								theMappedField.getSetter().invoke(newObject, getShell(fieldObject));
							}
						} catch(Exception e) {
							e.printStackTrace();
						}
					else
						theMappedField.getSetter().invoke(newObject, cleanObject(fieldObject, s, inProcessObjects, userId));
						*/
				}
				else if (fieldObject instanceof List) {
					handleCollection(objectClass, newObject, mc, theMappedField, (List) fieldObject, new ArrayList(), userId, mcm, s, inProcessObjects, isClean);
				}
			}
			
			for(MappedField theMappedField : mc.toOneFields) {
				Object fieldObject = theMappedField.getGetter().invoke(object);
				if (fieldObject == null)
					theMappedField.getSetter().invoke(newObject, fieldObject);
				else if (fieldObject instanceof HibernateProxy) {
					handleFieldObject(object, newObject, mc, theMappedField, fieldObject, userId, mcm, s, inProcessObjects, isClean);
					/**
					boolean hasAccess = true;
					if (userId != null && userId.length() > 0) {
						// Need to check read access.
						hasAccess = hasAccess(fieldObject, userId, mcm, s);
					}
					
					if (hasAccess) {
						if (theMappedField.getUseLazyLoading())
							try {
								theMappedField.getSetter().invoke(newObject, getShell(fieldObject));
							} catch(Exception e) {
								e.printStackTrace();
							}
						else
							theMappedField.getSetter().invoke(newObject, cleanObject(fieldObject, s, inProcessObjects, userId));
					}
					*/
				} else if (fieldObject instanceof IPerceroObject) {
					handleFieldObject(object, newObject, mc, (MappedFieldPerceroObject) theMappedField, (IPerceroObject) fieldObject, userId, mcm, s, inProcessObjects, isClean);
					/**
					boolean hasAccess = true;
					if (userId != null && userId.length() > 0) {
						// Need to check read access.
						hasAccess = hasAccess(fieldObject, userId, mcm, s);
					}

					if (hasAccess) {
						if (s != null) {
							theMappedField.getSetter().invoke(newObject, getShell(fieldObject));
							//theMappedField.getSetter().invoke(newObject, s.load(fieldObject.getClass(), ((IPerceroObject) fieldObject).getID()));
						}
						else
							theMappedField.getSetter().invoke(newObject, fieldObject);
					}
					*/
				}
			}
			
			for(MappedField theMappedField : mc.propertyFields) {
				Object fieldObject = theMappedField.getGetter().invoke(object);
				if (fieldObject == null)
					theMappedField.getSetter().invoke(newObject, fieldObject);
				else if (fieldObject instanceof HibernateProxy) {
					handleFieldObject(object, newObject, mc, theMappedField, fieldObject, userId, mcm, s, inProcessObjects, isClean);
					/**
					boolean hasAccess = true;
					if (userId != null && userId.length() > 0) {
						// Need to check read access.
						hasAccess = hasAccess(fieldObject, userId, mcm, s);
					}

					if (hasAccess) {
						if (theMappedField.getUseLazyLoading())
							try {
								theMappedField.getSetter().invoke(newObject, getShell(fieldObject));
							} catch(Exception e) {
								e.printStackTrace();
							}
						else
							theMappedField.getSetter().invoke(newObject, cleanObject(fieldObject, s, inProcessObjects, userId));
					}
					*/
				} else {
					handleFieldObject(object, newObject, mc, theMappedField, fieldObject, userId, mcm, s, inProcessObjects, isClean);
					/**
					if (theMappedField.getUseLazyLoading())
						theMappedField.getSetter().invoke(newObject, fieldObject);
					else
						theMappedField.getSetter().invoke(newObject, cleanObject(fieldObject, s, inProcessObjects, userId));
					*/
				}
			}
			/**
			for(MappedField theMappedField : mc.allFields) {
				Object fieldObject = theMappedField.getGetter().invoke(object);
				if (fieldObject == null)
					theMappedField.getSetter().invoke(newObject, fieldObject);
				else if (fieldObject instanceof PersistentList) {
					try {
						PersistentList pList = (PersistentList) fieldObject;
						SessionImplementor session = pList.getSession();
						if (session != null && session.isOpen()) {
							List ar = new ArrayList();
							for (Object ob : pList) {
								if (theMappedField.getUseLazyLoading())
									ar.add(getShell(ob));
								else
									ar.add(cleanObject(ob, s, inProcessObjects));
							}
							theMappedField.getSetter().invoke(newObject, ar);
						} else {
							log.warn("Session is not open.  Unable to clean field.");
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else if (fieldObject instanceof PersistentBag) {
					try {
						PersistentBag pBag = (PersistentBag) fieldObject;
						SessionImplementor session = (SessionImplementor) s;
						if (s == null)
							session = pBag.getSession();
						if (session != null && session.isOpen()) {
							List ar = new ArrayList();
							for (Object ob : pBag) {
								if (theMappedField.getUseLazyLoading())
									ar.add(getShell(ob));
								else
									ar.add(cleanObject(ob, s, inProcessObjects));
							}
							theMappedField.getSetter().invoke(newObject, ar);
						} else {
							log.warn("Session is not open.  Unable to clean field.");
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else if (fieldObject instanceof PersistentSortedSet) {
					try {
						PersistentSortedSet pSortedSet = (PersistentSortedSet) fieldObject;
						SessionImplementor session = pSortedSet.getSession();
						if (session != null && session.isOpen()) {
							SortedSet ar = new TreeSet();
							for (Object ob : pSortedSet) {
								if (theMappedField.getUseLazyLoading())
									ar.add(getShell(ob));
								else
									ar.add(cleanObject(ob, s, inProcessObjects));
							}
							theMappedField.getSetter().invoke(newObject, ar);
						} else {
							log.warn("Session is not open.  Unable to clean field.");
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else if (fieldObject instanceof PersistentSet) {
					try {
						PersistentSet pSet = (PersistentSet) fieldObject;
						SessionImplementor session = pSet.getSession();
						if (session != null && session.isOpen()) {
							Set ar = new HashSet();
							for (Object ob : pSet) {
								if (theMappedField.getUseLazyLoading())
									ar.add(getShell(ob));
								else
									ar.add(cleanObject(ob, s, inProcessObjects));
							}
							theMappedField.getSetter().invoke(newObject, ar);
						} else {
							log.warn("Session is not open.  Unable to clean field.");
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else if (fieldObject instanceof PersistentMap) {
					try {
						PersistentMap pMap = (PersistentMap) fieldObject;
						SessionImplementor session = pMap.getSession();
						if (session != null && session.isOpen()) {
							Map hm = new HashMap();
							for (Object nextKey : pMap.keySet()) {
								try {
									if (theMappedField.getUseLazyLoading())
										hm.put(nextKey, getShell(pMap.get(nextKey)));
									else
										hm.put(nextKey, cleanObject(pMap.get(nextKey), s, inProcessObjects));
								} catch(Exception e) {
									e.printStackTrace();
								}
							}
							theMappedField.getSetter().invoke(newObject, hm);
						} else {
							log.warn("Session is not open.  Unable to clean field.");
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else if (fieldObject instanceof HibernateProxy) {
					if (theMappedField.getUseLazyLoading())
						try {
							theMappedField.getSetter().invoke(newObject, getShell(fieldObject));
						} catch(Exception e) {
							e.printStackTrace();
						}
					else
						theMappedField.getSetter().invoke(newObject, cleanObject(fieldObject, s, inProcessObjects));
				} else if (fieldObject instanceof IPerceroObject) {
					if (s != null)
						theMappedField.getSetter().invoke(newObject, s.load(fieldObject.getClass(), ((IPerceroObject) fieldObject).getID()));
					else
						theMappedField.getSetter().invoke(newObject, fieldObject);
				} else {
					if (theMappedField.getUseLazyLoading())
						theMappedField.getSetter().invoke(newObject, fieldObject);
					else
						theMappedField.getSetter().invoke(newObject, cleanObject(fieldObject, s, inProcessObjects));
				}
			}
			*/
			if (newObject instanceof BaseDataObject) {
				((BaseDataObject) newObject).setIsClean(true);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return newObject;
	}
	
	private static void handleFieldObject(Object theObject, Object newObject, MappedClass mc, MappedField theMappedField, Object theFieldObject, String userId, IMappedClassManager mcm, Session s, Map<Object, Object> inProcessObjects, boolean isClean) {
		try {
			if (theFieldObject == null) {
				theMappedField.getSetter().invoke(newObject, theFieldObject);
				return;
			}
			
			boolean isUser = StringUtils.hasText(userId);
			if (isClean) {
				// Object is clean.
				if (!theMappedField.getHasReadAccessRights()) {
					// Since the MappedField has no ReadAccessRights, we can do a straight copy here.
					theMappedField.getSetter().invoke(newObject, theFieldObject);
					return;
				}
				else if (!isUser) {
					// Since there is no User, we have no need to check AccessRights.
					theMappedField.getSetter().invoke(newObject, theFieldObject);
					return;
				}
			}

			if (theMappedField.getReadQuery() != null && StringUtils.hasText(theMappedField.getReadQuery().getQuery())) {
				if (hasFieldAccess(theObject, userId, theMappedField, s)) {
					//  Since the user has access to this entire field, no need to check each individual object.
					theMappedField.getSetter().invoke(newObject, theFieldObject);
					return;
				}
				else {
					// Since the user does NOT have access to this entire field, we return an empty collection.
					Object[] nullObject = new Object[1];
					nullObject[0] = null;
					theMappedField.getSetter().invoke(newObject, nullObject);
					return;
				}
			}
			
			if (theFieldObject instanceof IPerceroObject) {
				if (s != null && s.isOpen()) {
					if (!isUser || hasAccess(theFieldObject, userId, mcm, s)) {
						if (theMappedField.getUseLazyLoading())
							theMappedField.getSetter().invoke(newObject, getShell(theFieldObject));
						else
							theMappedField.getSetter().invoke(newObject, cleanObject(theFieldObject, s, inProcessObjects, userId));
					}
				} else {
					Object[] nullObject = new Object[1];
					nullObject[0] = null;
					theMappedField.getSetter().invoke(newObject, nullObject);
					log.warn("Session is not open.  Unable to clean field.");
				}
			}
			else {
				// Must be some primitive type.
				if (theMappedField.getUseLazyLoading())
					theMappedField.getSetter().invoke(newObject, theFieldObject);
				else
					theMappedField.getSetter().invoke(newObject, cleanObject(theFieldObject, s, inProcessObjects, userId));
			}
		} catch(Exception e) {
			log.error("Unable to clean field " + mc.className + ":" + theMappedField.getField().getName(), e);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void handleCollection(Object theObject, Object newObject, MappedClass mc, MappedField theMappedField, Collection theCollection, Collection newCollection, String userId, IMappedClassManager mcm, Session s, Map<Object, Object> inProcessObjects, boolean isClean) {
		try {
			boolean isUser = StringUtils.hasText(userId);
			if (isClean) {
				// Object is clean.
				if (!theMappedField.getHasReadAccessRights()) {
					// Since the MappedField has no ReadAccessRights, we can do a straight copy here.
					newCollection.addAll(theCollection);
					theMappedField.getSetter().invoke(newObject, newCollection);
					return;
				}
				else if (!isUser) {
					// Since there is no User, we have no need to check AccessRights.
					newCollection.addAll(theCollection);
					theMappedField.getSetter().invoke(newObject, newCollection);
					return;
				}
			}
			
			if (theMappedField.getReadQuery() != null && StringUtils.hasText(theMappedField.getReadQuery().getQuery())) {
				if (hasFieldAccess(theObject, userId, theMappedField, s)) {
					//  Since the user has access to this entire field, no need to check each individual object.
					//	However, since the object isn't clean, need to get clean version of each object.
					for (Object nextCollectionObject : theCollection) {
						// If there is no user, or the user has access...
						if (theMappedField.getUseLazyLoading())
							newCollection.add(getShell(nextCollectionObject));
						else
							newCollection.add(cleanObject(nextCollectionObject, s, inProcessObjects, userId));
					}
					theMappedField.getSetter().invoke(newObject, newCollection);
					return;
				}
				else {
					// Since the user does NOT have access to this entire field, we return an empty collection.
					theMappedField.getSetter().invoke(newObject, newCollection);
					return;
				}
			}
			
			if (s != null && s.isOpen()) {
				for (Object nextCollectionObject : theCollection) {
					// If there is no user, or the user has access...
					if (!isUser || hasAccess(nextCollectionObject, userId, mcm, s)) {
						if (theMappedField.getUseLazyLoading())
							newCollection.add(getShell(nextCollectionObject));
						else
							newCollection.add(cleanObject(nextCollectionObject, s, inProcessObjects, userId));
					}
				}
			} else {
				log.warn("Session is not open.  Unable to clean field.");
			}
			theMappedField.getSetter().invoke(newObject, newCollection);
		} catch(Exception e) {
			log.error("Unable to clean field " + mc.className + ":" + theMappedField.getField().getName(), e);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void handleMap(Object object, Object newObject, MappedClass mc, MappedField theMappedField, Map pMap, Map newMap, String userId, IMappedClassManager mcm, Session s, Map<Object, Object> inProcessObjects, boolean isClean) {
		try {
			boolean isUser = StringUtils.hasText(userId);
			if (isClean) {
				// Object is clean.
				if (!theMappedField.getHasReadAccessRights()) {
					// Since the MappedField has no ReadAccessRights, we can do a straight copy here.
					theMappedField.getSetter().invoke(newObject, pMap);
					return;
				}
				else if (!isUser) {
					// Since there is no User, we have no need to check AccessRights.
					theMappedField.getSetter().invoke(newObject, pMap);
					return;
				}
			}

			if (theMappedField.getReadQuery() != null && StringUtils.hasText(theMappedField.getReadQuery().getQuery())) {
				if (hasFieldAccess(object, userId, theMappedField, s)) {
					//  Since the user has access to this entire field, no need to check each individual object.
					theMappedField.getSetter().invoke(newObject, pMap);
					return;
				}
				else {
					// Since the user does NOT have access to this entire field, we return an empty collection.
					theMappedField.getSetter().invoke(newObject, newMap);
					return;
				}
			}
			
			if (s != null && s.isOpen()) {
				Iterator<Map.Entry> itrPMapEntries = pMap.entrySet().iterator();
				while (itrPMapEntries.hasNext()) {
					Map.Entry nextEntry = itrPMapEntries.next();
					Object ob = nextEntry.getValue();
					if (!isUser || hasAccess(ob, userId, mcm, s)) {
						Object nextKey = nextEntry.getKey();
						try {
							if (theMappedField.getUseLazyLoading())
								newMap.put(nextKey, getShell(ob));
							else
								newMap.put(nextKey, cleanObject(ob, s, inProcessObjects, userId));
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
//				for (Object nextKey : pMap.keySet()) {
//					Object ob = pMap.get(nextKey);
//					if (!isUser || hasAccess(ob, userId, mcm, s)) {
//						try {
//							if (theMappedField.getUseLazyLoading())
//								newMap.put(nextKey, getShell(ob));
//							else
//								newMap.put(nextKey, cleanObject(ob, s, inProcessObjects, userId));
//						} catch(Exception e) {
//							e.printStackTrace();
//						}
//					}
//				}
			} else {
				log.warn("Session is not open.  Unable to clean field.");
			}
			theMappedField.getSetter().invoke(newObject, newMap);
		} catch(Exception e) {
			log.error("Unable to clean field " + mc.className + ":" + theMappedField.getField().getName(), e);
		}
	}
	
	
	
	private static boolean hasAccess(Object ob, String userId, IMappedClassManager mcm, Session s) {
		boolean hasAccess = true;
		if (StringUtils.hasText(userId)) {
			try {
				ob = getShell(ob);
				MappedClass mappedClass = mcm.getMappedClassByClassName(ob.getClass().getCanonicalName());
				boolean isValidReadQuery = false;
				if (mappedClass != null) {
					if (mappedClass.getReadQuery() != null && StringUtils.hasText(mappedClass.getReadQuery().getQuery())) {
						isValidReadQuery = true;
					}
				}
				
				if (isValidReadQuery) {
					// There is a ReadQuery, so process that.
					Query readFilter = s.createQuery(mappedClass.getReadQuery().getQuery());
					mappedClass.getReadQuery().setQueryParameters(readFilter.getQueryString(), ob, userId);
					Number readFilterResult = (Number) readFilter.uniqueResult();
					if (readFilterResult == null || readFilterResult.intValue() <= 0)
						hasAccess = false;
				}
			} catch (Exception e) {
				hasAccess = false;
			}
		}
		
		return hasAccess;
	}
	
	
	private static boolean hasFieldAccess(Object ob, String userId, MappedField mappedField, Session s) {
		boolean hasAccess = true;
		if (StringUtils.hasText(userId)) {
			try {
				boolean isValidReadQuery = false;
				if (mappedField.getReadQuery() != null && StringUtils.hasText(mappedField.getReadQuery().getQuery()))
					isValidReadQuery = true;
				
				if (isValidReadQuery) {
					ob = getShell(ob);
					// There is a ReadQuery, so process that.
					Query readFilter = s.createQuery(mappedField.getReadQuery().getQuery());
					mappedField.getReadQuery().setQueryParameters(readFilter.getQueryString(), ob, userId);
					Number readFilterResult = (Number) readFilter.uniqueResult();
					if (readFilterResult == null || readFilterResult.intValue() <= 0)
						hasAccess = false;
				}
			} catch (Exception e) {
				hasAccess = false;
			}
		}
		
		return hasAccess;
	}
	
	public static Object getShell(Object object) {
		if (object instanceof HibernateProxy) {
			object = initHibernateProxy((HibernateProxy) object);
		}
		
		return object;
	}
	
	@SuppressWarnings("rawtypes")
	public static List<Field> getClassFields(Class theClass) {
		List<Field> fieldList = new ArrayList<Field>();
		Field[] theFields = theClass.getDeclaredFields();
		
		for(Field nextField : theFields) {
			boolean isStatic = Modifier.STATIC == (nextField.getModifiers() & Modifier.STATIC);
			if (!isStatic)
				fieldList.add(nextField);
		}
		
		if (theClass.getSuperclass() != null)
			fieldList.addAll(getClassFields(theClass.getSuperclass()));
		
		return fieldList;
	}

	@SuppressWarnings("rawtypes")
	public static Method getFieldGetters(Class theClass, Field theField) {
		Method theMethod = null;
		Method[] theMethods = theClass.getMethods();
		String theModifiedFieldName = theField.getName();
		if (theModifiedFieldName.indexOf("_") == 0)
			theModifiedFieldName = theModifiedFieldName.substring(1);
		
		for(Method nextMethod : theMethods) {
			if (nextMethod.getName().equalsIgnoreCase("get" + theModifiedFieldName)) {
				theMethod = nextMethod;
				break;
			}
		}
		
		return theMethod;
	}

	@SuppressWarnings("rawtypes")
	public static Method getFieldSetters(Class theClass, Field theField) {
		Method theMethod = null;
		Method[] theMethods = theClass.getMethods();
		String theModifiedFieldName = theField.getName();
		if (theModifiedFieldName.indexOf("_") == 0)
			theModifiedFieldName = theModifiedFieldName.substring(1);
		
		for(Method nextMethod : theMethods) {
			if (nextMethod.getName().equalsIgnoreCase("set" + theModifiedFieldName)) {
				theMethod = nextMethod;
				break;
			}
		}
		
		return theMethod;
	}
	
	@SuppressWarnings("rawtypes")
	public static Object getUniqueResult(Object anObject) {
		if (anObject instanceof List) {
			List listObject = (List) anObject;
			if (listObject.size() >= 1)
				return listObject.get(0);
			else
				return null;

		} else {
			return anObject;
		}
	}

}
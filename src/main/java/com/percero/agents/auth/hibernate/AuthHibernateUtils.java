package com.percero.agents.auth.hibernate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Entity;

import org.apache.log4j.Logger;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.internal.PersistentSortedSet;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;

public class AuthHibernateUtils {

	private static Logger log = Logger.getLogger(AuthHibernateUtils.class);

	public static Object initHibernateProxy(HibernateProxy hp) {
		log.debug("initHibernateProxy");
		return hp.getHibernateLazyInitializer().getImplementation();
	}
	
	public static Object loadObject(Object object) {
		if (object instanceof HibernateProxy)
			object = initHibernateProxy((HibernateProxy) object);
		
		return object;
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Object cleanObject(Object object) {
		if (object == null)
			return null;
		
		if (object instanceof List<?>) {
			List<Object> list = (List<Object>) object;
			for (int i = 0; i < list.size(); i++) {
				list.set(i, cleanObject(list.get(i)));
			}
			
			return list;
		}
		
		if (object instanceof Map<?, ?>) {
			Map<Object, Object> map = (Map<Object, Object>) object;
			Map<Object, Object> newMap = new HashMap<Object, Object>();
			Iterator<Map.Entry<Object, Object>> itrMapEntry = map.entrySet().iterator();
			while (itrMapEntry.hasNext()) {
				Map.Entry<Object, Object> nextMapEntry = itrMapEntry.next();
				Object key = nextMapEntry.getKey();
				Object value = nextMapEntry.getValue();
				newMap.put( key, cleanObject(value) );
			}
			
			return newMap;
		}
		
		Entity entity = object.getClass().getAnnotation(Entity.class);
		if (entity == null && !(object instanceof HibernateProxy))
			return object;
		else if (object instanceof HibernateProxy)
			object = ((HibernateProxy) object).getHibernateLazyInitializer().getImplementation();
		
		Class objectClass = HibernateProxyHelper.getClassWithoutInitializingProxy(object);
		Object newObject = null;
		try {
			newObject = objectClass.newInstance();
			List<Field> fieldList = getClassFields(objectClass);
			for(Field theField : fieldList) {
				if ((theField.getModifiers() & Modifier.STATIC) == Modifier.STATIC)
					continue;
				
				Method getterMethod = getFieldGetters(objectClass, theField);
				Method setterMethod = getFieldSetters(objectClass, theField);

				Object fieldObject = getterMethod.invoke(object);
				if (fieldObject == null) {
					setterMethod.invoke(newObject, (Object) null);
				}
				else if (fieldObject instanceof PersistentList) {
					try {
						org.hibernate.collection.internal.PersistentList pList = (org.hibernate.collection.internal.PersistentList) fieldObject;
						org.hibernate.engine.spi.SessionImplementor session = pList.getSession();
						if (session != null && session.isOpen()) {
							List ar = new ArrayList();
							for (Object ob : pList) {
								ar.add(ob);
							}
							setterMethod.invoke(newObject, ar);
						} else {
							System.out.println("Session is not open.  Unable to clean field.");
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else if (fieldObject instanceof PersistentBag) {
					try {
						org.hibernate.collection.internal.PersistentBag pBag = (org.hibernate.collection.internal.PersistentBag) fieldObject;
						org.hibernate.engine.spi.SessionImplementor session = pBag.getSession();
						if (session != null && session.isOpen()) {
							List ar = new ArrayList();
							for (Object ob : pBag) {
								ar.add(ob);
							}
							setterMethod.invoke(newObject, ar);
						} else {
							System.out.println("Session is not open.  Unable to clean field.");
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else if (fieldObject instanceof PersistentSortedSet) {
					try {
						org.hibernate.collection.internal.PersistentSortedSet pSortedSet = (org.hibernate.collection.internal.PersistentSortedSet) fieldObject;
						org.hibernate.engine.spi.SessionImplementor session = pSortedSet.getSession();
						if (session != null && session.isOpen()) {
							SortedSet ar = new TreeSet();
							for (Object ob : pSortedSet) {
								ar.add(ob);
							}
							setterMethod.invoke(newObject, ar);
						} else {
							System.out.println("Session is not open.  Unable to clean field.");
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else if (fieldObject instanceof PersistentSet) {
					try {
						org.hibernate.collection.internal.PersistentSet pSet = (org.hibernate.collection.internal.PersistentSet) fieldObject;
						org.hibernate.engine.spi.SessionImplementor session = pSet.getSession();
						if (session != null && session.isOpen()) {
							Set ar = new HashSet();
							for (Object ob : pSet) {
								ar.add(ob);
							}
							setterMethod.invoke(newObject, ar);
						} else {
							System.out.println("Session is not open.  Unable to clean field.");
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else if (fieldObject instanceof PersistentMap) {
					try {
						org.hibernate.collection.internal.PersistentMap pMap = (org.hibernate.collection.internal.PersistentMap) fieldObject;
						org.hibernate.engine.spi.SessionImplementor session = pMap.getSession();
						if (session != null && session.isOpen()) {
							Map hm = new HashMap();
							for (Object nextKey : pMap.keySet()) {
								try {
									hm.put(nextKey, pMap.get(nextKey));
								} catch(Exception e) {
									e.printStackTrace();
								}
							}
							setterMethod.invoke(newObject, hm);
						} else {
							System.out.println("Session is not open.  Unable to clean field.");
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else if (fieldObject instanceof HibernateProxy) {
					setterMethod.invoke(newObject, cleanObject(fieldObject));
				} else {
					setterMethod.invoke(newObject, loadObject(fieldObject));
				}
			}
		} catch(Exception e) {
			log.error(e);
		}
		
		return newObject;
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
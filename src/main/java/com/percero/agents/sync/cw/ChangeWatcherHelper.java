package com.percero.agents.sync.cw;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.mchange.v2.lang.ObjectUtils;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.hibernate.SyncHibernateUtils;
import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.services.IDataProvider;
import com.percero.agents.sync.services.IDataProviderManager;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.services.ISyncAgentService;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.IClassIDPair;
import com.percero.agents.sync.vo.PushCWUpdateResponse;
import com.percero.framework.vo.IPerceroObject;

@Component
public class ChangeWatcherHelper implements IChangeWatcherHelper {

	private static final Logger log = Logger.getLogger(ChangeWatcherHelper.class);

	@Autowired
	ObjectMapper objectMapper;
	
//	@Scheduled(fixedRate=10000)
//	public void printStats() {
//		log.debug(ChangeWatcherReporting.stringResults());
//	}

	@Autowired
	protected IAccessManager accessManager;
	public void setAccessManager(IAccessManager value) {
		accessManager = value;
	}

	@Autowired
	protected IDataProviderManager dataProviderManager;
	public void setDataProviderManager(IDataProviderManager value) {
		dataProviderManager = value;
	}
	public IDataProviderManager getDataProviderManager() {
		return dataProviderManager;
	}
	
	@Autowired
	protected ISyncAgentService syncAgentService;
	public void setSyncAgentService(ISyncAgentService value) {
		syncAgentService = value;
	}
	public ISyncAgentService getSyncAgentService() {
		return syncAgentService;
	}

	@Autowired
	protected IPushSyncHelper pushSyncHelper;
	public void setPushSyncHelper(IPushSyncHelper value) {
		pushSyncHelper = value;
	}
	

	public Object get(String fieldName, ClassIDPair classIdPair) {
		return get(fieldName, classIdPair, null, null);
	}

	public Object get(String fieldName, ClassIDPair classIdPair, String clientId) {
		return get(fieldName, classIdPair, null, clientId);
	}


	public Object get(String fieldName, ClassIDPair classIdPair, String[] params) {
		return get(fieldName, classIdPair, params, null);
	}
	
	public Object get(String fieldName, ClassIDPair classIdPair, String[] params, String clientId) {
		Object result = null;
		try {
			// Make sure this being requested by a valid person.
			if (StringUtils.hasText(clientId)) {
				String userId = accessManager.getClientUserId(clientId);
				if (!StringUtils.hasLength(userId)) {
					log.warn("Invalid clientId in get: " + fieldName);
					return result;
				}
			}
			
			// Check to see if this value has already been calculated.
			Boolean resultExists = accessManager.getChangeWatcherResultExists(classIdPair, fieldName, params);
			
			if (resultExists) {
//				Long resultTimestamp = accessManager.getChangeWatcherResultTimestamp(classIdPair, fieldName, params);
				result = accessManager.getChangeWatcherResult(classIdPair, fieldName, params);
			}
			else {
				// Result has not been calculated, so need to calculate.
				result = calculate(fieldName, classIdPair, params);
			}
			
			if (StringUtils.hasText(clientId)) {
				result = getForClient(fieldName, classIdPair, params, result, clientId);
				accessManager.addWatcherClient(classIdPair, fieldName, clientId, params);
			}
		} catch(Exception e) {
			log.error("Unable to get " + fieldName, e);
		}
		
		return result;
	}
	
	@SuppressWarnings("rawtypes")
	protected Object getForClient(String fieldName, ClassIDPair classIdPair, String[] params, Object result, String clientId) {
		
		String userId = accessManager.getClientUserId(clientId);

		if (result instanceof IPerceroObject) {
			if (validateReadAccess((BaseDataObject) result, userId))
				return result;
			else
				return null;
		}
		if (result instanceof IClassIDPair) {
			if (validateReadAccess((ClassIDPair) result, userId))
				return result;
			else
				return null;
		}
		else if (result instanceof Collection) {
			Iterator itr = ((Collection) result).iterator();
			while(itr.hasNext()) {
				Object next = itr.next();
				
				if (next instanceof IPerceroObject) {
					if (!validateReadAccess((BaseDataObject) next, userId)) {
						itr.remove();
					}
				}
				else if (next instanceof IClassIDPair) {
					if (!validateReadAccess((ClassIDPair) next, userId)) {
						itr.remove();
					}
				}
			}
			
			return result;
		}
		else {
			return result;
		}
	}
	
	protected Boolean validateReadAccess(BaseDataObject bdo, String userId) {
		return validateReadAccess(new ClassIDPair(bdo.getID(), bdo.getClass().getCanonicalName()), userId);
	}
	
	protected Boolean validateReadAccess(ClassIDPair classIdPair, String userId) {
		String className = classIdPair.getClassName();
		String id = classIdPair.getID();
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(className);
		Boolean hasAccess = true;
		
		if (mappedClass != null) {
			IDataProviderManager dataProviderManager = getDataProviderManager();
			
			IDataProvider dataProvider = dataProviderManager.getDataProviderByName(mappedClass.dataProviderName);
			hasAccess = dataProvider.getReadAccess(new ClassIDPair(id, className), userId);
		}
		
		if (hasAccess)
			return true;
		else
			return false;
	}
	
	public Object calculate(String fieldName, ClassIDPair classIdPair) {
		return null;
	}
	
	public Object calculate(String fieldName, ClassIDPair classIdPair, String[] params) {
		// This is really an error.
		StringBuilder strBuilder = new StringBuilder("No value calculate method found for: ").append(classIdPair.getClassName()).append(":").append(fieldName);
		for(String nextString : params) {
			strBuilder.append(".").append(nextString);
		}
		log.error(strBuilder.toString());
		return null;
	}
	
	/**protected void postCalculate(String fieldName, ClassIDPair classIdPair, Object result, Object oldValue) {
		postCalculate(fieldName, classIdPair, result, null);
	}*/
	private Map<String, ChangeWatcherResult> changeWatcherResults = new HashMap<String, ChangeWatcherResult>();
	protected void postCalculate(String fieldName, ClassIDPair classIdPair, String[] params, Object result, Object oldValue) {
		// Now run past the ChangeWatcher.
		try {
			String cwResultKey = fieldName + classIdPair.toString() + (params == null ? "" : params.toString());
			ChangeWatcherResult cwResult = changeWatcherResults.get(cwResultKey);
			if (cwResult == null) {
				cwResult = new ChangeWatcherResult(oldValue, result, eqOrBothNull(oldValue, result));
			}
			else {
				cwResult.setNewValue(result);
				cwResult.setEqOrBothNull(eqOrBothNull(oldValue, result));
			}

			// Check to see if this value has already been calculated.
			if (cwResult.getEqOrBothNull()) {
				ChangeWatcherReporting.unchangedResultsCounter++;
//				log.debug("Object has not changed, so not checking ChangeWatchers.");
			}
			else {
				ChangeWatcherReporting.changedResultsCounter++;
				String[] fieldNames = null;
				if (StringUtils.hasText(fieldName)) {
					fieldNames = new String[1];
					fieldNames[0] = fieldName;
				}
				accessManager.checkChangeWatchers(classIdPair, fieldNames, params);
			}
		} catch (Exception e) {
			log.error("Unable to chech change watchers for " + classIdPair.getID() + " / " + fieldName + " in " + getClass().getCanonicalName());
		}
	}
	
	
	public void recalculate(String fieldName, ClassIDPair classIdPair, Collection<String> clientIds, String[] params, Long requestTimestamp) {
		Object value = null;
		
		// Check to see if this value has already been calculated.
		Object currentValue = null;
		ChangeWatcherReporting.recalcsCounter++;
		try {
			if (requestTimestamp != null) {
				Long resultTimestamp = accessManager.getChangeWatcherResultTimestamp(classIdPair, fieldName, params);
				if (resultTimestamp != null && resultTimestamp.compareTo(requestTimestamp) > 0) {
					// If the result timestamp is after the request timestamp, then no need to recalculate.
					//	In essence, the recalc was already done, albeit at the behest of another process.
					ChangeWatcherReporting.abortedRecalcsCounter++;
//					log.debug("Aborting ChangeWatcherHelper.recalculate request since it has already been fulfilled.");
					return;
				}
			}
			currentValue = accessManager.getChangeWatcherResult(classIdPair, fieldName, params);
		} catch(Exception e) {}
		

		String cwResultKey = fieldName + classIdPair.toString() + (params == null ? "" : params.toString());
		ChangeWatcherResult cwResult = new ChangeWatcherResult(currentValue);
		changeWatcherResults.put(cwResultKey, cwResult);

		// TODO: Pass the ChangeWatcherResult to calculate so that we don't have to retrieve the current value twice.
		value = calculate(fieldName, classIdPair, params);
		changeWatcherResults.remove(cwResultKey);
		
		if (!cwResult.getEqOrBothNull()) {
//			ChangeWatcherReporting.unchangedResultsCounter++;
			log.debug("Object has changed, pushing update.");
			pushUpdate(classIdPair, fieldName, params, value, clientIds);
		}
		
		log.debug(ChangeWatcherReporting.stringResults());
	}

	protected void pushUpdate(ClassIDPair classIdPair, String fieldName, String[] params, Object update, Collection<String> clientIds) {
		// Now push the result to all interested clients.
		try {
			Iterator<String> itrClientIds = clientIds.iterator();
			while(itrClientIds.hasNext()) {
				String nextClient = itrClientIds.next();
				Object nextUpdate = getForClient(fieldName, classIdPair, params, update, nextClient);
				pushChangeWatcherUpdate(nextClient, classIdPair, fieldName, params, nextUpdate);
			}
		} catch(Exception e) {
			log.error("Unable to pushUpdate", e);
		}
	}


	public void pushChangeWatcherUpdate(Collection<Object> clientIds, ClassIDPair classIdPair, String fieldName, String[] params, Object value) {
		if (clientIds != null && clientIds.size() > 0) {
			try {
				PushCWUpdateResponse pushUpdateResponse = new PushCWUpdateResponse();
				pushUpdateResponse.setFieldName(fieldName);
				pushUpdateResponse.setParams(params);
				pushUpdateResponse.setClassIdPair(classIdPair);

				for(Object nextClient : clientIds) {
					
					String clientId = (String) nextClient;
					if (!StringUtils.hasLength(clientId))
						continue;
					
					//if (value instanceof BaseDataObject) {
						String userId = accessManager.getClientUserId(clientId);
						// Clean the value if it is a framework object.
						value = SyncHibernateUtils.cleanObject(value, null, userId);
					//}
					pushUpdateResponse.setValue(value);
					pushUpdateResponse.setClientId(clientId);
					
					pushSyncHelper.pushSyncResponseToClient(pushUpdateResponse, clientId);
				}
				
			} catch(Exception e) {
				log.error("Error sending ChangeWatcher Updates to clients", e);
			}
		}
	}
	public void pushChangeWatcherUpdate(String clientId, ClassIDPair classIdPair, String fieldName, String[] params, Object value) {
		if (StringUtils.hasText(clientId)) {
			try {
				PushCWUpdateResponse pushUpdateResponse = new PushCWUpdateResponse();
				pushUpdateResponse.setFieldName(fieldName);
				pushUpdateResponse.setParams(params);
				pushUpdateResponse.setClassIdPair(classIdPair);
				
				if (value instanceof IPerceroObject) {
					String userId = accessManager.getClientUserId(clientId);
					// Clean the value if it is a framework object.
					value = (BaseDataObject) SyncHibernateUtils.cleanObject(value, null, userId);
				}
				pushUpdateResponse.setValue(value);
				
				pushUpdateResponse.setClientId(clientId);
				
				pushSyncHelper.pushSyncResponseToClient(pushUpdateResponse, clientId);
				
			} catch(Exception e) {
				log.error("Error sending ChangeWatcher Updates to clients", e);
			}
		}
	}
	
	
	/****************************
	 * HELPER FUNCTIONS
	 ****************************/
	public DateTime parseDateTime(String theDate) {
		String testDate = theDate.replace('_', ' ');
		DateTime dateTime = null;
		if (dateTime == null) {
			try {
				dateTime = new DateTime(Long.parseLong(theDate));
			} catch(IllegalArgumentException e) {
				// Possibly the wrong format date, try a different format.
			}
		}
		
		//testDate = "2013-10-01 +07:00";
		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss ZZ");
		if (dateTime == null) {
			try {
				dateTime = formatter.withOffsetParsed().parseDateTime(testDate);
			} catch(IllegalArgumentException iae) {
				// Possibly the wrong format date, try a different format.
			}
		}
		if (dateTime == null) {
			try {
				formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z");
				dateTime = formatter.withOffsetParsed().parseDateTime(testDate);
			} catch(IllegalArgumentException iae) {
				// Possibly the wrong format date, try a different format.
			}
		}
		if (dateTime == null) {
			try {
				formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss ZZZ");
				dateTime = formatter.withOffsetParsed().parseDateTime(testDate);
			} catch(IllegalArgumentException iae) {
				// Possibly the wrong format date, try a different format.
			}
		}
		if (dateTime == null) {
			try {
				formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
				dateTime = formatter.parseDateTime(testDate);
			} catch(IllegalArgumentException iae) {
				// Possibly the wrong format date, try a different format.
			}
		}
		if (dateTime == null) {
			try {
				formatter = DateTimeFormat.forPattern("yyyy-MM-dd ZZZ");
				dateTime = formatter.withOffsetParsed().parseDateTime(testDate);
			} catch(IllegalArgumentException iae) {
				// Possibly the wrong format date, try a different format.
			}
		}
		if (dateTime == null) {
			try {
				formatter = DateTimeFormat.forPattern("yyyy-MM-dd ZZ");
				dateTime = formatter.withOffsetParsed().parseDateTime(testDate);
			} catch(IllegalArgumentException iae) {
				// Possibly the wrong format date, try a different format.
			}
		}
		if (dateTime == null) {
			try {
				formatter = DateTimeFormat.forPattern("yyyy-MM-dd Z");
				dateTime = formatter.withOffsetParsed().parseDateTime(testDate);
			} catch(IllegalArgumentException iae) {
				// Possibly the wrong format date, try a different format.
			}
		}
		if (dateTime == null) {
			try {
				formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
				dateTime = formatter.parseDateTime(testDate);
			} catch(IllegalArgumentException iae) {
				// Possibly the wrong format date, try a different format.
			}
		}
		
		return dateTime;
	}
	
	@SuppressWarnings("unchecked")
	public static boolean eqOrBothNull(Object resultA, Object resultB) {
		if (resultA == null && resultB == null) {
			return true;
		}
		else if (resultA == null || resultB == null) {
			return false;
		}
		else {
			Class<? extends Object> classA = resultA.getClass();
			Class<? extends Object> classB = resultB.getClass();
			
			if (classA != classB) {
				return false;
			}
			else if (List.class.isAssignableFrom(classA)) {
				// This is a List of objects.
				List<? extends Object> listA = (List<? extends Object>) resultA;
				List<? extends Object> listB = (List<? extends Object>) resultB;
				
				// If the Lists are different sizes, then they are different.
				if (listA.size() != listB.size()) {
					return false;
				}
				// Else if the Lists have 0 size, then they are the same.
				else if (listA.size() == 0) {
					return true;
				}

				// Lists are ordered, so if elements are out of order then they are different.
				for(int i=0; i< listA.size(); i++) {
					Object nextA = listA.get(i);
					Object nextB = listB.get(i);

					Class<? extends Object> nextClassA = nextA.getClass();
					
					Class<? extends Object> nextClassB = nextB.getClass();
					if (nextClassA != nextClassB) {
						return false;
					}
					else if (ClassIDPair.class.isAssignableFrom(nextClassA)) {
						// ClassIDPair objects.
						ClassIDPair pairA = (ClassIDPair) nextA;
						ClassIDPair pairB = (ClassIDPair) nextB;
						
						if (!pairA.equals(pairB)) {
							return false;
						}
					}
					else if (IPerceroObject.class.isAssignableFrom(nextClassA)) {
						// Is some sort of Percero Object.
						IPerceroObject poA = (IPerceroObject) nextA;
						IPerceroObject poB = (IPerceroObject) nextB;
						
						if (poA.getID() == null && poB.getID() == null) {
							continue;
						}
						else if (poA.getID() == null || poB.getID() == null) {
							return false;
						}
						else {
							if (!poA.getID().equals(poB.getID())) {
								return false;
							}
						}
					}
					else {
						// Some sort of other base/primitive type.
						if (!ObjectUtils.eqOrBothNull(nextA, nextB)) {
							return false;
						}
					}
				}
				
				// At this point, the sizes of the List are the same 
				//	and every item in A exists in B, therefore every item 
				//	in B exists in A.  Also, the order is the same.
				return true;
			}
			else if (Collection.class.isAssignableFrom(classA)) {
				// This is a collection of objects.
				Collection<? extends Object> collA = (Collection<? extends Object>) resultA;
				Collection<? extends Object> collB = (Collection<? extends Object>) resultB;
				
				// If the collections are different sizes, then they are different.
				if (collA.size() != collB.size()) {
					return false;
				}
				// Else if the collections have 0 size, then they are the same.
				else if (collA.size() == 0) {
					return true;
				}
				
				Iterator<? extends Object> itrA = collA.iterator();
				Iterator<? extends Object> itrB = null;
				while (itrA.hasNext()) {
					Object nextA = itrA.next();
					Class<? extends Object> nextClassA = nextA.getClass();
					boolean nextAExistsInB = false;
					
					itrB = collB.iterator();
					while (itrB.hasNext()) {
						Object nextB = itrB.next();
						
						Class<? extends Object> nextClassB = nextB.getClass();
						if (nextClassA != nextClassB) {
							continue;
						}
						else if (ClassIDPair.class.isAssignableFrom(nextClassA)) {
							// ClassIDPair objects.
							ClassIDPair pairA = (ClassIDPair) nextA;
							ClassIDPair pairB = (ClassIDPair) nextB;
							
							if (pairA.equals(pairB)) {
								nextAExistsInB = true;
								break;
							}
						}
						else if (IPerceroObject.class.isAssignableFrom(nextClassA)) {
							// Is some sort of Percero Object.
							IPerceroObject poA = (IPerceroObject) nextA;
							IPerceroObject poB = (IPerceroObject) nextB;
							
							if (poA.getID() == null && poB.getID() == null) {
								nextAExistsInB = true;
								break;
							}
							else if (poA.getID() == null || poB.getID() == null) {
								continue;
							}
							else {
								if (poA.getID().equals(poB.getID())) {
									nextAExistsInB = true;
									break;
								}
							}
						}
						else {
							// Some sort of other base/primitive type.
							if (ObjectUtils.eqOrBothNull(nextA, nextB)) {
								nextAExistsInB = true;
								break;
							}
						}
					}
					
					if (!nextAExistsInB) {
						// nextA does not exist in B, so these are different.
						return false;
					}
				}
				
				// At this point, the sizes of the collections are the same 
				//	and every item in A exists in B, therefore every item 
				//	in B exists in A.
				return true;
			}
			else if (ClassIDPair.class.isAssignableFrom(classA)) {
				// ClassIDPair objects.
				ClassIDPair pairA = (ClassIDPair) resultA;
				ClassIDPair pairB = (ClassIDPair) resultB;
				
				return pairA.equals(pairB);
			}
			else if (IPerceroObject.class.isAssignableFrom(classA)) {
				// Is some sort of Percero Object.
				IPerceroObject poA = (IPerceroObject) resultA;
				IPerceroObject poB = (IPerceroObject) resultB;
				
				if (poA.getID() == null && poB.getID() == null) {
					return true;
				}
				else if (poA.getID() == null || poB.getID() == null) {
					return false;
				}
				else {
					return poA.getID().equals(poB.getID());
				}
			}
			else {
				// Some sort of other base/primitive type.
				return ObjectUtils.eqOrBothNull(resultA, resultB);
			}
		}
	}
	
	/**
	 * @param theObject
	 * @param results
	 * @return	TRUE if theObject was added to results. FALSE if results already contains theObject (by ID).
	 */
	public static Boolean addResultIfNotExists(BaseDataObject theObject, List<IPerceroObject> results) {
		Boolean objExists = false;
		Iterator<? extends IPerceroObject> itrResults = results.iterator();
		while (itrResults.hasNext()) {
			IPerceroObject nextResult = itrResults.next();
			if (nextResult.getID().equalsIgnoreCase(theObject.getID())) {
				objExists = true;
				break;
			}
		}
		
		if (!objExists) {
			results.add(theObject);
			return true;
		}
		else {
			return false;
		}
	}
}

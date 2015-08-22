package com.percero.agents.sync.cw;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
public class DerivedValueChangeWatcherHelper extends ChangeWatcherHelper implements IChangeWatcherValueHelper {

	private static final Logger log = Logger.getLogger(ChangeWatcherHelper.class);
	
//	@Autowired
//	ObjectMapper objectMapper;
	
	@Autowired
	protected IPushSyncHelper pushSyncHelper;
	public void setPushSyncHelper(IPushSyncHelper value) {
		pushSyncHelper = value;
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


	public Object calculate(String fieldName, ClassIDPair classIdPair) {
		// This is really an error.
		StringBuilder strBuilder = new StringBuilder("No value calculate method found for: ").append(classIdPair.getClassName()).append(":").append(fieldName);
		log.error(strBuilder.toString());
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
	
	@Override
	public void process(String category, String subCategory, String fieldName) {
		calculate(fieldName, new ClassIDPair(subCategory, category));
	}

	@Override
	public void process(String category, String subCategory, String fieldName, String[] params) {
		calculate(fieldName, new ClassIDPair(subCategory, category), params);
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

	@Override
	public void reprocess(String category, String subCategory, String fieldName, Collection<String> clientIds, String[] params, Long requestTimestamp) {
		ChangeWatcherReporting.reprocessCounter++;
		this.recalculate(fieldName, new ClassIDPair(subCategory, category), clientIds, params, requestTimestamp);
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
	
}

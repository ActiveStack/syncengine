package com.percero.agents.sync.helpers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.metadata.MappedClass.MappedClassMethodPair;
import com.percero.agents.sync.services.ISyncAgentService;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ServerResponse;
import com.percero.framework.vo.IPerceroObject;

//@Component
public class ProcessHelper {

	private static final Logger log = Logger.getLogger(ProcessHelper.class);
	
	public ProcessHelper() {
		ProcessHelper.PROCESS_HELPER = this;
	}
	
	private static ProcessHelper PROCESS_HELPER = null;
	
	@Autowired
	protected IAccessManager accessManager;
	public void setAccessManager(IAccessManager value) {
		accessManager = value;
	}

	@Autowired
	protected SessionFactory appSessionFactory;
	public void setAppSessionFactory(SessionFactory value) {
		appSessionFactory = value;
	}
	
	@Autowired
	protected ISyncAgentService syncAgentService;
	public void setSyncAgentService(ISyncAgentService value) {
		syncAgentService = value;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static final void mergeObjects(IPerceroObject fromObject, IPerceroObject toObject, List<MappedClassMethodPair> listSetters, String userId, String clientId) throws SyncException {
		String mergeFault = null;
		Map<Method, List> mergedObjectsMap = new HashMap<Method, List>();
//		List<MappedClassMethodPair> listSetters = new ArrayList<MappedClassMethodPair>();
		Iterator<MappedClassMethodPair> itrSetters = listSetters.iterator();
		
		while(itrSetters.hasNext()) {
			MappedClassMethodPair nextItem = itrSetters.next();
			Method theSetter = nextItem.method;

			Class nextClazz = nextItem.mappedClass.clazz;
			IPerceroObject nextSampleObject = null;
			try {
				nextSampleObject = (IPerceroObject) nextClazz.newInstance();
				theSetter.invoke(nextSampleObject, fromObject);
			} catch (InstantiationException e) {
				log.error("Unable to get sample object for merge Object: " + theSetter.toGenericString(), e);
				continue;
			} catch (IllegalAccessException e) {
				log.error("Unable to get sample object for merge Object: " + theSetter.toGenericString(), e);
				continue;
			} catch (IllegalArgumentException e) {
				log.error("Unable to get sample object for merge Object: " + theSetter.toGenericString(), e);
				continue;
			} catch (InvocationTargetException e) {
				log.error("Unable to get sample object for merge Object: " + theSetter.toGenericString(), e);
				continue;
			}

			List listFoundObjects = PROCESS_HELPER.syncAgentService.systemFindByExample(nextSampleObject, null);
			if (listFoundObjects != null && !listFoundObjects.isEmpty()) {
				List<IPerceroObject> mergedObjects = new ArrayList<IPerceroObject>();
				mergedObjectsMap.put(theSetter, mergedObjects);
				Iterator<IPerceroObject> itrFoundObjects = listFoundObjects.iterator();
				while (itrFoundObjects.hasNext()) {
					IPerceroObject nextFoundObject = PROCESS_HELPER.syncAgentService.systemGetByObject(itrFoundObjects.next());
					if (nextFoundObject != null) {
						try {
							theSetter.invoke(nextFoundObject, toObject);
						} catch (IllegalArgumentException e) {
							log.error("Unable to set field for merge Object: " + theSetter.toGenericString(), e);
							continue;
						} catch (IllegalAccessException e) {
							log.error("Unable to set field for merge Object: " + theSetter.toGenericString(), e);
							continue;
						} catch (InvocationTargetException e) {
							log.error("Unable to set field for merge Object: " + theSetter.toGenericString(), e);
							continue;
						}

						if (!PROCESS_HELPER.syncAgentService.systemPutObject(nextFoundObject, null, null, userId, true)) {
							log.warn("mergeObjects: Unable to merge " + nextClazz.getName());
							mergeFault = "Unable to merge " + nextClazz.getName();
							break;
						}
						else {
							mergedObjects.add(nextFoundObject);
						}
					}
				}
			}
		}
		
		if (mergeFault == null) {
			try {
				ServerResponse deleteResponse = PROCESS_HELPER.syncAgentService.deleteObject(BaseDataObject.toClassIdPair(fromObject), clientId, true);
				if (deleteResponse != null) {
					if (!deleteResponse.getIsSuccessful()) {
						throw new SyncException("Unable to remove from object", -111);
					}
				}
				else {
					throw new SyncException("User does not have rights to remove Object", -101);
				}
			} catch (Exception e) {
				log.error("Unable to remove ProductType", e);
				throw new SyncException("Error removing from data source: " + e.getMessage(), -102);
			}
		}
		else {
			// There was a merge fault somewhere along the line, so we need to revert all the changes...
			Iterator<Method> itrMergedSetters = mergedObjectsMap.keySet().iterator();
			while (itrMergedSetters.hasNext()) {
				Method nextSetter = itrMergedSetters.next();
				
				List mergedObjects = mergedObjectsMap.get(nextSetter);
				Iterator<IPerceroObject> itrMergedObjects = mergedObjects.iterator();
				while (itrMergedObjects.hasNext()) {
					IPerceroObject nextMergedObject = itrMergedObjects.next();
					try {
						nextSetter.invoke(nextMergedObject, fromObject);
						PROCESS_HELPER.syncAgentService.systemPutObject(nextMergedObject, null, null, userId, true);
					} catch (IllegalArgumentException e) {
						log.error("Unable to revert merge object", e);
					} catch (IllegalAccessException e) {
						log.error("Unable to revert merge object", e);
					} catch (InvocationTargetException e) {
						log.error("Unable to revert merge object", e);
					}
				}
			}
			
			throw new SyncException("Unable to merge Objects", -103);
		}
	}
}

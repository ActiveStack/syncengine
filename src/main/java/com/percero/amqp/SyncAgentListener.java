package com.percero.amqp;

import com.percero.agents.auth.helpers.IAccountHelper;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.services.ISyncAgentService;
import com.percero.agents.sync.vo.*;
import com.percero.framework.accessor.IAccessor;
import com.percero.framework.accessor.IAccessorService;
import com.percero.framework.vo.IPerceroObject;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;

/**
 * This class supplies the main method that creates the spring context
 * and then all processing is invoked asynchronously by messaging.
 * 
 * This class' onMessage function will be invoked when the process receives a 'test' message from the broker
 * @author Jonathan Samples
 *
 */
//@Component("syncAgentListener")
public class SyncAgentListener implements MessageListener{

	@Autowired
	AmqpTemplate template;
	@Autowired
	AmqpAdmin amqpAdmin;
	@Autowired
	IDecoder decoder;
	@Autowired
	ISyncAgentService syncAgentService;
	private Boolean manifestProcessed = true;	// Turned off for now.
	@Autowired
	IAccessorService accessorService;
	@Autowired
	JsonMessageConverter jsonMessageConverter;
	@Autowired
	ObjectMapper safeObjectMapper;
	@Autowired
	IAccessManager accessManager;
	@Autowired
	IAccountHelper accountHelper;
	@Autowired
	IPushSyncHelper pushSyncHelper;
	public void setPushSyncHelper(IPushSyncHelper value) {
		pushSyncHelper = value;
	}

	
	public static final String CONNECT = "connect";
	public static final String HIBERNATE = "hibernate";
	public static final String UPGRADE_CLIENT = "upgradeClient";
	public static final String LOGOUT = "logout";
	public static final String DISCONNECT = "disconnect";
	public static final String FIND_BY_ID = "findById";
	public static final String FIND_BY_IDS = "findByIds";
	public static final String FIND_BY_EXAMPLE = "findByExample";
	public static final String CREATE_OBJECT = "createObject";
	public static final String PROCESS_TRANSACTION = "processTransaction";
	public static final String UPDATES_RECEIVED = "updatesReceived";
	public static final String DELETES_RECEIVED = "deletesReceived";
	public static final String PUT = "putObject";
	public static final String REMOVE = "removeObject";
	public static final String GET_ALL_BY_NAME = "getAllByName";
	public static final String RUN_QUERY = "runQuery";
	public static final String RUN_PROCESS = "runProcess";
	public static final String GET_ACCESSOR = "getAccessor";
	public static final String GET_HISTORY = "getHistory";
	public static final String GET_CHANGE_WATCHER = "getChangeWatcher";

	private static Logger logger = Logger.getLogger("com.percero");

	/**
	 * Message handling function
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	public void onMessage(Message message) {	
		Object result = "";
		SyncRequest request = null;
		SyncResponse response = null;
		String key = "";
		try{
			if (!manifestProcessed) {
				syncAgentService.processManifest();
				manifestProcessed = true;
			}
			
			Object ob = decoder.decode(message.getBody());
			if (ob instanceof SyncRequest)
				request = (SyncRequest) ob;
			
			key = message.getMessageProperties().getReceivedRoutingKey();
			
			logger.debug("Received message " + key);
			
			if(CONNECT.equals(key)){
				if (request instanceof ConnectRequest) {
					// Need to check for existing client here, before authenticateOAuth because authenticateOAuth will create
					//	the UserDevice client record if it does not exist.
					ConnectRequest connectRequest = (ConnectRequest) request;
					Set<String> existingClientIds = accessManager.findClientByUserIdDeviceId(connectRequest.getUserId(), connectRequest.getDeviceId());
					Principal pUser = accountHelper.authenticateOAuth(connectRequest.getRegAppKey(), connectRequest.getSvcOauthKey(), connectRequest.getUserId(), connectRequest.getToken(), connectRequest.getClientId(), connectRequest.getClientType(), connectRequest.getDeviceId());
					ConnectResponse connectResponse = new ConnectResponse();
					connectResponse.setCurrentTimestamp( (new Date()).getTime() );
					
					if (pUser != null) {
						final Collection<String> updateJournals = accessManager.getClientUpdateJournals(connectRequest.getClientId(), true);
						final Collection<String> deleteJournals = accessManager.getClientDeleteJournals(connectRequest.getClientId(), true);

						// The client will only have UpdateJournals and DeleteJournals if the UserDevice exists.
						accessManager.registerClient(connectRequest.getClientId(), connectRequest.getUserId(), connectRequest.getDeviceId());
						connectResponse.setClientId(connectRequest.getClientId());
						connectResponse.setDataID(syncAgentService.getDataId(connectRequest.getClientId()));
						
						// Check to see if UserDevice exists.
						if (existingClientIds != null && !existingClientIds.isEmpty()) {
							Iterator<String> itrExistingClientIds = existingClientIds.iterator();
							while (itrExistingClientIds.hasNext()) {
								String nextExistingClientId = itrExistingClientIds.next();
								connectResponse.setData(nextExistingClientId);
	
								// Push all UpdateJournals for this Client.
								updateJournals.addAll( accessManager.getClientUpdateJournals(nextExistingClientId, true) );
								//pushSyncHelper.pushUpdateJournals(updateJournals);
								
								// Push all DeleteJournals for this Client.
								deleteJournals.addAll( accessManager.getClientDeleteJournals(nextExistingClientId, true) );
								//pushSyncHelper.pushDeleteJournals(deleteJournals);
							}
						}
						else {
							connectResponse.setData(null);
						}
						
						syncAgentService.pushClientUpdateJournals(connectRequest.getClientId(), updateJournals);
						syncAgentService.pushClientDeleteJournals(connectRequest.getClientId(), deleteJournals);
					} else {
						logger.warn("Unable to get valid Principal User");
					}

					response = connectResponse;
				}
			}
			else if(HIBERNATE.equals(key)){
				if (request instanceof HibernateRequest) {
					HibernateRequest hibernateRequest = (HibernateRequest) request;
					HibernateResponse hibernateResponse = new HibernateResponse();
					hibernateResponse.setResult(accessManager.hibernateClient(hibernateRequest.getClientId(), hibernateRequest.getUserId()));
					response = hibernateResponse;
					//amqpAdmin.purgeQueue(hibernateRequest.getClientId(), true);
					//boolean deleteResult = amqpAdmin.deleteQueue(hibernateRequest.getClientId());
					//System.out.println("Deleted Queue " + hibernateRequest.getClientId() + ": " + deleteResult);
				}
			}
			else if(UPGRADE_CLIENT.equals(key)){
				if (request instanceof UpgradeClientRequest) {
					UpgradeClientRequest upgradeClientRequest = (UpgradeClientRequest) request;
					UpgradeClientResponse upgradeClientResponse = new UpgradeClientResponse();
					upgradeClientResponse.setResult(accessManager.upgradeClient(upgradeClientRequest.getClientId(), upgradeClientRequest.getDeviceId(), upgradeClientRequest.getDeviceType(), upgradeClientRequest.getUserId()));
					response = upgradeClientResponse;
					
					if (upgradeClientResponse.getResult()) {
						// If upgrade successful, then check for existing updates and push to client.
						accessManager.registerClient(upgradeClientRequest.getClientId(), upgradeClientRequest.getUserId(), upgradeClientRequest.getDeviceId());
						upgradeClientResponse.setClientId(upgradeClientRequest.getClientId());

						// Push all UpdateJournals for this Client.
						final Collection<String> updateJournals = accessManager.getClientUpdateJournals(upgradeClientRequest.getClientId(), true);
						//pushSyncHelper.pushUpdateJournals(updateJournals);
						syncAgentService.pushClientUpdateJournals(upgradeClientRequest.getClientId(), updateJournals);
						
						// Push all DeleteJournals for this Client.
						final Collection<String> deleteJournals = accessManager.getClientDeleteJournals(upgradeClientRequest.getClientId(), true);
						//pushSyncHelper.pushDeleteJournals(deleteJournals);
						syncAgentService.pushClientDeleteJournals(upgradeClientRequest.getClientId(), deleteJournals);
					}
					else
					{
						logger.warn("Unable to upgrade client");
					}
				}
			}
			else if(DISCONNECT.equals(key)){
				if (request instanceof DisconnectRequest) {
					DisconnectRequest disconnectRequest = (DisconnectRequest) request;
					if (accessManager.isNonPersistentClient(disconnectRequest.getClientId())) {
						// Need to delete the queue that this client is connected to since it is a non-persistent client.
						MessageProperties mp = message.getMessageProperties();
						//amqpAdmin.deleteQueue(mp.getReplyTo());
					}
					else {
						logger.debug("Leaving MessageQueue intact since client is NOT a non-persistent client.");
					}
					accessManager.logoutClient(disconnectRequest.getClientId(), false);
					response = null;
				}
			}
			else if(LOGOUT.equals(key)){
				if (request instanceof LogoutRequest) {
					LogoutRequest logoutRequest = (LogoutRequest) request;
					if (accessManager.isNonPersistentClient(logoutRequest.getClientId())) {
						// Need to delete the queue that this client is connected to since it is a non-persistent client.
						MessageProperties mp = message.getMessageProperties();
						//amqpAdmin.deleteQueue(mp.getReplyTo());
					}
					else {
						logger.debug("Leaving MessageQueue intact since client is NOT a non-persistent client.");
					}
					accessManager.logoutClient(logoutRequest.getClientId(), logoutRequest.getPleaseDestroyClient());
					response = null;
				}
			}
			else if(FIND_BY_IDS.equals(key)){
				if (request instanceof FindByIdsRequest) {
					FindByIdsRequest findByIdsRequest = (FindByIdsRequest) request;
					result = syncAgentService.findByIds(findByIdsRequest.getTheClassIdList(), findByIdsRequest.getClientId());
					response = new FindByIdsResponse();
					((FindByIdsResponse)response).setResult((List<BaseDataObject>)result);
				}
			}
			else if(FIND_BY_ID.equals(key)){
				if (request instanceof FindByIdRequest) {
					FindByIdRequest findByIdRequest = (FindByIdRequest) request;
					result = syncAgentService.findById(findByIdRequest.getTheClassName(), findByIdRequest.getTheClassId(), findByIdRequest.getClientId());
					response = new FindByIdResponse();
					((FindByIdResponse)response).setResult((BaseDataObject)result);
				}
			}
			else if(FIND_BY_EXAMPLE.equals(key)){
				if (request instanceof FindByExampleRequest) {
					FindByExampleRequest findByExampleRequest = (FindByExampleRequest) request;
					result = syncAgentService.findByExample(findByExampleRequest.getTheObject(), null, findByExampleRequest.getClientId());
					response = new FindByExampleResponse();
					((FindByExampleResponse)response).setResult((List<BaseDataObject>)result);
				}
			}
			else if(CREATE_OBJECT.equals(key)){
				if (request instanceof CreateRequest) {
					CreateRequest createRequest = (CreateRequest) request;
					ServerResponse createServerResponse = syncAgentService.createObject((IPerceroObject) createRequest.getTheObject(), createRequest.getClientId());
					response = new CreateResponse();
					((CreateResponse)response).setResult(createServerResponse.getIsSuccessful());
					((CreateResponse)response).setTheObject((BaseDataObject) createServerResponse.getResultObject());
				}
			}
			else if(PROCESS_TRANSACTION.equals(key)){
				if (request instanceof TransactionRequest) {
					TransactionRequest processTransactionRequest = (TransactionRequest) request;
					List<IPerceroObject> objectsToSave = new ArrayList<IPerceroObject>();
					objectsToSave.addAll(processTransactionRequest.getObjectsToSave());
					List<IPerceroObject> objectsToRemove = new ArrayList<IPerceroObject>();
					objectsToRemove.addAll(processTransactionRequest.getObjectsToRemove());
					
					if (processTransactionRequest.getTransTimestamp() == null || processTransactionRequest.getTransTimestamp() <= 0)
						processTransactionRequest.setTransTimestamp((new Date()).getTime());
					
					ServerResponse processTransServerResponse = syncAgentService.processTransaction(objectsToSave, objectsToRemove, processTransactionRequest.getTransactionId(), new Date(processTransactionRequest.getTransTimestamp()), processTransactionRequest.getClientId());
					response = new TransactionResponse();
					((TransactionResponse)response).setTransactionId(processTransactionRequest.getTransactionId());
					((TransactionResponse)response).setResult(processTransServerResponse.getIsSuccessful());
					
					// Add lists of saved and removed objects.
					//	If Transaction failed, then this provides a list of objects to rollback.
					((TransactionResponse)response).setObjectsSaved(new ArrayList<ClassIDPair>());
					for(IPerceroObject nextSavedBDO : processTransactionRequest.getObjectsToSave()) {
						ClassIDPair nextSavePair = new ClassIDPair(nextSavedBDO.getID(), nextSavedBDO.getClass().getName());
						((TransactionResponse)response).getObjectsSaved().add(nextSavePair);
					}

					((TransactionResponse)response).setObjectsRemoved(new ArrayList<ClassIDPair>());
					for(IPerceroObject nextRemovedBDO : processTransactionRequest.getObjectsToRemove()) {
						ClassIDPair nextRemovePair = new ClassIDPair(nextRemovedBDO.getID(), nextRemovedBDO.getClass().getName());
						((TransactionResponse)response).getObjectsRemoved().add(nextRemovePair);
					}
				}
			}
			else if(UPDATES_RECEIVED.equals(key)){
				if (request instanceof PushUpdatesReceivedRequest) {
					PushUpdatesReceivedRequest pushUpdatesReceivedRequest = (PushUpdatesReceivedRequest) request;
					syncAgentService.updatesReceived(pushUpdatesReceivedRequest.getClassIdPairs(), pushUpdatesReceivedRequest.getClientId());
					response = new PushUpdatesReceivedResponse();
					((PushUpdatesReceivedResponse)response).setResult(true);
				}
			}
			else if(DELETES_RECEIVED.equals(key)){
				if (request instanceof PushDeletesReceivedRequest) {
					PushDeletesReceivedRequest pushDeletesReceivedRequest = (PushDeletesReceivedRequest) request;
					syncAgentService.deletesReceived(pushDeletesReceivedRequest.getClassIdPairs(), pushDeletesReceivedRequest.getClientId());
					response = new PushDeletesReceivedResponse();
					((PushDeletesReceivedResponse)response).setResult(true);
				}
			}
			else if(PUT.equals(key)){
				if (request instanceof PutRequest) {
					PutRequest putRequest = (PutRequest) request;
					
					
					if (putRequest.getPutTimestamp() == null || putRequest.getPutTimestamp() <= 0)
						putRequest.setPutTimestamp((new Date()).getTime());
					ServerResponse putServerResponse = syncAgentService.putObject((IPerceroObject) putRequest.getTheObject(), putRequest.getTransId(), new Date(putRequest.getPutTimestamp()), putRequest.getClientId());
					response = new PutResponse();
					((PutResponse)response).setResult(putServerResponse.getIsSuccessful());
					//((PutResponse)response).setResult(false);
				}
			}
			else if(REMOVE.equals(key)){
				if (request instanceof RemoveRequest) {
					RemoveRequest removeRequest = (RemoveRequest) request;
					ServerResponse removeServerResponse = syncAgentService.deleteObject(removeRequest.getRemovePair(), removeRequest.getClientId());
					response = new RemoveResponse();
					((RemoveResponse)response).setResult(removeServerResponse.getIsSuccessful());
				}
			}
			else if(key.equals("findUnique")){
				// TODO: result = syncAgentService.findUnique(ob, userId);
			}
			else if(GET_ALL_BY_NAME.equals(key)){
				if (request instanceof GetAllByNameRequest) {
					GetAllByNameRequest getAllByNameRequest = (GetAllByNameRequest) request;
					result = syncAgentService.getAllByName(getAllByNameRequest.getTheClassName(), getAllByNameRequest.getPageNumber(), getAllByNameRequest.getPageSize(), getAllByNameRequest.getReturnTotal(), getAllByNameRequest.getClientId());
					response = new GetAllByNameResponse();
					((GetAllByNameResponse)response).setResult((List<BaseDataObject>)result);
					((GetAllByNameResponse)response).setPageNumber(getAllByNameRequest.getPageNumber());
					((GetAllByNameResponse)response).setPageSize(getAllByNameRequest.getPageSize());
				}
			}
			else if(RUN_QUERY.equals(key)){
				if (request instanceof RunQueryRequest) {
					RunQueryRequest runQueryRequest = (RunQueryRequest) request;
					result = syncAgentService.runQuery(runQueryRequest.getTheClassName(), runQueryRequest.getQueryName(), runQueryRequest.getQueryArguments(), runQueryRequest.getClientId());
					response = new RunQueryResponse();
					((RunQueryResponse)response).setResult((List<Object>)result);
				}
			}
			else if(GET_CHANGE_WATCHER.equals(key)){
				if (request instanceof PushCWUpdateRequest) {
					PushCWUpdateRequest pushCwUpdateRequest = (PushCWUpdateRequest) request;
					result = syncAgentService.getChangeWatcher(pushCwUpdateRequest.getClassIdPair(), pushCwUpdateRequest.getFieldName(), pushCwUpdateRequest.getParams(), pushCwUpdateRequest.getClientId());
					response = new PushCWUpdateResponse();
					((PushCWUpdateResponse)response).setFieldName(pushCwUpdateRequest.getFieldName());
					((PushCWUpdateResponse)response).setParams(pushCwUpdateRequest.getParams());
					((PushCWUpdateResponse)response).setClassIdPair(pushCwUpdateRequest.getClassIdPair());
					((PushCWUpdateResponse)response).setValue(result);
				}
			}
			else if(RUN_PROCESS.equals(key)){
				if (request instanceof RunProcessRequest) {
					RunProcessRequest runProcessRequest = (RunProcessRequest) request;
					result = syncAgentService.runProcess(runProcessRequest.getQueryName(), runProcessRequest.getQueryArguments(), runProcessRequest.getClientId());
					response = new RunProcessResponse();
					((RunProcessResponse)response).setResult(result);
				}
			}
			else if(GET_ACCESSOR.equals(key)){
				if (request instanceof GetAccessorRequest) {
					GetAccessorRequest getAccessorRequest = (GetAccessorRequest) request;
					IAccessor accessor = accessorService.getAccessor(getAccessorRequest.getUserId(), getAccessorRequest.getTheClassName(), getAccessorRequest.getTheClassId());
					result = accessor;
					response = new GetAccessorResponse();
					((GetAccessorResponse)response).setAccessor(accessor);
					
					if (accessor != null && accessor.getCanRead() && getAccessorRequest.getReturnObject()) {
						// User has access and has requested that the object be returned.
						Object foundObject = syncAgentService.findById(getAccessorRequest.getTheClassName(), getAccessorRequest.getTheClassId(), getAccessorRequest.getClientId());
						
						if (foundObject instanceof BaseDataObject) {
							((GetAccessorResponse)response).setResultObject((BaseDataObject)foundObject);
						}
					}
				}
			}
			else if(GET_HISTORY.equals(key)){
				if (request instanceof GetHistoryRequest) {
					GetHistoryRequest getHistoryRequest = (GetHistoryRequest) request;
					result = syncAgentService.getHistory(getHistoryRequest.getTheClassName(), getHistoryRequest.getTheClassId(), getHistoryRequest.getClientId());
					response = new GetHistoryResponse();
					((GetHistoryResponse)response).setResult((List<HistoricalObject>)result);
				}
			}
			else if(key.equals("searchByExample")) {
				// TODO: result = syncAgentService.searchByExample(ob, userId);
			}
			else {
				System.out.println("Unknown Message Type");
			}
		} catch(SyncException e) {
			logger.error(e.getCode().toString() + ": " + e.getName(), e);

			response = new SyncErrorResponse();
			((SyncErrorResponse) response).setErrorName(e.getName());
			((SyncErrorResponse) response).setErrorCode(e.getCode());
			((SyncErrorResponse) response).setErrorDesc(e.getMessage());
		} catch(Exception e) {
			logger.error(e.getMessage(), e);

			response = new SyncErrorResponse();
			((SyncErrorResponse) response).setErrorName(e.getMessage());
			((SyncErrorResponse) response).setErrorCode(0);
			((SyncErrorResponse) response).setErrorDesc(e.getMessage());
		} finally {
			// Send a message back to the originator
			try {
				if (message.getMessageProperties().getReplyTo() != null && !message.getMessageProperties().getReplyTo().isEmpty()) {
					if (request != null)
					{
						if (response == null)
							response = new SyncResponse();
						response.setClientId(request.getClientId());
						response.setCorrespondingMessageId(request.getMessageId());
						if (response.getData() == null)
							response.setData(result);
						
						if (response instanceof GetHistoryResponse) {
							template.convertAndSend(message.getMessageProperties().getReplyTo(), response);
						}
						else {
							pushSyncHelper.pushSyncResponseToClient(response, message.getMessageProperties().getReplyTo());
							//String json = response.toJson(safeObjectMapper);
							//pushSyncHelper.pushJsonToRouting(json, response.getClass(), message.getMessageProperties().getReplyTo());
							/*String jsonString = jsonObjectMapper.writeValueAsString(response);
							if (jsonString.compareTo(json) != 0)
							{
								System.out.println("JSON_NEW:" + json);
								System.out.println("JSON_OLD:" + jsonString);
								SyncResponse object_New = (SyncResponse) decoder.decode(json.getBytes());
								SyncResponse object_Old = (SyncResponse) decoder.decode(json.getBytes());
								System.out.println("Are Equal: " + com.mchange.v2.lang.ObjectUtils.eqOrBothNull(object_New, object_Old));
							}*/
							//template.convertAndSend(message.getMessageProperties().getReplyTo(), response);
						}
					}
					else
						template.convertAndSend(message.getMessageProperties().getReplyTo(), result);

					logger.debug("Finished message " + key);
				}
			}
			catch(Exception e){
				logger.error(e.getMessage(), e);
			}
		}
	}
}

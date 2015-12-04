package com.percero.amqp;

import com.percero.agents.auth.helpers.IAccountHelper;
import com.percero.agents.auth.hibernate.AuthHibernateUtils;
import com.percero.agents.auth.services.AuthProviderRegistry;
import com.percero.agents.auth.services.AuthService2;
import com.percero.agents.auth.services.IAuthService;
import com.percero.agents.auth.vo.*;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.services.ISyncAgentService;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.amqp.handlers.*;
import com.percero.framework.accessor.IAccessorService;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class supplies the main method that creates the spring context
 * and then all processing is invoked asynchronously by messaging.
 *
 * This class' onMessage function will be invoked when the process receives a 'test' message from the broker
 * @author Collin Brown
 *
 */
@Component("perceroAgentListener")
public class PerceroAgentListener implements MessageListener {
	
	private static PerceroAgentListener perceroAgentListener = null;
	public static PerceroAgentListener getInstance() {
		return perceroAgentListener;
	}

    @Autowired
    AmqpTemplate template;
    @Autowired
    AmqpAdmin amqpAdmin;
    @Autowired
    IDecoder decoder;
    @Autowired
    ISyncAgentService syncAgentService;
    @Autowired
    IAuthService authService;
    @Autowired
    AuthService2 authService2;
    @Autowired
    IAccessorService accessorService;
    @Autowired
    JsonMessageConverter jsonMessageConverter;
    @Autowired
    IAccessManager accessManager;
    @Autowired
    IAccountHelper accountHelper;
    @Autowired
    IPushSyncHelper pushSyncHelper;

    @Autowired @Qualifier("executorWithCallerRunsPolicy")
    TaskExecutor taskExecutor;
    @Autowired
    ConnectHandler connectHandler;
    @Autowired
    ReconnectHandler reconnectHandler;
    @Autowired
    FindByIdHandler findByIdHandler;
    @Autowired
    FindByExampleHandler findByExampleHandler;
    @Autowired
    SearchByExampleHandler searchByExampleHandler;
    @Autowired
    CreateObjectHandler createObjectHandler;
    @Autowired
    UpdatesReceivedHandler updatesReceivedHandler;
    @Autowired
    DeletesReceivedHandler deletesReceivedHandler;
    @Autowired
    PutObjectHandler putObjectHandler;
    @Autowired
    RemoveObjectHandler removeObjectHandler;
    @Autowired
    GetAllByNameHandler getAllByNameHandler;
    @Autowired
    CountAllByNameHandler countAllByNameHandler;
    @Autowired
    HibernateHandler hibernateHandler;
    @Autowired
    UpgradeClientHandler upgradeClientHandler;
    @Autowired
    DisconnectHandler disconnectHandler;
    @Autowired
    LogoutHandler logoutHandler;
    @Autowired
    FindByIdsHandler findByIdsHandler;
    @Autowired
    RunQueryHandler runQueryHandler;
    @Autowired
    RunProcessHandler runProcessHandler;
    @Autowired
    GetChangeWatcherHandler getChangeWatcherHandler;
    @Autowired
    GetAccessorHandler getAccessorHandler;
    @Autowired
    GetHistoryHandler getHistoryHandler;

    @Autowired
    AuthProviderRegistry authProviderRegistry;


    public static final String PROCESS_TRANSACTION = "processTransaction";

    private static Logger logger = Logger.getLogger(PerceroAgentListener.class);
    
    public PerceroAgentListener() {
    	PerceroAgentListener.perceroAgentListener = this;
    }

    /**
     * Message handling function
     */
    @Transactional
    public void onMessage(Message message) {
        Object ob;
        try {
            taskExecutor = null;
            ob = decoder.decode(message.getBody());

            String key = message.getMessageProperties().getReceivedRoutingKey();
            String replyTo = message.getMessageProperties().getReplyTo();

            if (ob instanceof SyncRequest) {
                handleSyncRequest((SyncRequest) ob, key, replyTo);
            }
            else if (ob instanceof AuthRequest) {
                handleAuthRequest((AuthRequest) ob, key, replyTo);
            }
        } catch (Exception e) {
            logger.error("Unable to process message", e);
        }
    }

    private void handleSyncRequest(SyncRequest request, String routingKey, String replyTo) throws Exception {
        //logger.debug("Received message " + routingKey);
        SyncMessageHandler messageHandler = null;

        if(ConnectHandler.CONNECT.equals(routingKey)) {
            messageHandler = connectHandler;
        }
        else if(ReconnectHandler.RECONNECT.equals(routingKey)) {
            messageHandler = reconnectHandler;
        }
        else if(HibernateHandler.HIBERNATE.equals(routingKey)){
            messageHandler = hibernateHandler;
        }
        else if(UpgradeClientHandler.UPGRADE_CLIENT.equals(routingKey)){
            messageHandler = upgradeClientHandler;
        }
        else if(DisconnectHandler.DISCONNECT.equals(routingKey)){
            messageHandler = disconnectHandler;
        }
        else if(LogoutHandler.LOGOUT.equals(routingKey)){
            messageHandler = logoutHandler;
        }
        else if(FindByIdsHandler.FIND_BY_IDS.equals(routingKey)){
            messageHandler = findByIdsHandler;
        }
        else if(FindByIdHandler.FIND_BY_ID.equals(routingKey)){
            messageHandler = findByIdHandler;
        }
        else if(FindByExampleHandler.FIND_BY_EXAMPLE.equals(routingKey)){
            messageHandler = findByExampleHandler;
        }
        else if(SearchByExampleHandler.SEARCH_BY_EXAMPLE.equals(routingKey)){
            messageHandler = searchByExampleHandler;
        }
        else if(CreateObjectHandler.CREATE_OBJECT.equals(routingKey)){
            messageHandler = createObjectHandler;
        }
        else if(PROCESS_TRANSACTION.equals(routingKey)){
            throw new SyncException("Transactions are not currently supported", -101);
            /**
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
             */
        }
        else if(UpdatesReceivedHandler.UPDATES_RECEIVED.equals(routingKey)){
            messageHandler = updatesReceivedHandler;
        }
        else if(DeletesReceivedHandler.DELETES_RECEIVED.equals(routingKey)){
            messageHandler = deletesReceivedHandler;
        }
        else if(PutObjectHandler.PUT.equals(routingKey)){
            messageHandler = putObjectHandler;
        }
        else if(RemoveObjectHandler.REMOVE.equals(routingKey)){
            messageHandler = removeObjectHandler;
        }
        else if(routingKey.equals("findUnique")){
            // TODO: result = syncAgentService.findUnique(ob, userId);
        }
        else if(GetAllByNameHandler.GET_ALL_BY_NAME.equals(routingKey)){
            messageHandler = getAllByNameHandler;
        }
        else if(CountAllByNameHandler.COUNT_ALL_BY_NAME.equals(routingKey)){
            messageHandler = countAllByNameHandler;
        }
        else if(RunQueryHandler.RUN_QUERY.equals(routingKey)){
            messageHandler = runQueryHandler;
        }
        else if(GetChangeWatcherHandler.GET_CHANGE_WATCHER.equals(routingKey)){
            messageHandler = getChangeWatcherHandler;
        }
        else if(RunProcessHandler.RUN_PROCESS.equals(routingKey)){
            messageHandler = runProcessHandler;
        }
        else if(GetAccessorHandler.GET_ACCESSOR.equals(routingKey)){
            messageHandler = getAccessorHandler;
        }
        else if(GetHistoryHandler.GET_HISTORY.equals(routingKey)){
            messageHandler = getHistoryHandler;
        }
        else if(routingKey.equals("searchByExample")) {
            // TODO: result = syncAgentService.searchByExample(ob, userId);
        }
        else {
            System.out.println("Unknown Message Type");
        }

        if (messageHandler != null) {
            if (taskExecutor != null) {
                taskExecutor.execute(new MessageHandlerTask(messageHandler, request, replyTo));
            } else {
                messageHandler.run(request, replyTo);
            }
        }
    }


    public void handleAuthRequest(AuthRequest request, String key, String replyTo) {
        Object result = "INVALID DEFAULT VALUE";
        AuthResponse response = null;
        try{
            /** authentication provider infrastructure **/
            if(key.equals("authenticate") && request instanceof AuthenticationRequest){
                AuthenticationRequest authRequest = (AuthenticationRequest) request;
                response = authService2.authenticate(authRequest);
                result = response;
            }
            else if(key.equals("reauthenticate") && request instanceof ReauthenticationRequest){
                ReauthenticationRequest authRequest = (ReauthenticationRequest) request;
                response = authService2.reauthenticate(authRequest);
                result = response;
            }
            /** Essentially, Re-login **/
            else if(key.equals(ValidateUserByTokenRequest.ID) || key.equals("validateUserByToken")){
                if (request instanceof ValidateUserByTokenRequest) {
                    ValidateUserByTokenRequest authReqest = (ValidateUserByTokenRequest) request;
                    result = authService.validateUserByToken(authReqest.getRegAppKey(), authReqest.getUserId(), authReqest.getToken(), authReqest.getClientId());
                    response = new ValidateUserByTokenResponse();
                    result = AuthHibernateUtils.cleanObject(result);
                    ((ValidateUserByTokenResponse) response).setResult((Boolean)result);
                }
            }
            /** For OAuth2 WebApp style **/
            else if(key.equals(AuthenticateOAuthCodeRequest.ID) || key.equals("authenticateOAuthCode")){
                if (request instanceof AuthenticateOAuthCodeRequest) {
                    AuthenticateOAuthCodeRequest authRequest = (AuthenticateOAuthCodeRequest) request;
                    OAuthToken token = new OAuthToken();
                    token.setToken(authRequest.getRequestToken());
                    token.setTokenSecret(authRequest.getRequestSecret());
                    String authProviderID = authRequest.getAuthProvider();

                    result = authService.authenticateOAuthCode(authProviderID, authRequest.getCode(), replyTo, authRequest.getDeviceId(), authRequest.getRedirectUri(), token);
                    response = new AuthenticateOAuthCodeResponse();

                    if (result != null) {
                        result = AuthHibernateUtils.cleanObject(result);
                        ((AuthenticateOAuthCodeResponse) response).setResult(((OAuthResponse)result).userToken);
                        ((AuthenticateOAuthCodeResponse) response).setAccessToken(((OAuthResponse)result).accessToken);
                        ((AuthenticateOAuthCodeResponse) response).setRefreshToken(((OAuthResponse)result).refreshToken);
                    }
                }
            }
            /** For Basic OAuth2 style **/
            else if(key.equals("authenticateBasicOAuth")){
                if (request instanceof AuthenticateBasicOAuthRequest) {
                    AuthenticateBasicOAuthRequest authRequest = (AuthenticateBasicOAuthRequest) request;
                    OAuthToken token = new OAuthToken();
                    token.setToken(authRequest.getRequestToken());
                    token.setTokenSecret(authRequest.getRequestSecret());
                    String authProviderID = authRequest.getAuthProvider();

                    result = authService.authenticateBasicOAuth(authProviderID, authRequest.getUserName(), authRequest.getPassword(), authRequest.getScopes(), authRequest.getAppUrl(), replyTo, authRequest.getDeviceId(), token);
                    response = new AuthenticateBasicOAuthResponse();

                    if (result != null) {
                        result = AuthHibernateUtils.cleanObject(result);
                        ((AuthenticateBasicOAuthResponse) response).setResult(((OAuthResponse)result).userToken);
                        ((AuthenticateBasicOAuthResponse) response).setAccessToken(((OAuthResponse)result).accessToken);
                        ((AuthenticateBasicOAuthResponse) response).setRefreshToken(((OAuthResponse)result).refreshToken);
                    }
                }
            }
            /** For OAuth2 Installed App style - @DEPRICATED **/
            else if(key.equals(AuthenticateOAuthAccessTokenRequest.ID) || key.equals("authenticateOAuthAccessToken")){
                if (request instanceof AuthenticateOAuthAccessTokenRequest) {
                    AuthenticateOAuthAccessTokenRequest authRequest = (AuthenticateOAuthAccessTokenRequest) request;
                    String authProviderID = authRequest.getAuthProvider();
                    result = authService.authenticateOAuthAccessToken(authProviderID, authRequest.getAccessToken(), authRequest.getRefreshToken(), replyTo, authRequest.getDeviceId());
                    response = new AuthenticateOAuthAccessTokenResponse();
                    result = AuthHibernateUtils.cleanObject(result);
                    if (result != null) {
                        ((AuthenticateOAuthAccessTokenResponse) response).setResult(((OAuthResponse)result).userToken);
                        ((AuthenticateOAuthAccessTokenResponse) response).setAccessToken(((OAuthResponse)result).accessToken);
                        ((AuthenticateOAuthAccessTokenResponse) response).setRefreshToken(((OAuthResponse)result).refreshToken);
                    }
                }
            }
            else if(key.equals("disconnectAuth")){
                if (request instanceof com.percero.agents.auth.vo.DisconnectRequest) {
                    response = new DisconnectResponse();
                    result = authService.logoutUser(request.getUserId(), request.getToken(), request.getClientId());
                    ((DisconnectResponse) response).setResult((Boolean)result);
                }
            }
            else{
                System.out.println("Unknown Message Type");
            }
        } catch(Exception e){
            logger.error(e.getMessage(), e);
        } finally{
            // Send a message back to the originator
            if (request != null)
            {
                if (response == null)
                    response = new AuthResponse();
                response.setClientId(request.getClientId());
                response.setCorrespondingMessageId(request.getMessageId());
                template.convertAndSend(replyTo, response);
            }
            else
                template.convertAndSend(replyTo, result);
        }
    }

    /**
     * Main function for starting the process
     */
    @SuppressWarnings("resource")
    public static void main(String[] args){
        ApplicationContext context =
                new ClassPathXmlApplicationContext(new String[] {"spring-listener.xml"});
        System.out.println(context.toString());
    }
}

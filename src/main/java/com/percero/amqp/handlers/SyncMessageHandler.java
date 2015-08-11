package com.percero.amqp.handlers;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.exceptions.ClientException;
import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.services.ISyncAgentService;
import com.percero.agents.sync.vo.GetHistoryResponse;
import com.percero.agents.sync.vo.SyncErrorResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class SyncMessageHandler {
	
	protected static final Logger log = Logger.getLogger(SyncMessageHandler.class);
	
	@Autowired
	AmqpTemplate template;
	@Autowired
	ISyncAgentService syncAgentService;
	@Autowired
	ObjectMapper safeObjectMapper;
	@Autowired
	IPushSyncHelper pushSyncHelper;
	
	String routingKey = null;

	public void preProcessor(SyncRequest request) {
		// Do nothing.
	}
	
	
	public void run(SyncRequest request, String replyTo) {
		SyncResponse response = null;
		preProcessor(request);
		try {
			response = handleMessage(request, replyTo);
		} catch(SyncException e) {
			if (ClientException.INVALID_CLIENT.equals(e.getName())) {
				log.debug("Invalid Client - Setting response to NULL", e);
				
				// No need to send a response to an invalid client.
				response = null;
			}
			else {
				if (e.getCode() != null) {
					log.error(e.getCode().toString() + ": " + e.getName(), e);
				}
				else {
					log.error(e.getName(), e);
				}
				
				response = new SyncErrorResponse();
				((SyncErrorResponse) response).setErrorName(e.getName());
				((SyncErrorResponse) response).setErrorCode(e.getCode());
				((SyncErrorResponse) response).setErrorDesc(e.getMessage());
			}
		} catch(Exception e) {
			log.error(e.getMessage(), e);
			response = new SyncErrorResponse();
			((SyncErrorResponse) response).setErrorName(e.getMessage());
			((SyncErrorResponse) response).setErrorCode(0);
			((SyncErrorResponse) response).setErrorDesc(e.getMessage());
		} catch(Error e){
			log.error(e.getMessage(), e);
			response = new SyncErrorResponse();
			((SyncErrorResponse) response).setErrorName(e.getMessage());
			((SyncErrorResponse) response).setErrorCode(0);
			((SyncErrorResponse) response).setErrorDesc(e.getMessage());
		}
		finally {
			if (response != null) {
				sendResponse(response, request.getMessageId(), request.getClientId(), replyTo);
			}
		}
	}
	
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		// Do nothing.
		return null;
	}
	
	protected void sendResponse(SyncResponse response, String requestMessageId, String clientId, String replyTo) {
		// Send a message back to the originator
		try {
			if (replyTo != null && !replyTo.isEmpty()) {
				if (response == null)
					response = new SyncResponse();
				response.setClientId(clientId);
				response.setCorrespondingMessageId(requestMessageId);
				
				if (response instanceof GetHistoryResponse) {
					template.convertAndSend(replyTo, response);
				}
				else {
					pushSyncHelper.pushSyncResponseToClient(response, replyTo);
					//String json = response.toJson(safeObjectMapper);
					//pushSyncHelper.pushJsonToRouting(json, response.getClass(), replyTo);
				}
			}
		}
		catch(Exception e){
			log.error(e.getMessage(), e);
		}
	}
}

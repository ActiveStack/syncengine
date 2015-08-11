package com.percero.amqp.handlers;

import java.util.UUID;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.auth.helpers.IAccountHelper;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.vo.RunProcessRequest;
import com.percero.agents.sync.vo.RunProcessResponse;
import com.percero.agents.sync.vo.RunShardedProcess;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;
import com.percero.amqp.IDecoder;

@Component
public class RunProcessHandler extends SyncMessageHandler {

	public static final String RUN_PROCESS = "runProcess";
	
	public RunProcessHandler() {
		routingKey = RUN_PROCESS;
	}
	
	@Autowired
	IAccessManager accessManager;
	@Autowired
	IAccountHelper accountHelper;
	@Autowired
	IPushSyncHelper pushSyncHelper;
	@Autowired
	AmqpAdmin amqpAdmin;
	@Autowired
	AmqpTemplate template;
	@Autowired
	IDecoder decoder;

	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		RunProcessResponse response = new RunProcessResponse();
		RunProcessRequest runProcessRequest = (RunProcessRequest) request;
		Boolean isShardedProcess = runProcessRequest.getShardedProcess();
		String clientId = null;
		if (isShardedProcess) {
			clientId = UUID.randomUUID().toString();
			// TODO: Does deviceId need to be the ServiceGroup ID? Or some other known ID?
			String deviceId = UUID.randomUUID().toString();
			String userId = accessManager.getClientUserId(runProcessRequest.getClientId());
			String serviceGroupId = runProcessRequest.getServiceGroupId();
			String processId = runProcessRequest.getProcessId();
			
			if (StringUtils.hasText(userId)) {
				// The client will only have UpdateJournals and DeleteJournals if the UserDevice exists.
				accessManager.registerClient(clientId, userId, deviceId);
				// TODO: Not needed? String dataId = syncAgentService.getDataId(clientId);
				
				response = null;
				
				try {
					org.springframework.amqp.core.Queue theQueue = new org.springframework.amqp.core.Queue(serviceGroupId);
					amqpAdmin.declareQueue(theQueue);
				} catch(Exception e) {
					// Do nothing.
				}
				
				RunShardedProcess runShardedProcess = new RunShardedProcess();
				runShardedProcess.setArguments(runProcessRequest.getQueryArguments());
				runShardedProcess.setClientId(clientId);
				runShardedProcess.setOriginatingClientId(request.getClientId());
				runShardedProcess.setProcessId(processId);
				runShardedProcess.setServiceGroupId(serviceGroupId);

				try{
					// TODO: Optimize by generating JSON directly instead of using Jackson.
					// Send a message to the serviceGroupId queue.
					template.convertAndSend(runProcessRequest.getServiceGroupId(), runShardedProcess);
					//Message convertedMessage = toMessage(objectJson, objectClass, new MessageProperties());
					//template.send(routingKey, convertedMessage);
				}
				catch(Exception e){
					log.error(e.getMessage(), e);
				}
			} else {
				log.warn("Unable to get UserID from request.");
			}
		}
		else {
			clientId = runProcessRequest.getClientId();
			Object result = syncAgentService.runProcess(runProcessRequest.getQueryName(), runProcessRequest.getQueryArguments(), clientId);
			response.setResult(result);
		}


		return response;
	}

	/**
	@SuppressWarnings("rawtypes")
	public final Message toMessage(String objectJson, Class objectClass, MessageProperties messageProperties)
			throws MessageConversionException {
		if (messageProperties==null) {
			messageProperties = new MessageProperties();
		}
		Message message = createMessage(objectJson, objectClass, messageProperties);
		messageProperties = message.getMessageProperties();
		if (this.createMessageIds && messageProperties.getMessageId()==null) {
			messageProperties.setMessageId(UUID.randomUUID().toString());
		}
		return message;
	}

	@SuppressWarnings("rawtypes")
	protected Message createMessage(String objectJson, Class objectClass, MessageProperties messageProperties)
			throws MessageConversionException {
		byte[] bytes = null;
		try {
			String jsonString = objectJson;
			bytes = jsonString.getBytes(this.defaultCharset);
		} catch (UnsupportedEncodingException e) {
			throw new MessageConversionException("Failed to convert Message content", e);
		}
		messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		messageProperties.setContentEncoding(this.defaultCharset);
		if (bytes != null) {
			messageProperties.setContentLength(bytes.length);
		}
		classMapper.fromClass(objectClass, messageProperties);
		return new Message(bytes, messageProperties);
	}
	*/
}

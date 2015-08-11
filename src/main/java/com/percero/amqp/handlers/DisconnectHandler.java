package com.percero.amqp.handlers;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.vo.DisconnectRequest;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class DisconnectHandler extends SyncMessageHandler {

	public static final String DISCONNECT = "disconnect";
	
	public DisconnectHandler() {
		routingKey = DISCONNECT;
	}
	
	@Autowired
	IAccessManager accessManager;
	@Autowired
	AmqpAdmin amqpAdmin;

	@Override
	public void run(SyncRequest request, String replyTo) {
		try {
			handleMessage(request, replyTo);
		} catch(Exception e) {
			log.error("Error disconnecting client", e);
		}
	}

	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		DisconnectRequest disconnectRequest = (DisconnectRequest) request;
		accessManager.hibernateClient(disconnectRequest.getClientId(), disconnectRequest.getUserId());
		
		return null;
	}
}

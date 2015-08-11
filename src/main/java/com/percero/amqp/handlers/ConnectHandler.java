package com.percero.amqp.handlers;

import java.security.Principal;
import java.util.Date;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.auth.helpers.IAccountHelper;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.vo.ConnectRequest;
import com.percero.agents.sync.vo.ConnectResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;
import com.percero.amqp.IDecoder;

@Component
public class ConnectHandler extends SyncMessageHandler {

	public static final String CONNECT = "connect";
	
	public ConnectHandler() {
		routingKey = CONNECT;
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
		ConnectRequest connectRequest = (ConnectRequest) request;
		ConnectResponse connectResponse = new ConnectResponse();
		connectResponse.setCurrentTimestamp( (new Date()).getTime() );
		
		if (connectRequest == null) {
			log.warn("Invalid ConnectRequest: NULL");
			return connectResponse;
		}
		else {
			log.debug("Handling ConnectRequest from client " + request.getClientId());
		}

		Principal pUser = accountHelper.authenticateOAuth(connectRequest.getRegAppKey(), connectRequest.getSvcOauthKey(), connectRequest.getUserId(), connectRequest.getToken(), connectRequest.getClientId(), connectRequest.getClientType(), connectRequest.getDeviceId());
		
		if (pUser != null) {
			// The client will only have UpdateJournals and DeleteJournals if the UserDevice exists.
			log.debug("Valid Principal User found and authenticated. Registering new client");
			accessManager.registerClient(connectRequest.getClientId(), connectRequest.getUserId(), connectRequest.getDeviceId(), connectRequest.getClientType());
			connectResponse.setClientId(connectRequest.getClientId());
			connectResponse.setDataID(syncAgentService.getDataId(connectRequest.getClientId()));
			connectResponse.setData("CONNECT");
		} else {
			log.warn("CONNECT: Unable to get valid Principal User");
		}

		return connectResponse;
	}
}

package com.percero.amqp.handlers;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.vo.LogoutRequest;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class LogoutHandler extends SyncMessageHandler {

	public static final String LOGOUT = "logout";
	
	public LogoutHandler() {
		routingKey = LOGOUT;
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
			log.error("Unable to logout client", e);
		}
	}
	
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		LogoutRequest logoutRequest = (LogoutRequest) request;
		accessManager.logoutClient(logoutRequest.getClientId(), logoutRequest.getPleaseDestroyClient());
		return null;
	}
}

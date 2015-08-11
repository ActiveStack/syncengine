package com.percero.amqp.handlers;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.vo.HibernateRequest;
import com.percero.agents.sync.vo.HibernateResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class HibernateHandler extends SyncMessageHandler {

	public static final String HIBERNATE = "hibernate";
	
	public HibernateHandler() {
		routingKey = HIBERNATE;
	}
	
	@Autowired
	IAccessManager accessManager;
	@Autowired
	AmqpAdmin amqpAdmin;

	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		HibernateResponse response = new HibernateResponse();
		HibernateRequest hibernateRequest = (HibernateRequest) request;
		response.setResult(accessManager.hibernateClient(hibernateRequest.getClientId(), hibernateRequest.getUserId()));
		return response;
	}
}

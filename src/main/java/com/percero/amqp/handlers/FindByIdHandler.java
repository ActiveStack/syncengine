package com.percero.amqp.handlers;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.FindByIdRequest;
import com.percero.agents.sync.vo.FindByIdResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class FindByIdHandler extends SyncMessageHandler {

	public static final String FIND_BY_ID = "findById";
	
	public FindByIdHandler() {
		routingKey = FIND_BY_ID;
	}
	
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		FindByIdResponse response = new FindByIdResponse();
		FindByIdRequest findByIdRequest = (FindByIdRequest) request;
		Object result = syncAgentService.findById(findByIdRequest.getTheClassName(), findByIdRequest.getTheClassId(), findByIdRequest.getClientId());
		((FindByIdResponse)response).setResult((BaseDataObject)result);

		return response;
	}
}

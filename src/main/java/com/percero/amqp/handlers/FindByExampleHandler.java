package com.percero.amqp.handlers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.FindByExampleRequest;
import com.percero.agents.sync.vo.FindByExampleResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class FindByExampleHandler extends SyncMessageHandler {

	public static final String FIND_BY_EXAMPLE = "findByExample";
	
	public FindByExampleHandler() {
		routingKey = FIND_BY_EXAMPLE;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		FindByExampleResponse response = new FindByExampleResponse();
		FindByExampleRequest findByExampleRequest = (FindByExampleRequest) request;
		Object result = syncAgentService.findByExample(findByExampleRequest.getTheObject(), null, findByExampleRequest.getClientId());
		response = new FindByExampleResponse();
		((FindByExampleResponse)response).setResult((List<BaseDataObject>)result);

		return response;
	}
}

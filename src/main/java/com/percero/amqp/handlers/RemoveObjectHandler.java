package com.percero.amqp.handlers;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.RemoveRequest;
import com.percero.agents.sync.vo.RemoveResponse;
import com.percero.agents.sync.vo.ServerResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class RemoveObjectHandler extends SyncMessageHandler {

	public static final String REMOVE = "removeObject";
	
	public RemoveObjectHandler() {
		routingKey = REMOVE;
	}
	
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		RemoveResponse response = new RemoveResponse();
		RemoveRequest removeRequest = (RemoveRequest) request;
		ServerResponse removeServerResponse = syncAgentService.deleteObject(removeRequest.getRemovePair(), removeRequest.getClientId());
		response.setResult(removeServerResponse.getIsSuccessful());

		return response;
	}
}

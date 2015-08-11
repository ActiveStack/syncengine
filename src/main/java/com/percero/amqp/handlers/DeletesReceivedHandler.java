package com.percero.amqp.handlers;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.PushDeletesReceivedRequest;
import com.percero.agents.sync.vo.PushDeletesReceivedResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class DeletesReceivedHandler extends SyncMessageHandler {

	public static final String DELETES_RECEIVED = "deletesReceived";
	
	public DeletesReceivedHandler() {
		routingKey = DELETES_RECEIVED;
	}
	
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		PushDeletesReceivedResponse response = new PushDeletesReceivedResponse();
		PushDeletesReceivedRequest pushDeletesReceivedRequest = (PushDeletesReceivedRequest) request;
		syncAgentService.deletesReceived(pushDeletesReceivedRequest.getClassIdPairs(), pushDeletesReceivedRequest.getClientId());
		response.setResult(true);

		return response;
	}
}

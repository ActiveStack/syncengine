package com.percero.amqp.handlers;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.PushUpdatesReceivedRequest;
import com.percero.agents.sync.vo.PushUpdatesReceivedResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class UpdatesReceivedHandler extends SyncMessageHandler {

	public static final String UPDATES_RECEIVED = "updatesReceived";
	
	public UpdatesReceivedHandler() {
		routingKey = UPDATES_RECEIVED;
	}
	
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		PushUpdatesReceivedResponse response = new PushUpdatesReceivedResponse();
		PushUpdatesReceivedRequest pushUpdatesReceivedRequest = (PushUpdatesReceivedRequest) request;
		syncAgentService.updatesReceived(pushUpdatesReceivedRequest.getClassIdPairs(), pushUpdatesReceivedRequest.getClientId());
		response.setResult(true);

		return response;
	}
}

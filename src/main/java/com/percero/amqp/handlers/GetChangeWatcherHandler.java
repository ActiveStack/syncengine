package com.percero.amqp.handlers;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.PushCWUpdateRequest;
import com.percero.agents.sync.vo.PushCWUpdateResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class GetChangeWatcherHandler extends SyncMessageHandler {

	public static final String GET_CHANGE_WATCHER = "getChangeWatcher";
	
	public GetChangeWatcherHandler() {
		routingKey = GET_CHANGE_WATCHER;
	}
	
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		PushCWUpdateResponse response = new PushCWUpdateResponse();
		PushCWUpdateRequest pushCwUpdateRequest = (PushCWUpdateRequest) request;
		Object result = syncAgentService.getChangeWatcherValue(pushCwUpdateRequest.getClassIdPair(), pushCwUpdateRequest.getFieldName(), pushCwUpdateRequest.getParams(), pushCwUpdateRequest.getClientId());
		response.setFieldName(pushCwUpdateRequest.getFieldName());
		response.setParams(pushCwUpdateRequest.getParams());
		response.setClassIdPair(pushCwUpdateRequest.getClassIdPair());
		response.setValue(result);

		return response;
	}
}

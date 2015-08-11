package com.percero.amqp.handlers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.services.AccessorService;
import com.percero.agents.sync.vo.GetHistoryRequest;
import com.percero.agents.sync.vo.GetHistoryResponse;
import com.percero.agents.sync.vo.HistoricalObject;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class GetHistoryHandler extends SyncMessageHandler {

	public static final String GET_HISTORY = "getHistory";
	
	public GetHistoryHandler() {
		routingKey = GET_HISTORY;
	}
	
	@Autowired
	AccessorService accessorService;
	
	@SuppressWarnings("unchecked")
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		GetHistoryResponse response = new GetHistoryResponse();
		GetHistoryRequest getHistoryRequest = (GetHistoryRequest) request;
		Object result = syncAgentService.getHistory(getHistoryRequest.getTheClassName(), getHistoryRequest.getTheClassId(), getHistoryRequest.getClientId());
		response = new GetHistoryResponse();
		response.setResult((List<HistoricalObject>)result);

		return response;
	}
}

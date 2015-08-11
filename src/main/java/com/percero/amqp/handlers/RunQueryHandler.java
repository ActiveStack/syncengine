package com.percero.amqp.handlers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.RunQueryRequest;
import com.percero.agents.sync.vo.RunQueryResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class RunQueryHandler extends SyncMessageHandler {

	public static final String RUN_QUERY = "runQuery";
	
	public RunQueryHandler() {
		routingKey = RUN_QUERY;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		RunQueryResponse response = new RunQueryResponse();
		RunQueryRequest runQueryRequest = (RunQueryRequest) request;
		Object result = syncAgentService.runQuery(runQueryRequest.getTheClassName(), runQueryRequest.getQueryName(), runQueryRequest.getQueryArguments(), runQueryRequest.getClientId());
		response.setResult((List<Object>)result);

		return response;
	}
}

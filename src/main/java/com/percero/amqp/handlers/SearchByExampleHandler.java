package com.percero.amqp.handlers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.SearchByExampleRequest;
import com.percero.agents.sync.vo.SearchByExampleResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class SearchByExampleHandler extends SyncMessageHandler {

	public static final String SEARCH_BY_EXAMPLE = "searchByExample";
	
	public SearchByExampleHandler() {
		routingKey = SEARCH_BY_EXAMPLE;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		SearchByExampleResponse response = new SearchByExampleResponse();
		SearchByExampleRequest findByExampleRequest = (SearchByExampleRequest) request;
		Object result = syncAgentService.searchByExample(findByExampleRequest.getTheObject(), null, findByExampleRequest.getClientId());
		
		response.setResult((List<BaseDataObject>)result);

		return response;
	}
}

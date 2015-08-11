package com.percero.amqp.handlers;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.CountAllByNameRequest;
import com.percero.agents.sync.vo.CountAllByNameResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class CountAllByNameHandler extends SyncMessageHandler {

	public static final String COUNT_ALL_BY_NAME = "countAllByName";
	
	public CountAllByNameHandler() {
		routingKey = COUNT_ALL_BY_NAME;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		CountAllByNameResponse response = new CountAllByNameResponse();
		CountAllByNameRequest countAllByNameRequest = (CountAllByNameRequest) request;
		Object result = syncAgentService.countAllByName(countAllByNameRequest.getClassNames(), countAllByNameRequest.getClientId());
		response = new CountAllByNameResponse();
		response.setResult((Map<String, Integer>)result);

		return response;
	}
}

package com.percero.amqp.handlers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.FindByIdsRequest;
import com.percero.agents.sync.vo.FindByIdsResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;

@Component
public class FindByIdsHandler extends SyncMessageHandler {

	public static final String FIND_BY_IDS = "findByIds";
	
	public FindByIdsHandler() {
		routingKey = FIND_BY_IDS;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		FindByIdsResponse response = new FindByIdsResponse();
		FindByIdsRequest findByIdsRequest = (FindByIdsRequest) request;
		Object result = syncAgentService.findByIds(findByIdsRequest.getTheClassIdList(), findByIdsRequest.getClientId());
		response = new FindByIdsResponse();
		response.setResult((List<BaseDataObject>)result);

		return response;
	}
}

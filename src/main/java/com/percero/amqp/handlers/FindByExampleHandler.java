package com.percero.amqp.handlers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
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
		String originalId = null;
		if (StringUtils.hasText(findByExampleRequest.getTheObject().getID())) {
			originalId = findByExampleRequest.getTheObject().getID();
			findByExampleRequest.getTheObject().setID(null);
		}
		List<BaseDataObject> result = (List<BaseDataObject>) syncAgentService.findByExample(findByExampleRequest.getTheObject(), null, findByExampleRequest.getClientId());
		response = new FindByExampleResponse();
		
		if (result == null || result.isEmpty()) {
			if (originalId != null) {
				// Couldn't find by example, but if the original object had an ID, let's try and find by id.
				BaseDataObject findByIdResult = (BaseDataObject) syncAgentService.findById(
						new ClassIDPair(originalId, findByExampleRequest.getTheObject().getClass().getCanonicalName()),
						findByExampleRequest.getClientId());
				if (findByIdResult != null) {
					if (result == null) {
						result = new ArrayList<BaseDataObject>();
					}
					result.add(findByIdResult);
				}
			}
		}

		((FindByExampleResponse)response).setResult((List<BaseDataObject>)result);

		return response;
	}
}

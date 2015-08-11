package com.percero.amqp.handlers;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.CreateRequest;
import com.percero.agents.sync.vo.CreateResponse;
import com.percero.agents.sync.vo.ServerResponse;
import com.percero.agents.sync.vo.SyncDataError;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;
import com.percero.framework.vo.IPerceroObject;

@Component
public class CreateObjectHandler extends SyncMessageHandler {

	public static final String CREATE_OBJECT = "createObject";
	
	public CreateObjectHandler() {
		routingKey = CREATE_OBJECT;
	}
	
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		CreateResponse response = new CreateResponse();
		CreateRequest createRequest = (CreateRequest) request;

		try {
			ServerResponse createServerResponse = syncAgentService.createObject((IPerceroObject) createRequest.getTheObject(), createRequest.getClientId());
			((CreateResponse)response).setResult(createServerResponse.getIsSuccessful());
			((CreateResponse)response).setTheObject((BaseDataObject) createServerResponse.getResultObject());
		} catch(SyncException se) {
			SyncDataError error = new SyncDataError();
			error.setCode(se.getCode().toString());
			error.setName(se.getName());
			error.setMessage(se.getMessage());
			response.setResult(false);
			response.setData(error);
			response.setTheObject(createRequest.getTheObject());
		}

		return response;
	}
}

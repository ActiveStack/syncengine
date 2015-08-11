package com.percero.amqp.handlers;

import java.util.Date;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.PutRequest;
import com.percero.agents.sync.vo.PutResponse;
import com.percero.agents.sync.vo.ServerResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;
import com.percero.framework.vo.IPerceroObject;

@Component
public class PutObjectHandler extends SyncMessageHandler {

	public static final String PUT = "putObject";
	
	public PutObjectHandler() {
		routingKey = PUT;
	}
	
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		PutResponse response = new PutResponse();
		PutRequest putRequest = (PutRequest) request;
		
		if (putRequest.getPutTimestamp() == null || putRequest.getPutTimestamp() <= 0)
			putRequest.setPutTimestamp((new Date()).getTime());
		ServerResponse putServerResponse = syncAgentService.putObject((IPerceroObject) putRequest.getTheObject(), putRequest.getTransId(), new Date(putRequest.getPutTimestamp()), putRequest.getClientId());
		response.setResult(putServerResponse.getIsSuccessful());

		return response;
	}
}

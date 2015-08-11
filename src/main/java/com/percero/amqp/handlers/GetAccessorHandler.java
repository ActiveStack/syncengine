package com.percero.amqp.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.services.AccessorService;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.GetAccessorRequest;
import com.percero.agents.sync.vo.GetAccessorResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;
import com.percero.framework.accessor.IAccessor;
import com.percero.framework.vo.IPerceroObject;

@Component
public class GetAccessorHandler extends SyncMessageHandler {

	public static final String GET_ACCESSOR = "getAccessor";
	
	public GetAccessorHandler() {
		routingKey = GET_ACCESSOR;
	}
	
	@Autowired
	AccessorService accessorService;
	
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		GetAccessorResponse response = new GetAccessorResponse();
		GetAccessorRequest getAccessorRequest = (GetAccessorRequest) request;
		IAccessor accessor = accessorService.getAccessor(getAccessorRequest.getUserId(), getAccessorRequest.getTheClassName(), getAccessorRequest.getTheClassId());
		response = new GetAccessorResponse();
		((GetAccessorResponse)response).setAccessor(accessor);
		
		if (accessor != null && accessor.getCanRead() && getAccessorRequest.getReturnObject()) {
			// User has access and has requested that the object be returned.
			Object foundObject = syncAgentService.findById(getAccessorRequest.getTheClassName(), getAccessorRequest.getTheClassId(), getAccessorRequest.getClientId());
			
			if (foundObject instanceof IPerceroObject) {
				((GetAccessorResponse)response).setResultObject((BaseDataObject)foundObject);
			}
		}

		return response;
	}
}

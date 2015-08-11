package com.percero.amqp.handlers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.GetAllByNameRequest;
import com.percero.agents.sync.vo.GetAllByNameResponse;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;
import com.percero.framework.vo.IPerceroObject;
import com.percero.framework.vo.PerceroList;

@Component
public class GetAllByNameHandler extends SyncMessageHandler {

	public static final String GET_ALL_BY_NAME = "getAllByName";
	
	public GetAllByNameHandler() {
		routingKey = GET_ALL_BY_NAME;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		GetAllByNameResponse response = new GetAllByNameResponse();
		GetAllByNameRequest getAllByNameRequest = (GetAllByNameRequest) request;
		Object result = syncAgentService.getAllByName(getAllByNameRequest.getTheClassName(), getAllByNameRequest.getPageNumber(), getAllByNameRequest.getPageSize(), getAllByNameRequest.getReturnTotal(), getAllByNameRequest.getClientId());
		response = new GetAllByNameResponse();
		response.setResult((List<BaseDataObject>)result);
		response.setPageNumber(getAllByNameRequest.getPageNumber());
		response.setPageSize(getAllByNameRequest.getPageSize());
		if (result != null)
			response.setTotalCount(((PerceroList<IPerceroObject>)result).getTotalLength());
		else
			response.setTotalCount(0);

		return response;
	}
}

package com.percero.agents.sync.rr;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.vo.ServerRequest;
import com.percero.agents.sync.vo.ServerResponse;

@Component
public class RequestHandler implements IRequestHandler {
	
	private static final Logger log = Logger.getLogger(RequestHandler.class);

	public RequestHandler() {
	}
	
	public ServerResponse handleRequest(ServerRequest aRequest) {
		ServerResponse response = null;
		
		log.warn("RequestHandler: No request handler found for " + aRequest.toString());
		
		return response;
	}
	
}

package com.percero.agents.sync.rr;

import com.percero.agents.sync.vo.ServerRequest;
import com.percero.agents.sync.vo.ServerResponse;

public interface IRequestHandler {

	public ServerResponse handleRequest(ServerRequest aRequest);
}

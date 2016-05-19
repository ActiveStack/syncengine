package com.percero.agents.auth.services;

import com.percero.agents.auth.vo.AuthenticationRequest;
import com.percero.agents.auth.vo.AuthenticationResponse;
import com.percero.agents.auth.vo.ServiceUser;

public interface IPostRegisterHandler {

	/**
	 * Takes in an AuthenticationResponse and is able to modify that response or
	 * even return a new one. Note that the returned response is the response
	 * sent back to the registering client.
	 * 
	 * @param registerRequest
	 * @param registerResponse
	 * @param serviceUser
	 * @return
	 * @throws AuthException
	 */
	AuthenticationResponse run(AuthenticationRequest request, AuthenticationResponse registerResponse, ServiceUser serviceUser) throws AuthException;
}

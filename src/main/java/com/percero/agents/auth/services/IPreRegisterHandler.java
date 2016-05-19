package com.percero.agents.auth.services;

import com.percero.agents.auth.vo.AuthenticationRequest;

public interface IPreRegisterHandler {

	/**
	 * Takes in an AuthenticationResponse and to perform any necessary
	 * pre-register checks. If an Exception is thrown, then the register request
	 * is cancelled.
	 * 
	 * @param registerRequest
	 * @param registerResponse
	 * @return
	 * @throws AuthException
	 */
	void run(AuthenticationRequest request) throws AuthException;
}

package com.percero.agents.sync.aspects;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.exceptions.ClientException;

@Component
@Aspect
public class ValidClientAspect {

	@Autowired
	IAccessManager accessManager;
	
	//@Before(value="@annotation(com.com.percero.agents.sync.annotations.ValidateClient) && args(clientId)", argNames="clientId")
	@Before(value="@annotation(com.percero.agents.sync.annotations.ValidateClient)", argNames="clientId")
	public void assertValidClient(String clientId) throws Exception {
		//String clientId = "";
		Boolean isValidClient = accessManager.validateClientByClientId(clientId);
		if (!isValidClient)
			throw new ClientException(ClientException.INVALID_CLIENT, ClientException.INVALID_CLIENT_CODE, "", clientId);
	}
}

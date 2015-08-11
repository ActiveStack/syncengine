package com.percero.amqp.handlers;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.auth.helpers.IAccountHelper;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;
import com.percero.agents.sync.vo.UpgradeClientRequest;
import com.percero.agents.sync.vo.UpgradeClientResponse;

@Component
public class UpgradeClientHandler extends SyncMessageHandler {

	public static final String UPGRADE_CLIENT = "upgradeClient";
	
	public UpgradeClientHandler() {
		routingKey = UPGRADE_CLIENT;
	}
	
	@Autowired
	IAccessManager accessManager;
	@Autowired
	IAccountHelper accountHelper;

	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		UpgradeClientRequest upgradeClientRequest = (UpgradeClientRequest) request;
		UpgradeClientResponse response = new UpgradeClientResponse();
		response.setResult(accessManager.upgradeClient(upgradeClientRequest.getClientId(), upgradeClientRequest.getDeviceId(), upgradeClientRequest.getDeviceType(), upgradeClientRequest.getUserId()));
		
		if (response.getResult()) {
			// If upgrade successful, then check for existing updates and push to client.
			accessManager.registerClient(upgradeClientRequest.getClientId(), upgradeClientRequest.getUserId(), upgradeClientRequest.getDeviceId());
			response.setClientId(upgradeClientRequest.getClientId());

			// Push all UpdateJournals for this Client.
			final Collection<String> updateJournals = accessManager.getClientUpdateJournals(upgradeClientRequest.getClientId(), true);
			syncAgentService.pushClientUpdateJournals(upgradeClientRequest.getClientId(), updateJournals);
			
			// Push all DeleteJournals for this Client.
			final Collection<String> deleteJournals = accessManager.getClientDeleteJournals(upgradeClientRequest.getClientId(), true);
			syncAgentService.pushClientDeleteJournals(upgradeClientRequest.getClientId(), deleteJournals);
		}
		else
		{
			log.warn("Unable to upgrade client");
		}

		return response;
	}
}

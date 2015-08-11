package com.percero.amqp.handlers;

import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.auth.helpers.IAccountHelper;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.vo.ConnectResponse;
import com.percero.agents.sync.vo.ReconnectRequest;
import com.percero.agents.sync.vo.SyncRequest;
import com.percero.agents.sync.vo.SyncResponse;
import com.percero.amqp.IDecoder;

@Component
public class ReconnectHandler extends SyncMessageHandler {

	public static final String RECONNECT = "reconnect";
	
	public ReconnectHandler() {
		routingKey = RECONNECT;
	}
	
	@Autowired
	IAccessManager accessManager;
	@Autowired
	IAccountHelper accountHelper;
	@Autowired
	IPushSyncHelper pushSyncHelper;
	@Autowired
	AmqpAdmin amqpAdmin;
	@Autowired
	AmqpTemplate template;
	@Autowired
	IDecoder decoder;

	@Override
	public SyncResponse handleMessage(SyncRequest request, String replyTo) throws Exception {
		ReconnectRequest reconnectRequest = (ReconnectRequest) request;
		ConnectResponse reconnectResponse = new ConnectResponse();
		reconnectResponse.setCurrentTimestamp( (new Date()).getTime() );

		if (reconnectRequest == null) {
			log.warn("Invalid reconnect request: NULL");
			return reconnectResponse;
		}
		else {
			log.debug("Handling ReconnectRequest from client " + request.getClientId());
		}
		
		Set<String> existingClientIds = accessManager.findClientByUserIdDeviceId(reconnectRequest.getUserId(), reconnectRequest.getDeviceId());
		if (existingClientIds == null || existingClientIds.isEmpty()) {
			// No existing client IDs, thus this reconnect is not valid.
			log.warn("Invalid reconnect, no existing client IDs for user " + reconnectRequest.getUserId() + ", device " + reconnectRequest.getDeviceId() + ", client " + reconnectRequest.getClientId());
			return reconnectResponse;
		}
		else {
			log.debug((existingClientIds != null ? existingClientIds.size() : 0) + " existingClientIds for client");
		}
		
		// In order for this to be a valid reconnect request, there should be an existing ClientId that matches the request's existing client Id.
		Boolean foundMatchingClientId = false;
		try {
			if (reconnectRequest.getExistingClientIds() == null || reconnectRequest.getExistingClientIds().length == 0) {
				log.warn("Manually setting ExistingClientIds array to ExistingClientId");
				
				if (reconnectRequest.getExistingClientId() == null || reconnectRequest.getExistingClientId().isEmpty()) {
					log.error("Unable to find valid existing ClientID in ReconnectRequest");
					return reconnectResponse;
				}
				reconnectRequest.setExistingClientIds(new String[1]);
				reconnectRequest.getExistingClientIds()[0] = reconnectRequest.getExistingClientId();
			}
		} catch(Exception e) {
			log.error(e);

			if (reconnectRequest.getExistingClientId() == null || reconnectRequest.getExistingClientId().isEmpty()) {
				log.error("Unable to find valid existing ClientID in ReconnectRequest");
				return reconnectResponse;
			}
			reconnectRequest.setExistingClientIds(new String[1]);
			reconnectRequest.getExistingClientIds()[0] = reconnectRequest.getExistingClientId();
		}

		for(int i=0; i<reconnectRequest.getExistingClientIds().length; i++) {
			if ( existingClientIds.contains(reconnectRequest.getExistingClientIds()[i]) ) {
				foundMatchingClientId = true;
				break;
			}
		}
		if ( foundMatchingClientId ) {
			log.debug("RECONNECT: Existing Client IDs Match!");
		}
		else {
			log.error("RECONNECT: Existing Client IDs Do NOT Match...");
			existingClientIds = null;
		}
		
		Principal pUser = accountHelper.authenticateOAuth(reconnectRequest.getRegAppKey(), reconnectRequest.getSvcOauthKey(), reconnectRequest.getUserId(), reconnectRequest.getToken(), reconnectRequest.getClientId(), reconnectRequest.getClientType(), reconnectRequest.getDeviceId(), existingClientIds);
		if (pUser != null) {
			log.debug("Reconnect found valid principal user");
			// The client will only have UpdateJournals and DeleteJournals if the UserDevice exists.
			accessManager.registerClient(reconnectRequest.getClientId(), reconnectRequest.getUserId(), reconnectRequest.getDeviceId(), reconnectRequest.getClientType());
			reconnectResponse.setClientId(reconnectRequest.getClientId());
			reconnectResponse.setDataID(syncAgentService.getDataId(reconnectRequest.getClientId()));
			reconnectResponse.setData("RECONNECT");
			
			final Collection<String> updateJournals = accessManager.getClientUpdateJournals(reconnectRequest.getClientId(), true);
			log.debug((updateJournals != null ? updateJournals.size() : 0) + " updateJournal(s) for Client");
			final Collection<String> deleteJournals = accessManager.getClientDeleteJournals(reconnectRequest.getClientId(), true);
			log.debug((deleteJournals != null ? deleteJournals.size() : 0) + " deleteJournal(s) for Client");
			
			if (!replyTo.equalsIgnoreCase(reconnectRequest.getClientId())) {
				log.debug("Request ClientID does not match replyTo, retrieving Journals for replyTo");
				updateJournals.addAll( accessManager.getClientUpdateJournals(replyTo, true) );
				log.debug((updateJournals != null ? updateJournals.size() : 0) + " updateJournal(s) for Client");
				deleteJournals.addAll( accessManager.getClientDeleteJournals(replyTo, true) );
				log.debug((deleteJournals != null ? deleteJournals.size() : 0) + " deleteJournal(s) for Client");
			}
			
			// Check to see if UserDevice exists.
			Iterator<String> itrExistingClientIds = existingClientIds.iterator();
			while (itrExistingClientIds.hasNext()) {
				String existingClientId = itrExistingClientIds.next();
				if (existingClientId.equalsIgnoreCase(replyTo)) {
					// We have already retrieved the Journals for this client IDs.
					continue;
				}

				// ExistingClientId is different than new clientId, need to transfer all messages from old queue and then remove that queue.
				log.debug("Renaming client " + existingClientId + " to " + replyTo);
				pushSyncHelper.renameClient(existingClientId, replyTo);
				
				// TODO: Do we need to also get updates/deletes for existingClientId?

				// Push all UpdateJournals for this Client.
				updateJournals.addAll( accessManager.getClientUpdateJournals(existingClientId, true) );
				log.debug((updateJournals != null ? updateJournals.size() : 0) + " updateJournal(s) for Client");
				
				// Push all DeleteJournals for this Client.
				deleteJournals.addAll( accessManager.getClientDeleteJournals(existingClientId, true) );
				log.debug((deleteJournals != null ? deleteJournals.size() : 0) + " deleteJournal(s) for Client");
			}

//			syncAgentService.pushClientUpdateJournals(reconnectRequest.getClientId(), updateJournals);
//			syncAgentService.pushClientDeleteJournals(reconnectRequest.getClientId(), deleteJournals);
			syncAgentService.pushClientUpdateJournals(replyTo, updateJournals);
			syncAgentService.pushClientDeleteJournals(replyTo, deleteJournals);
		} else {
			if (existingClientIds == null) {
				// This is a valid case where the client reconnect failed due to invalid credentials
				log.debug("RECONNECT: Unable to get valid Principal User");
			}
			else {
				// This is an unexpected case and probably signifies some issue that needs to be resolved.
				if (!existingClientIds.isEmpty()) {
					log.error("RECONNECT: Existing Client IDs found for client " + reconnectRequest.getClientId() + " but no valid user!");
					log.error("Existing Client IDs: " + StringUtils.collectionToCommaDelimitedString(existingClientIds));
				}
				else {
					log.error("RECONNECT: No Existing Client IDs found for client " + reconnectRequest.getClientId() + " and no valid user");
				}
			}
		}
		
		log.debug("Sending Reconnect Response for Client " + reconnectResponse.getClientId() + " on channel " + replyTo);

		return reconnectResponse;
	}
}

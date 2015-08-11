package com.percero.amqp;

import org.apache.log4j.Logger;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.percero.agents.auth.hibernate.AuthHibernateUtils;
import com.percero.agents.auth.services.IAuthService;
import com.percero.agents.auth.vo.AuthProvider;
import com.percero.agents.auth.vo.AuthRequest;
import com.percero.agents.auth.vo.AuthResponse;
import com.percero.agents.auth.vo.AuthenticateOAuthAccessTokenRequest;
import com.percero.agents.auth.vo.AuthenticateOAuthAccessTokenResponse;
import com.percero.agents.auth.vo.AuthenticateOAuthCodeRequest;
import com.percero.agents.auth.vo.AuthenticateOAuthCodeResponse;
import com.percero.agents.auth.vo.DisconnectRequest;
import com.percero.agents.auth.vo.DisconnectResponse;
import com.percero.agents.auth.vo.OAuthResponse;
import com.percero.agents.auth.vo.OAuthToken;
import com.percero.agents.auth.vo.ValidateUserByTokenRequest;
import com.percero.agents.auth.vo.ValidateUserByTokenResponse;
import com.percero.serial.IDecoder;

/**
 * This class supplies the main method that creates the spring context
 * and then all processing is invoked asynchronously by messaging.
 * 
 * This class' onMessage function will be invoked when the process receives a message from the broker
 * @author Jonathan Samples
 *
 */
//@Component("authAgentListener")
public class AuthAgentListener implements MessageListener{

	@Autowired
	AmqpTemplate template;
	@Autowired
	IDecoder decoder;
	@Autowired
	IAuthService authService;
	
	private static Logger logger = Logger.getLogger("com.percero");

	/**
	 * Message handling function
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	public void onMessage(Message message) {
		Object result = "INVALID DEFAULT VALUE";
		AuthRequest request = null;
		AuthResponse response = null;
		try{
			Object ob = decoder.decode(message.getBody());
			if (ob instanceof AuthRequest)
				request = (AuthRequest) ob;

			String key = message.getMessageProperties().getReceivedRoutingKey();
			//String userId = message.getMessageProperties().getUserId();

			/** Essentially, Re-login **/
			if(key.equals(ValidateUserByTokenRequest.ID) || key.equals("validateUserByToken")){
				if (request instanceof ValidateUserByTokenRequest) {
					ValidateUserByTokenRequest authRequest = (ValidateUserByTokenRequest) request;
					result = authService.validateUserByToken(authRequest.getRegAppKey(), authRequest.getUserId(), authRequest.getToken(), authRequest.getClientId());
					response = new ValidateUserByTokenResponse();
					result = AuthHibernateUtils.cleanObject(result);
					((ValidateUserByTokenResponse) response).setResult((Boolean)result);
				}
			}
			/** For OAuth2 WebApp style **/
			else if(key.equals(AuthenticateOAuthCodeRequest.ID) || key.equals("authenticateOAuthCode")){
				if (request instanceof AuthenticateOAuthCodeRequest) {
					AuthenticateOAuthCodeRequest authRequest = (AuthenticateOAuthCodeRequest) request;
					OAuthToken token = new OAuthToken();
					token.setToken(authRequest.getRequestToken());
					token.setTokenSecret(authRequest.getRequestSecret());
					AuthProvider authProvider = authRequest.getAuthProvider();
					
					result = authService.authenticateOAuthCode(authProvider, authRequest.getCode(), message.getMessageProperties().getReplyTo(), authRequest.getDeviceId(), authRequest.getRedirectUri(), token);
					response = new AuthenticateOAuthCodeResponse();
					
					if (result != null) {
						result = AuthHibernateUtils.cleanObject(result);
						((AuthenticateOAuthCodeResponse) response).setResult(((OAuthResponse)result).userToken);
						((AuthenticateOAuthCodeResponse) response).setAccessToken(((OAuthResponse)result).accessToken);
						((AuthenticateOAuthCodeResponse) response).setRefreshToken(((OAuthResponse)result).refreshToken);
					}
				}
			}
			/** For OAuth2 Installed App style - @DEPRICATED **/
			else if(key.equals(AuthenticateOAuthAccessTokenRequest.ID) || key.equals("authenticateOAuthAccessToken")){
				if (request instanceof AuthenticateOAuthAccessTokenRequest) {
					AuthenticateOAuthAccessTokenRequest authRequest = (AuthenticateOAuthAccessTokenRequest) request;
					AuthProvider authProvider = authRequest.getAuthProvider();
					result = authService.authenticateOAuthAccessToken(authProvider, authRequest.getAccessToken(), authRequest.getRefreshToken(), message.getMessageProperties().getReplyTo(), authRequest.getDeviceId());
					response = new AuthenticateOAuthAccessTokenResponse();
					result = AuthHibernateUtils.cleanObject(result);
					if (result != null) {
						((AuthenticateOAuthAccessTokenResponse) response).setResult(((OAuthResponse)result).userToken);
						((AuthenticateOAuthAccessTokenResponse) response).setAccessToken(((OAuthResponse)result).accessToken);
						((AuthenticateOAuthAccessTokenResponse) response).setRefreshToken(((OAuthResponse)result).refreshToken);
					}
				}
			}
			// This can be removed
//			else if(key.equals("authenticateUserAccount")){
//				if (request instanceof AuthenticateUserAccountRequest) {
//					AuthenticateUserAccountRequest authReqest = (AuthenticateUserAccountRequest) request;
//					result = authService.authenticateUserAccount(authReqest.getRegAppKey(), authReqest.getSvcOauthKey(), authReqest.getUserAccount(), request.getClientId(), authReqest.getDeviceId());
//					response = new AuthenticateUserAccountResponse();
//					result = AuthHibernateUtils.cleanObject(result);
//					((AuthenticateUserAccountResponse) response).setResult((UserToken)result);
//				}
//			}
			// Removable
//			else if(key.equals("getRegisteredApplication")){
//				if (request instanceof GetRegisteredApplicationRequest) {
//					GetRegisteredApplicationRequest authRequest = (GetRegisteredApplicationRequest) request;
//					result = authService.getRegisteredApplication(authRequest.getAppKey());
//					response = new GetRegisteredApplicationResponse();
//					((GetRegisteredApplicationResponse)response).setResult((RegisteredApplication)result);
//				}
//				else
//					result = authService.getAllServiceProviders();
//			}
			// Removable
//			else if(key.equals("getRegAppOAuths")){
//				if (request instanceof GetRegAppOAuthsRequest) {
//					GetRegAppOAuthsRequest authRequest = (GetRegAppOAuthsRequest) request;
//					result = authService.getRegAppOAuths(authRequest.getRegAppKey(), authRequest.getRegAppSecret(), authRequest.getOauthType());
//					response = new GetRegAppOAuthsResponse();
//					((GetRegAppOAuthsResponse)response).setResult((List<ServiceApplicationOAuth>)result);
//				}
//			}
			// Removable
//			else if(key.equals("getOAuthRequestToken")){
//				if (request instanceof GetOAuthRequestTokenRequest) {
//					GetOAuthRequestTokenRequest authRequest = (GetOAuthRequestTokenRequest) request;
//					result = authService.getOAuthRequestToken(authRequest.getRegAppKey(), authRequest.getSvcOauthKey());
//					response = new GetOAuthRequestTokenResponse();
//					((GetOAuthRequestTokenResponse)response).setToken(((OAuthToken)result).getToken());
//					((GetOAuthRequestTokenResponse)response).setTokenSecret(((OAuthToken)result).getTokenSecret());
//					((GetOAuthRequestTokenResponse)response).setExpiresIn(((OAuthToken)result).getExpiresIn());
//				}
//			}
			// Removable
//			else if(key.equals("getAllServiceProviders")){
//				if (request instanceof GetAllServiceProvidersRequest) {
//					result = authService.getAllServiceProviders();
//					response = new GetAllServiceProvidersResponse();
//					((GetAllServiceProvidersResponse)response).setResult((List<ServiceProvider>)result);
//				}
//				else
//					result = authService.getAllServiceProviders();
//			}
			else if(key.equals("disconnectAuth")){
				if (request instanceof DisconnectRequest) {
					response = new DisconnectResponse();
					result = authService.logoutUser(request.getUserId(), request.getToken(), request.getClientId());
					((DisconnectResponse) response).setResult((Boolean)result);
				}
			}
			else if(key.equals("testCall")){
//				result = authService.testCall((String)ob);
			}
			else{
				System.out.println("Unknown Message Type");
			}
		} catch(Exception e){
			logger.error(e.getMessage(), e);
		} finally{
			// Send a message back to the originator
			if (request != null)
			{
				if (response == null)
					response = new AuthResponse();
				response.setClientId(request.getClientId());
				response.setCorrespondingMessageId(request.getMessageId());
				template.convertAndSend(message.getMessageProperties().getReplyTo(), response);
			}
			else
				template.convertAndSend(message.getMessageProperties().getReplyTo(), result);
		}
	}
}

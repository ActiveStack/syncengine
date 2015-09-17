package com.percero.client;

import com.percero.agents.auth.vo.AuthenticationRequest;
import com.percero.agents.auth.vo.AuthenticationResponse;
import com.percero.agents.auth.vo.UserToken;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

import java.util.UUID;

/**
 * Intended to maintain session with the server and listen for
 * push messages.
 */
public class AStackClient {

    /**
     * Some service deps
     */
    private AStackRPCService rpcService;
    private String clientId = UUID.randomUUID().toString();
    private ConnectionFactory amqpConnectionFactory;
    public AStackClient(AStackRPCService rpcService, ConnectionFactory amqpConnectionFactory){
        this.rpcService             = rpcService;
        this.amqpConnectionFactory  = amqpConnectionFactory;
    }

    /**
     * Some internal state
     */
    private UserToken userToken;

    public boolean authenticateAnonymously(){
        AuthenticationRequest request = new AuthenticationRequest();
        request.setAuthProvider("anonymous");
        request.setClientId(clientId);

        AuthenticationResponse response = rpcService.authenticate(request);
        userToken = response.getResult();
        boolean result = (userToken != null);
        if(result){
            setupClientQueueAndStartListening();
        }

        return result;
    }

    /**
     * Sets up the queue to listen for messages to the client (including responses to RPC)
     */
    private SimpleMessageListenerContainer listenerContainer;
    private void setupClientQueueAndStartListening(){
        listenerContainer = new SimpleMessageListenerContainer(amqpConnectionFactory);
        listenerContainer.setQueueNames(clientId);
        listenerContainer.setMessageListener(getMessageListener());
        listenerContainer.start();
    }

    private MessageListener messageListener;
    private MessageListener getMessageListener(){
        if(messageListener == null)
            messageListener = new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    String key = message.getMessageProperties().getReceivedRoutingKey();
                    System.out.println("Got Message from server: "+key);
                }
            };

        return messageListener;
    }
}

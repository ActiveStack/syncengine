package com.percero.amqp;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.cw.CheckChangeWatcherMessage;
import com.percero.agents.sync.metadata.MappedClass;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Created by jonnysamps on 11/23/15.
 */
@Component("checkChangeWatcherListener")
public class CheckChangeWatcherListener implements MessageListener {

    private static Logger logger = Logger.getLogger(CheckChangeWatcherListener.class);

    @Autowired
    ObjectMapper om;

    @Autowired
    IAccessManager accessManager;
    public void setAccessManager(IAccessManager value) {
        accessManager = value;
    }

    @Override
    public void onMessage(Message message) {
        try {
            JsonNode node = om.readTree(message.getBody());
            CheckChangeWatcherMessage checkChangeWatcherMessage = om.readValue(node, CheckChangeWatcherMessage.class);
            accessManager.checkChangeWatchers(checkChangeWatcherMessage.classIDPair,
                    checkChangeWatcherMessage.fieldNames,
                    checkChangeWatcherMessage.params);
        }catch(Exception e){
            logger.error(e.getMessage(), e);
        }
    }
}

package com.percero.agents.sync.jobs;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * @author Collin Brown
 *
 */
@Component
public class UpdateTableRegistry {

    private static Logger logger = Logger.getLogger(UpdateTableRegistry.class);

    @SuppressWarnings("unchecked")
	private List<UpdateTableConnectionFactory> connectionFactories = Collections.synchronizedList(new ArrayList<UpdateTableConnectionFactory>());
    public List<UpdateTableConnectionFactory> getConnectionFactories() {
    	return connectionFactories;
    }
   
    @PostConstruct
    public void init() throws YamlException {
        try {
        	File configFile = new File(UpdateTableRegistry.class.getClassLoader().getResource("updateTables.yml").getFile());
        	YamlReader reader = new YamlReader(new FileReader(configFile));
        	while (true) {
        		UpdateTableConnectionFactory updateTableConnectionFactory = reader.read(UpdateTableConnectionFactory.class);
        		if (updateTableConnectionFactory == null) {
        			break;
        		}
        		
        		try {
					updateTableConnectionFactory.init();
					connectionFactories.add(updateTableConnectionFactory);
				} catch (PropertyVetoException e) {
					logger.warn("Unable to initialize Update Table Connection Factory", e);
				}
        		
        	}
        }
        catch (FileNotFoundException e) {
        	logger.warn("No configuration found for UpdateTables (updateTables.yml), skipping UpdateTables");
		}
    }
}

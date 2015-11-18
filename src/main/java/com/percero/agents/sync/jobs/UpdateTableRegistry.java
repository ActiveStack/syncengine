package com.percero.agents.sync.jobs;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private static UpdateTableRegistry instance;

    @SuppressWarnings("unchecked")
	private List<UpdateTableConnectionFactory> connectionFactories = Collections.synchronizedList(new ArrayList<UpdateTableConnectionFactory>());
    public List<UpdateTableConnectionFactory> getConnectionFactories() {
    	return connectionFactories;
    }

    private Map<String, UpdateTableMapping> tableMap = new HashMap<>();
    public UpdateTableMapping getTableMapping(String tableName){
        return tableMap.get(tableName);
    }

    public UpdateTableRegistry(){
        instance = this;
    }

    public static UpdateTableRegistry getInstance(){
        return instance;
    }

    @Autowired(required=false) @Qualifier("updateTableMapFile")
    private String updateTableMapFile = "updateTableMap.yml";

    @Autowired(required=false) @Qualifier("updateTableConfigFile")
    private String updateTableConfigFile = "updateTables.yml";
   
    @PostConstruct
    public void init() throws YamlException {
        loadConnectionFactories();
		loadTableMappings();
    }

    private void loadTableMappings() throws YamlException{
        try{
            URL ymlUrl = UpdateTableRegistry.class.getClassLoader().getResource(updateTableMapFile);
            if (ymlUrl == null) {
                logger.warn("No table mapping found for UpdateTables ("+updateTableMapFile+"), skipping UpdateTables");
                return;
            }
            File configFile = new File(ymlUrl.getFile());
            YamlReader reader = new YamlReader(new FileReader(configFile));
            while (true) {
                UpdateTableMapping updateTableMapping = reader.read(UpdateTableMapping.class);
                if (updateTableMapping == null) {
                    break;
                }

                updateTableMapping.init();
                tableMap.put(updateTableMapping.tableName, updateTableMapping);
            }
        }catch (FileNotFoundException e) {
            logger.warn("No table mapping found for UpdateTables ("+updateTableMapFile+"), skipping UpdateTables");
        }
    }

    private void loadConnectionFactories() throws YamlException{
        try {
            URL ymlUrl = UpdateTableRegistry.class.getClassLoader().getResource(updateTableConfigFile);
            if (ymlUrl == null) {
                logger.warn("No configuration found for UpdateTables ("+updateTableConfigFile+"), skipping UpdateTables");
                return;
            }
            File configFile = new File(ymlUrl.getFile());
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
            logger.warn("No configuration found for UpdateTables ("+updateTableConfigFile+"), skipping UpdateTables");
        }

    }
}

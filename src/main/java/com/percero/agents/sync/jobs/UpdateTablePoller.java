package com.percero.agents.sync.jobs;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.percero.agents.sync.helpers.PostDeleteHelper;
import com.percero.framework.bl.IManifest;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by jonnysamps on 8/31/15.
 */
@Component
public class UpdateTablePoller {

    private static Logger logger = Logger.getLogger(UpdateTablePoller.class);

    private String[] tableNames = new String[0];
    @Autowired
    @Value("$pf{updateTable.tableNames}")
    public void setTableNames(String val){
        tableNames = val.split(",");
    }

    @Autowired
    UpdateTableProcessorFactory updateTableProcessorFactory;

    public boolean enabled = true;

    /**
     * Run every minute
     */
    @Scheduled(fixedDelay=10000, initialDelay=10000)
    public void pollUpdateTables(){
        if(enabled)
            for(String tableName : tableNames){
                UpdateTableProcessor processor = updateTableProcessorFactory.getProcessor(tableName);
                ProcessorResult result = processor.process();
                if(result.isSuccess()){
                    logger.debug("Update table processor ("+tableName+") finished successfully. Total rows ("+result.getTotal()+")");
                }
                else{
                    logger.warn("Update table processor ("+tableName+") failed. Details:");
                    logger.warn(result);
                }
            }
    }


}

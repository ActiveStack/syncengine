//package com.percero.agents.sync.jobs;
//
//import com.mchange.v2.c3p0.ComboPooledDataSource;
//import org.apache.log4j.Logger;
//import org.hibernate.SessionFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
///**
// * Created by jonnysamps on 8/31/15.
// */
//@Component
//public class UpdateTablePoller {
//
//    private static Logger logger = Logger.getLogger(UpdateTablePoller.class);
//
//    private String[] tableNames = new String[0];
//    @Autowired
//    @Value("$pf{updateTable.tableNames}")
//    public void setTableNames(String val){
//        tableNames = val.split(",");
//    }
//
//
//    @Autowired
//    UpdateTableConnectionFactory connectionFactory;
//
//    /**
//     * Run every minute
//     */
//    @Scheduled(fixedDelay=6000, initialDelay=6000)
//    public void pollUpdateTables(){
//        logger.info("Polling Update Tables...");
//        for(String tableName : tableNames){
//            UpdateTableProcessor processor = new UpdateTableProcessor(tableName, connectionFactory);
//            ProcessorResult result = processor.process();
//            if(result.isSuccess()){
//                logger.debug("Update table processor ("+tableName+") finished successfully. Total rows ("+result.getTotal()+")");
//            }
//            else{
//                logger.warn("Update table processor ("+tableName+") failed. Details:");
//                logger.warn(result);
//            }
//        }
//    }
//
//
//}

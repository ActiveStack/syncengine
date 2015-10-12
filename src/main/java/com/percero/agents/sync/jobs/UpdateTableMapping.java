package com.percero.agents.sync.jobs;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a mapping from a table to a list of classes that
 * are affected by a change in it.
 *
 * Created by jonnysamps on 10/10/15.
 */
public class UpdateTableMapping {

    private static Logger logger = Logger.getLogger(UpdateTableMapping.class);
    public String tableName;
    public List<String> classNames;
    public List<Class> classes = new ArrayList<>();

    /**
     * Look up the classes and cache references to them
     */
    public void init(){
        for(String className: classNames){
            try{
                Class c = Class.forName(className);
                classes.add(c);
            }catch(ClassNotFoundException e){
                logger.warn("Class in updateTableMap.yml not found: "+className);
            }
        }
    }
}

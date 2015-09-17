package com.percero.test.utils;

import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.services.PerceroRedisTemplate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by jonnysamps on 9/14/15.
 */
@Component
public class CleanerUtil {

    @Autowired
    PerceroRedisTemplate redisTemplate;

    @Autowired
    SessionFactory appSessionFactory;

    /**
     * Wipe the database and redis
     */
    public void cleanAll(){
        cleanDB();
        cleanCache();
    }

    @Transactional
    public void cleanDB(){
        Session s = null;
        try {
            s = appSessionFactory.getCurrentSession();
            IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
            for (MappedClass mappedClass : mcm.getAllMappedClasses()) {
                s.createQuery("delete from "+mappedClass.tableName).executeUpdate();
            }
        } catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    public void cleanCache(){
        RedisConnection connection = null;
        try{
            connection = redisTemplate.getConnectionFactory().getConnection();
            connection.flushAll();
        } finally{
            if(connection != null)
                connection.close();
        }
    }
}

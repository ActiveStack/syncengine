package com.percero.agents.sync.jobs;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by jonnysamps on 9/2/15.
 */
@Component
public class UpdateTableConnectionFactory {

    private static Logger logger = Logger.getLogger(UpdateTableConnectionFactory.class);

    @Autowired
    @Value("$pf{updateTable.driverClassName:com.mysql.jdbc.Driver}")
    private String driverClassName;
    public void setDriverClassName(String val){
        this.driverClassName = val;
    }

    @Autowired
    @Value("$pf{updateTable.username}")
    private String username;
    public void setUsername(String val){
        this.username = val;
    }

    @Autowired
    @Value("$pf{updateTable.username}")
    private String password;
    public void setPassword(String val){
        this.password = val;
    }

    @Autowired
    @Value("$pf{updateTable.jdbcUrl:jdbc:mysql://localhost/db}")
    private String jdbcUrl;
    public void setJdbcUrl(String val){
        this.jdbcUrl = val;
    }

    private ComboPooledDataSource cpds;
    @PostConstruct
    public void init() throws PropertyVetoException{
        try {
            cpds = new ComboPooledDataSource();
            cpds.setDriverClass(driverClassName); //loads the jdbc driver
            cpds.setJdbcUrl(jdbcUrl);
            cpds.setUser(username);
            cpds.setPassword(password);

            // the settings below are optional -- c3p0 can work with defaults
            cpds.setMinPoolSize(5);
            cpds.setAcquireIncrement(5);

        }catch(PropertyVetoException pve){
            logger.error(pve.getMessage(), pve);
            throw pve;
        }
    }

    public Connection getConnection() throws SQLException{
        try{
            return cpds.getConnection();
        }catch(SQLException e){
            logger.error(e.getMessage(), e);
            throw e;
        }
    }
}

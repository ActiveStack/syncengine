package com.percero.agents.sync.jobs;

import com.percero.example.Email;
import com.percero.example.Person;
import com.percero.test.utils.AuthUtil;
import com.percero.test.utils.CleanerUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Jonathan Samples<jonnysamps@gmail.com> on 9/4/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/update_table_processor.xml" })
public class UpdateTableProcessorTest {

    @Autowired
    UpdateTableProcessorFactory processorFactory;
    @Autowired
    UpdateTableConnectionFactory connectionFactory;
    @Autowired
    UpdateTablePoller poller;
    @Autowired
    CleanerUtil cleanerUtil;
    @Autowired
    AuthUtil authUtil;

    String tableName = "update_table";

    @Before
    public void before() throws Exception{
        // Disable the poller so it doesn't step on our toes
        poller.enabled = false;
        cleanerUtil.cleanAll();
        try(Connection connection = connectionFactory.getConnection();
            Statement statement = connection.createStatement())
        {
            // Truncate the update table
            String sql = "delete from " + tableName;
            statement.executeUpdate(sql);
        }
    }

    @Test
    public void getClassForTableName_NoTableAnnotation() throws Exception{
        UpdateTableProcessor processor = processorFactory.getProcessor(tableName);
        Class clazz = processor.getClassForTableName("Email");
        Assert.assertEquals(Email.class, clazz);
    }

    @Test
    public void getClassForTableName_TableAnnotation() throws Exception{
        UpdateTableProcessor processor = processorFactory.getProcessor(tableName);
        Class clazz = processor.getClassForTableName("Person");
        Assert.assertEquals(Person.class, clazz);
    }

    @Test
    public void getClassForTableName_NotFound() throws Exception{
        UpdateTableProcessor processor = processorFactory.getProcessor(tableName);
        Class clazz = processor.getClassForTableName("NotAnEntity");
        Assert.assertNull(clazz);
    }

    // Shared setup method
    public void setupThreeRowsInUpdateTable() throws SQLException{
        try(Connection connection = connectionFactory.getConnection();
            Statement statement = connection.createStatement())
        {
            // Add some fixture data
            statement.executeUpdate("insert into " + tableName + "(tableName, rowId, type) values " +
                    "('Email','1','UPDATE')," +
                    "('Person','1','INSERT')," +
                    "('Block','1','DELETE')");
        }
    }

    @Test
    public void getRow() throws Exception {
        setupThreeRowsInUpdateTable();
        UpdateTableProcessor processor = processorFactory.getProcessor(tableName);
        UpdateTableRow row = processor.getRow();

        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getLockId());
        Assert.assertNotNull(row.getLockDate());

        String sql = "select * from "+tableName+" where ID="+row.getID();

        try(Connection connection = connectionFactory.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql))
        {
            Assert.assertTrue(resultSet.next());
            Assert.assertNotNull(resultSet.getInt("lockID"));
            Assert.assertNotNull(resultSet.getDate("lockDate"));
        }
    }


    @Test
    public void processMultipleRows() throws Exception {
        setupThreeRowsInUpdateTable();

        UpdateTableProcessor processor = processorFactory.getProcessor(tableName);
        ProcessorResult result = processor.process();
        Assert.assertEquals(3, result.getTotal());
        Assert.assertEquals(0, result.getNumFailed());
        Assert.assertTrue(result.isSuccess());
        try(Connection connection = connectionFactory.getConnection();
            Statement statement = connection.createStatement())
        {
            String sql = "select count(*) as 'count' from " + tableName;
            ResultSet resultSet = statement.executeQuery(sql);
            Assert.assertTrue(resultSet.next());
            Assert.assertEquals(0, resultSet.getInt("count"));
        }
    }

    @Test
    public void singleRowInsertAllList(){

    }

    @Test
    public void singleRowInsertRefList(){

    }

}

package com.percero.agents.sync.jobs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by Jonathan Samples<jonnysamps@gmail.com> on 9/4/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/update_table_processor.xml" })
public class UpdateTableProcessorTest {
    @Test
    public void test(){
        System.out.println("A Test ran");
    }
}

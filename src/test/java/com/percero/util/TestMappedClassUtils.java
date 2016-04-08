package com.percero.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.percero.example.PostalAddress;

public class TestMappedClassUtils {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetClassFields() {
		List<Field> result = MappedClassUtils.getClassFields(PostalAddress.class);
		
		assertNotNull(result);
		assertEquals(17, result.size());
	}

	@Test
	public void testGetFieldGetters() throws NoSuchFieldException, SecurityException {
		List<Field> fields = MappedClassUtils.getClassFields(PostalAddress.class);
		for(Field field : fields) {
			Method result = MappedClassUtils.getFieldGetters(PostalAddress.class, field);
			assertNotNull(result);
		}
	}

	@Test
	public void testGetFieldSetters() throws NoSuchFieldException, SecurityException {
		List<Field> fields = MappedClassUtils.getClassFields(PostalAddress.class);
		for(Field field : fields) {
			Method result = MappedClassUtils.getFieldGetters(PostalAddress.class, field);
			assertNotNull(result);
		}
	}

}

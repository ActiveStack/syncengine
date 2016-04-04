package com.percero.agents.sync.metadta;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.metadata.MappedFieldPerceroObject;
import com.percero.example.CountryPermit;
import com.percero.example.ExampleManifest;
import com.percero.example.Person;
import com.percero.example.PostalAddress;
import com.percero.example.ShippingAddress;
import com.percero.framework.bl.IManifest;

import ch.qos.logback.classic.turbo.MatchingFilter;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/percero-spring-config.xml" })
public class TestMappedClass {


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
	public void testProcessManifest() {
		IManifest manifest = new ExampleManifest();
		MappedClass.processManifest(manifest);
		
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass personMappedClass = mcm.getMappedClassByClassName(Person.class.getCanonicalName());
		assertNotNull(personMappedClass);
		
		MappedClass countryPermitMappedClass = mcm.getMappedClassByClassName(CountryPermit.class.getCanonicalName());
		assertNotNull(countryPermitMappedClass);
		
		// Check one-to-one relationship on class.
		MappedClass postalAddressMappedClass = mcm.getMappedClassByClassName(PostalAddress.class.getCanonicalName());
		assertNotNull(postalAddressMappedClass);
		
		Map<MappedField, MappedField> nulledOnRemoveFieldReferences = postalAddressMappedClass.getNulledOnRemoveFieldReferences();
		assertNotNull(nulledOnRemoveFieldReferences);
		assertEquals(1, nulledOnRemoveFieldReferences.size());
		
		Map.Entry<MappedField, MappedField> nextEntry = nulledOnRemoveFieldReferences.entrySet().iterator().next();
		MappedField nextKey = nextEntry.getKey();
		assertNotNull(nextKey);
		assertTrue(nextKey instanceof MappedFieldPerceroObject);
		assertEquals("issuerAddress", nextKey.getField().getName());
		assertNotNull(nextKey.getReverseMappedField());
		
		MappedField nextValue = nextEntry.getValue();
		assertNotNull(nextKey);
		assertTrue(nextValue instanceof MappedFieldPerceroObject);
		assertEquals("countryPermit", nextValue.getField().getName());
		assertNotNull(nextValue.getReverseMappedField());
		
		// Check one-to-one relationship on parent class.
		MappedClass shippingAddressMappedClass = mcm.getMappedClassByClassName(ShippingAddress.class.getCanonicalName());
		assertNotNull(shippingAddressMappedClass);
		assertSame(postalAddressMappedClass, shippingAddressMappedClass.parentMappedClass);
		assertTrue(postalAddressMappedClass.getChildMappedClasses().contains(shippingAddressMappedClass));
		
		nulledOnRemoveFieldReferences = shippingAddressMappedClass.getNulledOnRemoveFieldReferences();
		assertNotNull(nulledOnRemoveFieldReferences);
		assertEquals(1, nulledOnRemoveFieldReferences.size());

		nextEntry = nulledOnRemoveFieldReferences.entrySet().iterator().next();
		nextKey = nextEntry.getKey();
		assertNotNull(nextKey);
		assertTrue(nextKey instanceof MappedFieldPerceroObject);
		assertEquals("issuerAddress", nextKey.getField().getName());
		assertNotNull(nextKey.getReverseMappedField());
		
		nextValue = nextEntry.getValue();
		assertNotNull(nextKey);
		assertTrue(nextValue instanceof MappedFieldPerceroObject);
		assertEquals("countryPermit", nextValue.getField().getName());
		assertNotNull(nextValue.getReverseMappedField());
		
		assertSame(postalAddressMappedClass, shippingAddressMappedClass.parentMappedClass);
		assertTrue(postalAddressMappedClass.getChildMappedClasses().contains(shippingAddressMappedClass));
		
		// Checking inheritance.
		for(MappedClass mc : mcm.getAllMappedClasses()) {
			for(MappedClass childClass : mc.getChildMappedClasses()) {
				assertTrue(mc.toManyFields.size() <= childClass.toManyFields.size());
				for(MappedField nextToManyMappedField : mc.toManyFields) {
					boolean matchFound = false;
					for(MappedField nextToManyMappedField2 : childClass.toOneFields) {
						if (nextToManyMappedField.getField().equals(nextToManyMappedField2.getField())) {
							matchFound = true;
							assertSame(nextToManyMappedField.getReverseMappedField(), nextToManyMappedField2.getReverseMappedField());
							break;
						}
					}
					
					assertTrue(matchFound);
				}
				assertTrue(mc.toOneFields.size() <= childClass.toOneFields.size());
				for(MappedField nextToOneMappedField : mc.toOneFields) {
					boolean matchFound = false;
					for(MappedField nextToOneMappedField2 : childClass.toOneFields) {
						if (nextToOneMappedField.getField().equals(nextToOneMappedField2.getField())) {
							matchFound = true;
							assertSame(nextToOneMappedField.getReverseMappedField(), nextToOneMappedField2.getReverseMappedField());
							break;
						}
					}
					
					assertTrue(matchFound);
				}
				
				assertEquals(shippingAddressMappedClass.externalizableFields.size(), postalAddressMappedClass.externalizableFields.size());
				for(MappedField nextExternalizableField : postalAddressMappedClass.externalizableFields) {
					boolean matchFound = false;
					for(MappedField nextExternalizableField2 : shippingAddressMappedClass.externalizableFields) {
						if (nextExternalizableField.getField().equals(nextExternalizableField2.getField())) {
							matchFound = true;
							assertSame(nextExternalizableField.getReverseMappedField(), nextExternalizableField2.getReverseMappedField());
							break;
						}
					}
					
					assertTrue(matchFound);
				}
				
				assertEquals(shippingAddressMappedClass.propertyFields.size(), postalAddressMappedClass.propertyFields.size());
				for(MappedField nextPropertyField : postalAddressMappedClass.propertyFields) {
					boolean matchFound = false;
					for(MappedField nextPropertyField2 : shippingAddressMappedClass.propertyFields) {
						if (nextPropertyField.getField().equals(nextPropertyField2.getField())) {
							matchFound = true;
							assertSame(nextPropertyField.getReverseMappedField(), nextPropertyField2.getReverseMappedField());
							break;
						}
					}
					
					assertTrue(matchFound);
				}
			}
		}
	}

}

package com.percero.example.mo_super;

import java.util.List;

import javax.persistence.MappedSuperclass;
import javax.persistence.SecondaryTable;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.gson.JsonObject;
import com.percero.agents.sync.metadata.MappedClass.MappedClassMethodPair;

@MappedSuperclass
@SecondaryTable(name="ShippingAddress")
/*
*/
public class _Super_ShippingAddress extends com.percero.example.PostalAddress
{
	//////////////////////////////////////////////////////
	// VERSION
	//////////////////////////////////////////////////////
	@Override
	public String classVersion() {
		return "0.0.0";
	}


	//////////////////////////////////////////////////////
	// ID
	//////////////////////////////////////////////////////
	/** Inherits from another Model Object Class, so no ID here. **/
	
	//////////////////////////////////////////////////////
	// Properties
	//////////////////////////////////////////////////////

	//////////////////////////////////////////////////////
	// Source Relationships
	//////////////////////////////////////////////////////

	//////////////////////////////////////////////////////
	// Target Relationships
	//////////////////////////////////////////////////////


	
	//////////////////////////////////////////////////////
	// JSON
	//////////////////////////////////////////////////////
	@Override
	public String retrieveJson(ObjectMapper objectMapper) {
		StringBuilder objectJson = new StringBuilder(super.retrieveJson(objectMapper));

		// Properties
		// Source Relationships
		// Target Relationships
		
		return objectJson.toString();
	}

	@Override
	protected void fromJson(JsonObject jsonObject) {
	    super.fromJson(jsonObject);

		// Properties

		// Source Relationships

		// Target Relationships
	}

	@Override
	protected List<MappedClassMethodPair> getListSetters() {
		List<MappedClassMethodPair> listSetters = super.getListSetters();

		return listSetters;
	}
}
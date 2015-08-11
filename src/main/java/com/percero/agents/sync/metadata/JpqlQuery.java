package com.percero.agents.sync.metadata;

public class JpqlQuery extends MappedQuery {

	@Override
	public void setQuery(String value) {
		int index = value.indexOf("jpql:");
		if (index >= 0)
			value = value.substring(index + 5);
		super.setQuery(value);
	}
}

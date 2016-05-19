package com.percero.agents.auth.vo;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ServiceIdentifier implements Serializable {

	public static final String ACTIVESTACK_USERID = "activestack:userid";
	public static final String EMAIL = "email";
	public static final String GITHUB = "github";
	public static final String GOOGLE = "google";
	public static final String LINKED_ID = "linkedin";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4432059633329174997L;
	
	public ServiceIdentifier(String paradigm, String value) {
		this.paradigm = paradigm;
		this.value = value;
	}
	
	private String value = null;
	private String paradigm = EMAIL;

	public String getParadigm() {
		return paradigm;
	}
	public void setParadigm(String paradigm) {
		this.paradigm = paradigm;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}

package com.percero.agents.auth.vo;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ServiceRole implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4432059633329104997L;
	
	private String serviceProviderId = "0";
	private String id = null;
	private String name = null;
	private Set<String> memberIds = new HashSet<String>();
	private Set<ServiceRole> serviceRoles = new HashSet<ServiceRole>();

	public String getServiceProviderId() {
		return serviceProviderId;
	}
	public void setServiceProviderId(String serviceProviderId) {
		this.serviceProviderId = serviceProviderId;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Set<String> getMemberIds() {
		return memberIds;
	}
	public void setMemberIds(Set<String> memberIds) {
		this.memberIds = memberIds;
	}
	public Set<ServiceRole> getServiceRoles() {
		return serviceRoles;
	}
	public void setServiceRoles(Set<ServiceRole> serviceRoles) {
		this.serviceRoles = serviceRoles;
	}
	
	public Collection<ServiceRole> getInheritedServiceRoles() {
		Collection<ServiceRole> result = new HashSet<ServiceRole>();
		
		if (getServiceRoles() != null) {
			Iterator<ServiceRole> itrServiceRoles = getServiceRoles().iterator();
			while(itrServiceRoles.hasNext()) {
				ServiceRole nextServiceRole = itrServiceRoles.next();
				if (!result.contains(nextServiceRole)) {
					result.add(nextServiceRole);
					result.addAll(nextServiceRole.getInheritedServiceRoles());
				}
			}
		}
		
		return result;
	}
}

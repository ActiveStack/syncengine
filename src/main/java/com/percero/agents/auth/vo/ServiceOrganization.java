package com.percero.agents.auth.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ServiceOrganization implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4932059633329104997L;
	
	private String serviceProviderId = "0";
	private String id = null;
	private String ownerId = null;
	public String getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	private String name = null;
	private String avatarUrl = "";
	private List<ServiceTeam> teams = new ArrayList<ServiceTeam>();
	
	public List<ServiceTeam> getTeams() {
		return teams;
	}
	public void setTeams(List<ServiceTeam> teams) {
		this.teams = teams;
	}
	public String getAvatarUrl() {
		return avatarUrl;
	}
	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}
	public String getGravatarId() {
		return gravatarId;
	}
	public void setGravatarId(String gravatarId) {
		this.gravatarId = gravatarId;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	private String gravatarId = "";
	private String url = "";
	//private Set<String> memberIds = new HashSet<String>();
	//private Set<ServiceOrganization> serviceRoles = new HashSet<ServiceOrganization>();

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
	/*
	public Set<String> getMemberIds() {
		return memberIds;
	}
	public void setMemberIds(Set<String> memberIds) {
		this.memberIds = memberIds;
	}
	public Set<ServiceOrganization> getServiceRoles() {
		return serviceRoles;
	}
	public void setServiceRoles(Set<ServiceOrganization> serviceRoles) {
		this.serviceRoles = serviceRoles;
	}
	
	public Collection<ServiceOrganization> getInheritedServiceRoles() {
		Collection<ServiceOrganization> result = new HashSet<ServiceOrganization>();
		
		if (getServiceRoles() != null) {
			Iterator<ServiceOrganization> itrServiceRoles = getServiceRoles().iterator();
			while(itrServiceRoles.hasNext()) {
				ServiceOrganization nextServiceRole = itrServiceRoles.next();
				if (!result.contains(nextServiceRole)) {
					result.add(nextServiceRole);
					result.addAll(nextServiceRole.getInheritedServiceRoles());
				}
			}
		}
		
		return result;
	}
	*/
}

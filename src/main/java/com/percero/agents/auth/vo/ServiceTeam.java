package com.percero.agents.auth.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ServiceTeam implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4492059633329104997L;
	
	private String serviceProviderId = "0";
	private String id = null;
	private String name = null;
	private String permissions = "";
	private List<ServiceUser> members = new ArrayList<ServiceUser>();
	
	public List<ServiceUser> getMembers() {
		return members;
	}
	public void setMembers(List<ServiceUser> members) {
		this.members = members;
	}
	public String getPermissions() {
		return permissions;
	}
	public void setPermissions(String avatarUrl) {
		this.permissions = avatarUrl;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	private String url = "";

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
}

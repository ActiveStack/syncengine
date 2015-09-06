package com.percero.agents.auth.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5880752591299642651L;
	
	private String authProvider;
	private String id = "";
	private String login = "";
	public String getLogin() {
		return login;
	}
	public void setLogin(String login) {
		this.login = login;
	}
	private String firstName = "";
	private String lastName = "";
	private String name = "";
	private List<String> emails = new ArrayList<String>();
	private String locale = "";
	private String link = "";
	private List<String> roleNames = new ArrayList<String>();
	private Boolean areRoleNamesAccurate = false;
	private Boolean isSupended = false;
	private Boolean isAdmin = false;
	private String accessToken = "";
	private String refreshToken = "";
	private String avatarUrl = "";
	private String gravatarId = "";
	private List<ServiceOrganization> organizations = new ArrayList<ServiceOrganization>();
	
	private List<ServiceIdentifier> identifiers = new ArrayList<ServiceIdentifier>();

	public List<ServiceIdentifier> getIdentifiers() {
		return identifiers;
	}
	public void setIdentifiers(List<ServiceIdentifier> identifiers) {
		this.identifiers = identifiers;
	}
	public List<ServiceOrganization> getOrganizations() {
		return organizations;
	}
	public void setOrganizations(List<ServiceOrganization> organizations) {
		this.organizations = organizations;
	}
	public Boolean getAreRoleNamesAccurate() {
		return areRoleNamesAccurate;
	}
	public void setAreRoleNamesAccurate(Boolean value) {
		this.areRoleNamesAccurate = value;
	}
	public Boolean getIsAdmin() {
		return isAdmin;
	}
	public void setIsAdmin(Boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	public Boolean getIsSupended() {
		return isSupended;
	}
	public void setIsSupended(Boolean isSupended) {
		this.isSupended = isSupended;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<String> getEmails() {
		return emails;
	}
	public void setEmails(List<String> emails) {
		this.emails = emails;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public List<String> getRoleNames() {
		return roleNames;
	}
	public void setRoleNames(List<String> roleNames) {
		this.roleNames = roleNames;
	}
	
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String value) {
		accessToken = value;
	}
	public String getRefreshToken() {
		return refreshToken;
	}
	public void setRefreshToken(String value) {
		refreshToken = value;
	}
	public String getGravatarId() {
		return gravatarId;
	}
	public void setGravatarId(String value) {
		gravatarId = value;
	}
	public String getAvatarUrl() {
		return avatarUrl;
	}
	public void setAvatarUrl(String value) {
		avatarUrl = value;
	}
	public String getAuthProviderID() {
		return authProvider;
	}
	public void setAuthProviderID(String authProviderID) {
		this.authProvider = authProviderID;
	}
}

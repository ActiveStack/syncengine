package com.percero.agents.sync.connectors;

import freemarker.template.Template;

public class HttpConnectorOperation {

	public HttpConnectorOperation() {
		// TODO Auto-generated constructor stub
	}
	
	private String operationName;
	private Template template = null;
	private String templatePath;
	private String protocol;
	private String host;
	private Integer port;
	private String file;
	private String command = "POST";
	
	public String getOperationName() {
		return operationName;
	}
	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	public Template getTemplate() {
		return template;
	}
	public void setTemplate(Template template) {
		this.template = template;
	}
	public String getTemplatePath() {
		return templatePath;
	}
	public void setTemplatePath(String templatePath) {
		this.templatePath = templatePath;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String get) {
		this.command = get;
	}
	

}

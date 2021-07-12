package com.projectronin.integration.demo.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ronin.mdaoc")
public class MdaOcConfiguration {
	private String username;
	private String password;
	private String appName;
	private String appKey;
	private String apiEndpoint;
	private String stsEndpoint;
	private String pdfEndpoint;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getApiEndpoint() {
		return apiEndpoint;
	}

	public void setApiEndpoint(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
	}

	public String getStsEndpoint() {
		return stsEndpoint;
	}

	public void setStsEndpoint(String stsEndpoint) {
		this.stsEndpoint = stsEndpoint;
	}

	public String getPdfEndpoint() {
		return pdfEndpoint;
	}

	public void setPdfEndpoint(String pdfEndpoint) {
		this.pdfEndpoint = pdfEndpoint;
	}

}

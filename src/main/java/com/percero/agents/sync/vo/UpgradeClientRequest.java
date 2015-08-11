package com.percero.agents.sync.vo;

public class UpgradeClientRequest extends SyncRequest {

	private String deviceType;
	public void setDeviceType(String value) {
		deviceType = value;
	}
	public String getDeviceType() {
		return deviceType;
	}
}

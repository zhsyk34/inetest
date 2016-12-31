package com.dnake.smart.core.dict;

import lombok.Getter;

public enum Device {
	GATEWAY(0, "智能网关"), APP(1, "手机应用程序");

	@Getter
	private final int type;
	@Getter
	private final String description;

	Device(int type, String description) {
		this.type = type;
		this.description = description;
	}

	public static Device get(int type) {
		for (Device device : values()) {
			if (device.getType() == type) {
				return device;
			}
		}
		return null;
	}
}
package com.dnake.smart.core.dict;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Result {
	OK("ok", "正确响应"),
	NO("no", "错误响应");

	private String name;
	private String description;

	public static Result get(String name) {
		for (Result result : values()) {
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}
}

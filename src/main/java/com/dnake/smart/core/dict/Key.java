package com.dnake.smart.core.dict;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 指令可能存在的key值,只枚举服务端需要处理的情况,转包情况未列入
 */
@Getter
@AllArgsConstructor
public enum Key {
	ACTION("action", "指令"),
	RESULT("result", "结果"),
	TYPE("clientType", "设备类型"),
	SN("devSN", "网关SN码"),
	VERSION("appVersionNo", "网关版本"),
	UDP_PORT("UDPPort", "网关UDP端口"),
	ERRNO("errno", "错误码"),
	KEY("key", "密钥信息"),
	KEYCODE("keyCode", "密钥值");

	private String name;
	private String description;
}

package com.dnake.smart.core.kit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class JsonKit {

	public static JSONObject map(String str) {
		return JSON.parseObject(str);
	}

	public static String getString(String str, String key) {
		return map(str).getString(key);
	}

	public static int getInt(String str, String key) {
		return map(str).getIntValue(key);
	}

//	public static String action(String command) {
//		return map(command).getString("action");
//	}
//
//	public static int type(String command) {
//		return map(command).getIntValue("clientType");
//	}
//
//	//登录验证信息
//	public static String key(String command) {
//		return map(command).getString("keyCode");
//	}

	public static void main(String[] args) {
		String s = "{\"action\":\"loginReq\",\"clientType\":0,\"devSN\":\"2-1-1-100\",\"UDPPort\":50000}";
//		System.out.println(map(s));
//		System.out.println(action(s));
//
//		JSONObject map = map(s);
		System.out.println(getString(s, "action"));
		System.out.println(getInt(s, "UDPPort"));
	}
}

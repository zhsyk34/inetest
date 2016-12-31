package com.dnake.smart.core.dict;

import io.netty.util.AttributeKey;

/**
 * 登录相关信息
 */
public class SessionAttributeKey {
	//设备类型
	public static final AttributeKey<Device> TYPE = AttributeKey.newInstance(Key.TYPE.getName());
	//网关SN号
	public static final AttributeKey<String> SN = AttributeKey.newInstance(Key.SN.getName());
	//网关请求的UDP端口
	public static final AttributeKey<Integer> UDP_PORT = AttributeKey.newInstance(Key.UDP_PORT.getName());
	//当前连接登录验证码
	public static final AttributeKey<String> KEYCODE = AttributeKey.newInstance(Key.KEYCODE.getName());
	//是否通过验证
	public static final AttributeKey<Boolean> PASS = AttributeKey.newInstance("pass");
}

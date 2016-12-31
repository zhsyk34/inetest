package com.dnake.smart.core.dict;

import com.dnake.smart.core.kit.ValidateKit;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

//	public static void start(String[] args) {
//		String s = "1  ctrlScene  场景控制\n" +
//				"2  ctrlDev  设备控制\n" +
//				"3  readDev  读取设备状态\n" +
//				"4  ctrlDef  防区布撤防控制，单独防区布撤防控制，一键全部防区布撤防控制\n" +
//				"5  readDef  读防区布撤防状态\n" +
//				"6  readIRCode  读取红外遥控器红外码数据，用于识别红外遥控器对应品牌\n" +
//				"7  downIRLib  下载红外码库，将红外码库保存至网关本地\n" +
//				"8  readIRLib  获取网关中已下载红外码库信息\n" +
//				"9  setScene  场景设置，添加和编辑场景，实现场景联动配置\n" +
//				"10  delScene  删除场景，包括场景联动信息\n" +
//				"11  setTimer  设置定时器，添加和编辑定时器功能，实现定时联动配置\n" +
//				"12  delTimer  删除定时器，包括定时联动信息\n" +
//				"13  setDef  配置防区报警联动，即设置防区报警触发后关联的动作\n" +
//				"14  delDef  删除防区，包括防区绑定关系以及防区报警联动关系\n" +
//				"15  setPwd  主锁普通密码设置，添加和编辑普通密码。以及设置密码开锁的联动信息\n" +
//				"16  delPwd  删除主锁普通密码，包括密码信息以及密码开锁联动信息\n" +
//				"17  setTempPwd  主锁临时密码设置，添加主锁临时密码\n" +
//				"18  setFp  主锁指纹设置，添加和编辑指纹，以及设置指纹开锁的联动信息\n" +
//				"19  delFp  删除主锁指纹，包括指纹信息以及指纹开锁的联动信息\n" +
//				"20  readDevList  读取保存在网关中的设备列表\n" +
//				"21  delDevList  删除网关中的设备列表\n" +
//				"22  setDevBind  设置设备绑定关系，即设置手机 App 上设备图标与硬件设备的对应关系\n" +
//				"23  readDevBind  读取网关中的设备绑定列表\n" +
//				"24  setNet  配置网关的网络参数\n" +
//				"25  readNet  读取网关中的网络参数\n" +
//				"26  setBind  对码控制\n" +
//				"27  restartDev  重启设备，用于现场施工配置时，区分不同设备\n" +
//				"28  cmtHeartbeat  心跳包，由网关定时发往云服务器，保持二者有效通讯连接\n" +
//				"29  cmtUnlock  开锁信息推送，即开锁信息主动发给云服务器\n" +
//				"30  cmtAlarm  报警信息推送，即产生报警时主动将报警信息发送给云服务器\n" +
//				"31  getVersion  智能网关获取服务器上最新软件版本信息";
//	}

/**
 * 可能的指令action,,只枚举服务端需要处理的情况,转包情况未列入
 */
@Getter
public enum Action {

	/**
	 * 1.登录相关
	 */
	LOGIN_READY(1, 1, "loginReady", "登录通知"),
	LOGIN_REQ(1, 2, "loginReq", "登录请求"),
	LOGIN_VERIFY(1, 3, "loginVerify", "登录验证"),

	/**
	 * 4.心跳/推送
	 */
	HEART_BEAT(4, 1, "cmtHeartbeat", "登录验证"),
	UNLOCK(4, 1, "cmtUnlock", "开锁信息"),
	ALARM(4, 1, "cmtAlarm", "报警信息"),
	DEV_INFO(4, 1, "cmtDevInfo", "设备信息"),

	//TODO:测试
	TEST(5, 1, "test", "测试");

	private static final Map<String, Action> MAP = new HashMap<>();

	static {
		for (Action action : values()) {
			MAP.put(action.getName(), action);
		}
	}

	private final int type;
	private final int index;
	private final String name;
	private final String description;

	/**
	 * @param type        指令类型:1.登录相关,2.控制命令,3.设置命令,4.推送命令
	 * @param index       指令编号(暂时无用)
	 * @param name        指令名称
	 * @param description 指令描述
	 */
	Action(int type, int index, String name, String description) {
		this.type = type;
		this.index = index;
		this.name = name;
		this.description = description;
	}

	public static Action get(String name) {
		return ValidateKit.isEmpty(name) ? null : MAP.get(name);
	}

}

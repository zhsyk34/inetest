package com.dnake.smart.core.reply;

import lombok.Getter;
import lombok.ToString;

/**
 * 接收到的app请求,保留app请求id以回复
 */
@Getter
@ToString
public class Message {
	private final String src;//来源(app channel id)
	private final String command;//指令内容

	//private String dest;//目标(gateway sn)放置于队列中
	//private String action;//action
	//private byte[] data;//发送数据
	//private boolean send;//是否发送(由队列统一维护)
	//private long time;//开始发送时间(由队列统一维护)

	private Message(String src, String command) {
		this.src = src;
		this.command = command;
	}

	public static Message of(String src, String command) {
		return new Message(src, command);
	}
}
package com.dnake.smart.core.session.udp;

import lombok.Getter;
import lombok.Setter;

/**
 * UDP端口分配信息
 */
@Getter
@Setter
public final class UDPPortRecord {

	private int port;

	/**
	 * 记录登记时间,用以在定时任务中删除过期数据
	 * 防止因网关ip的频繁变动导致的端口占用
	 */
	private long happen;

	private UDPPortRecord(int port, long happen) {
		this.port = port;
		this.happen = happen;
	}

	public static UDPPortRecord instance(int port, long happen) {
		return new UDPPortRecord(port, happen);
	}

	public static UDPPortRecord instance(int port) {
		return instance(port, System.currentTimeMillis());
	}
}

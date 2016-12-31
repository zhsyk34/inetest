package com.dnake.smart.core.session.udp;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;

/**
 * UDP心跳信息
 */
@Getter
@Setter
@Accessors(chain = true)
public final class UDPSession {
	private String ip;
	private int port;
	private String sn;
	private String version;
	private long createTime;

	private UDPSession() {
	}

	public static UDPSession from(InetSocketAddress address) {
		UDPSession session = new UDPSession();
		session.setIp(address.getAddress().getHostAddress());
		session.setPort(address.getPort());
		session.createTime = System.currentTimeMillis();
		return session;
	}
}

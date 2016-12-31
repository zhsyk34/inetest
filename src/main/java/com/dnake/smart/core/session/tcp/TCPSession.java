package com.dnake.smart.core.session.tcp;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.kit.ConvertKit;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 * TCP连接信息
 */
final class TCPSession {
	private static final long MIN_MILL = ConvertKit.from(ConvertKit.from(Config.START_TIME));

	//连接通道
	private final Channel channel;
	//连接的创建时间
	private final long create;

	private final String ip;
	//TCP端口
	private final int port;
	//app请求网关 或 登录网关的sn号
	private String sn;

	private TCPSession(Channel channel, long create) {
		if (channel == null || create < MIN_MILL) {
			throw new RuntimeException("params is invalid.");
		}
		this.channel = channel;
		this.create = create;

		InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
		this.ip = address.getHostString();
		this.port = address.getPort();
	}

	public static TCPSession init(Channel channel) {
		return new TCPSession(channel, System.currentTimeMillis());
	}

	public TCPSession build(String sn) {
		this.sn = sn;
		return this;
	}

	public Channel channel() {
		return channel;
	}

	public long create() {
		return create;
	}

	public String ip() {
		return ip;
	}

}

package com.dnake.smart.core.server.udp;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.dict.Action;
import com.dnake.smart.core.dict.Key;
import com.dnake.smart.core.dict.Result;
import com.dnake.smart.core.kit.JsonKit;
import com.dnake.smart.core.kit.ValidateKit;
import com.dnake.smart.core.log.Category;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.session.udp.UDPSession;
import com.dnake.smart.core.session.udp.UDPSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

/**
 * UDP服务器处理器,接收网关心跳
 */
final class UDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

	private UDPSession validate(DatagramPacket msg) {
		InetSocketAddress address = msg.sender();

		String command = msg.content().toString(CharsetUtil.UTF_8);

		Log.logger(Category.UDP, "接收到[" + address + "]数据:" + command);

		JSONObject map = JsonKit.map(command);
		String action = map.getString(Key.ACTION.getName());
		String sn = map.getString(Key.SN.getName());
		String version = map.getString(Key.VERSION.getName());

		Result result = Result.get(map.getString(Key.RESULT.getName()));

		if (result == Result.OK) {
			Log.logger(Category.UDP, "网关响应登录唤醒");
			return null;
		}

		if (Action.get(action) != Action.HEART_BEAT || ValidateKit.isEmpty(sn) || ValidateKit.isEmpty(version)) {
			Log.logger(Category.UDP, "非法的心跳信息");
			return null;
		}

		return UDPSession.from(address).setSn(sn).setVersion(version);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		UDPSession session = validate(msg);
		if (session != null) {
			UDPSessionManager.append(session);
			UDPSessionManager.awake(msg.sender());
		}
	}

}

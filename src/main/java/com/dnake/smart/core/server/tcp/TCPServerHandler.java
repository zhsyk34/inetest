package com.dnake.smart.core.server.tcp;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.dict.Action;
import com.dnake.smart.core.dict.Device;
import com.dnake.smart.core.dict.Key;
import com.dnake.smart.core.dict.Result;
import com.dnake.smart.core.kit.JsonKit;
import com.dnake.smart.core.log.Category;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.reply.Message;
import com.dnake.smart.core.reply.MessageManager;
import com.dnake.smart.core.session.tcp.TCPSessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 处理除登录以外的请求,登录验证已在此前的
 *
 * @see TCPLoginHandler 中处理
 * <p>
 * 1.网关心跳:直接回复
 * 2.网关推送信息:保存至数据库
 * 3.app控制指令与网关响应信息交由
 * @see MessageManager 统一管理
 */
final class TCPServerHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof String)) {
			return;
		}
		String command = (String) msg;

		JSONObject json = JsonKit.map(command);
		Action action = Action.get(json.getString(Key.ACTION.getName()));
		String result = json.getString(Key.RESULT.getName());

		if (action == null && result == null) {
			Log.logger(Category.EVENT, "无效的指令:\n" + command);
			return;
		}

		Channel channel = ctx.channel();
		String sn = TCPSessionManager.sn(channel);
		Device device = TCPSessionManager.type(channel);

		switch (device) {
			case APP:
				Log.logger(Category.EVENT, "客户端请求,将其添加到消息处理队列...");
				MessageManager.request(sn, Message.of(TCPSessionManager.id(channel), command));
				break;
			case GATEWAY:
				System.err.println("网关接收到数据:" + command);

				//1.心跳
				if (action == Action.HEART_BEAT) {
					Log.logger(Category.EVENT, "网关[" + channel.remoteAddress() + "] 发送心跳");
					JSONObject heartResp = new JSONObject();
					heartResp.put(Key.RESULT.getName(), Result.OK.getName());
					channel.writeAndFlush(heartResp);
					return;
				}

				//2.推送
				if (action != null && action.getType() == 4) {
					Log.logger(Category.EVENT, "网关推送数据,直接保存到数据库");
					MessageManager.save(sn, command);
					return;
				}

				//3.响应请求
				if (result != null) {
					Log.logger(Category.EVENT, "网关回复app的请求,转发...");
					MessageManager.response(sn, command);
				}
				break;
			default:
				break;
		}
	}
}

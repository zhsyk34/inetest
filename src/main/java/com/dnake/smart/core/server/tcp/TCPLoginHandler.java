package com.dnake.smart.core.server.tcp;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.dict.*;
import com.dnake.smart.core.dict.Error;
import com.dnake.smart.core.kit.CodecKit;
import com.dnake.smart.core.kit.JsonKit;
import com.dnake.smart.core.kit.RandomKit;
import com.dnake.smart.core.kit.ValidateKit;
import com.dnake.smart.core.log.Category;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.session.tcp.TCPSessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 处理登录
 */
final class TCPLoginHandler extends ChannelInboundHandlerAdapter {

	//测试统计用
	//private static final AtomicInteger count = new AtomicInteger();

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		//TODO:统计接收到的数据(有效即可,测试用,不验证登录)
		//Log.logger(Category.EVENT, ctx.channel().remoteAddress() + "---valid count:" + count.incrementAndGet());

		if (!(msg instanceof String)) {
			return;
		}
		String command = (String) msg;
		Log.logger(Category.RECEIVE, "command:\n" + command);

		Channel channel = ctx.channel();

		JSONObject json = JsonKit.map(command);
		Action action = Action.get(json.getString(Key.ACTION.getName()));

		//登录请求
		if (action == Action.LOGIN_REQ) {
			ready(channel, json);
			return;
		}

		//登录验证
		String result = JsonKit.getString(command, Key.RESULT.getName());
		String keyCode = JsonKit.getString(command, Key.KEYCODE.getName());
		if (Result.OK.getName().equals(result) && keyCode != null) {
			pass(channel, keyCode);
			return;
		}

		//filter
		if (!TCPSessionManager.pass(channel)) {
			Log.logger(Category.EVENT, "尚未登录,拒绝请求");
			TCPSessionManager.close(channel);
		} else {
			ctx.fireChannelRead(command);
		}

	}

	/**
	 * 验证登录信息
	 * 1.必需信息:clientType+devSn(网关登录为其本身sn号,app登录为其交互的网关sn)
	 * 2.网关额外信息:UDPPort
	 */
	private boolean validate(Channel channel, JSONObject json) {
		Device device = Device.get(json.getIntValue(Key.TYPE.getName()));
		String sn = json.getString(Key.SN.getName());
		if (device == null || ValidateKit.isEmpty(sn)) {
			Log.logger(Category.EVENT, "无效的登录请求(未明确登录类型或表明身份)");
			return false;
		}
		//缓存登录类型及登录身份sn(同时可实现sn与channel的双向绑定)
		TCPSessionManager.type(channel, device);
		TCPSessionManager.sn(channel, sn);

		switch (device) {
			case APP:
				Log.logger(Category.EVENT, "app登录请求");
				return true;
			case GATEWAY:
				Integer apply = json.getIntValue(Key.UDP_PORT.getName());

				if (ValidateKit.invalid(apply)) {
					Log.logger(Category.EVENT, "网关登录请求被拒绝(错误的登录数据)");
					return false;
				}

				Log.logger(Category.EVENT, "网关[" + sn + "]请求登录");
				//将网关额外的登录信息缓存在channel上
				TCPSessionManager.port(channel, apply);
				return true;
			default:
				Log.logger(Category.EVENT, "无效的登录请求(未知的登录类型).");
				return false;
		}

	}

	/**
	 * 处理登录请求(准备阶段)
	 * 1.非法或错误的请求将被拒绝
	 * 2.否则发送验证码
	 */
	private void ready(Channel channel, JSONObject json) {
		JSONObject response = new JSONObject();

		if (!validate(channel, json)) {
			response.put(Key.RESULT.getName(), Result.NO.getName());
			response.put(Key.ERRNO.getName(), Error.PARAMETER.getNo());
			channel.writeAndFlush(response);

			Log.logger(Category.EVENT, "无效的登录请求,拒绝连接");
			TCPSessionManager.close(channel);
			return;
		}

		int group = RandomKit.randomInteger(0, 49);
		int offset = RandomKit.randomInteger(0, 9);
		response.put(Key.ACTION.getName(), Action.LOGIN_VERIFY.getName());
		//本次登录的验证信息
		response.put(Key.KEY.getName(), CodecKit.loginKey(group, offset));
		//缓存本次登录的验证码
		TCPSessionManager.code(channel, CodecKit.loginVerify(group, offset));

		channel.writeAndFlush(response);
	}

	/**
	 * 登录验证码校验
	 * 验证码在此前步骤已被缓存(如果没有则为非法攻击)
	 */
	private boolean validate(Channel channel, String keyCode) {
		//必需信息
		Device device = TCPSessionManager.type(channel);
		String verify = TCPSessionManager.code(channel);
		String sn = TCPSessionManager.sn(channel);
		//网关额外信息
		Integer apply = TCPSessionManager.port(channel);

		//非法登录,关闭连接
		if (device == null || ValidateKit.isEmpty(verify) || ValidateKit.isEmpty(sn) || device == Device.GATEWAY && ValidateKit.invalid(apply)) {
			Log.logger(Category.EVENT, "非法登录");
			return false;
		}

		Log.logger(Category.EVENT, "客户端[" + channel.remoteAddress() + "] 进行登录验证[" + keyCode + "]/[" + verify + "](正确值)");

		Log.logger(Category.EVENT, verify.equals(keyCode) ? "登录验证码校验通过" : "登录验证码错误");
		return verify.equals(keyCode);
	}

	/**
	 * 处理登录请求(验证阶段)
	 */
	private void pass(Channel channel, String keyCode) {
		JSONObject response = new JSONObject();

		if (!validate(channel, keyCode)) {
			response.put(Key.RESULT.getName(), Result.NO.getName());
			response.put(Key.ERRNO.getName(), Error.UNKNOWN.getNo());
			channel.writeAndFlush(response);
			TCPSessionManager.close(channel);
			return;
		}

		//登录通过
		TCPSessionManager.pass(channel, true);

		int allocation = TCPSessionManager.login(channel);

		if (allocation == -1) {
			response.put(Key.RESULT.getName(), Result.NO.getName());
			response.put(Key.ERRNO.getName(), Error.TIMEOUT.getNo());
			channel.writeAndFlush(response);
			TCPSessionManager.close(channel);
			return;
		}

		response.put(Key.RESULT.getName(), Result.OK.getName());

		if (allocation > 0) {
			response.put(Key.UDP_PORT.getName(), allocation);//网关登录
		}
		channel.writeAndFlush(response);
	}
}

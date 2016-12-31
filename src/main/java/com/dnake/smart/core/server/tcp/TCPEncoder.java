package com.dnake.smart.core.server.tcp;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.kit.CodecKit;
import com.dnake.smart.core.kit.ValidateKit;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * TCP服务器发送数据前进行编码(加密等)
 * 支持直接发送 String 或 JSONObject
 */
public final class TCPEncoder extends MessageToByteEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		String result = null;
		if (msg instanceof String) {
			result = (String) msg;
		} else if (msg instanceof JSONObject) {
			result = msg.toString();
		}

		if (ValidateKit.isEmpty(result)) {
			return;
		}
		byte[] data = CodecKit.encode(result);
		out.writeBytes(Unpooled.wrappedBuffer(data));
	}
}

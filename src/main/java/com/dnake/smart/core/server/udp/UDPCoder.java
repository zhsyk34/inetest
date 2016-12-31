package com.dnake.smart.core.server.udp;

import com.dnake.smart.core.dict.Packet;
import com.dnake.smart.core.kit.CodecKit;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;

import java.util.List;

/**
 * UDP编码解码处理器
 */
final class UDPCoder extends MessageToMessageCodec<DatagramPacket, DatagramPacket> {
	@Override
	protected void encode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
		String command = msg.content().toString(CharsetUtil.UTF_8);
		out.add(msg.replace(Unpooled.wrappedBuffer(CodecKit.encode(command))));
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
		ByteBuf buf = msg.content();
		ByteBuf command = buf.slice(Packet.HEADERS.size() + Packet.LENGTH, buf.readableBytes() - Packet.REDUNDANT);
		out.add(msg.replace(CodecKit.decode(command)));
	}
}

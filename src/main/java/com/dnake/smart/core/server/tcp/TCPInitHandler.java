package com.dnake.smart.core.server.tcp;

import com.dnake.smart.core.log.Category;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.session.tcp.TCPSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 监听TCP连接事件
 */
final class TCPInitHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Log.logger(Category.EVENT, ctx.channel().remoteAddress() + " 发起连接");
		TCPSessionManager.init(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Log.logger(Category.EXCEPTION, ctx.channel().remoteAddress() + " 关闭连接");
		TCPSessionManager.close(ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Log.logger(Category.EXCEPTION, ctx.channel().remoteAddress() + " 发生错误");
		TCPSessionManager.close(ctx.channel());
	}

	@Override
	public boolean isSharable() {
		return true;
	}
}

package com.dnake.smart.core.server.udp;

import com.dnake.smart.core.log.Category;
import com.dnake.smart.core.log.Log;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.Getter;

import static com.dnake.smart.core.config.Config.UDP_SERVER_PORT;

/**
 * UDP服务器,主要用于接收、保存心跳信息以唤醒下线网关登录
 */
public final class UDPServer {
	@Getter
	private static volatile boolean started = false;

	@Getter
	private static Channel channel;

	public static void start() {
		if (started) {
			return;
		}

		Bootstrap bootstrap = new Bootstrap();
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			bootstrap.group(group).channel(NioDatagramChannel.class);
			bootstrap.option(ChannelOption.SO_BROADCAST, false);
			bootstrap.handler(new ChannelInitializer<DatagramChannel>() {
				@Override
				protected void initChannel(DatagramChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new UDPCoder());
					pipeline.addLast(new UDPServerHandler());
				}
			});

			channel = bootstrap.bind(UDP_SERVER_PORT).syncUninterruptibly().channel();
			Log.logger(Category.UDP, UDPServer.class.getSimpleName() + " 在端口[" + UDP_SERVER_PORT + "]启动完毕");

			started = true;

			channel.closeFuture().await();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			group.shutdownGracefully();
		}
	}
}

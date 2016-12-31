package old;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;

public class Client {
	private volatile boolean finished = false;
	private Channel channel;

	private volatile boolean started = false;

	public Channel getChannel() {
		if (!started) {
			new Thread(new Start()).start();
		}
		while (!finished) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.err.println("channel init>>>>>>>");
		return channel;
	}

	private class Start implements Runnable {
		@Override
		public void run() {
			if (started) {
				return;
			}
			started = true;
			Bootstrap bootstrap = new Bootstrap();
			EventLoopGroup group = new NioEventLoopGroup();
			try {
				bootstrap.group(group).channel(NioSocketChannel.class);
				bootstrap.option(ChannelOption.TCP_NODELAY, true);

				bootstrap.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new MessageToByteEncoder<Object>() {
							@Override
							protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
								if (msg instanceof JSONObject) {
									out.writeBytes(Unpooled.wrappedBuffer(msg.toString().getBytes()));
								}
								if (msg instanceof ByteBuf) {
									out.writeBytes((ByteBuf) msg);
								}
							}
						});
					}
				});

				ChannelFuture future = bootstrap.connect(Config.LOCAL_HOST, Config.TCP_SERVER_PORT).sync();
				channel = future.channel();
				finished = true;
				channel.closeFuture().sync();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				System.err.println("close");
				group.shutdownGracefully();
			}
		}
	}

}

package kit;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.dict.Action;
import com.dnake.smart.core.dict.Key;
import com.dnake.smart.core.dict.Packet;
import com.dnake.smart.core.dict.Result;
import com.dnake.smart.core.kit.CodecKit;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ALL")
public class UDPClientKit {

	private Channel channel;

	public synchronized void start(String sn) {
		System.out.println("starting...");
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(new NioEventLoopGroup())
				.channel(NioDatagramChannel.class)
				.option(ChannelOption.SO_BROADCAST, false)
				.handler(new ChannelInitializer<DatagramChannel>() {
					@Override
					protected void initChannel(DatagramChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast(new MessageToMessageCodec<DatagramPacket, DatagramPacket>() {
							@Override
							protected void encode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
								String command = msg.content().toString(CharsetUtil.UTF_8);
								byte[] bytes = CodecKit.encode(command);
								out.add(msg.replace(Unpooled.wrappedBuffer(bytes)));
							}

							@Override
							protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
								ByteBuf buf = msg.content();
								ByteBuf command = buf.slice(Packet.HEADERS.size() + Packet.LENGTH, buf.readableBytes() - Packet.REDUNDANT);
								ByteBuf data = CodecKit.decode(command);
								out.add(msg.replace(data));
							}
						});

						pipeline.addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
							@Override
							protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
								String x = msg.content().toString(CharsetUtil.UTF_8);

								//模拟网关被唤醒
								if (x.equals("{\"action\":\"loginReady\"}")) {
									JSONObject json = new JSONObject();
									json.put(Key.RESULT.getName(), Result.OK.getName());
									ctx.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(json.toString().getBytes(CharsetUtil.UTF_8)), msg.sender()));

									new TCPLoginKit().start(0, sn, 50000);
								}
							}
						});
					}
				});

		channel = bootstrap.bind(0).syncUninterruptibly().channel();
		System.err.println("udp client start.");
		heart(sn);
		try {
			channel.closeFuture().await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private void heart(String sn) {
		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleWithFixedDelay(() -> {
			JSONObject json = new JSONObject();
			json.put(Key.ACTION.getName(), Action.HEART_BEAT.getName());
			json.put(Key.VERSION.getName(), "v2.2");
			json.put(Key.SN.getName(), sn);
			ByteBuf buf = Unpooled.wrappedBuffer(json.toString().getBytes(CharsetUtil.UTF_8));
			InetSocketAddress address = new InetSocketAddress(Config.LOCAL_HOST, Config.UDP_SERVER_PORT);
			channel.writeAndFlush(new DatagramPacket(buf, address));
		}, 1, 10, TimeUnit.SECONDS);
	}

}

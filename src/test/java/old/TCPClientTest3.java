package old;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.kit.CodecKit;
import com.dnake.smart.core.kit.JsonKit;
import com.dnake.smart.core.server.tcp.TCPDecoder;
import com.dnake.smart.core.server.tcp.TCPEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import kit.CommandFactory;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试登录
 */
@SuppressWarnings("Duplicates")
public class TCPClientTest3 {
	private static final AttributeKey<Integer> TYPE = AttributeKey.newInstance("type");
	private static final Map<String, Channel> APP = new ConcurrentHashMap<>();
	private static final Map<String, Channel> GATEWAY = new ConcurrentHashMap<>();

	private static final AtomicInteger count = new AtomicInteger();

	public static void start(int type, String sn, int port) {
		Bootstrap bootstrap = new Bootstrap();
		EventLoopGroup group = new NioEventLoopGroup();
		bootstrap.group(group).channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);

		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new TCPDecoder());
				ch.pipeline().addLast(new TCPEncoder());
				ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
					@Override
					public void channelInactive(ChannelHandlerContext ctx) throws Exception {
						System.err.println(">>>>>>>>>>>>>>>>>>>closed.");
					}

					@Override
					public void channelActive(ChannelHandlerContext ctx) throws Exception {
						JSONObject login = CommandFactory.login(type, sn, port);
						ctx.writeAndFlush(login);
						//System.out.println(">>>>>>>>请求登录:" + login + "\n");
						ctx.channel().attr(TYPE).set(type);
					}

					@Override
					public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
						Channel channel = ctx.channel();
						int type = channel.attr(TYPE).get();
						String r = (String) msg;
//						System.out.println((type == 1 ? "客户端" : "网关") + "接收到数据:" + r);

						JSONObject map = JsonKit.map(r);
						//verify
						String key = map.getString("key");
						if (key != null && key.length() == 8) {
							int pos1 = Integer.parseInt(key.substring(2, 4));
							int pos2 = Integer.parseInt(key.substring(6, 8));
							String keycode = CodecKit.loginVerify(pos1, pos2);
							ctx.writeAndFlush(CommandFactory.verify(keycode));
						}
						//ok
						String desc = map.getString("desc");
						if ("ok".equals(map.getString("result"))) {
							if (type == 1) {
								if (desc == null) {
									APP.put(sn, channel);
									synchronized (count) {
										count.incrementAndGet();
										System.out.println("---------发送请求数:" + count.get() + "--------");
//										System.err.println("客户端登录成功,发送请求: " + CommandFactory.ask(count.get()));
//										channel.writeAndFlush(CommandFactory.ask(count.get()));
									}
								} else {
									System.out.println("网关响应:" + msg);
								}
							}
							if (map.getInteger("UDPPort") != null) {
								GATEWAY.put(sn, channel);
								System.err.println("网关登录成功,port:" + map.getInteger("UDPPort"));
							}
						}

						if (type == 0 && map.containsKey("action") && key == null) {
							System.err.println("网关收到请求[" + msg + "] ==> 进行响应: " + CommandFactory.answer(desc));
							channel.writeAndFlush(CommandFactory.answer(desc));
						}

						/*if (type == 1 && map.containsKey("result")) {
							System.out.println("gw response:" + msg);
							channel.writeAndFlush(Command.answer());
						}*/
					}

					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
						cause.printStackTrace();
//						System.out.println("error");
					}
				});
			}
		});

		try {
			Channel channel = bootstrap.connect(Config.LOCAL_HOST, Config.TCP_SERVER_PORT).sync().channel();

			channel.closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			group.shutdownGracefully();
		}
	}

	public static void main(String[] args) {
		//gateway
		ExecutorService service = Executors.newCachedThreadPool();
		for (int i = 0; i < 8; i++) {
			final int k = i;
			service.submit(() -> start(0, "2-1-1-" + (100 + k), 50000));
		}
		//app
		for (int i = 0; i < 4; i++) {
			final int sn = new Random().nextInt(100) % 11;
			System.err.println("gw sn:" + (100 + sn));
			service.submit(() -> start(1, "2-1-1-" + (100 + sn), 50000));
		}
		service.shutdown();
	}
}

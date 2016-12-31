package kit;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.dict.Action;
import com.dnake.smart.core.dict.Key;
import com.dnake.smart.core.dict.Result;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试登录+通讯
 */
@SuppressWarnings("Duplicates")
public class TCPClientKit {
	private static final AttributeKey<Integer> TYPE = AttributeKey.newInstance("type");
	public static final AttributeKey<String> SN = AttributeKey.newInstance("SN");
	public static final Map<String, Channel> APP = new ConcurrentHashMap<>();
	public static final Map<String, Channel> GATEWAY = new ConcurrentHashMap<>();

	private static final AtomicInteger count = new AtomicInteger();

	public void start(int type, String sn, int port) {
		Bootstrap bootstrap = new Bootstrap();
		NioEventLoopGroup group = new NioEventLoopGroup();
		bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true);

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
						//login
						JSONObject login = CommandFactory.login(type, sn, port);
						ctx.writeAndFlush(login);
						ctx.channel().attr(TYPE).set(type);
						ctx.channel().attr(SN).set(sn);
					}

					@Override
					public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
						Channel channel = ctx.channel();
						int type = channel.attr(TYPE).get();
						String r = (String) msg;

						//System.err.println("------------------" + (type == 0 ? "app" : "gateway") + r);

						JSONObject map = JsonKit.map(r);

						String key = map.getString(Key.KEY.getName());
						Action action = Action.get(map.getString(Key.ACTION.getName()));
						Result result = Result.get(map.getString(Key.RESULT.getName()));
						//test
						String desc = map.getString("desc");

						//verify
						if (key != null && key.length() == 8) {
							int pos1 = Integer.parseInt(key.substring(2, 4));
							int pos2 = Integer.parseInt(key.substring(6, 8));
							String keycode = CodecKit.loginVerify(pos1, pos2);
							ctx.writeAndFlush(CommandFactory.verify(keycode));
						}

						//app
						if (type == 1) {
							if (result == Result.OK) {
								if (desc == null) {
									APP.put(sn, channel);
									synchronized (count) {
										count.incrementAndGet();
//										System.err.println("---------发送请求数:" + count.get() + "--------");
										System.err.println("客户端登录成功,向[" + sn + "]发送请求: " + CommandFactory.ask(sn));
										channel.writeAndFlush(CommandFactory.ask(sn));
									}
								} else {
									System.err.println("-----------gateway answer:" + r);
								}
							}
						}

						//gateway
						if (type == 0) {
							//网关接收请求-回复
							if (action != null && key == null) {
								System.out.println("网关收到请求[" + msg + "] ==> 进行响应: " + CommandFactory.answer(desc));
								channel.writeAndFlush(CommandFactory.answer(desc));
							}

							if (result == Result.OK && map.getInteger("UDPPort") != null) {
								GATEWAY.put(sn, channel);
								System.out.println("网关[" + sn + "]登录成功,port:" + map.getInteger("UDPPort"));
							}
						}
					}

					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//						cause.printStackTrace();
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

}

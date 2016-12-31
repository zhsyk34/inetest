package kit;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.dict.Action;
import com.dnake.smart.core.dict.Key;
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

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 测试登录
 */
@SuppressWarnings("ALL")
public class TCPLoginKit {
	private static final AttributeKey<Integer> TYPE = AttributeKey.newInstance("type");

	public void start(int type, String sn, int port) {
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
						System.err.println("请求登录:" + login + "\n");
						ctx.channel().attr(TYPE).set(type);
					}

					@Override
					public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
						int type = ctx.channel().attr(TYPE).get();
						String r = (String) msg;
						System.out.println((type == 1 ? "客户端" : "网关") + "接收到数据:" + r);

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
						if ("ok".equals(map.getString("result"))) {
							if (type == 1) {
								JSONObject test = CommandFactory.answer("");
								System.err.println("客户端登录成功,发送请求:\n" + test);
								ctx.writeAndFlush(test);
							}
							if (map.getInteger("UDPPort") != null) {
								System.err.println("网关登录成功,ip:" + map.getInteger("UDPPort"));
							}
						}

						//测试请求
						Action action = Action.get(map.getString(Key.ACTION.getName()));
						String desc = map.getString("desc");
						if (action == Action.TEST) {
							System.out.println("网关收到请求[" + msg + "] ==> 进行响应: " + CommandFactory.answer(desc));
							ctx.writeAndFlush(CommandFactory.answer(desc));
						}
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
		ExecutorService service = Executors.newCachedThreadPool();
		for (int i = 0; i < 5; i++) {
			final int k = i;
			final int type = new Random().nextInt(100) % 2;
//			service.submit(() -> new start(type, "2-1-1-10" + k, 50000));
		}
		service.shutdown();
	}
}

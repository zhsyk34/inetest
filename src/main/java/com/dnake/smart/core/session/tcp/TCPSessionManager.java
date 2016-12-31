package com.dnake.smart.core.session.tcp;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.dict.Device;
import com.dnake.smart.core.dict.SessionAttributeKey;
import com.dnake.smart.core.kit.ConvertKit;
import com.dnake.smart.core.kit.ThreadKit;
import com.dnake.smart.core.kit.ValidateKit;
import com.dnake.smart.core.log.Category;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.session.udp.UDPSession;
import com.dnake.smart.core.session.udp.UDPSessionManager;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dnake.smart.core.config.Config.TCP_APP_COUNT_PREDICT;
import static com.dnake.smart.core.config.Config.TCP_GATEWAY_COUNT_PREDICT;

/**
 * TCP会话(连接)管理
 */
public final class TCPSessionManager {
	/**
	 * 请求连接(登录后移除)
	 * 一连接即记录,开启线程扫描以关闭超时未登录的连接,故此在登录验证中可不验证登录时间
	 */
	private static final Map<String, TCPSession> ACCEPT_MAP = new ConcurrentHashMap<>();

	/**
	 * 登录的app连接,key为channel默认id
	 */
	private static final Map<String, TCPSession> APP_MAP = new ConcurrentHashMap<>(TCP_APP_COUNT_PREDICT);

	/**
	 * 登录的网关连接,key为网关sn号
	 */
	private static final Map<String, TCPSession> GATEWAY_MAP = new ConcurrentHashMap<>(TCP_GATEWAY_COUNT_PREDICT);

	/**
	 * 释放资源
	 */
	private static void remove(Channel channel) {
		if (channel != null && channel.isOpen()) {
			channel.close();
		}
	}

	/**
	 * 尝试唤醒网关
	 *
	 * @return 是否已唤醒
	 */
	private static boolean awake(String sn) {
		TCPSession session = GATEWAY_MAP.get(sn);
		if (session != null) {
			return true;
		}
		UDPSession udpSession = UDPSessionManager.find(sn);
		if (udpSession == null) {
			Log.logger(Category.EVENT, "网关[" + sn + "]掉线(无udp心跳),无法唤醒");
			return false;
		}

		String ip = udpSession.getIp();
		int chance = 0;//尝试次数

		while (chance < 3 && !GATEWAY_MAP.containsKey(sn)) {
			chance++;
			//网关心跳端口
			UDPSessionManager.awake(ip, udpSession.getPort());
			ThreadKit.await(Config.GATEWAY_AWAKE_CHECK_TIME);
			if (GATEWAY_MAP.containsKey(sn)) {
				return true;
			}
			//服务器分配端口
			int port = PortManager.port(ip, sn);
			if (port >= Config.UDP_CLIENT_MIN_PORT) {
				UDPSessionManager.awake(ip, udpSession.getPort());
			}
			ThreadKit.await(Config.GATEWAY_AWAKE_CHECK_TIME);
		}
		return GATEWAY_MAP.containsKey(sn);
	}

	/**
	 * 分配默认id:accept||app
	 */
	public static String id(Channel channel) {
		return channel.id().asShortText();
	}

	/**
	 * 缓存登录类型
	 */
	public static Channel type(Channel channel, Device device) {
		channel.attr(SessionAttributeKey.TYPE).set(device);
		return channel;
	}

	public static Device type(Channel channel) {
		return channel.attr(SessionAttributeKey.TYPE).get();
	}

	/**
	 * 缓存登录验证码
	 */
	public static Channel code(Channel channel, String code) {
		channel.attr(SessionAttributeKey.KEYCODE).set(code);
		return channel;
	}

	public static String code(Channel channel) {
		return channel.attr(SessionAttributeKey.KEYCODE).get();
	}

	/**
	 * 缓存网关连接信息sn
	 */
	public static Channel sn(Channel channel, String sn) {
		channel.attr(SessionAttributeKey.SN).set(sn);
		return channel;
	}

	public static String sn(Channel channel) {
		return channel.attr(SessionAttributeKey.SN).get();
	}

	/**
	 * 缓存网关连接信息udpPort
	 */
	public static Channel port(Channel channel, int port) {
		channel.attr(SessionAttributeKey.UDP_PORT).set(port);
		return channel;
	}

	public static int port(Channel channel) {
		return ConvertKit.primitive(channel.attr(SessionAttributeKey.UDP_PORT).get());
	}

	/**
	 * 缓存网关登录状态,用于过滤非法连接请求
	 */
	public static Channel pass(Channel channel, boolean pass) {
		channel.attr(SessionAttributeKey.PASS).set(pass);
		return channel;
	}

	public static boolean pass(Channel channel) {
		return ConvertKit.primitive(channel.attr(SessionAttributeKey.PASS).get());
	}

	/**
	 * 转发客户端请求至网关
	 *
	 * @param sn  网关sn
	 * @param msg 客户端的请求指令
	 */
	public static boolean forward(String sn, String msg) {
		TCPSession session = GATEWAY_MAP.get(sn);
		if (session == null) {
			awake(sn);
		}
		session = GATEWAY_MAP.get(sn);
		if (session == null) {
			Log.logger(Category.EVENT, "唤醒网关[" + sn + "]失败");
			return false;
		}
		session.channel().writeAndFlush(msg);
		Log.logger(Category.EVENT, "转发[" + msg + "] ==> 网关[" + sn + "]");
		return true;
	}

	/**
	 * @param id  客户端连接标识
	 * @param msg 网关对客户端请求的响应
	 */
	public static boolean respond(String id, String msg) {
		TCPSession session = APP_MAP.get(id);
		if (session == null) {
			Log.logger(Category.EVENT, "客户端[" + id + "]已下线");
			return false;
		}
		session.channel().writeAndFlush(msg);
		Log.logger(Category.EVENT, "响应客户端[" + id + "]的请求");
		return true;
	}

	/**
	 * 初始连接时保存数据
	 * 默认生成的id不会重复
	 * <p>
	 * 不需要ACCEPT_MAP.remove(id)
	 */
	public static void init(Channel channel) {
		String id = id(channel);
		ACCEPT_MAP.put(id, TCPSession.init(channel));
	}

	/**
	 * 通过验证后处理(调用前已验证channel缓存数据,此处重新验证...)
	 *
	 * @return 分配端口{-1:失败,0:APP,50000+:网关}
	 */
	public static int login(Channel channel) {
		//1.通过登录验证,从 ACCEPT_MAP 中移除(可能存在超时)
		String id = id(channel);
		TCPSession session = ACCEPT_MAP.remove(id);
		if (session == null) {
			Log.logger(Category.EVENT, "登录超时(未及时登录,会话已关闭)");
			return -1;
		}

		//2.登录类型
		Device device = type(channel);
		if (device == null) {
			Log.logger(Category.EVENT, "验证失败(错误的登录信息)");
			return -1;
		}

		String sn = sn(channel);
		switch (device) {
			case APP:
				APP_MAP.put(id, session.build(sn));
				Log.logger(Category.EVENT, "app登录成功");
				return 0;
			case GATEWAY:
				String ip = session.ip();
				int apply = port(channel);

				if (ValidateKit.isEmpty(sn) || ValidateKit.invalid(apply)) {
					Log.logger(Category.EVENT, "验证失败(错误的登录信息)");
					return -1;
				}

				//关闭已存在的连接,防止重复登录
				TCPSession exist = GATEWAY_MAP.remove(sn);
				if (exist != null) {
					Log.logger(Category.EVENT, "关闭[" + sn + "]已有的连接,上次登录时间:" + ConvertKit.from(exist.create()));
					remove(exist.channel());
				}

				//设置sn
				int allocation = PortManager.allocate(sn, ip, apply);
				GATEWAY_MAP.put(sn, session.build(sn));
				Log.logger(Category.EVENT, "网关[" + sn + "]登录成功");
				return allocation;
			default:
				Log.logger(Category.EVENT, "未知设备");
				return -1;
		}
	}

	/**
	 * 关闭连接并删除连接记录
	 * WARN:通过SN查找网关可能查询到"后来的"的连接
	 */
	public static boolean close(Channel channel) {
		if (channel == null || !channel.isOpen()) {
			return true;
		}
		Log.logger(Category.EVENT, "关闭[" + channel.remoteAddress() + "]连接");
		//先直接关闭channel
		remove(channel);
		String id = id(channel);

		//在未登录队列中查找
		TCPSession session = ACCEPT_MAP.remove(id);
		if (session != null) {
			return true;
		}

		Device device = type(channel);
		//尚未登录,应在此前已删除
		if (device == null) {
			Log.logger(Category.EXCEPTION, channel.remoteAddress() + "该连接超时未登录已被关闭");
			return false;
		}

		//已进入登录环节
		switch (device) {
			case APP:
				session = APP_MAP.remove(id);
				if (session == null) {
					Log.logger(Category.EXCEPTION, channel.remoteAddress() + "客户端关闭出错,在app队列中查找不到该连接(可能在线时长已到被移除)");
				}
				return session != null;
			case GATEWAY:
				String sn = sn(channel);
				if (ValidateKit.isEmpty(sn)) {
					Log.logger(Category.EXCEPTION, channel.remoteAddress() + "网关关闭出错,非法的登录数据");
					return false;
				}
				//网关队列key为sn,可能被后来的连接所覆盖
				session = GATEWAY_MAP.get(sn);
				if (session == null) {
					Log.logger(Category.EXCEPTION, channel.remoteAddress() + " 网关关闭出错,在网关队列中查找不到该连接(可能在线时长已到被移除)");
					return false;
				}
				if (session.channel() == channel) {
					Log.logger(Category.EXCEPTION, channel.remoteAddress() + " 该网关已重新上线");
				} else {
					GATEWAY_MAP.remove(sn);
				}
				return true;
			default:
				Log.logger(Category.EXCEPTION, "关闭出错,非法的登录数据");
				return false;
		}
	}

	/**
	 * 启动线程扫描并移除
	 * 1.登录超时的连接
	 * 2.在线超时的连接
	 */
	public static void monitor() {
		//TODO:分别扫描
		Log.logger(Category.EVENT, "共有[" + GATEWAY_MAP.size() + "]个网关在线");
//		GATEWAY_MAP.forEach((sn, ch) -> System.out.print(sn + "  "));
		Log.logger(Category.EVENT, "共有[" + APP_MAP.size() + "]个APP在线");
//		Runnable task = () -> {
//			Log.logger(Category.EVENT, "TCP扫描任务开始执行...");
//			//登录超时
//			Log.logger(Category.EVENT, "当前未登录连接:[" + ACCEPT_MAP.size() + "]");
//			ACCEPT_MAP.forEach((id, baseSession) -> {
//				if (!ValidateKit.time(baseSession.createTime, Config.LOGIN_TIME_OUT)) {
//					Log.logger(Category.EVENT, "超时未登录");
//					ACCEPT_MAP.remove(id);
//					remove(baseSession.channel);
//				}
//			});
//
//			//app在线超时
//			Log.logger(Category.EVENT, "当前APP连接:[" + APP_MAP.size() + "]");
//			APP_MAP.forEach((id, appSession) -> {
//				if (!ValidateKit.time(appSession.createTime, APP_TIME_OUT)) {
//					Log.logger(Category.EVENT, "APP在线时长已到,移除!");
//					APP_MAP.remove(id);
//					remove(appSession.channel);
//				}
//			});
//
//			//gateway在线超时:Iterator 获取最新数据
//			Log.logger(Category.EVENT, "当前网关连接:[" + GATEWAY_MAP.size() + "]");
//			Iterator<Map.Entry<String, TCPGatewaySession>> iterator = GATEWAY_MAP.entrySet().iterator();
//			while (iterator.hasNext()) {
//				TCPGatewaySession gatewaySession = iterator.next().getValue();
//				if (!ValidateKit.time(gatewaySession.createTime, GATEWAY_TIME_OUT)) {
//					iterator.remove();
//					remove(gatewaySession.channel);
//				}
//
//			}
//
//			GATEWAY_MAP.forEach((sn, appSession) -> {
//				if (!ValidateKit.time(appSession.createTime, GATEWAY_TIME_OUT)) {
//					Log.logger(Category.EVENT, "网关在线时长已到,移除!");
//					APP_MAP.remove(sn);
//					remove(appSession.channel);
//				}
//			});
//		};

	}

}

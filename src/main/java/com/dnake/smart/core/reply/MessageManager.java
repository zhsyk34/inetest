package com.dnake.smart.core.reply;

import com.dnake.smart.core.kit.ValidateKit;
import com.dnake.smart.core.log.Category;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.session.tcp.TCPSessionManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dnake.smart.core.config.Config.MESSAGE_SEND_AWAIT;

/**
 * TODO:同步
 */
public final class MessageManager {
	/**
	 * app请求消息处理队列
	 */
	private static final Map<String, MessageQueue> APP_REQUEST = new ConcurrentHashMap<>();

	/**
	 * 网关推送消息处理队列
	 */
	private static final List<Record> GATEWAY_PUSH = new CopyOnWriteArrayList<>();

	/**
	 * 将app请求添加到消息处理队列
	 *
	 * @param sn      app请求的网关
	 * @param message app请求的相关信息
	 * @return 是否受理
	 */
	public static boolean request(String sn, Message message) {
		final MessageQueue queue;
		synchronized (APP_REQUEST) {
			if (APP_REQUEST.containsKey(sn)) {
				queue = APP_REQUEST.get(sn);
			} else {
				queue = MessageQueue.instance();
				APP_REQUEST.put(sn, queue);
			}
		}
		return queue.offer(message);
	}

	/**
	 * 网关响应app请求后,将其请求从相应的队列中移除
	 *
	 * @param sn       受理的网关
	 * @param response 网关的回复
	 */
	public static void response(String sn, String response) {
		MessageQueue queue = APP_REQUEST.get(sn);

		Message message = queue.poll();
		if (message == null) {
			Log.logger(Category.EVENT, "消息队列已被清空(网关异常导致响应超时)");
		} else {
			TCPSessionManager.respond(message.getSrc(), response);
		}
	}

	/**
	 * TODO
	 * 保存网关推送的数据
	 */
	public static void save(String sn, String command) {
		GATEWAY_PUSH.add(new Record(sn, command));
	}

	/**
	 * 处理app消息队列
	 */
	public static void process() {
		AtomicInteger count = new AtomicInteger();

		APP_REQUEST.forEach((sn, queue) -> {
			count.addAndGet(queue.getQueue().size());
			Message message = queue.peek();
			if (message != null) {
				TCPSessionManager.forward(sn, message.getCommand());
			}
		});

		Log.logger(Category.EVENT, "开始处理消息队列,共[" + count.get() + "]条");
	}

	/**
	 * TODO:推送至web服务器
	 * 持久化数据
	 */
	public static void persistent() {
		GATEWAY_PUSH.forEach(record -> {
			System.out.println(record.getSn());
			System.out.println(record.getCommand());
		});
	}

	/**
	 * TODO:关闭连接同时移除队列中所有数据并进行回复
	 * 移除响应时间超时(自发送起**秒内未及时回复)的消息
	 */
	public static void monitor() {
		APP_REQUEST.forEach((sn, queue) -> {
			if (queue.isSend()) {
				if (!ValidateKit.time(queue.getTime(), MESSAGE_SEND_AWAIT)) {
					Log.logger(Category.EXCEPTION, "消息响应超时,关闭连接");
					queue.poll();
				}
			}
		});
	}
}

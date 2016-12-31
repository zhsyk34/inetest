package com.dnake.smart.core.reply;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 网关待处理的消息队列
 */
@Getter
@Setter
final class MessageQueue {
	private final BlockingQueue<Message> queue;
	private volatile boolean send;
	private volatile long time;

	private MessageQueue() {
		queue = new LinkedBlockingQueue<>();
		this.reset();
	}

	static MessageQueue instance() {
		return new MessageQueue();
	}

	/**
	 * 重置队列状态
	 */
	private synchronized MessageQueue reset() {
		this.send = false;
		this.time = -1;
		return this;
	}

	/**
	 * 当队列数据被处理时开启警戒状态以进行监测
	 */
	private synchronized MessageQueue guard() {
		this.send = true;
		this.time = System.currentTimeMillis();
		return this;
	}

	/**
	 * 添加数据
	 */
	boolean offer(Message message) {
		return message != null && queue.offer(message);
	}

	/**
	 * 查看队列首元素是否正被处理,如是则不进行任何操作,否则取出并进入警戒状态
	 */
	synchronized Message peek() {
		if (send) {
			return null;
		}
		Message message = queue.peek();
		if (message != null) {
			guard();
		}
		return message;
	}

	/**
	 * 移除已处理完的数据
	 */
	synchronized Message poll() {
		Message message = queue.poll();
		reset();
		return message;
	}
}

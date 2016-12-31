package com.dnake.smart.core.log;

import io.netty.handler.logging.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

import static com.dnake.smart.core.config.Config.LOGGER_CAPACITY;

/**
 * 日志管理
 */
public class Log {
	private final static String name = Log.class.getSimpleName();

	/**
	 * 待写入日志队列
	 */
	private final static LinkedBlockingQueue<Content> queue;

	/**
	 * TCP日志记录器
	 */
	private final static Logger receiveLogger;
	private final static Logger sendLogger;
	private final static Logger exceptionLogger;
	private final static Logger eventLogger;

	/**
	 * UDP日志记录
	 */
	private final static Logger udpLogger;

	static {
		queue = new LinkedBlockingQueue<>(LOGGER_CAPACITY);

		receiveLogger = LoggerFactory.getLogger(name + ".receive");
		sendLogger = LoggerFactory.getLogger(name + ".send");
		exceptionLogger = LoggerFactory.getLogger(name + ".exception");
		eventLogger = LoggerFactory.getLogger(name + ".event");
		udpLogger = LoggerFactory.getLogger(name + ".udp");

		//execute();
	}

	private static void write(Logger logger, LogLevel level, String message) {
		switch (level) {
			case ERROR:
				logger.error(message);
				break;
			case WARN:
				logger.warn(message);
				break;
			case INFO:
				logger.info(message);
				break;
			case DEBUG:
				logger.debug(message);
				break;
			case TRACE:
				logger.trace(message);
				break;
			default:
				break;
		}
	}

	private static void execute() {
		if (queue.size() > 0) {
			try {
				Content content = queue.take();
				Category category = content.getCategory();
				LogLevel level = content.getLevel();
				String message = content.getMessage().toString();
				switch (category) {
					case RECEIVE:
						write(receiveLogger, level, message);
						break;
					case SEND:
						write(sendLogger, level, message);
						break;
					case EXCEPTION:
						write(exceptionLogger, level, message);
						break;
					case EVENT:
						write(eventLogger, level, message);
						break;
					case UDP:
						write(udpLogger, level, message);
						break;
					default:
						break;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				write(exceptionLogger, LogLevel.ERROR, e.getMessage());
			}
		}
	}

	public static void logger(Category category, String message) {
		logger(category, null, message);
	}

	/**
	 * @param category 日志类型
	 * @param level    日志级别
	 * @param message  日志内容
	 */
	public static void logger(Category category, LogLevel level, Object message) {
		if (message == null) {
			return;
		}
		if (level == null) {
			switch (category) {
				case EVENT:
					level = LogLevel.WARN;
					break;
				case EXCEPTION:
					level = LogLevel.ERROR;
					break;
				case RECEIVE:
					level = LogLevel.INFO;
					break;
				case SEND:
					level = LogLevel.INFO;
					break;
				case UDP:
					level = LogLevel.INFO;
					break;
			}
		}
		try {
			queue.add(new Content(category, level, message));
			execute();
		} catch (IllegalStateException e) {
			System.err.println("日志队列已满...");
		}
	}

	/**
	 * 日志内容
	 */
	@Getter
	@AllArgsConstructor
	private static class Content {
		private Category category;
		private LogLevel level;
		private Object message;
	}

}

package com.dnake.smart.core.entry;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.kit.ThreadKit;
import com.dnake.smart.core.log.Category;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.reply.MessageManager;
import com.dnake.smart.core.server.tcp.TCPServer;
import com.dnake.smart.core.server.udp.UDPServer;
import com.dnake.smart.core.session.tcp.PortManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Entry {

	public static void start() {
		//TODO:从数据加载网关端口使用数据
		PortManager.load();

		//TCP服务器
		ExecutorService tcpService = Executors.newSingleThreadExecutor();
		tcpService.submit(TCPServer::start);
		while (!TCPServer.isStarted()) {
			Log.logger(Category.EVENT, TCPServer.class.getSimpleName() + " 正在启动...");
			ThreadKit.await(Config.SERVER_START_MONITOR_TIME);
		}

		//UDP服务器
		ExecutorService udpService = Executors.newSingleThreadExecutor();
		udpService.submit(UDPServer::start);
		while (!UDPServer.isStarted()) {
			Log.logger(Category.EVENT, UDPServer.class.getSimpleName() + " 正在启动...");
			ThreadKit.await(Config.SERVER_START_MONITOR_TIME);
		}

		//TODO
		ScheduledExecutorService service = Executors.newScheduledThreadPool(8);
		//端口回收
		//service.scheduleWithFixedDelay(PortManager::reduce, 1, UDP_PORT_COLLECTION_SCAN_FREQUENCY, TimeUnit.SECONDS);
		//TCP连接监控
		//service.scheduleWithFixedDelay(TCPSessionManager::monitor, 1, TCP_TIME_OUT_SCAN, TimeUnit.SECONDS);
		//UDP连接监控
		//service.scheduleWithFixedDelay(UDPSessionManager::monitor, 1, UDP_ONLINE_SCAN_FREQUENCY, TimeUnit.SECONDS);
		//消息处理和监控
		service.scheduleWithFixedDelay(MessageManager::monitor, 1, 10, TimeUnit.SECONDS);
		service.scheduleWithFixedDelay(MessageManager::process, 1, 10, TimeUnit.SECONDS);
		//service.scheduleWithFixedDelay(MessageManager::persistent, 1, 5, TimeUnit.SECONDS);
		//TODO:日志处理
	}

}

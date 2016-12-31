package test;

import kit.CommandFactory;
import kit.TCPClientKit;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TCPClientTest {

	public static void main(String[] args) {
		//gateway
		ExecutorService service = Executors.newCachedThreadPool();
		for (int i = 0; i < 3; i++) {
			final int k = i;
			service.submit(() -> new TCPClientKit().start(0, "2-1-1-" + (100 + k), 50000));
		}

		//app
		for (int i = 0; i < 1; i++) {
			final int sn = new Random().nextInt(100) % 2;
			service.submit(() -> new TCPClientKit().start(1, "2-1-1-" + (100 + sn), 50000));
		}
		service.shutdown();

		//定时发送请求
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(() -> TCPClientKit.APP.forEach((id, channel) -> {
			String sn = channel.attr(TCPClientKit.SN).get();
			channel.writeAndFlush(CommandFactory.ask(sn));
		}), 10, 10, TimeUnit.SECONDS);
	}
}

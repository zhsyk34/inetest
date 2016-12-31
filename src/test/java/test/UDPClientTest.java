package test;

import kit.UDPClientKit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UDP client
 */
public class UDPClientTest {

	public static void main(String[] args) {
		ExecutorService service = Executors.newCachedThreadPool();

		for (int i = 0; i < 20; i++) {
			final int k = i;
			if (i == 3) {
				continue;
			}
			service.submit(() -> new UDPClientKit().start("2-1-1-" + (100 + k)));
		}

		service.shutdown();
	}
}

package old;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import kit.CommandFactory;

import static com.dnake.smart.core.kit.CodecKit.encode;

/**
 * tcp测试:发送夹杂垃圾数据,测试解码
 */
//TODO AtomicIntegerFieldUpdater
public class TCPClientTest1 {

	//有效数据
	private static ByteBuf fine(String command) {
		return Unpooled.copiedBuffer(encode(command));
	}

	//垃圾数据
	private static ByteBuf mix(String command, int index, byte... b) {
		byte[] src = encode(command);
		//正常数据
		byte[] result = new byte[src.length + b.length];
		System.arraycopy(src, 0, result, 0, index);
		System.arraycopy(b, 0, result, index, b.length);
		System.arraycopy(src, index, result, index + b.length, src.length - index);

		return Unpooled.copiedBuffer(result);
	}

	/**
	 * @param total 总共发送记录数
	 * @param count 有效记录数
	 * @return 发送内容
	 */
	private static ByteBuf content(int total, AtomicInteger count) {
		try {
			String cmd = CommandFactory.login(0, "2-1-1-1", 50000).toString();
			int maxLen = cmd.getBytes().length;

			ByteBuf buf = Unpooled.buffer();
			for (int i = 0; i < total; i++) {
				int r = (int) (Math.random() * 21);
				if (r < 16) {
					buf.writeBytes(fine(CommandFactory.login(0, "2-1-1-1", 50000 + r).toString()));
					count.incrementAndGet();
				} else {
					int p = (int) (Math.random() * maxLen);
					//TODO
					//buf.writeBytes(mix(cmd, p, (byte) 0xa5, (byte) 1, (byte) 2, (byte) 3, (byte) 0x5a));
					buf.writeBytes(mix(cmd, 4, (byte) 1));
				}
			}
			return buf;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 发送任务
	 */
	public static class SendTask implements Callable<Integer> {
		private AtomicInteger count = new AtomicInteger();
		private Channel channel;
		private int total;

		public SendTask(Channel channel, int total) {
			this.channel = channel;
			this.total = total;
		}

		@Override
		public Integer call() throws Exception {
			ByteBuf content = content(total, count);
			if (content != null) {
				channel.writeAndFlush(content);
			}
			return count.get();
		}
	}

	public static void main(String[] args) throws Exception {
		sends(1, 2);
	}

	/**
	 * @param clients client count
	 * @param total   total msg count
	 */
	public static void sends(int clients, int total) {
		System.out.println("------------answer start---------------");
		List<Channel> channels = new ArrayList<>(clients);
		for (int i = 0; i < clients; i++) {
			channels.add(new Client().getChannel());
		}
		AtomicInteger all = new AtomicInteger();
		System.out.println("-----------init channel success-------------");
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleWithFixedDelay(() -> {
			try {
				List<Future<Integer>> futures = new ArrayList<>(clients);
				ExecutorService service = Executors.newCachedThreadPool();
				System.out.println("------------begin------------");
				for (int i = 0; i < clients; i++) {
					Future<Integer> future = service.submit(new SendTask(channels.get(i), total));
					futures.add(future);
				}
				service.shutdown();

				boolean r = false;
				while (!r) {
					r = true;
					for (Future future : futures) {
						if (!future.isDone()) {
							r = false;
							break;
						}
					}
				}
				int count = 0;
				for (Future<Integer> future : futures) {
					count += future.get();
				}
				all.addAndGet(count);
				System.out.println("------------end------------send valid message count[" + count + "]");
				System.out.println("------------all count------------[" + all + "]\n");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 1, 3, TimeUnit.SECONDS);
	}

}

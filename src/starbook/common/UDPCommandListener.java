package starbook.common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * Listen on the network for UDP packets. When receiving a new packet, use the specified command
 * handler factory to create a new command handler object, pass it the network data (packet or
 * socket), and start the handler thread.
 *
 * @author Josh Endries (josh@endries.org)
 *
 */
public class UDPCommandListener implements Runnable {
	private static final Logger log = Logger.getLogger(UDPCommandListener.class);
	private final DatagramSocket serverSocket;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final CommandHandlerFactory factory;

	public UDPCommandListener(InetSocketAddress address, CommandHandlerFactory factory) throws IOException {
		super();
		serverSocket = new DatagramSocket(address);
		serverSocket.setSoTimeout(5000);
		this.factory = factory;
	}

	@Override
	public void run() {
		log.debug("Waiting for packets on "+serverSocket.getLocalAddress()+":"+serverSocket.getLocalPort());
		boolean running = true;

		while (running) {
			try {
				/*
				 * Wait for a connection.
				 */
				byte[] buffer = new byte[2048];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				serverSocket.receive(packet);

				/*
				 * Get a handler for the connection and handle the command.
				 */
				CommandHandler ch = factory.getHandler(packet);
				executor.execute(ch);
			} catch (SocketTimeoutException e) {
				/*
				 * This is normal, and allows is to check if we're supposed to shut down.
				 */
				if (Thread.currentThread().isInterrupted()) {
					running = false;
				}
			} catch (IOException e) {
				/*
				 * Log the error and continue listening. Hopefully.
				 */
				e.printStackTrace();
			}
		}
		
		stopThreads();
	}

	public void stopThreads() {
		executor.shutdown();
		while (!executor.isTerminated()) {
			log.debug("Waiting for thread pool to shut down...");
			try {
				executor.awaitTermination(15, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

package starbook.monitor;

import java.net.DatagramPacket;
import java.net.Socket;

/**
 * Creates a CommandHandler factory that creates a handler when given a TCP socket or UDP packet. 
 *
 * @author Josh Endries (josh@endries.org)
 *
 */
public class CommandHandlerFactory implements starbook.common.CommandHandlerFactory {
	private final Monitor monitor;
	
	public CommandHandlerFactory(Monitor monitor) {
		this.monitor = monitor;
	}

	@Override
	public CommandHandler getHandler(Socket socket) {
		CommandHandler ch = new CommandHandler(monitor);
		ch.setSocket(socket);
		return ch;
	}

	@Override
	public starbook.common.CommandHandler getHandler(DatagramPacket packet) {
		CommandHandler ch = new CommandHandler(monitor);
		ch.setPacket(packet);
		return ch;
	}
}

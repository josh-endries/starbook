package starbook.nodes.index;

import java.net.DatagramPacket;
import java.net.Socket;

public class CommandHandlerFactory implements starbook.common.CommandHandlerFactory {
	private final IndexNode node;
	
	public CommandHandlerFactory(IndexNode node) {
		this.node = node;
	}

	@Override
	public CommandHandler getHandler(Socket socket) {
		CommandHandler ch = new CommandHandler(node);
		ch.setSocket(socket);
		return ch;
	}

	@Override
	public starbook.common.CommandHandler getHandler(DatagramPacket packet) {
		CommandHandler ch = new CommandHandler(node);
		ch.setPacket(packet);
		return ch;
	}
}

package starbook.nodes.worker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import starbook.common.BaseCommandHandler;
import starbook.common.CK;
import starbook.common.Command;
import starbook.common.Command.Type;
import starbook.common.Configuration;
import starbook.common.Edge;
import starbook.common.Message;

public class CommandHandler extends BaseCommandHandler {
	private final static Logger log = Logger.getLogger(CommandHandler.class);
	private final WorkerNode node;

	public CommandHandler(WorkerNode node) {
		this.node = node;
	}

	@Override
	public void processCommand(Command command) {
		Map<String, Object> data = command.getData();
		InetAddress source = (InetAddress) data.get("source");
		log.debug(String.format("Received %s from %s.", command, source));
		switch (command.getType()) {
			case MessageRequest: {
				/*
				 * Send back a MessageUpload with the message.
				 */
				String guid = (String) data.get("guid");
				Message message = node.getPublishedMessageStore().getMessageByGUID(guid);
				if (message != null) {
					Map<String, Object> responseData = new HashMap<String, Object>(1);
					responseData.put("message", message);
					responseData.put("source", node.getInetAddress());
					Command c2 = new Command(Type.MessageUpload, responseData);
					try {
						c2.sendViaUDP(new InetSocketAddress(source, Configuration.getInt(CK.CommandPort)));
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					log.warn("Request to download a message that doesn't exist: " + guid);
				}
				break;
			}
			case MessageUpload: {
				/*
				 * Add the message to our message store.
				 */
				Message message = (Message) data.get("message");
				node.getStoredMessageStore().addMessage(message);

				/*
				 * Tell the monitor.
				 */
				String name = "mr"+node.getInetAddress()+source;
				Edge e = new Edge(name, node.getInetAddress(), source);
				Map<String, Object> monitorData = new HashMap<String, Object>();
				monitorData.put("edge", e);
				monitorData.put("source", node.getInetAddress());
				Command monitorCommand = new Command(Command.Type.ToggleEdge, monitorData);
				try {
					monitorCommand.sendViaUDP(new InetSocketAddress(node.getMonitorAddress(), Configuration.getInt(CK.CommandPort)));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				break;
			}
		}
	}
}

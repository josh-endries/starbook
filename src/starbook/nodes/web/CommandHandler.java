package starbook.nodes.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import starbook.common.BaseCommandHandler;
import starbook.common.CK;
import starbook.common.Command;
import starbook.common.User;
import starbook.common.Command.Type;
import starbook.common.Configuration;
import starbook.common.Edge;
import starbook.common.Message;

public class CommandHandler extends BaseCommandHandler {
	private final static Logger log = Logger.getLogger(CommandHandler.class);
	private final WebNode node;

	public CommandHandler(WebNode node) {
		this.node = node;
	}

	@Override
	public void processCommand(Command command) {
		try {
			Map<String, Object> data = command.getData();
			InetAddress source = (InetAddress) data.get("source");
			log.debug(String.format("Received %s from %s.", command, source));
			switch (command.getType()) {
				case AddUser: {
					User user = (User) data.get("user");

					/*
					 * Wait a small amount of time so concurrent requests don't result in using the
					 * "same" limit, e.g. because changes in users.size() aren't reflected yet.
					 */
					try {
						Thread.sleep(Configuration.rng.nextInt(1000) + 500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					/*
					 * Determine if it's safe to replicate a new user. If so, replicate it. If not, we
					 * still replicate it if it's a brand new user, otherwise the user would not have
					 * anywhere to go.
					 */
					if (node.canReplicate()) {
						addUser(data, source);
					} else {
						log.debug(String.format("Can't replicate %s", user));
					}
					break;
				}
				case CreateUser: {
					User user = (User) data.get("user");

					/*
					 * Determine if we already track this user.
					 */
					if (node.getUser(user.getName()) != null)
						break;
					
					/*
					 * This is a new user; they need somewhere to live, so just add them.
					 */
					addUser(data, source);
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
				case MessageRequest: {
					/*
					 * Send back a MessageUpload with the message.
					 */
					String guid = (String) data.get("guid");
					Message message = node.getPublishedMessageStore().downloadMessage(guid, Configuration.getInt(CK.DownloadCount));
					if (message != null) {
						Map<String, Object> responseData = new HashMap<String, Object>(1);
						responseData.put("message", message);
						responseData.put("source", node.getInetAddress());
						Command c = new Command(Type.MessageUpload, responseData);
						c.sendViaTCP(new InetSocketAddress(source, Configuration.getInt(CK.CommandPort)));
					} else {
						log.warn("Request to download a message that doesn't exist: " + guid);
					}
					break;
				}
				case UserUpload: {
					log.debug("Replicating additional user: " + data.get("user"));
					addUser(data);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addUser(Map<String, Object> data, InetAddress source) throws IOException {
		/*
		 * We can replicate; begin tracking the new user.
		 */
		addUser(data);

		/*
		 * Send a reply packet back to the index with an updated source attribute.
		 */
		data.put("source", node.getInetAddress());
		Command c1 = new Command(Type.AddedUser, data);
		c1.sendViaUDP(new InetSocketAddress(source, Configuration.getInt(CK.CommandPort)));
	}


	/**
	 * Adds the user specified in the packet data to this node's user list and sends a command to the
	 * monitor notifying it of the user transfer.
	 * 
	 * @param data The packet data from which to get the user.
	 */
	private void addUser(Map<String, Object> data) {
		InetAddress source = (InetAddress) data.get("source");
		User user = (User) data.get("user");
		node.addUser(user);

		/*
		 * Send a command to the monitor to indicate that a user was added.
		 */
		Edge e = new Edge("ur", node.getInetAddress(), source);
		Map<String, Object> monitorData = new HashMap<String, Object>();
		monitorData.put("edge", e);
		monitorData.put("source", node.getInetAddress());
		Command monitorCommand = new Command(Command.Type.ToggleEdge, monitorData);
		try {
			monitorCommand.sendViaUDP(new InetSocketAddress(node.getMonitorAddress(), Configuration.getInt(CK.CommandPort)));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}

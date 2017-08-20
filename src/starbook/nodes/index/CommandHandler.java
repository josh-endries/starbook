package starbook.nodes.index;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import starbook.common.BaseCommandHandler;
import starbook.common.CK;
import starbook.common.Command;
import starbook.common.Edge;
import starbook.common.Command.Type;
import starbook.common.Configuration;
import starbook.common.User;

public class CommandHandler extends BaseCommandHandler {
	private final static Logger log = Logger.getLogger(CommandHandler.class);
	private final IndexNode node;

	public CommandHandler(IndexNode node) {
		this.node = node;
	}

	@Override
	public void processCommand(Command command) {
		Map<String, Object> data = command.getData();
		InetAddress source = (InetAddress) data.get("source");
		log.debug(String.format("Received %s from %s.", command, source));
		switch (command.getType()) {
			case CreateUser: {
				/*
				 * We were told from another index node about a new user.
				 */
				User user = (User) data.get("user");
				InetAddress userNode = (InetAddress) data.get("node");
				try {
					node.associateUser(user, userNode);
				} catch (IOException e2) {
					e2.printStackTrace();
				}
				
				/*
				 * Tell the monitor.
				 */
				String name = "ur"+node.getInetAddress()+source;
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
			}
			case AddedUser: {
				/*
				 * The source node told the monitor about this transfer...
				 */
				User user = (User) data.get("user");
				log.debug(String.format("Received AddedUser command from %s for %s.", source, user));
				try {
					node.associateUser(user, source);
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
			case UserRequest: {
				String userName = (String) data.get("name");
				User user = node.getUser(userName);
				if (user != null) {
					HashMap<String, Object> responseData = new HashMap<String, Object>();
					responseData.put("source", node.getInetAddress());
					responseData.put("user", user);
					Command c2 = new Command(Type.UserUpload, responseData);
					try {
						c2.sendViaUDP(new InetSocketAddress(source, Configuration.getInt(CK.CommandPort)));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}

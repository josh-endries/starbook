package starbook.monitor;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;

import starbook.common.BaseCommandHandler;
import starbook.common.BaseNode.Type;
import starbook.common.Command;
import starbook.common.Edge;

/**
 * Acts on a command received over the network. 
 *
 * @author Josh Endries (josh@endries.org)
 *
 */
public class CommandHandler extends BaseCommandHandler {
//	private final static Logger log = Logger.getLogger(CommandHandler.class);
	private final Monitor monitor;

	public CommandHandler(Monitor monitor) {
		this.monitor = monitor;
	}

	@Override
	public void processCommand(Command command) {
		Map<String, Object> data = command.getData();
		InetAddress source = (InetAddress) data.get("source");
//		log.debug(String.format("Received %s command from %s", command.getType(), source));
		switch (command.getType()) {
			case Ping: {
				Type type = (Type) data.get("type");
				ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<String, Object>();
				map.put("type", type);
				map.put("age", new DateTime());
				map.put("leader", data.get("leader"));
				String a = source.getHostAddress();
				monitor.vertexMap.put(a.substring(a.lastIndexOf('.')+1), map);
				break;
			}
			case ToggleEdge: {
				Edge e = (Edge) data.get("edge");
				monitor.edgeList.add(e);

				try {
					Thread.sleep(Monitor.EdgeDelay);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				} finally {
					monitor.edgeList.remove(e);
				}

				break;
			}
		}
	}
}

package starbook.common;

import java.net.InetSocketAddress;
import java.util.Map;

public class MonitorPinger extends BasePinger {
	private final Node node;

	public MonitorPinger(Node node, InetSocketAddress destination, int delay) {
		super(node.getInetAddress(), destination, delay);
		this.node = node;
	}

	/**
	 * Appends the node type to the underlying data payload.
	 * 
	 * @return The new data map.
	 * @see BasePinger#createData()
	 */
	@Override
	public Map<String, Object> createData() {
		Map<String, Object> data = super.createData();
		data.put("type", node.getType());
		return data;
	}
}

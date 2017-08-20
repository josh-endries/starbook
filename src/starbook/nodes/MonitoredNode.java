package starbook.nodes;

import java.net.InetAddress;

public interface MonitoredNode {
	public InetAddress getMonitorAddress();
	public void setMonitorAddress(InetAddress address);
}

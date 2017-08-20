package starbook.common;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

public interface DiscoveryProtocol extends Protocol, Serializable {
	public void addNode(TCPAddress address, NodeInfo nodeInfo);
	public ConcurrentHashMap<TCPAddress, NodeInfo> getNodes();
}

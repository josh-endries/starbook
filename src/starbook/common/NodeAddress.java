package starbook.common;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;

import org.joda.time.DateTime;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

public class NodeAddress extends TCPAddress {
	private static final long serialVersionUID = 4075616422732882922L;
//	private static final Logger log = Logger.getLogger(NodeInfo.class);
	protected DateTime latestActivity = DateTime.now();
	protected Collection<String> topics = new HashSet<String>();
	
	public NodeAddress(InetAddress a, int p) {
		super(a, p);
	}
	
	public NodeAddress clone() {
		NodeAddress a = null;
		try {
			a = new NodeAddress(InetAddress.getByName(getInetAddressAddress().getHostAddress()), getPort());
			a.setLatestActivity(new DateTime(latestActivity));
			Collection<String> newTopics = new HashSet<String>();
			for (String s: topics) {
				newTopics.add(new String(s));
			}
			a.setTopics(newTopics);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return a;
	}
	
	public DateTime getLatestActivity() {
		return latestActivity;
	}

	public Collection<String> getTopics() {
		return topics;
	}

	public NodeAddress setLatestActivity(DateTime latestActivity) {
		this.latestActivity = latestActivity;
		return this;
	}

	public NodeAddress setTopics(Collection<String> topics) {
		this.topics = topics;
		return this;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder("NodeInfo[address=");
		sb.append(address).append(", activity="+latestActivity+", topics=[");
		int i=1;
		for (String t: topics) {
			sb.append(t);
			if (i<topics.size()) {
				sb.append(", ");
			}
			i++;
		}
		sb.append("]]");
		return sb.toString();
	}
}

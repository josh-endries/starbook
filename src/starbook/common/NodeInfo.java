package starbook.common;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.joda.time.DateTime;

import starbook.common.BaseNode.Type;

/**
 * Contains information about a node in the network: the time at which the node was last changed,
 * a list of topics that node subscribes to (if applicable, empty otherwise), a list of user
 * names--not User objects--that the node tracks (if applicable, empty otherwise), and the node
 * type.
 *
 * @author Josh Endries (josh@endries.org)
 *
 */
public class NodeInfo implements Serializable, Comparable<NodeInfo> {
	private static final long serialVersionUID = 4075616422732882922L;
//	private static final Logger log = Logger.getLogger(NodeInfo.class);
	protected DateTime latestActivity = new DateTime();;
	protected Collection<String> subscribedTopics = new ConcurrentSkipListSet<String>();;
	protected Set<String> userNames = new ConcurrentSkipListSet<String>();
	private final Type type;
	
	/**
	 * Creates a new NodeInfo object with an empty publishedTopics set and
	 * sets latestActivity to the current time.
	 */
	public NodeInfo(Type type) {
		super();
		this.type = type;
	}

	/**
	 * Creates a new NodeInfo object that is a deep copy of the provided
	 * NodeInfo object.
	 * 
	 * @param ni The NodeInfo object to copy.
	 */
	public NodeInfo(NodeInfo ni) {
		this(ni.getType());
		setLatestActivity(new DateTime(ni.getLatestActivity()));
		for (String s: ni.getSubscribedTopics()) {
			subscribedTopics.add(new String(s));
		}
		userNames = Util.copyCCS(ni.getUserNames());
	}

	@Override
	public int compareTo(NodeInfo o) {
		return (latestActivity.compareTo(o.latestActivity));
	}

	public NodeInfo copy() {
		return new NodeInfo(this);
	}
	
//	@Override
//	public boolean equals(Object obj) {
//		if (obj instanceof NodeInfo) {
//			return (hashCode() == obj.hashCode());
//		}
//		return false;
//	}
//
//	@Override
//	public int hashCode() {
//		String s = address.getAddress().getHostAddress()+address.getPort();
//		return s.hashCode();
//	}
	
	public DateTime getLatestActivity() {
		return latestActivity;
	}

	public Collection<String> getSubscribedTopics() {
		return subscribedTopics;
	}
	
	public Type getType() {
		return type;
	}

	/**
	 * Updates this NodeInfo object to the provided time and returns itself.
	 * 
	 * @param latestActivity The time to set.
	 * @return A reference to the updated NodeInfo object.
	 */
	public NodeInfo setLatestActivity(DateTime latestActivity) {
		this.latestActivity = latestActivity;
		return this;
	}

	public void setSubscribedTopics(Collection<String> topics) {
		this.subscribedTopics = topics;
	}
	
	@Override
	public String toString() {
		return String.format("%s<type=%s, activity=%s, subscribedTopics=%s, userNames=%s>", "NodeInfo", type, latestActivity.toString("HH:mm:ss"), subscribedTopics, userNames);
	}
	
	public void setUserNames(Set<String> names) {
		synchronized (userNames) {
			userNames.clear();
			for (String s: names) userNames.add(s);
		}
	}
	
	/**
	 * Retrieve a reference to the list of user names tracked by this node.
	 * 
	 * @return The set of user names.
	 */
	public Set<String> getUserNames() {
		return userNames;
	}
}

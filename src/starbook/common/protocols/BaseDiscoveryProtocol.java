package starbook.common.protocols;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.util.Distribution;

import starbook.common.BaseNode.Type;
import starbook.common.CK;
import starbook.common.Command;
import starbook.common.Configuration;
import starbook.common.DiscoverableNode;
import starbook.common.Edge;
import starbook.common.NodeInfo;

public abstract class BaseDiscoveryProtocol extends org.princehouse.mica.base.BaseProtocol implements DiscoveryProtocol {
	private static final Logger log = Logger.getLogger(BaseDiscoveryProtocol.class);
	private static final long serialVersionUID = 4017453592964037397L;
	private final Set<TCPAddress> ignoredAddresses = new HashSet<TCPAddress>();
	private final Set<Type> ignoredTypes = new HashSet<Type>();
	private Set<TCPAddress> persistentAddresses = new HashSet<TCPAddress>();
	protected InetSocketAddress monitorAddress;
	private String prefix = "";
	protected double minimumRate = 0.1;
	
	/**
	 * The default round interval is 500ms (down from MiCA's default of 1000), so this adjusts the
	 * default rate back to 1s. The shorter interval is so protocols can "burst".
	 */
	private double rate = 0.5;
	
	/**
	 * This is used to determine if we are still converged, or are newly converged. The converge
	 * method is called only when we are newly converged.
	 */
	private double previousRate = rate;
	
	public BaseDiscoveryProtocol() {
		super();
		try {
			monitorAddress = new InetSocketAddress(InetAddress.getByName(Configuration.getStr(CK.MonitorIP)), Configuration.getInt(CK.CommandPort));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void burst() {
		rate = 1.0;
	}
	
	@Override
	public void converge() {
		Object o = Configuration.getParameter("node");
		if (o != null && o instanceof DiscoverableNode) {
			DiscoverableNode n = (DiscoverableNode) Configuration.getParameter("node");
			n.converge(getClass());
		}
	}
	
	/*
	 * input				output		converge?
	 * rate	prev		rate	prev
	 * 0.3	0.4		0.2	0.3
	 * 0.2	0.3		0.1	0.2
	 * 0.1	0.2		0.1	0.1	Y
	 * 0.1	0.1		0.1	0.1
	 */
	private void decreaseRate() {
		if (rate < (minimumRate + 0.000001)) {
			rate = minimumRate;
			if (rate == minimumRate && previousRate != minimumRate) converge();
			previousRate = rate;
		} else {
			previousRate = rate;
			rate -= 0.1;
		}
		log.debug(String.format("%s rate: %.2f", getPrefix(), rate()));
	}
	
	@Override
	public Set<TCPAddress> getIgnoredAddresses() {
		return ignoredAddresses;
	}
	
	@Override
	public Set<Type> getIgnoredTypes() {
		return ignoredTypes;
	}

	@Override
	public String getPrefix() {
		return prefix;
	}
	
	@Override
	public void postGossip(final Address other) {
		super.postGossip(other);
		decreaseRate();
	}
	
	@Override
	public void postUpdate(final Protocol other) {
		super.postUpdate(other);
//		decreaseRate();
	}

	@Override
	public void preGossip(final Address other) {
		super.preGossip(other);
		toggleEdge(other);
	}

	@Override
	@GossipRate
	public double rate() {
		return rate;
	}
	
	/**
	 * Creates a uniformly distributed set using copies of the addresses in the parent node's
	 * neighbor list.
	 * 
	 * @return The distribution.
	 */
	@Select
	public Distribution<Address> select() {
		DiscoverableNode node = (DiscoverableNode) Configuration.getParameter("node");
		
		/*
		 * Create the set to return.
		 */
		Set<Address> set = new HashSet<Address>();

		/*
		 * Copy the current nodes into the set.
		 */
		Iterator<Map.Entry<TCPAddress, NodeInfo>> i = node.getNeighbors().entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<TCPAddress, NodeInfo> e = i.next();
			TCPAddress a = e.getKey();
			NodeInfo ni = e.getValue();
			
			/*
			 * Don't include this address if it's in the ignore lists.
			 */
			if (ignoredAddresses.contains(a)) continue;
			if (ignoredTypes.contains(ni.getType())) continue;
			
			set.add(new TCPAddress(a));
		}
		
		/*
		 * Make sure our persistent addresses are in the returned list.
		 */
		set.addAll(persistentAddresses);

//		log.debug("Returning: " + set);
		return Distribution.uniform(set);
	}

	@Override
	public void setIgnoredAddresses(final Set<TCPAddress> addresses) {
		synchronized (ignoredAddresses) {
			ignoredAddresses.clear();
			ignoredAddresses.addAll(addresses);
		}
		log.debug(getPrefix()+" ignoring addresses: "+ignoredAddresses);
	}
	
	@Override
	public void setIgnoredTypes(final Set<Type> types) {
		synchronized (ignoredTypes) {
			ignoredTypes.clear();
			ignoredTypes.addAll(types);
		}
		log.debug(getPrefix()+": ignoring types: "+ignoredTypes);
	}

	protected void setMinimumRate(double r) {
		minimumRate = r;
	}
	
	@Override
	public void setPersistentAddresses(final Set<TCPAddress> addresses) {
		Set<TCPAddress> s = new HashSet<TCPAddress>(addresses.size());
		for (TCPAddress a: addresses) s.add(a);
		synchronized (persistentAddresses) {
			persistentAddresses = Collections.unmodifiableSet(s);
		}
		log.debug(getPrefix()+" persisting addresses: "+persistentAddresses);
	}
	


	@Override
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	

	@Override
	public void toggleEdge(final Address other) {
		/*
		 * We use a name containing the prefix and both end points because the graph uses the edge
		 * name to determine duplicates, and we want edges using the same protocol (prefix) to show
		 * up at the same time, so we need more data in the name. Including the end points will make
		 * sure that two instances of the same protocol (but between different nodes) in the same
		 * graph update round will both be displayed.
		 */
		String name = getPrefix()+getOrigin()+other.getInetAddressAddress();
		Edge e = new Edge(name, getOrigin(), other.getInetAddressAddress());
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("edge", e);
		data.put("source", getOrigin());
		Command c = new Command(Command.Type.ToggleEdge, data); 
		try {
			c.sendViaUDP(monitorAddress);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s<address=%s, prefix=%s>", getName(), getOrigin(), getPrefix());
	}
}

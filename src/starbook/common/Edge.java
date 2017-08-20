package starbook.common;

import java.io.Serializable;
import java.net.InetAddress;

public class Edge implements Serializable {
	private static final long serialVersionUID = -2181503544604683530L;
	protected final String name;
	protected final String nodeA;
	protected final String nodeB;
	
	/**
	 * Create a new Edge between two nodes with the specified name.
	 * 
	 * @param n The name of the edge. Used to distinguish which type of protocol is in use.
	 * @param a The name of one vertex.
	 * @param b The name of the other vertex.
	 */
	public Edge(String n, String a, String b) {
		name = n;
		nodeA = a;
		nodeB = b;
	}

	/**
	 * Create a new Edge between the specified nodes with the specified name. This constructor will
	 * strip off the first three octets of the IP addresses to make the names shorter.
	 * 
	 * @param n The name of the edge.
	 * @param a One vertex.
	 * @param b The other vertex.
	 */
	public Edge(String n, InetAddress a, InetAddress b) {
		this(n, a.getHostAddress().substring(a.getHostAddress().lastIndexOf('.')+1), b.getHostAddress().substring(b.getHostAddress().lastIndexOf('.')+1));
	}

	
//	@Override
//	public int compareTo(Edge e) {
//		return name.compareTo(e.name);
//	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Edge) {
			Edge e = (Edge) obj;
			return (name.equals(e.name) && nodeA.equals(e.nodeA) && nodeB.equals(e.nodeB));
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() { return name.hashCode(); }

	public String getA() { return nodeA; }
	public String getB() { return nodeB; }
	public String getName() { return name; }
	
	@Override
	public String toString() {
		return String.format("%s<name=%s, a=%s, b=%s>", getClass().getSimpleName(), name, nodeA, nodeB);
	}
}
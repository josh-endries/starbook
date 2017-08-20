package starbook.common.protocols;

import java.io.Serializable;
import java.util.Set;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

import starbook.common.BaseNode.Type;

public interface DiscoveryProtocol extends Protocol, Serializable {
	public String getPrefix();
	
	/**
	 * Retrieve a reference to the set of ignored addresses for this protocol instance. WARNING!
	 * Modifying this while the protocol is running is very risky and results in undefined behavior.
	 * 
	 * @return A reference to the set of ignored addresses.
	 */
	public Set<TCPAddress> getIgnoredAddresses();
	
	public void setIgnoredAddresses(final Set<TCPAddress> addresses);
	
	public void setPersistentAddresses(final Set<TCPAddress> addresses);

	/**
	 * Retrieve a reference to the set of ignored node types for this protocol instance.
	 * 
	 * <p><b>WARNING</b>: Accessing and/or modifying this set after the protocol has started results in
	 * undefined behavior and is not thread-safe!</p>
	 * 
	 * @return
	 */
	public Set<Type> getIgnoredTypes();
	
	public void setIgnoredTypes(Set<Type> types);
	
	public void setPrefix(String prefix);
	
	public void toggleEdge(final Address other);
	
	/**
	 * This method is called when the minimum rate has been reached.
	 */
	public void converge();
	
	/**
	 * Adjust the rate beyond the default to converge more quickly.
	 */
	public void burst();
}
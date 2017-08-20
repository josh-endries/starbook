package starbook.common.protocols;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joda.time.Seconds;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.util.Distribution;

import starbook.common.BaseNode.Type;
import starbook.common.CK;
import starbook.common.Configuration;
import starbook.common.Operation;
import starbook.common.User;
import starbook.common.UserStoreNode;

/**
 * A protocol that keeps the index and web nodes updated as to what subscriptions a user has. Each
 * User has a latestActivity date, updated when the user makes a chance, and this is used to
 * compare when that user was last changed. Newer users overwrite older users.
 * 
 * This protocol does not replicate users. Therefore, it does not add (or remove) any users from
 * the parent node--it only updates users that already exist to be the most recent known version.
 * 
 * @author Josh Endries (josh@endries.org)
 */
public class UserDiscoveryProtocol extends BaseDiscoveryProtocol {
	private static final Logger log = Logger.getLogger(UserDiscoveryProtocol.class);
	private static final long serialVersionUID = 4491213623693984765L;
	private final HashSet<User> users = new HashSet<User>();
	private final Map<Operation, Set<User>> userUpdates = new HashMap<Operation, Set<User>>();
	protected final int portNumber;



	/**
	 * Create a UserDiscoveryProtocol instance using the default port number.
	 */
	public UserDiscoveryProtocol() {
		super();
		setName("UserDiscoveryProtocol");
		setPrefix("udp");
		portNumber = Configuration.getInt(CK.UDPPort);
		Set<Type> ignoredTypes = new HashSet<Type>();
		ignoredTypes.add(Type.Worker);
		setIgnoredTypes(ignoredTypes);
	}



	/**
	 * Copy the parent node's user list into this protocol instance's temporary variable for
	 * serialization and transport over the wire.
	 */
	private void loadUsers() {
		UserStoreNode node = (UserStoreNode) Configuration.getParameter("node");
		users.clear();
		users.addAll(node.getUsers());
	}



	/**
	 * Modify the users at the parent node.
	 * 
	 * @param other The remote address to which we were sent.
	 */
	@Override
	public void postGossip(final Address other) {
		super.postGossip(other);
		pushUserUpdates(((TCPAddress) other).getInetAddressAddress());
	}

	/**
	 * Push this instance's user updates (contained in the userUpdates map) to the parent node,
	 * using the specified source address as the origin of the updates (i.e. this instance's
	 * address).
	 * 
	 * @param source The source of the updates.
	 */
private void pushUserUpdates(InetAddress source) {
	UserStoreNode node = (UserStoreNode) Configuration.getParameter("node");
	node.updateUsers(userUpdates, source);
	
	/*
	 * If there were any changes, speed up the protocol for a time.
	 */
	if (userUpdates.get(Operation.Add).size() > 0 || userUpdates.get(Operation.Remove).size() > 0 || userUpdates.get(Operation.Modify).size() > 0) {
		burst();
	}
}

	/**
	 * @see #postGossip(Address)
	 */
	@Override
	public void postUpdate(final Protocol other) {
		super.postUpdate(other);
		pushUserUpdates(other.getOrigin());
	}



	/**
	 * Before being sent to the remote protocol instance, we need to assemble a list of our users
	 * and reset the user updates list so we don't overlap updates each round.
	 */
	@Override
	public void preGossip(final Address other) {
		super.preGossip(other);
		userUpdates.clear();
		for (Operation o: Operation.values())
			userUpdates.put(o, new HashSet<User>());
		loadUsers();
	}



	/**
	 * @see #preGossip(Address)
	 */
	@Override
	public void preUpdate(final Protocol other) {
		super.preUpdate(other);
		userUpdates.clear();
		for (Operation o: Operation.values())
			userUpdates.put(o, new HashSet<User>());
		loadUsers();
	}


	@Override
	@Select
	public Distribution<Address> select() {
		/*
		 * Run the normal select.
		 */
		Distribution<Address> distribution = super.select();


		/*
		 * Recreate the map but with different port numbers.
		 */
		Distribution<Address> d = new Distribution<Address>();
		Iterator<Map.Entry<Address, Double>> i = distribution.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<Address, Double> e = i.next();
			TCPAddress a = (TCPAddress) e.getKey();
			Double v = e.getValue();
			TCPAddress newAddress = new TCPAddress(a.getInetAddressAddress(), portNumber);
			d.put(newAddress, v);
		}

		return d;
	}

	/**
	 * Go through our user list and their user list and update any existing user objects with any
	 * newer versions.
	 * 
	 * @param that The other protocol (local to the current machine at runtime).
	 */
	@GossipUpdate
	public void update(UserDiscoveryProtocol that) {
		/*
		 * This protocol only updates user information, it doesn't replicate users. Therefore, we only
		 * care about users that are common to both protocol instances.
		 */
		for (User thatUser: that.users) {
			for (User thisUser: this.users) {
				if (thatUser.equals(thisUser)) {
					/*
					 * Figure out which protocol instance has the most recent version. Note that
					 * secondsBetween(a,b) actually processes b-a.
					 */
					int diff = Seconds.secondsBetween(thatUser.getLatestActivity(), thisUser.getLatestActivity()).getSeconds();
					if (diff == 0) {
						/*
						 * The times are the same, so don't do anything.
						 */
					} else if (diff < 0) {
						/*
						 * That instance is more recent.
						 */
						this.userUpdates.get(Operation.Modify).add(thatUser);
					} else if (diff > 0) {
						/*
						 * This instance is more recent.
						 */
						that.userUpdates.get(Operation.Modify).add(thisUser);
					} else {
						log.error("Unable to determine time difference between users.");
					}
				}
			}
		}
		
		/*
		 * Wipe out the user list to save bandwidth.
		 */
		this.users.clear();
	}
}

package starbook.nodes.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;

import starbook.common.BaseDiscoverableNode;
import starbook.common.BaseMessageStore;
import starbook.common.BaseUser;
import starbook.common.CK;
import starbook.common.Command;
import starbook.common.Configuration;
import starbook.common.MessageStore;
import starbook.common.NodeInfo;
import starbook.common.Operation;
import starbook.common.TCPCommandListener;
import starbook.common.UDPCommandListener;
import starbook.common.User;
import starbook.common.UserStoreNode;
import starbook.common.Util;
import starbook.common.protocols.MessageDownloadProtocol;
import starbook.common.protocols.UserDiscoveryProtocol;
import starbook.nodes.MessageStoreNode;

public class WebNode extends BaseDiscoverableNode implements MessageStoreNode, UserStoreNode {
	private static final Logger log = Logger.getLogger(WebNode.class);
	protected Runtime<MessageDownloadProtocol> mdpRuntime;
	protected Runtime<UserDiscoveryProtocol> udpRuntime;


	/**
	 * The current count of created messages. This is an Integer instead of an int so that it can be
	 * synchronized.
	 */
	private Integer currentID = 0;



	/**
	 * The message store that contains cached user messages.
	 */
	private final MessageStore userMessageStore = new BaseMessageStore();



	/**
	 * The message store that contains messages available for download.
	 */
	private final MessageStore publishedMessageStore = new BaseMessageStore();



	/**
	 * A list of topics to which this node is subscribed.
	 */
	private final ConcurrentSkipListSet<String> subscribedTopics = new ConcurrentSkipListSet<String>();



	/**
	 * The list of users for which we are responsible.
	 */
	private final ConcurrentSkipListSet<User> users = new ConcurrentSkipListSet<User>();



	/**
	 * Create a WebNode.
	 * 
	 * @param address The address to listen on.
	 * @throws UnknownHostException If the configured index or monitor addresses are invalid.
	 */
	public WebNode(InetAddress address) throws UnknownHostException {
		super(address, Type.Web);
		log.debug("Created: " + this);
	}



	@Override
	public void addUser(User u) {
		if (users.contains(u)) {
			/*
			 * Try to find the user.
			 */
			boolean removed = false;
			Iterator<User> i = users.iterator();
			while (i.hasNext()) {
				User currentUser = i.next();
				if (u.equals(currentUser)) {
					if (currentUser.getLatestActivity().isBefore(u.getLatestActivity())) {
						log.debug("Removing old user: " + currentUser);
						users.remove(currentUser);
						removed = true;
					} else {
						log.debug("Keeping existing user: " + currentUser);
					}
				}
			}
			if (removed) {
				log.debug("Adding newer user: " + u);
				users.add(u);
			}
		} else {
			log.debug("Adding new user: " + u);
			users.add(u);
		}
		recompileSubscribedTopics();
	}



	/**
	 * Determines if this node has sufficient resources to replicate new data.
	 * 
	 * @return True if this node can replicate new data, false otherwise.
	 */
	public boolean canReplicate() {
		/*
		 * Create a percentage-like limit. The smaller this number, the less likely we will be to
		 * replicate new data. One could think of this as "the percent chance this node has to
		 * replicate new data", e.g. 0.2 is a 20% chance to replicate and 0.8 is an 80% chance.
		 */
		double limit = 1.0;

		/*
		 * Alter the limit based on the number of users we already track, reducing the limit as the
		 * number of users grows.
		 */
		int numUsers = users.size();
		limit *= (numUsers == 0) ? 1.0 : (1.0 / numUsers);

		/*
		 * Alter the limit based on the percentage of free memory. As the amount of free memory is
		 * reduced, this will lower the chance of replicating new data.
		 */
		double max = java.lang.Runtime.getRuntime().maxMemory();
		double total = java.lang.Runtime.getRuntime().totalMemory();
		double free = max - total;
		double percentageFreeMemory = free / max;
		limit *= percentageFreeMemory;

		/*
		 * Generate a random number. If this number is higher than the limit, we will replicate new
		 * data.
		 */
		double attempt = Configuration.rng.nextDouble();

		log.debug(String.format("Chance: %s, Attempt: %s", limit, attempt));

		return (attempt < limit);
	}



	/**
	 * Retrieve a NodeInfo object with the latest time stamp and copies of this node's lists of
	 * subscribed topics and tracked users.
	 * 
	 * @see starbook.common.BaseNode#getInfo()
	 */
	@Override
	public NodeInfo getInfo() {
		NodeInfo ni = super.getInfo();
		ni.setSubscribedTopics(Util.copyCCS(getSubscribedTopics()));
		Set<String> names = new HashSet<String>();
		for (User u : getUsers())
			names.add(new String(u.getName()));
		ni.setUserNames(names);
		return ni;
	}



	/**
	 * Retrieve a reference to the MessageStore containing messages advertised for download.
	 * 
	 * @return The message store.
	 */
	@Override
	public MessageStore getPublishedMessageStore() {
		return publishedMessageStore;
	}

	/**
	 * Retrieves the next consecutive message ID.
	 * 
	 * TODO: This needs to roll over with the date or something.
	 * 
	 * @return The ID.
	 */
	public int getNextID() {
		int id;
		synchronized (currentID) {
			currentID++;
			id = currentID;
		}
		return id;
	}



	@Override
	public ConcurrentSkipListSet<String> getSubscribedTopics() {
		return subscribedTopics;
	}



	@Override
	public User getUser(String name) {
		Iterator<User> i = users.iterator();
		while (i.hasNext()) {
			User u = i.next();
			if (u.getName().equalsIgnoreCase(name)) {
				/*
				 * We found the user. Make a copy and return it.
				 */
				return new BaseUser(u);
			}
		}
		return null;
	}



	@Override
	public ConcurrentSkipListSet<User> getUsers() {
		ConcurrentSkipListSet<User> s = new ConcurrentSkipListSet<User>();
		Iterator<User> i = users.iterator();
		while (i.hasNext()) {
			User u = new BaseUser(i.next());
			s.add(u);
		}
		return s;
	}



	@Override
	public void converge(Class<?> t) {

	}



	/**
	 * Since we maintain two lists of subscribed topics (those from users and others through
	 * replication), the "authoritative" list of topics tracked by this node is assembled from these
	 * two sources. That happens here.
	 */
	public void recompileSubscribedTopics() {
		synchronized (subscribedTopics) {
			subscribedTopics.clear();
			for (User u : users) {
				subscribedTopics.addAll(Util.copyCCS(u.getSubscriptions()));
			}
		}
	}

	

	/**
	 * Update the user list. We don't want to replicate users, so we only process Modify operations
	 * here. Also, we don't use the source parameter.
	 */
	@Override
	public void updateUsers(Map<Operation, Set<User>> userMap, InetAddress source) {
		log.debug(String.format("Applying user changes from %s: %s", source, userMap));
		for (Operation o: Operation.values()) {
			if (userMap.containsKey(o)) {
				Set<User> users = userMap.get(o);
				
				switch (o) {
					case Modify:
						for (User u: users) {
							if (this.users.contains(u)) {
								Iterator<User> i = this.users.iterator();
								User existingUser = i.next();
								while (!existingUser.equals(u)) existingUser = i.next();
								log.debug(String.format("Updating user: %s --> %s", existingUser, u));
								this.users.remove(u);
								this.users.add(u);
							} else {
								log.warn(String.format("Attempt to modify non-existent user: %s", u));
							}
						}
						break;
				}
			}
		}
		
		recompileSubscribedTopics();
	}



	@Override
	public void start() throws IOException {
		super.start();

		/*
		 * Start the user replicator, which attempts to download new users.
		 */
		UserReplicator userReplicator = new UserReplicator(this);
		Thread userReplicatorThread = new Thread(userReplicator);
		userReplicatorThread.setName("User Replicator");
		addThread(userReplicatorThread);
		userReplicatorThread.start();

		/*
		 * Create the user discovery protocol (UDP), set it to always contain the index and ignore
		 * worker nodes, and ignore ourself.
		 */
		UserDiscoveryProtocol udp = new UserDiscoveryProtocol();
		udp.setOrigin(getInetAddress());
		HashSet<TCPAddress> udpIgnoredAddresses = new HashSet<TCPAddress>();
		udpIgnoredAddresses.add(new TCPAddress(getInetAddress(), Configuration.getInt(CK.UDPPort)));
		udp.setIgnoredAddresses(udpIgnoredAddresses);
		HashSet<TCPAddress> udpPersistentAddresses = new HashSet<TCPAddress>();
		for (InetAddress ia: getIndexAddresses()) udpPersistentAddresses.add(new TCPAddress(ia, Configuration.getInt(CK.UDPPort)));
		udp.setPersistentAddresses(udpPersistentAddresses);
		udpRuntime = SimpleRuntime.launchDaemon(udp, new TCPAddress(getInetAddress(), Configuration.getInt(CK.UDPPort)));

		/*
		 * Create the message download protocol (MDP), set it to ignore ourself, web nodes and index
		 * nodes. We ignore index nodes because they don't download messages, and web nodes because
		 * they download messages from worker nodes. If web nodes advertised to themselves, it's
		 * possible that only web nodes would download published messages and worker nodes would never
		 * see them, since web nodes remove messages from being advertised once they have been
		 * downloaded the requisite number of times.
		 */
		MessageDownloadProtocol mdp = new MessageDownloadProtocol();
		mdp.setOrigin(getInetAddress());
		Set<Type> mdpIgnoredTypes = new HashSet<Type>();
		mdpIgnoredTypes.add(Type.Index);
		mdpIgnoredTypes.add(Type.Web);
		mdp.setIgnoredTypes(mdpIgnoredTypes);
		Set<TCPAddress> mdpIgnoredAddresses = new HashSet<TCPAddress>();
		mdpIgnoredAddresses.add(new TCPAddress(getInetAddress(), Configuration.getInt(CK.MessageDownloadPort)));
		mdpRuntime = SimpleRuntime.launchDaemon(mdp, new TCPAddress(getInetAddress(), Configuration.getInt(CK.MessageDownloadPort)));

		CommandHandlerFactory chf = new CommandHandlerFactory(this);
		TCPCommandListener tcl = new TCPCommandListener(new InetSocketAddress(getInetAddress(), Configuration.getInt(CK.CommandPort)), chf);
		Thread tclt = new Thread(tcl, "TCP Command Handler");
		addThread(tclt);
		tclt.start();

		UDPCommandListener ucl = new UDPCommandListener(new InetSocketAddress(getInetAddress(), Configuration.getInt(CK.CommandPort)), chf);
		Thread uclt = new Thread(ucl, "UDP Command Handler");
		addThread(uclt);
		uclt.start();
	}



	@Override
	public String toString() {
		return String.format("%s<users=%s, subscribedTopics=%s>", getClass().getSimpleName(), users, subscribedTopics);
	}



	/**
	 * Send a UserRequest command to the index for the specified user name.
	 * 
	 * @param userName The user name for which to request an User object.
	 */
	public void fetchUser(String userName) {
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("source", getInetAddress());
		data.put("name", userName);
		Command c = new Command(Command.Type.UserRequest, data);
		try {
			c.sendViaUDP(new InetSocketAddress(getIndexAddresses().iterator().next(), Configuration.getInt(CK.CommandPort)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	@Override
	public MessageStore getStoredMessageStore() {
		return userMessageStore;
	}
}

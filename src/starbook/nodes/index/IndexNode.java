package starbook.nodes.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;

import starbook.common.BaseDiscoverableNode;
import starbook.common.BaseUser;
import starbook.common.CK;
import starbook.common.Command;
import starbook.common.Configuration;
import starbook.common.InetAddressComparator;
import starbook.common.NodeInfo;
import starbook.common.Operation;
import starbook.common.PeriodicFileWriter;
import starbook.common.TCPCommandListener;
import starbook.common.UDPCommandListener;
import starbook.common.User;
import starbook.common.UserStoreNode;
import starbook.common.Util;
import starbook.common.protocols.NodeDiscoveryProtocol;
import starbook.common.protocols.UserDiscoveryProtocol;

public class IndexNode extends BaseDiscoverableNode implements UserStoreNode {
	private static final Logger log = Logger.getLogger(IndexNode.class);
	protected Runtime<NodeDiscoveryProtocol> ndpRuntime;
	protected Runtime<UserDiscoveryProtocol> udpRuntime;
	private final String UsersFile;
	private InetAddress leaderAddress = null;
	private Thread userListUpdaterThread = null;

	/**
	 * This is a list of users and the node(s) that are responsible for them.
	 */
	private final ConcurrentHashMap<User, ConcurrentSkipListSet<InetAddress>> users = new ConcurrentHashMap<User, ConcurrentSkipListSet<InetAddress>>();



	/**
	 * Create an IndexNode.
	 * 
	 * @param address
	 *           The address of this node.
	 * @throws NamingException
	 *            If unable to retrieve the DB context information.
	 * @throws SQLException
	 *            If a connection to the DB was unsuccessful.
	 * @throws IOException
	 *            If unable to use the configured data directory.
	 */
	public IndexNode(InetAddress address) throws NamingException, SQLException, IOException {
		super(address, Type.Index);
		
		/*
		 * Connect to the database.
		 */
		DNS.Instance.initialize();

		UsersFile = getInetAddress().getHostAddress()+"-users";

		String dataDirectoryName = Configuration.getStr(CK.DataDirectory);
		log.debug("Using data directory: " + dataDirectoryName);
		File dataDirectory = new File(dataDirectoryName);
		if (!dataDirectory.isDirectory())
			throw new IOException("Data directory is not a directory.");
		if (!dataDirectory.canWrite())
			throw new IOException("Cannot write to data directory.");
		if (!dataDirectory.canRead())
			throw new IOException("Cannot read data directory.");
		for (File f : dataDirectory.listFiles()) {
			if (f.getName().equals(UsersFile) && f.length() > 0) {
				FileInputStream fis = new FileInputStream(f);
				ObjectInputStream ois = new ObjectInputStream(fis);
				try {
					@SuppressWarnings("unchecked")
					ConcurrentHashMap<User, ConcurrentSkipListSet<InetAddress>> userMap = (ConcurrentHashMap<User, ConcurrentSkipListSet<InetAddress>>) ois.readObject();
					int count = 0;
					for (Map.Entry<User, ConcurrentSkipListSet<InetAddress>> e : userMap.entrySet()) {
						users.put(e.getKey(), e.getValue());
						count++;
					}
					log.debug(String.format("Loaded %s users: %s", count, users));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} finally {
					ois.close();
					fis.close();
				}
			}
		}

		log.debug("Created " + this);
	}



	/**
	 * Since the IndexNode's user list is created from the node discovery results, this method
	 * doesn't actually add users, but it does update them if the provided user is newer than the
	 * user already in the list.
	 * 
	 * @param User
	 *           The user to update.
	 */
	@Override
	public void addUser(User u) {
		ConcurrentSkipListSet<InetAddress> addresses = users.get(u);
		if (addresses != null) {
			/*
			 * This user already exists in the list. Modify it.
			 */
			users.remove(u);
			users.put(u, addresses);
		} else {
			log.warn(String.format("Attempt to add a user with no nodes: %s", u));
		}
	}

	/**
	 * Add the specified user to the user list and associate it with the specified node.
	 * 
	 * @param user
	 *           The user to add
	 * @param address
	 *           The MiCA address to associate with the user.
	 * @throws IOException
	 *            If the AddUser command cannot be sent to the node.
	 */
	public void associateUser(User user, InetAddress address) throws IOException {
		log.debug(String.format("Associating user %s with node %s", user, address));
		ConcurrentHashMap<User, ConcurrentSkipListSet<InetAddress>> ua = getUserAssociations();
		ua.putIfAbsent(user, new ConcurrentSkipListSet<InetAddress>(new InetAddressComparator()));
		ua.get(user).add(address);
		
		/*
		 * Update DNS only if we are the leader.
		 */
		if (getInetAddress().equals(getLeaderAddress())) DNS.Instance.updateUser(user, ua.get(user));
	}



	@Override
	public void converge(Class<?> type) {
		if (type == NodeDiscoveryProtocol.class) {
			/*
			 * The node protocol has converged. Elect a leader to handle DNS updates. The leader is
			 * the node with the lowest IP address. Find that address.
			 */
			long leaderAddress = Util.ipToLong(getInetAddress().getHostAddress());
			InetAddress leaderIA = getInetAddress();
			Iterator<Map.Entry<TCPAddress, NodeInfo>> i = getNeighbors().entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<TCPAddress, NodeInfo> e = i.next();
				TCPAddress a = e.getKey();
				NodeInfo ni = e.getValue();
				if (ni.getType().equals(Type.Index)) {
					long candidateAddress = Util.ipToLong(a.getInetAddressAddress().getHostAddress());
					if (candidateAddress < leaderAddress) {
						log.debug(String.format("Leading candidate: %s %s", a, ni));
						leaderAddress = candidateAddress;
						leaderIA = a.getInetAddressAddress();
					}
				}
			}
			log.debug("Leader is "+leaderIA);
			this.leaderAddress = leaderIA;
			
			if (userListUpdaterThread == null) {
				log.debug("Starting user list updater thread.");
				UserListUpdater u = new UserListUpdater(this);
				Thread t = new Thread(u, "User list updater");
				addThread(t);
				userListUpdaterThread = t;
				t.start();
			}
		}
	}
	
	
	
	public InetAddress getLeaderAddress() { return leaderAddress;}



	@Override
	public User getUser(String name) {
		Iterator<User> i = users.keySet().iterator();
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



	/**
	 * Returns a reference to the user list.
	 * 
	 * @return The object reference.
	 */
	public ConcurrentHashMap<User, ConcurrentSkipListSet<InetAddress>> getUserAssociations() {
		return users;
	}



	@Override
	public ConcurrentSkipListSet<User> getUsers() {
		return Util.copyCCS(users.keySet());
	}

	/**
	 * Sends an AddUser command to a random node.
	 * 
	 * @param user
	 *           The user for the node to add.
	 * @throws IOException
	 *            If there is a problem sending the command.
	 */
	public TCPAddress sendAddUserCommand(User user) throws IOException {
		TCPAddress node = getRandomNeighbor(false, Type.Web);
		sendAddUserCommand(user, new InetSocketAddress(node.getInetAddressAddress(), Configuration.getInt(CK.CommandPort)));
		return node;
	}



	/**
	 * Sends an AddUser command to the given node.
	 * 
	 * @param user
	 *           The user for the node to add.
	 * @param commandAddress
	 *           The node to which the command is sent.
	 * @throws IOException
	 *            If there is a problem sending the command.
	 */
	public void sendAddUserCommand(User user, InetSocketAddress commandAddress) throws IOException {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("source", getInetAddress());
		data.put("user", user);
		Command c = new Command(Command.Type.AddUser, data);
		c.sendViaUDP(commandAddress);
	}



	@Override
	public void start() throws IOException {
		super.start();
		
		/*
		 * Start the user database backup process.
		 */
		PeriodicFileWriter pfw = new PeriodicFileWriter(users, Paths.get(UsersFile));
		Thread pfwt = new Thread(pfw, "User File Writer");
		addThread(pfwt);
		pfwt.start();

		/*
		 * Create the user discovery protocol (UDP) and set it to ignore worker nodes and ourself. On
		 * index nodes, we want to set this to ignore index nodes--we don't want index-to-index
		 * communication with this. If that happens, users appear to live on index nodes and end up
		 * in the user list and DNS that way.
		 */
		UserDiscoveryProtocol udp = new UserDiscoveryProtocol();
		udp.setOrigin(getInetAddress());
		udp.getIgnoredTypes().add(Type.Index);
		Set<TCPAddress> udpIgnoredAddresses = new HashSet<TCPAddress>();
		udpIgnoredAddresses.add(new TCPAddress(getInetAddress(), Configuration.getInt(CK.UDPPort)));
		udp.setIgnoredAddresses(udpIgnoredAddresses);
		udpRuntime = SimpleRuntime.launchDaemon(udp, new TCPAddress(getInetAddress(), Configuration.getInt(CK.UDPPort)));
		
		/*
		 * Start the command handlers.
		 */
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
	public void stopThreads() {
		/*
		 * Attempt to save the user list.
		 */
		if (users.size() > 0) {
			String dataDirectoryName = Configuration.getStr(CK.DataDirectory);
			File usersFile = new File(dataDirectoryName, UsersFile);
			try {
				FileOutputStream fos = new FileOutputStream(usersFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				synchronized (users) {
					oos.writeObject(users);
				}
				log.debug(String.format("Successfully saved %s users to disk.", users.size()));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			try {
				DriverManager.deregisterDriver(driver);
				log.debug(String.format("deregistering jdbc driver: %s", driver));
			} catch (SQLException e) {
				log.error(String.format("Error deregistering driver %s", driver), e);
			}
		}

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		super.stopThreads();
	}

	public void updateUserAssociations(HashMap<Operation, HashMap<User, HashSet<InetAddress>>> updates) {
		log.debug(String.format("Applying user association changes: %s", updates));
		for (Operation o: Operation.values()) {
			if (updates.containsKey(o)) {
				HashMap<User, HashSet<InetAddress>> updatedUsers = updates.get(o);
				
				switch (o) {
					case Add:
						for (Map.Entry<User, HashSet<InetAddress>> e: updatedUsers.entrySet()) {
							users.put(e.getKey(), new ConcurrentSkipListSet<InetAddress>(new InetAddressComparator()));
							Iterator<InetAddress> ei = e.getValue().iterator();
							while (ei.hasNext()) users.get(e.getKey()).add(ei.next());
						}
						break;
					case Modify:
						for (Map.Entry<User, HashSet<InetAddress>> e: updatedUsers.entrySet()) {
							if (users.remove(e.getKey()) != null) {
								users.put(e.getKey(), new ConcurrentSkipListSet<InetAddress>(new InetAddressComparator()));
								Iterator<InetAddress> ei = e.getValue().iterator();
								while (ei.hasNext()) users.get(e.getKey()).add(ei.next());
							}
						}
						break;
				}
			}
		}
	}


	/**
	 * Update user subscription information.
	 */
	@Override
	public void updateUsers(Map<Operation, Set<User>> userMap, InetAddress source) {
		log.debug(String.format("Applying user changes from %s: %s", source, userMap));
		for (Operation o: Operation.values()) {
			if (userMap.containsKey(o)) {
				Set<User> users = userMap.get(o);
				
				switch (o) {
					case Add:
					case Modify:
						for (User u: users) {
							if (this.users.containsKey(u)) {
								/*
								 * Remove the current entry and insert the newly-updated one. We remove the
								 * entry because User equality is based only on user name, which means the
								 * latestActivity won't be updated unless we actually put the new object
								 * into the list.
								 */
								ConcurrentSkipListSet<InetAddress> addresses = this.users.get(u);
								addresses.add(source);
								this.users.remove(u);
								this.users.put(u, addresses);
							} else {
								/*
								 * This is a new user, create a new entry.
								 */
								ConcurrentSkipListSet<InetAddress> addresses = new ConcurrentSkipListSet<InetAddress>(new InetAddressComparator());
								addresses.add(source);
								this.users.put(u, addresses);
							}
						}
						break;
				}
			}
		}
	}
}
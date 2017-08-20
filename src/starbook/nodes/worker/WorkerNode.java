package starbook.nodes.worker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;

import starbook.common.BaseDiscoverableNode;
import starbook.common.BaseMessageStore;
import starbook.common.CK;
import starbook.common.Configuration;
import starbook.common.MessageStore;
import starbook.common.NodeInfo;
import starbook.common.PeriodicFileWriter;
import starbook.common.TCPCommandListener;
import starbook.common.UDPCommandListener;
import starbook.common.Util;
import starbook.common.protocols.MessageDownloadProtocol;
import starbook.nodes.MessageStoreNode;

public class WorkerNode extends BaseDiscoverableNode implements MessageStoreNode {
	private static final Logger log = Logger.getLogger(WorkerNode.class);
	protected Runtime<MessageDownloadProtocol> mdpRuntime;
	private final Path MessageFilePath;
	private final Path TopicsFilePath;

	/**
	 * The message store.
	 */
	private final MessageStore messageStore;

	/**
	 * A list of topics we track.
	 */
	private final ConcurrentSkipListSet<String> subscribedTopics;

	@SuppressWarnings("unchecked")
	public WorkerNode(InetAddress address) throws IOException, ClassNotFoundException {
		super(address, Type.Worker);

		Path dataDirectoryPath = Paths.get(Configuration.getStr(CK.DataDirectory));
		MessageFilePath = dataDirectoryPath.resolve(String.format("%s-messages", address.getHostAddress()));
		TopicsFilePath = dataDirectoryPath.resolve(String.format("%s-topics", address.getHostAddress()));

		/*
		 * Load the message store;
		 */
		if (Files.exists(MessageFilePath)) {
			if (!Files.isReadable(MessageFilePath))
				throw new IOException("Message store file is not readable.");
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(Files.newInputStream(MessageFilePath));
				log.debug("Loading message store from disk.");
				messageStore = (MessageStore) ois.readObject();
				log.debug(String.format("%s messages loaded from disk.", messageStore.getMessages().size()));
				log.debug(String.format("%s message GUIDs loaded from disk.", messageStore.getMessageGUIDs().size()));
			} finally {
				if (ois != null)
					ois.close();
			}
		} else {
			log.info("Message store file does not exist, creating an empty message store.");
			messageStore = new BaseMessageStore();
		}

		/*
		 * Load the topic list.
		 */
		if (Files.exists(TopicsFilePath)) {
			if (!Files.isReadable(TopicsFilePath))
				throw new IOException("Topics list file is not readable.");
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(Files.newInputStream(TopicsFilePath));
				log.debug("Loading topics list from disk.");
				subscribedTopics = (ConcurrentSkipListSet<String>) ois.readObject();
				log.debug(String.format("%s topics loaded from disk.", subscribedTopics.size()));
			} finally {
				if (ois != null)
					ois.close();
			}
		} else {
			log.info("Topics list file does not exist, creating an empty topic list.");
			subscribedTopics = new ConcurrentSkipListSet<String>();
		}
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
		ni.setUserNames(names);
		return ni;
	}



	public void addSubscribedTopic(String topic) {
		subscribedTopics.add(topic);
	}



	/**
	 * Determines if this node has sufficient resources to replicate new data.
	 * 
	 * @return True if this node can replicate new data, false otherwise.
	 */
	public boolean canReplicate() {
		/*
		 * Wait a small amount of time so concurrent requests don't result in using the "same" limit,
		 * e.g. because changes in users.size() aren't reflected yet.
		 */
		try {
			Thread.sleep(Configuration.rng.nextInt(1000) + 500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		/*
		 * Create a percentage-like limit. The smaller this number, the less likely we will be to
		 * replicate new data. One could think of this as "the percent chance this node has to
		 * replicate new data", e.g. 0.2 is a 20% chance to replicate and 0.8 is an 80% chance.
		 */
		double limit = 1.0;

		/*
		 * Alter the limit based on the number of messages we already track, reducing the limit as the
		 * number of messages grows.
		 */
		int numMessages = messageStore.getMessageGUIDs().size();
		limit *= (numMessages == 0) ? 1.0 : (1.0 / numMessages);

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



	@Override
	public void start() throws IOException {
		super.start();

		/*
		 * Start the topic database backup process.
		 */
		PeriodicFileWriter tpfw = new PeriodicFileWriter(subscribedTopics, TopicsFilePath);
		Thread tpfwt = new Thread(tpfw, "Topic File Writer");
		addThread(tpfwt);
		tpfwt.start();

		/*
		 * Start the message database backup process.
		 */
		PeriodicFileWriter mpfw = new PeriodicFileWriter(messageStore, MessageFilePath);
		Thread mpfwt = new Thread(mpfw, "Message File Writer");
		addThread(mpfwt);
		mpfwt.start();

		TopicReplicator topicReplicator = new TopicReplicator(this);
		Thread topicReplicatorThread = new Thread(topicReplicator);
		topicReplicatorThread.setName("Topic Replicator");
		addThread(topicReplicatorThread);
		topicReplicatorThread.start();

		CommandHandlerFactory chf = new CommandHandlerFactory(this);
		TCPCommandListener tcl = new TCPCommandListener(new InetSocketAddress(getInetAddress(), Configuration.getInt(CK.CommandPort)), chf);
		Thread tclt = new Thread(tcl, "TCP Command Handler");
		addThread(tclt);
		tclt.start();

		UDPCommandListener ucl = new UDPCommandListener(new InetSocketAddress(getInetAddress(), Configuration.getInt(CK.CommandPort)), chf);
		Thread uclt = new Thread(ucl, "UDP Command Handler");
		addThread(uclt);
		uclt.start();

		/*
		 * Start the message download protocol.
		 */
		try {
			MessageDownloadProtocol mdp = new MessageDownloadProtocol();
			mdp.setOrigin(getInetAddress());

			/*
			 * We only want worker and web nodes participating in this protocol; index nodes don't
			 * download messages. Also, ignore ourself (as usual).
			 */
			Set<Type> mdpIgnoredTypes = new HashSet<Type>();
			mdpIgnoredTypes.add(Type.Index);
			mdp.setIgnoredTypes(mdpIgnoredTypes);
			Set<TCPAddress> mdpIgnoredAddresses = new HashSet<TCPAddress>();
			mdpIgnoredAddresses.add(new TCPAddress(getInetAddress(), Configuration.getInt(CK.MessageDownloadPort)));
			mdp.setIgnoredAddresses(mdpIgnoredAddresses);
			mdpRuntime = SimpleRuntime.launchDaemon(mdp,
					new TCPAddress(InetAddress.getByName(Configuration.getStr(CK.WorkerIP)), Configuration.getInt(CK.MessageDownloadPort)));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}



	@Override
	public void stopThreads() {
		if (mdpRuntime != null)
			mdpRuntime.stop();
		super.stopThreads();
	}



	@Override
	public void converge(Class<?> t) {
	}



	public void addNode(TCPAddress key, NodeInfo value) {
		Map<TCPAddress, NodeInfo> n = getNeighbors();
		if (n.containsKey(key)) {
			log.debug("Adding node " + key);
			n.put(key, value);
		} else {
			if (value.getLatestActivity().isAfter(n.get(key).getLatestActivity())) {
				log.debug("Updating node " + key);
				n.put(key, value);
			}
		}
	}



	@Override
	public Set<String> getSubscribedTopics() {
		return subscribedTopics;
	}



	@Override
	public MessageStore getPublishedMessageStore() {
		return messageStore;
	}



	@Override
	public MessageStore getStoredMessageStore() {
		return messageStore;
	}
}

package starbook.common;

import java.net.InetAddress;
import java.util.Comparator;

import org.joda.time.DateTime;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

public class BaseMessage implements Message, Comparable<Message> {
	private static final long serialVersionUID = 468622942495322655L;
	private final String content;
	private final DateTime creationDate;
	private final String GUID;
	private final InetAddress sourceAddress;
	private final int ID;
	private final String topic;

	/**
	 * Create a new BaseMessage with the specified parameters.
	 * 
	 * @param content The message content.
	 * @param creationDate The time at which this message was created.
	 * @param sourceAddress The node that initially accepted this message.
	 * @param id The identifier at the source node for this message.
	 * @param topic The topic of this message.
	 */
	public BaseMessage(String content, DateTime creationDate, InetAddress sourceAddress, int id, String topic) {
		super();
		if (topic == null || topic.length() < 1) throw new IllegalArgumentException("Topic cannot be empty.");
		this.content = content;
		this.creationDate = creationDate;
		this.ID = id;
		this.sourceAddress = sourceAddress;
		this.topic = topic;
		this.GUID = String.format("%s-%s-%s|%s", sourceAddress.getHostAddress(), creationDate.toString("yyyyMMddHHmmss"), id, topic);
	}

	/**
	 * Created a new BaseMessage with the specified parameters and the creation date set to the
	 * current time.
	 * 
	 * @param content The message content.
	 * @param sourceAddress The node that initially accepted this message.
	 * @param id The identifier at the source node for this message.
	 * @param topic The topic of this message.
	 */
	public BaseMessage(String content, InetAddress sourceAddress, int id, String topic) {
		this(content, new DateTime(), sourceAddress, id, topic);
	}

	/**
	 * @see BaseMessage#BaseMessage(String, InetAddress, int, String)
	 */
	public BaseMessage(String content, TCPAddress sourceAddress, int id, String topic) {
		this(content, sourceAddress.getInetAddressAddress(), id, topic);
	}
	
	/**
	 * Creates a new, deep-copied message from the data in the given message.
	 * 
	 * @param m The message to copy.
	 */
	public BaseMessage(Message m) {
		this(new String(m.getContent()), new DateTime(m.getCreationDate()), Util.copy(m.getSourceAddress()), m.getID(), new String(m.getTopic()));
	}
	
	@Override
	public int compareTo(Message m) {
		return creationDate.compareTo(m.getCreationDate());
//		return getGUID().compareTo(m.getGUID());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Message) {
			Message m = (Message) obj;
			return m.getGUID().equals(getGUID());
		}
		return false;
	}

	@Override
	public String getContent() {
		return content;
	}
	
	@Override
	public DateTime getCreationDate() {
		return creationDate;
	}
	
	@Override
	public String getDisplayDate() {
		if (creationDate.isBefore(DateTime.now().minusHours(24))) {
			return creationDate.toString("MMM dd kk:mm");
		} else {
			return creationDate.toString("kk:mm");
		}
	}

	@Override
	public String getGUID() {
		return GUID;
	}

	@Override
	public int getID() {
		return ID;
	}

	@Override
	public InetAddress getSourceAddress() {
		return sourceAddress;
	}

	@Override
	public String getTopic() {
		return topic;
	}
	
	@Override
	public int hashCode() {
		return getGUID().hashCode();
	}
	
	@Override
	public String toString() {
		return String.format("BaseMessage<GUID=%s>", getGUID());
	}
	
	public static class contentComparator implements Comparator<Message> {
		@Override
		public int compare(Message o1, Message o2) {
			return o1.getContent().compareTo(o2.getContent());
		}
	}
	
	public static class contentSizeComparator implements Comparator<Message> {
		@Override
		public int compare(Message o1, Message o2) {
			int l1 = o1.getContent().length();
			int l2 = o2.getContent().length();
			if (l1 == l2) return 0;
			if (l1 < l2) return -1;
			return 1;
		}
	}
}

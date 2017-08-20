package starbook.common;

import java.io.Serializable;
import java.net.InetAddress;

import org.joda.time.DateTime;

public interface Message extends Serializable {
	public String getContent();
	public DateTime getCreationDate();
	public String getDisplayDate();
	public String getGUID();
	public int getID();
	public InetAddress getSourceAddress();
	public String getTopic();
}

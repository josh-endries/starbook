package starbook.common;

import java.io.Serializable;
import java.util.concurrent.ConcurrentSkipListSet;

import org.joda.time.DateTime;

public interface User extends Serializable, Comparable<User> {
	public String getName();
	public DateTime getLatestActivity();
	public ConcurrentSkipListSet<String> getSubscriptions();
	public void setLatestActivity(DateTime time);
}

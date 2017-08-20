package starbook.common;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Comparator;

public class InetAddressComparator implements Comparator<InetAddress>, Serializable {
	private static final long serialVersionUID = -6460660187846531252L;

	@Override
	public int compare(InetAddress a, InetAddress b) {
		return a.getHostAddress().compareTo(b.getHostAddress());
	}
}

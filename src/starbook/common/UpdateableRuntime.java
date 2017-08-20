package starbook.common;

import java.util.concurrent.ConcurrentHashMap;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SimpleRuntime;

public class UpdateableRuntime<P extends Protocol> extends SimpleRuntime<P> {
	private final ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<String, Object>();

	protected UpdateableRuntime(Address address) {
		super(address);
	}

	ConcurrentHashMap<String, Object> getData() {
		return data;
	}
}

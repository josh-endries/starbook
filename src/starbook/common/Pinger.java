package starbook.common;

import java.util.Map;

import starbook.common.Command.Type;

public interface Pinger extends Runnable {
	public Map<String, Object> createData();
	public void setType(Type type);
}

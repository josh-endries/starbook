package starbook.tests;

import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

import org.princehouse.mica.base.net.tcpip.TCPAddress;

import starbook.common.BaseNode.Type;
import starbook.common.NodeInfo;
import starbook.common.PeriodicFileWriter;

public class PeriodicFileWriterTest extends Test {
	public static void main(String[] args) throws UnknownHostException {
		ConcurrentHashMap<TCPAddress, NodeInfo> test = new ConcurrentHashMap<TCPAddress, NodeInfo>();
		TCPAddress key = TCPAddress.valueOf("1.2.3.4:1234");
		test.put(key, new NodeInfo(Type.Worker));
		
		PeriodicFileWriter pfw = new PeriodicFileWriter(test, Paths.get("C:\\Users\\josh\\temp\\starbook\\blah"));
		Thread t = new Thread(pfw);
		t.start();
		
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

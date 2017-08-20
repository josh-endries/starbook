package starbook.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import starbook.common.UDPCommandListener;

public class Main {
	public static void main(String[] args) throws IOException, InterruptedException {
		/*
		 * Load the configuration.
		 */
		InputStream is = Main.class.getClass().getResource("/META-INF/MANIFEST.MF").openStream();
		Manifest m = new Manifest(is);
		Attributes a = m.getMainAttributes();
		System.out.println("Monitor version " + a.getValue("Build-Version") + " initializing.");
		System.out.println("Build number "+a.getValue("Build-Number")+" built by "+a.getValue("Build-User")+"@"+a.getValue("Build-Host")+" on "+a.getValue("Build-Timestamp"));

		/*
		 * Create the monitor.
		 */
		Monitor monitor = new Monitor();
		
		/*
		 * Start the command handler.
		 */
		String listenAddress = a.getValue("Monitor-IP");
		int port = Integer.valueOf(a.getValue("Monitor-Port"));
		InetSocketAddress address = new InetSocketAddress(listenAddress, port);
		CommandHandlerFactory chf = new CommandHandlerFactory(monitor);
		UDPCommandListener ucl = new UDPCommandListener(address, chf);
		Thread t = new Thread(ucl, "UDP Command Handler");
		t.start();
	}
}

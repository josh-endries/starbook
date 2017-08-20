package starbook.monitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class VertexPruner implements Runnable {
	private static final Logger log = Logger.getLogger(VertexPruner.class);
	private boolean running = false;
	private final Monitor monitor;
	
	VertexPruner(Monitor monitor) {
		this.monitor = monitor;
	}
	
	@Override
	public void run() {
		running = true;
		
		while (running) {
			try {
				Thread.sleep(Monitor.VertexPrunerDelay);
			} catch (InterruptedException e) {
				running = false;
			}
			
			log.debug("Pruning old vertices...");
			DateTime cutoff = DateTime.now().minusSeconds(Monitor.VertexCutoffSeconds);
			int count = 0;
			
			HashSet<String> verticesToRemove = new HashSet<String>();
			Iterator<Map.Entry<String, ConcurrentHashMap<String, Object>>> i = monitor.getVertexIterator();
			while (i.hasNext()) {
				Map.Entry<String, ConcurrentHashMap<String, Object>> e = i.next();
				DateTime age = (DateTime) e.getValue().get("age");
				if (age.isBefore(cutoff)) {
					verticesToRemove.add(e.getKey());
					count++;
				}
			}
			for (String vertex: verticesToRemove) monitor.vertexMap.remove(vertex);
			
			if (count > 0) log.debug(String.format("Pruned %s vertices.", count));
		}
	}
}

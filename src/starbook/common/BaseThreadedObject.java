package starbook.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class BaseThreadedObject implements Threaded, Stoppable {
	private static final Logger log = Logger.getLogger(BaseThreadedObject.class);
	private Collection<Thread> threads = new HashSet<Thread>();

	@Override
	public void addThread(Thread t) {
		synchronized (threads) {
			threads.add(t);
		}
	}
	
	@Override
	public void stopThreads() {
		synchronized (threads) {
			Iterator<Thread> ti = threads.iterator();
			for (Thread t: threads) t.interrupt();
			while (ti.hasNext()) {
				Thread t = ti.next();
				log.debug("Waiting for thread "+t.getName()+" to join...");
				try {
					t.join();
					ti.remove();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

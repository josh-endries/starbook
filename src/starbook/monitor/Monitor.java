package starbook.monitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;

import starbook.common.BaseNode.Type;
import starbook.common.Edge;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseMultigraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

/**
 * A monitor displays a circular ring of nodes (vertices) and draws edges between the nodes when
 * receiving certain command packets. 
 *
 * @author Josh Endries (josh@endries.org)
 *
 */
public class Monitor {
	protected JFrame frame = new JFrame("Monitor");
	public final Set<Edge> edgeList = Collections.newSetFromMap(new ConcurrentHashMap<Edge, Boolean>());
	public static final int Width = 1050;
	public static final int Height = 875;
	private static final Logger log = Logger.getLogger(Monitor.class);
	private final JLabel label = new JLabel("<html>Protocols                                                                                                                                                                                                                                                                                                                                        <br>   NDP: blue<br>   MDP: green<br>   UDP: red<br><br>Nodes<br>   Web: blue<br>   Worker: green<br>   Index: red</html>", JLabel.LEFT);
	
	
	/**
	 * The delay between redrawing the graph visualization.
	 */
	public static final int LayoutUpdateDelay = 250;

	
	
	/**
	 * key/Class: type/Type, age/DateTime
	 */
	public final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> vertexMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>>();

	
	
	/**
	 * How many seconds a vertex is allowed to be silent before being
	 * removed from the monitor.
	 */
	public static final int VertexCutoffSeconds = 10;
	
	
	
	/**
	 * How many milliseconds to wait in between checking for old vertices. Note
	 * that this process locks both the graph and vertexAges map as it may need
	 * to remove the same item from both, so this delay should probably be
	 * relatively long.
	 */
	protected static final int VertexPrunerDelay = 1000 * 11;
	
	
	
	/**
	 * The amount of milliseconds for which an edge appears on the graph.
	 */
	public static final int EdgeDelay = 500;

	
	
	/**
	 * Transform the given edge (by name) so that its color matches others that were generated from
	 * the same protocol.
	 */
	private final Transformer<String, Paint> edgeGossipTransformer = new Transformer<String, Paint>() {
		@Override
		public Paint transform(String s) {
			if (s.startsWith("ndp")) {
				return Color.BLUE;
			} else if (s.startsWith("mdp") || s.startsWith("mr")) {
				return Color.GREEN;
			} else if (s.startsWith("udp") || s.startsWith("ur")) {
				return Color.RED;
			} else if (s.startsWith("irp")) {
				return Color.CYAN;
			} else {
				log.error(String.format("Unknown prefix %s", s));
				return Color.BLACK;
			}
		}
	};

	
	
	/**
	 * Transform the given edge (based on name) so that certain commands (replication) have thicker
	 * edges than normal communication for that protocol.
	 */
	private final Transformer<String, Stroke> edgeCommandTransformer = new Transformer<String, Stroke>() {
		@Override
		public Stroke transform(String s) {
			Stroke bs;
			if (s.startsWith("mr")) {
				/*
				 * Message replication.
				 */
				bs = new BasicStroke(9.0f);
			} else if (s.startsWith("ur")) {
				/*
				 * User replication.
				 */
				bs = new BasicStroke(9.0f);
			} else {
				bs = new BasicStroke(1.0f);
			}
			return bs;
		}
	};
	
	
	
	/**
	 * Transform the given node (based on name) on the graph to match a color to the type of node.
	 */
	private final Transformer<String, Paint> nodeTypeTransformer = new Transformer<String, Paint>() {
		public Paint transform(String s) {
			ConcurrentHashMap<String, Object> m = vertexMap.get(s);
			if (m != null) {
				Object to = m.get("type");
				if (to != null) {
					Type t = (Type) to;
					switch (t) {
						case Web:
							return Color.BLUE;
						case Worker:
							return Color.GREEN;
						case Index:
							if ((boolean) vertexMap.get(s).get("leader")) {
								return Color.RED;
							} else {
								return Color.getHSBColor((float) 1.0, (float) 0.6, (float) 0.6);
							}
						default:
							log.error(String.format("Unknown node type %s", t));
					}
				}
			}
			return Color.BLACK;
		}
	};
	
	
	
	/**
	 * Create a new monitor and start the updater threads.
	 */
	public Monitor() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		Thread t = new Thread(new StaticLayoutUpdater(), "StaticLayoutUpdater");
		t.start();
		
		Thread p = new Thread(new VertexPruner(this), "VertexPruner");
		p.start();
	}
	
	
	
	/**
	 * Run through all the nodes and edges in the map and create graph edges for them.
	 * 
	 * @return The filled-in graph.
	 */
	public Graph<String, String> getGraph() {
		Graph<String, String> graph = new UndirectedSparseMultigraph<String, String>();
		Iterator<String> vi = vertexMap.keySet().iterator();
		while (vi.hasNext()) graph.addVertex(vi.next());
		Iterator<Edge> ei = edgeList.iterator();
		while (ei.hasNext()) {
			Edge edge = ei.next();
			try {
				graph.addEdge(edge.getName(), edge.getA(), edge.getB());
			} catch (IllegalArgumentException e) {
				/*
				 * Don't do anything; it will be erased soon...
				 */
				e.printStackTrace();
			}
		}
		return graph;
	}
	
	

	/**
	 * Retrieve an iterator over the map of vertices.
	 *  
	 * @return The iterator.
	 * @see Monitor#vertexMap
	 */
	public Iterator<Entry<String, ConcurrentHashMap<String, Object>>> getVertexIterator() { 
		return vertexMap.entrySet().iterator();
	}
	
	
	
	/**
	 * Retrieve the "viewer" (visualization handler) for the provided graph.
	 * 
	 * @param graph The graph to visualize.
	 * @return The viewer.
	 */
	public BasicVisualizationServer<String, String> getViewer(Graph<String, String> graph) {
		Dimension d = new Dimension(Width, Height);
		Layout<String, String> layout = new CircleLayout<String, String>(graph);
		layout.setSize(d);
		layout.initialize();
		BasicVisualizationServer<String, String> vv = new BasicVisualizationServer<String, String>(layout);
		vv.setBackground(Color.GRAY);
		vv.setPreferredSize(d);
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
		vv.getRenderContext().setEdgeDrawPaintTransformer(edgeGossipTransformer);
		vv.getRenderContext().setEdgeStrokeTransformer(edgeCommandTransformer);
		vv.getRenderContext().setVertexFillPaintTransformer(nodeTypeTransformer);
		vv.add(label);
		return vv;
	}
		
	
	
	/**
	 * Updates the graph when it contains a static layout.
	 *
	 * @author Josh Endries (josh@endries.org)
	 *
	 */
	protected class StaticLayoutUpdater implements Runnable {
		private boolean running = false;
		
		@Override
		public void run() {
			running = true;
			while (running) {
				Graph<String, String> g = getGraph();
				BasicVisualizationServer<String, String> vv = getViewer(g);
				Container content = frame.getContentPane();
				content.removeAll();
				content.add(vv);
				frame.pack();
				
				try {
					Thread.sleep(LayoutUpdateDelay);
				} catch (InterruptedException e) {
					running = false;
				}
			}
		}
	}
	
	
	
	/**
	 * Updates the graph when it contains a dynamic layout.
	 * 
	 * TODO: Finish this. Maybe.
	 *
	 * @author Josh Endries (josh@endries.org)
	 *
	 */
	protected class DynamicLayoutUpdater implements Runnable {
		@Override
		public void run() {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					Thread.sleep(LayoutUpdateDelay);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}
}
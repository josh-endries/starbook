package starbook.common;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class Configuration {
	private static final Logger log = Logger.getLogger(Configuration.class);
	protected static boolean initialized = false;
	public static Random rng = new Random();
	
	/**
	 * This stores the application's configuration parameters.
	 */
	private static final Map<String, Object> parameters = new ConcurrentHashMap<String, Object>();

	/**
	 * Retrieve a configuration parameter, or null if the parameter doesn't exist.
	 * 
	 * @param name
	 *            The name of the parameter.
	 * @return The value associated with the given parameter name or null.
	 */
	public static synchronized Object getParameter(String name) {
		return parameters.get(name);
	}

	/*
	 * Helper methods to allow more readable and compact code.
	 */
	public static Boolean getBool(String name) { synchronized (parameters) { return (Boolean) parameters.get(name); } }
	public static int getInt(String name) { synchronized (parameters) { return Integer.valueOf((String) parameters.get(name)); } }
	public static String getStr(String name) { synchronized (parameters) { return (String) parameters.get(name); } }

	/**
	 * Dynamically set configuration parameters.
	 * 
	 * @param name
	 *            The parameter to set.
	 * @param value
	 *            The new value for the given parameter.
	 */
	public static synchronized void setParameter(String name, Object value) {
		log.warn("Reconfiguring \"" + name + "\" to \"" + value.toString() + "\"");
		synchronized (parameters) {
			parameters.put(name, value);
		}
	}

	
	
	/**
	 * The constructor is private because this is a static class.
	 */
	protected Configuration() {}
	
	
	
	/**
	 * Determine if this Configuration is initialized or not.
	 * 
	 * @return True if this Configuration was initialized, false otherwise.
	 */
	public boolean isInitialized() { return (initialized == true); }
	
	

	/**
	 * Initialize the configuration.
	 */
	public void loadParameters(Map<String, Object> p) {
		log.debug("Loading configuration.");
	
		synchronized (parameters) {
			parameters.putAll(p);
		}
		
		log.debug("Configuration loaded successfully.");
		initialized = true;
	}
}
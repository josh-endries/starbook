package starbook.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.json.JSONException;
import org.json.JSONObject;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

public class Util {
	public static InetAddress copy(InetAddress ia) {
		try {
			return InetAddress.getByName(ia.getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static TCPAddress copy(TCPAddress a) {
		TCPAddress address = null;
		try {
			byte[] b = a.getInetAddressAddress().getAddress();
			int p = a.getPort();
			InetAddress ia = InetAddress.getByAddress(b);
			address = new TCPAddress(ia, p);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return address;
	}
	
	public static Set<TCPAddress> copy1(Set<TCPAddress> s1) {
		Set<TCPAddress> s2 = new HashSet<TCPAddress>();
		Iterator<TCPAddress> i = s1.iterator();
		while (i.hasNext()) {
			s2.add(copy(i.next()));
		}
		return s2;
	}
	
	/**
	 * Copy a ConcurrentHashMap with key and value objects that implement copy
	 * constructors (e.g. Object o2 = new Object(o1)).
	 * 
	 * @param map The map to copy.
	 * @return A copy of the map.
	 */
	public static <K extends Object, V extends Object> ConcurrentHashMap<K, V> copyCCCHM(ConcurrentHashMap<K, V> map) {
		ConcurrentHashMap<K, V> newMap = new ConcurrentHashMap<K, V>();

		try {
			Object vo = map.values().iterator().next();
			Object ko = map.keySet().iterator().next();
			try {
				Constructor<? extends Object> vc = vo.getClass().getConstructor(vo.getClass());
				Constructor<? extends Object> kc = ko.getClass().getConstructor(ko.getClass());
				Iterator<Map.Entry<K, V>> i = map.entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry<K, V> e = i.next();
					K oldKey = (K) e.getKey();
					V oldValue = (V) e.getValue();
					try {
						@SuppressWarnings("unchecked")
						K newKey = (K) kc.newInstance(oldKey);
						@SuppressWarnings("unchecked")
						V newValue = (V) vc.newInstance(oldValue);
						newMap.put(newKey, newValue);
					} catch (InstantiationException e1) {
						e1.printStackTrace();
					} catch (IllegalAccessException e1) {
						e1.printStackTrace();
					} catch (IllegalArgumentException e1) {
						e1.printStackTrace();
					} catch (InvocationTargetException e1) {
						e1.printStackTrace();
					}
				}
			} catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			} catch (SecurityException e1) {
				e1.printStackTrace();
			}
		} catch (NoSuchElementException e) {
			/*
			 * The map is empty.
			 */
		}
		
		return newMap;
	}
	
	/**
	 * Retrieve a deep copy of a Set consisting of objects with copy
	 * constructors (e.g. Object o2 = new Object(o1)).
	 * 
	 * @param currentSet The set to copy.
	 * @return A ConcurrentSkipListSet containing a copy of the Set.
	 */
	public static <T extends Object> ConcurrentSkipListSet<T> copyCCS(Set<T> currentSet) {
		ConcurrentSkipListSet<T> newSet = new ConcurrentSkipListSet<T>();
		
		if (currentSet == null || currentSet.size() < 1) return newSet;
		
		Iterator<T> i = currentSet.iterator();
		while (i.hasNext()) {
			T currentObject = i.next();
			try {
				Constructor<? extends Object> c = currentObject.getClass().getConstructor(currentObject.getClass());
				@SuppressWarnings("unchecked")
				T newObject = (T) c.newInstance(currentObject);
				newSet.add(newObject);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return newSet;
	}
	
	/**
	 * Determine how many bytes an object is when serialized.
	 * 
	 * @param s The Serializable object.
	 * @return The number of bytes the object is when serialized, or -1 if an I/O error occurred.
	 */
	public static int getNumBytes(Serializable s) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(s);
			oos.flush();
			return baos.toByteArray().length;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public static String intToIp(int i) {
		return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
	}
	
	public static Long ipToLong(String addr) {
		String[] addrArray = addr.split("\\.");
		long num = 0;
		for (int i=0;i<addrArray.length;i++) {
			int power = 3-i;
			num += ((Integer.parseInt(addrArray[i])%256 * Math.pow(256,power)));
		}
		return num;
	}
	
	public static TCPAddress ISAtoTCPA(InetSocketAddress a) {
		TCPAddress address = new TCPAddress(a.getAddress(), a.getPort());
		return address;
	}

	/**
	 * Return the serialized size (in bytes) of the specified object, or -1 if a problem occurs
	 * while serializing.
	 * 
	 * @param o The {@link Serializable} object for which the size will be determined.
	 * @return The size, in bytes.
	 */
	public static int sizeOf(Serializable o) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int size = -1;
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(o);
			oos.flush();
			size = baos.toByteArray().length;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return size;
	}
	
	public static InetSocketAddress TCPAtoISA(TCPAddress a) {
		InetSocketAddress isa = new InetSocketAddress(a.getInetAddressAddress(), a.getPort());
		return isa;
	}

	
	
	/**
	 * Create a JSONObject from the specified Message.
	 * 
	 * @param message The message to transform.
	 * @return The JSONObject version of the message.
	 */
	public static JSONObject toJSON(Message message) {
		JSONObject jo = null;
		try {
			jo = new JSONObject();
			jo.put("d", message.getCreationDate());
			jo.put("s", message.getSourceAddress());
			jo.put("i", message.getID());
			jo.put("t", message.getTopic());
			jo.put("c", message.getContent());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jo;
	}
}

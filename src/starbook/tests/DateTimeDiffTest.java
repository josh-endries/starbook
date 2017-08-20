package starbook.tests;

import java.net.UnknownHostException;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.json.JSONException;

public class DateTimeDiffTest extends Test {
	public static void main(String[] args) throws UnknownHostException, JSONException {
		DateTime a = DateTime.now();
		DateTime b = DateTime.now().minusMillis(1500);
		System.out.println(a.compareTo(b));
		System.out.println(b.compareTo(a));
		System.out.println(Seconds.secondsBetween(a, b).getSeconds());
		System.out.println(Seconds.secondsBetween(b, a).getSeconds());
	}
}

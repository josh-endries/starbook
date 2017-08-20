package starbook.tests;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;


public class StringSizeTester extends Test {
	public static void main(String[] args) throws Exception {
		String[] strings = { "", "a", "ab", "abc", "abcd", "abcde" };
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		
		for (String s: strings) {
			oos.reset();
			baos.reset();
			oos.writeObject(s);
			oos.flush();
			byte[] bytes = baos.toByteArray();
			StringBuilder sb = new StringBuilder();
			sb.append(s).append(": ").append(s.length()).append("c ").append(bytes.length).append("b");
			System.out.println(sb.toString());
		}
	}
}

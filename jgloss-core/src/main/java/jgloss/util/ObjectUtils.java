package jgloss.util;

public class ObjectUtils {
	public static boolean isEqual(Object o1, Object o2) {
		return o1 == null && o2 == null || o1 != null && o1.equals(o2);
	}
	
	private ObjectUtils() {
	}
}

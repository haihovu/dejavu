/*
 * DjvLogMsg.java
 *
 * Created on October 4, 2006, 2:52 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.dejavu.util;

/**
 * Class representing a log message
 *
 * @author haiv
 */
public class DjvLogMsg {
	/**
	 * Supported log categories
	 */
	public static enum Category {
		/**
		 * This category of log is meant for use by application designers for
		 * diagnostic purposes.
		 */
		DESIGN("Design"),
		/**
		 * This category of log is meant for conveying system status to the
		 * end-users of the applications.
		 */
		MAINTENANCE("Maint"),
		UNKNOWN("Unknown");

		private Category(String str) {
			stringValue = str;
		}
		public final String stringValue;

		@Override
		public String toString() {
			return stringValue;
		}
	}
	/**
	 * Create a new log message, for use by sub-classes.
	 *
	 * @param index Integer index of the message.
	 * @param timestamp The timestamp for the message
	 * @param cat One of DESIGN or MAINTENANCE
	 * @param severity Severity of the message:
	 * <ul>
	 * <li>0 - Error</li>
	 * <li>1 - Warning</li>
	 * <li>2 - Info</li>
	 * <li>3 - Trace</li>
	 * </ul>
	 * @param orig The class that originated this log message.
	 * @param method The method (in the above class) that originated this log
	 * message.
	 * @param msg The log message.
	 */
	protected DjvLogMsg(long index, long timestamp, Category cat, int severity, Class<?> orig, String method, String msg) {
		cat.getClass(); // Null check
		this.index = index;
		this.timestamp = timestamp;
		category = cat;
		this.severity = severity;
		originatorClass = orig;
		originatorClassName = orig != null ? orig.getName() : null;
		originatorMethod = method;
		message = msg;
	}

	/**
	 * Create a new log message, for use by sub-classes.
	 *
	 * @param index Integer index of the message.
	 * @param timestamp The timestamp for the message
	 * @param cat One of DESIGN or MAINTENANCE
	 * @param severity Severity of the message:
	 * <ul>
	 * <li>0 - Error</li>
	 * <li>1 - Warning</li>
	 * <li>2 - Info</li>
	 * <li>3 - Trace</li>
	 * </ul>
	 * @param origClass The name of the class that originated this log message.
	 * @param method The method (in the above class) that originated this log
	 * message.
	 * @param msg The log message.
	 */
	protected DjvLogMsg(long index, long timestamp, Category cat, int severity, String origClass, String method, String msg) {
		cat.getClass(); // Null check
		this.index = index;
		this.timestamp = timestamp;
		category = cat;
		this.severity = severity;
		originatorClass = null;
		originatorClassName = origClass;
		originatorMethod = method;
		message = msg;
	}

	/**
	 * Create a new log message, for use by sub-classes.
	 *
	 * @param index Integer index of the message.
	 * @param cat One of DESIGN or MAINTENANCE
	 * @param severity Severity of the message:
	 * <ul>
	 * <li>0 - Error</li>
	 * <li>1 - Warning</li>
	 * <li>2 - Info</li>
	 * <li>3 - Trace</li>
	 * </ul>
	 * @param orig The class that originated this log message.
	 * @param method The method (in the above class) that originated this log
	 * message.
	 * @param msg The log message.
	 */
	protected DjvLogMsg(long index, Category cat, int severity, Class<?> orig, String method, String msg) {
		this(index, System.currentTimeMillis(), cat, severity, orig, method, msg);
	}

	/**
	 * Create a new log message
	 *
	 * @param cat One of DESIGN or MAINTENANCE
	 * @param severity Severity of the message:
	 * <ul>
	 * <li>0 - Error</li>
	 * <li>1 - Warning</li>
	 * <li>2 - Info</li>
	 * <li>3 - Trace</li>
	 * </ul>
	 * @param orig The class that originated this log message.
	 * @param method The method (in the above class) that originated this log
	 * message.
	 * @param msg The log message.
	 */
	public DjvLogMsg(Category cat, int severity, Class<?> orig, String method, String msg) {
		this(incrementIndex(), cat, severity, orig, method, msg);
	}

	@Override
	public String toString() {
		return new StringBuilder(256).append(logSeverityToString(severity))
				.append(" - ").append(category).append(" - ")
				.append(originatorClassName).append(".").append(originatorMethod != null ? originatorMethod : "none")
				.append("(): ").append(message).toString();
	}

	/**
	 * Retrieves the message timestamp in human readable string format based on
	 * the default timezone/locale.
	 *
	 * @return The string representation of the timestamp, typically yyyy-mm-dd
	 * HH:MM:SS.
	 */
	public String tsToString() {
		try {
			return String.format("%1$tF %1$tT", timestamp);
		} catch (RuntimeException e) {
		}
		return "";
	}

	/**
	 * Converts an integer log severity into the string equivalent.
	 *
	 * @param logSeverity The integer log severity:
	 * <ul>
	 * <li>0 - ERROR</li>
	 * <li>1 - WARNING</li>
	 * <li>2 - INFO</li>
	 * <li>3 - TRACE</li>
	 * </ul>
	 * @return The string associated with the given value, non-null.
	 */
	public static String logSeverityToString(int logSeverity) {
		switch (logSeverity) {
			case 0:
				return "ERR";

			case 1:
				return "WARN";

			case 2:
				return "INFO";

			case 3:
				return "TRC";

			default:
				return "Unknown";
		}
	}

	/**
	 * Retrieves the category/type of this log message.
	 *
	 * @return The log category, non-null.
	 */
	public Category getCategory() {
		return category;
	}

	/**
	 * Increments the index series and returns the next value.
	 *
	 * @return The next increment in the index series
	 */
	private static long incrementIndex() {
		synchronized (DjvLogMsg.class) {
			return gIndex++;
		}
	}

	/**
	 * Specifies the initial index for all log messages.
	 *
	 * @param initialValue The initial log index for this run.
	 */
	public static void initializeIndex(long initialValue) {
		synchronized (DjvLogMsg.class) {
			gIndex = initialValue;
		}
	}

	private final Category category;

	/**
	 * Severity of log:
	 * <ul>
	 * <li>0 - Error</li>
	 * <li>1 - Warning</li>
	 * <li>2 - Info</li>
	 * <li>3 - Trace</li>
	 * </ul>
	 */
	public final int severity;

	public final Class<?> originatorClass;

	public final String originatorClassName;

	public final String originatorMethod;

	public final String message;

	public final long index;

	public final long timestamp;
	private static long gIndex;
}

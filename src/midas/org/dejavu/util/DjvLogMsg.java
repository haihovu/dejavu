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
 * @author haiv
 */
public class DjvLogMsg
{
	public static enum Category
	{
		/**
		 * This category of log is meant for use by application designers for diagnostic purposes.
		 */
		DESIGN("Design"),
		/**
		 * This category of log is meant for conveying system status to the end-users of the applications.
		 */
		MAINTENANCE("Maint"),
		UNKNOWN("Unknown");
		private Category(String str)
		{
			stringValue = str;
		}
		public final String stringValue;
		@Override
		public String toString()
		{
			return stringValue;
		}

		/**
		 * Converts an integer log category/type value into its enumeration equivalent
		 * @param value The integer value to be converted, must be one of:
		 * <ul>
		 * <li>DjvSystem.SYSTEM_MAINTENANCE_LOG</li>
		 * <li>DjvSystem.SYSTEM_SOFTWARE_LOG</li>
		 * </ul>
		 * @return The equivalent log category enumeration, non-null (default is UNKNOWN).
		 */
		static Category intToCategory(int value)
		{
			switch(value)
			{
				case DjvSystem.SYSTEM_MAINTENANCE_LOG:
					return MAINTENANCE;

				case DjvSystem.SYSTEM_SOFTWARE_LOG:
					return DESIGN;
			}
			return UNKNOWN;
		}
		
		/**
		 * Converts a  log category enumeration into its legacy integer equivalent
		 * @param value The category enumeration value to be converted
		 * @return The equivalent log category integer value, default is DjvSystem.SYSTEM_SOFTWARE_LOG.
		 */
		private static int categoryToInt(Category value)
		{
			switch(value)
			{
				case MAINTENANCE:
					return DjvSystem.SYSTEM_MAINTENANCE_LOG;
			}
			return DjvSystem.SYSTEM_SOFTWARE_LOG;
		}
	}

	/**
	 * Create a new log message, for use by sub-classes.
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
	 * @param method The method (in the above class) that originated this log message.
	 * @param msg The log message.
	 */
	protected DjvLogMsg(long index, long timestamp, Category cat, int severity, Class orig, String method, String msg)
	{
		cat.getClass(); // Null check
		m_Index = index;
		m_Timestamp = timestamp;
		m_Category = cat;
		m_Severity = severity;
		m_OriginatorClass = orig;
		m_OriginatorClassName = orig != null ? orig.getName() : null;
		m_OriginatorMethod = method;
		m_Message = msg;
	}
	
	/**
	 * Create a new log message, for use by sub-classes.
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
	 * @param method The method (in the above class) that originated this log message.
	 * @param msg The log message.
	 */
	protected DjvLogMsg(long index, long timestamp, Category cat, int severity, String origClass, String method, String msg)
	{
		cat.getClass(); // Null check
		m_Index = index;
		m_Timestamp = timestamp;
		m_Category = cat;
		m_Severity = severity;
		m_OriginatorClass = null;
		m_OriginatorClassName = origClass;
		m_OriginatorMethod = method;
		m_Message = msg;
	}
	
	/**
	 * Create a new log message, for use by sub-classes.
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
	 * @param method The method (in the above class) that originated this log message.
	 * @param msg The log message.
	 */
	protected DjvLogMsg(long index, Category cat, int severity, Class orig, String method, String msg)
	{
		this(index, System.currentTimeMillis(), cat, severity, orig, method, msg);
	}
	
	/**
	 * Create a new log message
	 * @param cat One of DESIGN or MAINTENANCE
	 * @param severity Severity of the message:
	 * <ul>
	 * <li>0 - Error</li>
	 * <li>1 - Warning</li>
	 * <li>2 - Info</li>
	 * <li>3 - Trace</li>
	 * </ul>
	 * @param orig The class that originated this log message.
	 * @param method The method (in the above class) that originated this log message.
	 * @param msg The log message.
	 */
	public DjvLogMsg(Category cat, int severity, Class orig, String method, String msg)
	{
		this(incrementIndex(), cat, severity, orig, method, msg);
	}

	@Override
	public String toString()
	{
		return new StringBuilder(256).append(logSeverityToString(m_Severity))
			.append(" - ").append(m_Category).append(" - ")
			.append(m_OriginatorClassName).append(".").append(m_OriginatorMethod != null ? m_OriginatorMethod : "none")
			.append("(): ").append(m_Message).toString();
	}

	/**
	 * Retrieves the message timestamp in human readable string format based on the default timezone/locale.
	 * @return The string representation of the timestamp, typically yyyy-mm-dd HH:MM:SS.
	 */
	public String tsToString()
	{
		try
		{
			return String.format("%1$tF %1$tT", m_Timestamp);
		}
		catch(RuntimeException e){}
		return "";
	}

	/**
	 * Converts an integer log type/category (DjvSystem.SYSTEM_MAINTENANCE_LOG | DjvSystem.SYSTEM_SOFTWARE_LOG) into the string equivalent.
	 * @param logType The integer value to be converted, DjvSystem.SYSTEM_MAINTENANCE_LOG | DjvSystem.SYSTEM_SOFTWARE_LOG.
	 * @return The string associated with the given value, non-null.
	 */
	public static String logTypeToString(int logType)
	{
		return Category.intToCategory(logType).toString();
	}

	/**
	 * Converts an integer log severity into the string equivalent.
	 * @param logSeverity The integer log severity:
	 * <ul>
	 * <li>0 - ERROR</li>
	 * <li>1 - WARNING</li>
	 * <li>2 - INFO</li>
	 * <li>3 - TRACE</li>
	 * </ul>
	 * @return The string associated with the given value, non-null.
	 */
	public static String logSeverityToString(int logSeverity)
	{
		switch(logSeverity)
		{
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
	 * @return The log category, non-null.
	 */
	public Category getCategory()
	{
		return m_Category;
	}
	
	/**
	 * Retrieves the integer type/category of this log message.
	 * @return The log type, one of:
	 * <ul>
	 * <li>DjvSystem.SYSTEM_SOFTWARE_LOG</li>
	 * <li>DjvSystem.SYSTEM_MAINTENANCE_LOG</li>
	 * </ul>
	 */
	public int getType()
	{
		return Category.categoryToInt(m_Category);
	}
	
	/**
	 * Increments the index series and returns the next value.
	 * @return The next increment in the index series
	 */
	private static long incrementIndex() {
		synchronized(s_Lock) {
			return s_Index++;
		}
	}
	
	/**
	 * Specifies the initial index for all log messages.
	 * @param initialValue The initial log index for this run.
	 */
	public static void initializeIndex(long initialValue) {
		synchronized(s_Lock) {
			s_Index = initialValue;
		}
	}
	
	private final Category m_Category;
	
	/**
	 * Severity of log:
	 * <ul>
	 * <li>0 - Error</li>
	 * <li>1 - Warning</li>
	 * <li>2 - Info</li>
	 * <li>3 - Trace</li>
	 * </ul>
	 */
	public final int m_Severity;

	public final Class m_OriginatorClass;

	public final String m_OriginatorClassName;

	public final String m_OriginatorMethod;
	
	public final String m_Message;
	
	public final long m_Index;
	
	public final long m_Timestamp;
	private static long s_Index;
	private static final Object s_Lock = new Object();
}

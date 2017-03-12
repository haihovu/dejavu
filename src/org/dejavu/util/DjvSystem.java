/* Generated by Together */
package org.dejavu.util;

import java.io.*;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * System utility containing system-related functions. This is an abstract class
 * that applications can extend in order to customize behaviour such as how
 * system properties are managed. A default implementation is provided by MiDAS,
 * {@link DjvSystemDefault}, it is recommended that application extend
 * DjvSystemDefault instead of DjvSystem directly.
 */
public abstract class DjvSystem {

	/**
	 * @supplierCardinality 1
	 * @label singletonImpl
	 */
	private static DjvSystem s_Singleton;

	/** @link dependency */
	/*# DjvSystemDefault lnkMiSystemDefault; */
	/**
	 * Determines whether the log level is set to include trace, i.e. log level
	 * > 2.
	 *
	 * @return True if trace is on, false otherwise.
	 */
	public static boolean traceOn() {
		return (getLogLevel() > 2);
	}

	/**
	 * Given a fully qualified directory name, create it along with any parent
	 * directories required.
	 *
	 * @param dir The fully qualified name of the directory to be created.
	 * @param deleteOnExit Flag indicating whether the created directory will be
	 * automatically deleted on exit.
	 */
	private static void createDir(File dir, boolean deleteOnExit) {
		File parent = dir.getParentFile();
		if (parent != null) {
			createDir(parent, deleteOnExit);
		}
		if (!dir.exists()) {
			dir.mkdir();
			if (deleteOnExit) {
				dir.deleteOnExit();
			}
		}
	}

	/**
	 * Sets the system log level, the higher the level, the more verbose the log
	 * output will be.
	 *
	 * @param logLevel The log level as described below:
	 * <ul>
	 * <li>0 - Only error messages will be generated.</li>
	 * <li>1 - Error & Warning messages will be generated.</li>
	 * <li>2 - Error & Warning & Info messages will be generated.</li>
	 * <li>3 - Error & Warning & Info & Trace messages will be generated.</li>
	 * </ul>
	 */
	public static void setLogLevel(int logLevel) {
		gLogLevel = logLevel;

		DjvLogHandler logHandler = getLogHandler();
		setProperty(DjvSystemProperty.MIPROPERTY_LOG_LEVEL, String.valueOf(logLevel));
		if (null != logHandler) {
			logHandler.setLogFilterLevel(logLevel);
		}
	}

	/**
	 * Retrieves the current system log level, the higher the level, the more
	 * verbose the log output will be. The log levels are described below:
	 * <ul>
	 * <li>0 - Only error messages will be generated.</li>
	 * <li>1 - Error & Warning messages will be generated.</li>
	 * <li>2 - Error & Warning & Info messages will be generated.</li>
	 * <li>3 - Error & Warning & Info & Trace messages will be generated.</li>
	 * </ul>
	 *
	 * @return The log level currently set.
	 */
	public static int getLogLevel() {
		DjvLogHandler handler = getLogHandler();
		if (null != handler) {
			return handler.getLogFilterLevel();
		}
		return gLogLevel;
	}

	/**
	 * Sets the version of a component.
	 *
	 * @param component Name of component whose version is to be set
	 * @param version The new version value
	 */
	public static void setVersion(String component, String version) {
		getImpl().sysSetVersion(component, version);
	}

	/**
	 * Gets the version of a component.
	 *
	 * @param component The target component whose version is being requested.
	 * @return The version of the desired component or empty string if the
	 * specified component could not be found.
	 */
	public static String getVersion(String component) {
		return getImpl().sysGetVersion(component);
	}

	private static StackTraceElement retrieveCallerTrace(int skip, StackTraceElement[] traces) {
		if (traces.length > skip) {
			return traces[skip];
		} else if (traces.length > 0) {
			return traces[traces.length - 1];
		}
		return Thread.currentThread().getStackTrace()[0];
	}

	/**
	 * Generate a trace log message. Typically used by designer to generate
	 * debug messages.
	 *
	 * @param cat The category of the log message being generated
	 * @param srcClass Class of the object from which the log is generated, just
	 * a way for designer to figure out where the log came from
	 * @param functionName Name of the function from whence the log came, again
	 * just a way for designer to figure out where the log came from
	 * @param message The log message
	 * @return The log message that was generated or null if none was generated,
	 * e.g. if the filter causes the message to be discarded.
	 */
	public static DjvLogMsg logTrace(DjvLogMsg.Category cat, Class srcClass, String functionName, String message) {
		if (getLogLevel() < 3) {
			// Trace is filtered
			return null;
		}

		DjvLogHandler logHandler = getLogHandler();
		if (null != logHandler) {
			return logHandler.logMsg(cat, 3, srcClass, functionName, message);
		}
		return null;
	}

	/**
	 * Generate an info log message. Typically used by designer to record
	 * progress events.
	 *
	 * @param cat The category of the log message being generated
	 * @param srcClass Class of the object from which the log is generated, just
	 * a way for designer to figure out where the log came from
	 * @param functionName Name of the function from whence the log came, again
	 * just a way for designer to figure out where the log came from
	 * @param message The log message
	 * @return The log message that was generated or null if none was generated,
	 * e.g. if the filter causes the message to be discarded.
	 */
	public static DjvLogMsg logInfo(DjvLogMsg.Category cat, Class srcClass, String functionName, String message) {
		if (getLogLevel() < 2) {
			// Info is filtered
			return null;
		}

		DjvLogHandler logHandler = getLogHandler();
		if (null != logHandler) {
			return logHandler.logMsg(cat, 2, srcClass, functionName, message);
		}
		return null;
	}

	/**
	 * Generate an info log message. Typically used by designer to record
	 * progress events.
	 *
	 * @param cat The category of the log message being generated
	 * @param message The log message
	 * @return The log message that was generated or null if none was generated,
	 * e.g. if the filter causes the message to be discarded.
	 */
	public static DjvLogMsg logInfo(DjvLogMsg.Category cat, String message) {
		Class srcClass = DjvSystem.class;
		StackTraceElement callerTrace = retrieveCallerTrace(2, Thread.currentThread().getStackTrace());
		try {
			srcClass = Class.forName(callerTrace.getClassName());
		} catch (ClassNotFoundException ex) {
		}
		return logInfo(cat, srcClass, callerTrace.getMethodName(), message);
	}

	/**
	 * Generate a warning log message. Significant events typically denote
	 * unusual runtime conditions.
	 *
	 * @param cat The category of the log message being generated
	 * @param srcClass Class of the object from which the log is generated, just
	 * a way for designer to figure out where the log came from
	 * @param functionName Name of the function from whence the log came, again
	 * just a way for designer to figure out where the log came from
	 * @param message The log message
	 * @return The log message that was generated or null if none was generated,
	 * e.g. if the filter causes the message to be discarded.
	 */
	public static DjvLogMsg logWarning(DjvLogMsg.Category cat, Class srcClass, String functionName, String message) {
		if (getLogLevel() < 1) {
			// Warning is filtered
			return null;
		}

		DjvLogHandler logHandler = getLogHandler();
		if (null != logHandler) {
			return logHandler.logMsg(cat, 1, srcClass, functionName, message);
		}
		return null;
	}

	/**
	 * Generate a warning log message. Significant events typically denote logic
	 * failure, things that should never happen but did.
	 *
	 * @param cat The category of the log message being generated
	 * @param message The log message
	 * @return The log message that was generated or null if none was generated,
	 * e.g. if the filter causes the message to be discarded.
	 */
	public static DjvLogMsg logWarning(DjvLogMsg.Category cat, String message) {
		Class srcClass = DjvSystem.class;
		StackTraceElement callerTrace = retrieveCallerTrace(2, Thread.currentThread().getStackTrace());
		try {
			srcClass = Class.forName(callerTrace.getClassName());
		} catch (ClassNotFoundException ex) {
		}
		return logWarning(cat, srcClass, callerTrace.getMethodName(), message);
	}

	/**
	 * Generate an error log message. Significant events typically denote logic
	 * failure, things that should never happen but did.
	 *
	 * @param cat The category of the log message being generated
	 * @param srcClass Class of the object from which the log is generated, just
	 * a way for designer to figure out where the log came from
	 * @param functionName Name of the function from whence the log came, again
	 * just a way for designer to figure out where the log came from
	 * @param message The log message
	 * @return The log message that was generated or null if none was generated,
	 * e.g. if the filter causes the message to be discarded.
	 */
	public static DjvLogMsg logError(DjvLogMsg.Category cat, Class srcClass, String functionName, String message) {
		DjvLogHandler logHandler = getLogHandler();
		if (null != logHandler) {
			return logHandler.logMsg(cat, 0, srcClass, functionName, message);
		}
		return null;
	}

	/**
	 * Generate an error log message. Significant events typically denote logic
	 * failure, things that should never happen but did.
	 *
	 * @param cat The category of the log message being generated
	 * @param message The log message
	 * @return The log message that was generated or null if none was generated,
	 * e.g. if the filter causes the message to be discarded.
	 */
	public static DjvLogMsg logError(DjvLogMsg.Category cat, String message) {
		Class srcClass = DjvSystem.class;
		StackTraceElement callerTrace = retrieveCallerTrace(2, Thread.currentThread().getStackTrace());
		try {
			srcClass = Class.forName(callerTrace.getClassName());
		} catch (ClassNotFoundException ex) {
		}
		return logError(cat, srcClass, callerTrace.getMethodName(), message);
	}

	/**
	 * Retrieves a system property identified by its unique name.
	 *
	 * @param propertyName Name of the desired property
	 * @return The value of the requested property, empty string if there is no
	 * property matching the specified name.
	 */
	public static String getProperty(String propertyName) {
		return getImpl().sysGetProperty(propertyName);
	}

	/**
	 * Sets a system property.
	 *
	 * @param propertyName Name of the desired property
	 * @param value The new value with which to set the specified property.
	 */
	public static void setProperty(String propertyName, String value) {
		getImpl().sysSetProperty(propertyName, value);
	}

	/**
	 * Imports system properties from an XML document that was previously
	 * exported via exportProperties(). Typically used at startup to restore the
	 * system properties from some saved files.
	 *
	 * @param xmlDoc The XML document containing the properties to be imported.
	 */
	public static void importProperties(Document xmlDoc) {
		getImpl().sysImportProperties(xmlDoc);
	}

	/**
	 * Exports all system properties to an XML element. Typically used at
	 * shutdown to save the system properties to some files.
	 *
	 * @param xmlDoc The root XML document used to generate XML elements
	 * representing the system properties.
	 * @return The top-level XML element containing all system properties.
	 */
	public static Element exportProperties(Document xmlDoc) {
		return getImpl().sysExportProperties(xmlDoc);
	}

	/**
	 * Retrieves ALL system properties.
	 *
	 * @return The set of name-value pairs representing all system properties.
	 */
	public static Set<Map.Entry<String, String>> getProperties() {
		return getImpl().sysGetProperties();
	}

	/**
	 * Registers a concrete implementation of the DjvLogHandler class. Only one
	 * instance of DjvLogHandler may be registered. Log requests are forwarded
	 * to the registered handler.
	 *
	 * @param handler The concrete MiLogHandlder implementation.
	 */
	public static void registerLogHandler(DjvLogHandler handler) {
		synchronized (gLogLock) {
			if (null != gLogHandler) {
				gLogHandler.stop();
			}
			gLogHandler = handler;
		}
	}

	/**
	 * Deregisters the implementation of the DjvLogHandler class.
	 */
	public static void deregisterLogHandler() {
		synchronized (gLogLock) {
			if (null != gLogHandler) {
				gLogHandler.stop();
			}
			gLogHandler = null;
		}
	}

	/**
	 * Retrieves the registered DjvLogHandler implementation.
	 *
	 * @return The registered log handler, or a default one if none was
	 * registered.
	 */
	public static DjvLogHandler getLogHandler() {
		synchronized (gLogLock) {
			if (null == gLogHandler) {
				// If no log handler already registered then use a simple implementation
				gLogHandler = new DjvLogHandler() {
					private int m_LogLevel = 2;

					@Override
					public synchronized int getLogFilterLevel() {
						return m_LogLevel;
					}

					@Override
					public DjvLogMsg logMsg(DjvLogMsg.Category category, int severity, Class origClass, String origMethod, String message) {
						return logMsg(new DjvLogMsg(category, severity, origClass, origMethod, message));
					}

					@Override
					public DjvLogMsg logMsg(DjvLogMsg msg) {
						System.out.println(msg.tsToString() + " - " + msg.toString());
						return msg;
					}

					@Override
					public synchronized void setLogFilterLevel(int logLevel) {
						m_LogLevel = logLevel;
					}

					@Override
					public void start() {
					}

					@Override
					public void stop() {
					}
				};
			}
			return gLogHandler;
		}
	}

	/**
	 * Registers a concrete implementation of the DjvSystem class. Typically
	 * done at start up by the application.
	 *
	 * @param impl The concrete implementation.
	 */
	public static void registerImpl(DjvSystem impl) {
		if (impl != null) {
			synchronized (DjvSystem.class) {
				s_Singleton = impl;
			}
		}
	}

	/**
	 * Deregisters the implementation.
	 *
	 * @param impl The concrete implementation to deregister.
	 */
	public static void deregisterImpl(DjvSystem impl) {
		synchronized (DjvSystem.class) {
			if (s_Singleton == impl) {
				s_Singleton = null;
			}
		}
	}

	/**
	 * Retrieves the registered DjvSystem implementation.
	 *
	 * @return The regsitered implementation.
	 */
	private static DjvSystem getImpl() {
		synchronized (DjvSystem.class) {
			if (null == s_Singleton) {
				s_Singleton = new DjvSystemDefault();
			}
			return s_Singleton;
		}
	}

	/**
	 * Default constructor.
	 */
	protected DjvSystem() {
	}

	/**
	 * Sets the version of a component. To be implemented by concrete
	 * sub-classes.
	 *
	 * @param component The name of the target component.
	 * @param version The version to set
	 */
	protected abstract void sysSetVersion(String component, String version);

	/**
	 * Gets the version of a component. To be implemented by concrete
	 * sub-classes.
	 *
	 * @param component The name of the component whose version is being
	 * requested.
	 * @return The version of the requested component, or empty string if
	 * component does not exist.
	 */
	protected abstract String sysGetVersion(String component);

	/**
	 * Retrieves the value of a system property. To be implemented by concrete
	 * sub-classes.
	 *
	 * @param propertyName Name of the desired property.
	 * @return The value of the requested property, empty string if there is no
	 * property matching the specified name.
	 */
	protected abstract String sysGetProperty(String propertyName);

	/**
	 * Sets a system property. To be implemented by concrete sub-classes.
	 *
	 * @param propertyName Name of the desired property
	 * @param value The new value with which to set the specified property.
	 */
	protected abstract void sysSetProperty(String propertyName, String value);

	/**
	 * Exports all system properties to an XML element. To be implemented by
	 * concrete sub-classes. Typically used at shutdown to save the system
	 * properties to some files. To be implemented by concrete sub-classes.
	 *
	 * @param ownerDoc The root XML document used to generate XML elements
	 * representing the system properties.
	 * @return The top-level XML element containing all system properties.
	 */
	protected abstract Element sysExportProperties(Document ownerDoc);

	/**
	 * Imports system properties from an XML document that was previously
	 * exported via exportProperties(). Typically used at startup to restore the
	 * system properties from some saved files. To be implemented by concrete
	 * sub-classes.
	 *
	 * @param xmlDocument The XML document containing the properties to be
	 * imported.
	 */
	protected abstract void sysImportProperties(Document xmlDocument);

	/**
	 * Retrieves ALL system properties.
	 *
	 * @return The set of name-value pairs representing all system properties.
	 */
	protected abstract Set<Map.Entry<String, String>> sysGetProperties();

	/**
	 * The registered log handler
	 */
	private static volatile DjvLogHandler gLogHandler;
	/**
	 * The log filter level
	 */
	private static volatile int gLogLevel;
	/**
	 * Used for synchronization when dealing with logging.
	 */
	private static final Object gLogLock = new Object();

	private static volatile boolean gDiagnosticEnabled;

	public static void enableDiagnostic() {
		gDiagnosticEnabled = true;
		setProperty(DjvSystemProperty.MIPROPERTY_DIAGNOSTIC_ENABLED, Boolean.toString(true));
	}

	public static void disableDiagnostic() {
		gDiagnosticEnabled = false;
		setProperty(DjvSystemProperty.MIPROPERTY_DIAGNOSTIC_ENABLED, Boolean.toString(false));
	}

	public static boolean diagnosticEnabled() {
		return gDiagnosticEnabled;
	}
}

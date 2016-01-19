/*
 * MiLogHandler.java
 *
 * Created on October 4, 2006, 2:51 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.mitel.miutil;

/**
 * Interface for implementing log handling logic.
 * @author haiv
 */
public interface MiLogHandler
{
	/**
	 * Logs a message.
	 * @param category The category of the message
	 * @param severity The severity of the message
	 * @param origClass The class from which the message was originated
	 * @param origMethod The method from which the message was originated
	 * @param message The text message to log.
	 * @return The log message that was saved. Not null.
	 */
	public MiLogMsg logMsg(MiLogMsg.Category category, int severity, Class origClass, String origMethod, String message);

	/**
	 * Logs a message.
	 * @param msg The message to log
	 * @return The same log message that was passed in.
	 */
	public MiLogMsg logMsg(MiLogMsg msg);

	/**
	 * Sets the system log filter level, the higher the level, the more verbose the log output will be.
	 * @param logLevel The log level as described below:
	 * <ul>
	 * <li>0 - Only error messages will be logged.</li>
	 * <li>1 - Error & Warning messages will be logged.</li>
	 * <li>2 - Error & Warning & Info messages will be logged.</li>
	 * <li>3 - Error & Warning & Info & Trace messages will be logged.</li>
	 * </ul>
	 */
	public void setLogFilterLevel(int logLevel);

	/**
	 * Retrieves the current system log filter level, the higher the level, the more verbose the log output will be.
	 * The log levels are described below:
	 * <ul>
	 * <li>0 - Only error messages will be logged.</li>
	 * <li>1 - Error & Warning messages will be logged.</li>
	 * <li>2 - Error & Warning & Info messages will be logged.</li>
	 * <li>3 - Error & Warning & Info & Trace messages will be logged.</li>
	 * </ul>
	 * @return The log filter level currently set.
	 */
	public int getLogFilterLevel();

	/**
	 * Starts the log handler.
	 */
	public void start();

	/**
	 * Stops the log handler.
	 */
	public void stop();
}

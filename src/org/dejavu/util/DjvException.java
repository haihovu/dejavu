/*
 * DjvException.java
 *
 * Created on July 4, 2005, 12:09 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package org.dejavu.util;

/**
 * Customised exception for use in the Dejavu libraries.
 *
 * @author haiv
 */
public class DjvException extends Exception {

	/**
	 * Creates a new instance of DjvException
	 *
	 * @param msg Arbitrary message associated with the exception.
	 */
	public DjvException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new instance of DjvException
	 *
	 * @param msg Arbitrary message associated with the exception.
	 * @param cause Root cause of this exception.
	 */
	public DjvException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Creates a new instance of DjvException
	 *
	 * @param cause Root cause of this exception.
	 */
	public DjvException(Throwable cause) {
		super(cause);
	}
}

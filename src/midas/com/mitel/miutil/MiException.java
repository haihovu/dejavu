/*
 * MiException.java
 *
 * Created on July 4, 2005, 12:09 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.mitel.miutil;

/**
 *
 * @author haiv
 */
public class MiException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	/** Creates a new instance of MiException
	 * @param desc
	 */
	public MiException(String desc)
	{
		super(desc);
	}

	/** Creates a new instance of MiException
	 * @param msg 
	 * @param cause
	 */
	public MiException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	/** Creates a new instance of MiException
	 * @param cause
	 */
	public MiException(Throwable cause)
	{
		super(cause);
	}
}

/*
 * IcpException.java
 *
 * Created on September 29, 2004, 1:35 PM
 */

package com.mitel.icp;

/**
 *
 * @author  haiv
 */
public class IcpException extends Exception
{
	
	/** Creates a new instance of IcpException */
	public IcpException(String desc)
	{
		super(desc);
	}
	
	/** Creates a new instance of IcpException */
	public IcpException(Throwable cause)
	{
		super(cause);
	}
	
}

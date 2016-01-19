/*
 * MiVersionException.java
 *
 * Created on December 13, 2006, 12:25 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.mitel;

/**
 *
 * @author haiv
 */
public class MiVersionException extends Exception
{
	
	/** Creates a new instance of MiVersionException */
	public MiVersionException(String desc)
	{
		super(desc);
	}
	
	/** Creates a new instance of MiVersionException */
	public MiVersionException(Throwable cause)
	{
		super(cause);
	}
	
}

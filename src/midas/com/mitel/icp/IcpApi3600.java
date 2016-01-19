/*
 * IcpApi3300.java
 *
 * Created on April 28, 2004, 10:53 AM
 */

package com.mitel.icp;

/**
 *
 * @author  haiv
 */
public class IcpApi3600 extends IcpApi3300_r60
{	
	/** Creates a new instance of IcpApi3600 */
	public IcpApi3600()
	{
		super("3600", IcpTypeInfo.IcpClass.MCD_Server);
	}	
}

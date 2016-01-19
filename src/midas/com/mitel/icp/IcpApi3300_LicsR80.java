/*
 * IcpApi3300.java
 *
 * Created on April 28, 2004, 10:53 AM
 */

package com.mitel.icp;

import com.mitel.httputil.HttpListener;

/**
 * This represents newer MCD-style servers with/without E2T cards, e.g. x86 MCD ISS.
 * @author  haiv
 */
class IcpApi3300_LicsR80 extends IcpApi3300_r60 implements IcpType, HttpListener
{	
	/** Creates a new instance of IcpApi3300_LicsR80
	 * @param typeName Name of the ICP type.
	 * @param icpClass Class of MCD. 
	 */
	IcpApi3300_LicsR80(String typeName, IcpTypeInfo.IcpClass icpClass)
	{
		super(typeName, icpClass);
	}	
}

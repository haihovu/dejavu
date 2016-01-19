/*
 * IcpCancelRequestRunnable.java
 *
 * Created on April 29, 2004, 11:39 PM
 */

package com.mitel.icp;

/**
 * Used by concrete ICP type implementations to return to clients for cancelling
 * requests. This basically invokes cancelRequest() on the originating ICP
 * instance.
 * @author Hai Vu
 */
public class IcpCancelRequestRunnable implements Runnable
{
	private IcpType m_myIcp;
	
	/** Creates a new instance of IcpCancelRequestRunnable */
	public IcpCancelRequestRunnable(IcpType theIcp)
	{
		m_myIcp = theIcp;
	}
	
	public void run()
	{
		if( null != m_myIcp )
		{
			m_myIcp.cancelRequest();
		}
	}
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dejavu.netutil;

import org.dejavu.util.DjvExceptionUtil;
import org.dejavu.util.DjvLogMsg.Category;
import org.dejavu.util.DjvSystem;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Class utility dealing with network interfaces on the local host.
 * @author haiv
 */
public class DjvNetworkInterfaceUtil
{
	/**
	 * Finds the local network interface(s) that is capable of reaching a particular remote address.
	 * Loopbacks and virtuals are not included in the result.
	 * @param remoteAddr The desired remote endpoint. If null then the returned list will contain ALL usable network interfaces for this host.
	 * @param waitMs Amount of time to wait while attempting to reach the far end. Negative is illegal but not sure what zero means, better to avoid those.
	 * @return The list of local network interfaces that can be used to reach the remote address. Non null.
	 */
	public static List<NetworkInterface> locateInterface(InetAddress remoteAddr, int waitMs)
	{
		List<NetworkInterface> ret = new ArrayList<>(16);
		try
		{
			Enumeration<NetworkInterface> networkIfs = NetworkInterface.getNetworkInterfaces();
			while(networkIfs.hasMoreElements())
			{
				NetworkInterface networkIf = networkIfs.nextElement();
				
				try
				{
					// Ignore network interfaces that are down
					if(!networkIf.isUp())
						continue;

					// Ignore network interfaces that are virtual
					if(networkIf.isVirtual())
						continue;

					// Exclude loopbacks
					if(networkIf.isLoopback())
						continue;

					if(remoteAddr == null)
					{
						ret.add(networkIf);
					}
					else if(remoteAddr.isReachable(networkIf, 0, waitMs))
					{
						ret.add(networkIf);
					}
				}
				catch(IOException ex)
				{
					DjvSystem.logWarning(Category.DESIGN,
						"Encountered " + ex + " while examining " + networkIf + " at "
						+ DjvExceptionUtil.getCompressedTrace(ex));
				}
			}
		}
		catch(SocketException ex)
		{
			DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
		}
		return ret;
	}
	
	/**
	 * Finds the network addresses that are capable of reaching a particular remote address.
	 * Loopbacks and virtuals are not included in th result.
	 * @param remoteAddr The desired remote endpoint. If null then the returned list will include ALL usable network interfaces for this host.
	 * @param ipv4Only Flag indicating whether the caller is only interested in IPV4 addresses (or all types)
	 * @param waitMs Amount of time to wait while attempting to reach the far end. Negative is illegal but not sure what zero means, better to avoid those.
	 * @return The list of network addresses that can be used to reach the remote address. Non null.
	 */
	public static List<InetAddress> locateAddresses(InetAddress remoteAddr, boolean ipv4Only, int waitMs)
	{
		List<InetAddress> ret = new ArrayList<>(16);
		List<NetworkInterface> ifs = locateInterface(remoteAddr, waitMs);
		if(ifs.isEmpty())
		{
			if(remoteAddr != null)
			{
				DjvSystem.logWarning(Category.DESIGN,
					"Failed to locate any interface that can reach " + remoteAddr);
			}
			else
			{
				DjvSystem.logWarning(Category.DESIGN,
					"Failed to locate any network interface");
			}
		}
		ifs.stream().map((netif) -> netif.getInetAddresses()).forEach((addrs) -> {
			while(addrs.hasMoreElements())
			{
				InetAddress addr = addrs.nextElement();
				if(addr.isLoopbackAddress())
					continue;
				if(ipv4Only && (!(addr instanceof Inet4Address)))
					continue;
				ret.add(addr);
			}
		});
		return ret;
	}
}

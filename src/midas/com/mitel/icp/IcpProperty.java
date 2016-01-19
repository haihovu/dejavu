/*
 * MiIcpProperty.java
 *
 * Created on December 9, 2006, 10:03 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.mitel.icp;

import com.mitel.miutil.MiSystem;
import com.mitel.miutil.MiSystemProperty;

/**
 *
 * @author haiv
 */
public class IcpProperty extends MiSystemProperty
{
	public static final String MIPROPERTY_DEFAULT_MINET_VERSION = "DefaultMinetVersion";
	
	/** Creates a new instance of MiIcpProperty */
	public IcpProperty()
	{
	}
	
	/**
	 * Retrieves the default MiNET version for this application.
	 * Depending on how the application sets up the MiSystem's property implementation,
	 * this information may come from a database, a file, or just from memory.
	 * @return The default MiNET version, non-null.
	 */
	public static String getDefaultMiNetVersion()
	{
		String ret = MiSystem.getProperty(MIPROPERTY_DEFAULT_MINET_VERSION);
		if(ret.length() < 1)
		{
			ret = "1.2.9";
			setDefaultMiNetVersion(ret);
		}
		return ret;
	}

	/**
	 * Sets the default MiNET version for this application.
	 * Depending on how the application sets up the MiSystem's property implementation,
	 * this information may be saved to a database, a file, or just to memory.
	 * @param version The new default MiNET version.
	 */
	public static void setDefaultMiNetVersion(String version)
	{
		MiSystem.setProperty(MIPROPERTY_DEFAULT_MINET_VERSION, version);
	}

}

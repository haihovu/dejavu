/*
 * MiDASLib.java
 *
 * Created on December 13, 2006, 11:02 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.mitel;

import java.util.regex.Pattern;

/**
 * Class utility representing the midas library.
 * @author haiv
 */
public class MiDASLib
{
	private static final Pattern s_SuppportedVersions = Pattern.compile("1\\.1\\.[0-2]");
	
	/**
	 * Determines whether this library supports the specified version.
	 * @param version The version string to be checked.
	 * @throws MiVersionException
	 */
	public static void versionCheck(String version) throws MiVersionException
	{
		if(!s_SuppportedVersions.matcher(version).find())
		{
			throw new MiVersionException(MiDASLib.class + " version " + version + " not supported");
		}
	}
	
	/**
	 * Retrieves the current version of this library.
	 * @return The version string in format major_ver.minor_ver.build_ver
	 */
	public static String getVersion()
	{
		return "1.1.2";
	}
}

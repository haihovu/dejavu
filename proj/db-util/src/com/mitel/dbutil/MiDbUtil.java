/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mitel.dbutil;

import java.net.URL;

/**
 * Class utility to manage the MiDAS database utility system.
 */
public class MiDbUtil
{
	/**
	 * Initializes the MiDAS database utility system, creates any resource needed to manage sessions.
	 * @param hibernateConfigFile The URL to the Hibernate configuration file
	 * @throws MiDbException 
	 */
	public static void init(URL hibernateConfigFile) throws MiDbException
	{
		MiDbSession.init(hibernateConfigFile);
		MiDbSession.getSessionFactory();
	}
	
	/**
	 * Destroys all session management resources created in the init() method.
	 */
	public static void dispose()
	{
		MiDbSession.destroySessionFactory();
	}
}

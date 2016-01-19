/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mitel.ssh;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the SSH authentication information.
 * @author haiv
 */
public class MiSshAuthInfo
{
	private String m_UserName;
	private String m_Password;
	/**
	 * Server address, invariant.
	 */
	private final String m_ServerAddr;
	/**
	 * Command-line prompt map, keyed on directory names.
	 */
	private final Map<String, String> m_CmdPrompts = new HashMap<String, String>();

	/**
	 * Creates a new SSH authentication info record.
	 * @param server The SSH server address/hostname.
	 * @param userName The username
	 * @param password The password.
	 */
	public MiSshAuthInfo(String server, String userName, String password)
	{
		m_UserName = userName;
		m_Password = password;
		m_ServerAddr = server;
	}

	/**
	 * Attempts to retrieve the command-line prompt based on a given directory.
	 * Some shell settings give different prompts depending on your current directory.
	 * @param dir The directory against which the command line prompt is requested.
	 * @return The command line prompt or null if one had not been set (see setCmdlinePrompt()).
	 */
	String getCmdlinePrompt(String dir)
	{
		synchronized(m_CmdPrompts)
		{
			Object ret = m_CmdPrompts.get(dir);
			if(ret instanceof String)
			{
				return (String)ret;
			}
		}
		return null;
	}

	/**
	 * Specifies the command-line prompt for a particular directory.
	 * @param dir The target directory
	 * @param prompt The command-line prompt.
	 * @return This object.
	 */
	public MiSshAuthInfo setCmdlinePrompt(String dir, String prompt)
	{
		synchronized(m_CmdPrompts)
		{
			m_CmdPrompts.put(dir, prompt);
		}
		return this;
	}


	/**
	 * Retrieves the SSH server address.
	 * @return The server address. Non-null.
	 */
	public String getServerAddr()
	{
		return (m_ServerAddr != null?m_ServerAddr:"");
	}

	/**
	 * Retrieves the user name.
	 * @return The user name, or null if one had not been set.
	 */
	public synchronized String getUserName()
	{
		return m_UserName;
	}

	/**
	 * Sets the user name.
	 * @param userName The new user name. Use null to unset the user name.
	 * @return This object.
	 */
	public synchronized MiSshAuthInfo setUserName(String userName)
	{
		this.m_UserName = userName;
		return this;
	}

	/**
	 * Retrieves the password.
	 * @return The password, or null if one had not been set.
	 */
	public synchronized String getPassword()
	{
		return m_Password;
	}

	/**
	 * Sets the password value.
	 * @param password The new password. Use null to unset the password.
	 * @return This object.
	 */
	public synchronized MiSshAuthInfo setPassword(String password)
	{
		this.m_Password = password;
		return this;
	}

	@Override
	public String toString()
	{
		return getUserName();
	}
}

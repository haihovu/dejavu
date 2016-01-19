/*
 * TelnetExecutor.java
 *
 * Created on May 27, 2004, 9:57 PM
 */

package com.mitel.netutil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class allows the clients to request an existing telnet session to execute a
 * command and periodically (500ms) invoke an action event to help support progress
 * measurement.
 * @author Hai Vu
 */
public class TelnetExecutor
{
	String m_telnetResponse = java.util.ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui").getString("inProgressLabel");
	private MiTelnet m_telnetSession;
	String m_cmd;
	private TelnetExecutor m_thisObject = this;
	
	/** Creates a new instance of TelnetExecutor */
	public TelnetExecutor(MiTelnet telnetSession)
	{
		m_telnetSession = telnetSession;
	}
	
	public String executeCommand(java.lang.String cmd, ActionListener listener) throws TelnetException
	{
		if(null != cmd)
		{
			m_cmd = cmd;
			if(null != m_telnetSession)
			{
				Thread worker = new Thread(new Runnable()
				{
					public void run()
					{
						m_telnetResponse = m_telnetSession.executeCommand(m_cmd);
					}
				});
				worker.start();
				int counter = 0;
				while(true)
				{
					if(null != listener)
						listener.actionPerformed(new ActionEvent(this, counter, java.util.ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui").getString("inProgressLabel")));
					
					if(++counter > 100)
						throw new TelnetException(java.util.ResourceBundle.getBundle("com/mitel/netutil/resource").getString("timedOutLabel"));
					
					try
					{
						worker.join(1000);
						if(!worker.isAlive())
							break;
					}
					catch(InterruptedException e)
					{
						// Someone interrupted this thread, exit this loop
						m_telnetResponse = "Interrupted";
						break;
					}
					catch(Exception e)
					{
						throw new TelnetException(e.getMessage());
					}
				}
			}
			else
			{
				throw new TelnetException("NULL telnet session");
			}
		}
		else
		{
			throw new TelnetException("NULL command");
		}
		
		return m_telnetResponse;
	}
	
	public static class TelnetException extends Exception
	{
		
		public TelnetException(java.lang.String desc)
		{
			super(desc);
		}
		
	}
	
}

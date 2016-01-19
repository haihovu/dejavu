/*
 * MiActiveObject.java
 *
 * Created on November 26, 2006, 11:51 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.mitel.miutil;

/**
 *
 * @author haiv
 */
public abstract class MiActiveObject implements Runnable
{
	
	/** Creates a new instance of MiActiveObject */
	public MiActiveObject()
	{
	}

	/**
	 * Starts the active object.
	 * The method taskEntry() will be invoked repeatedly by the active thread
	 * until the object is stopped.
	 * @throws MiException Subclasses may over-ride this method and throw exception as desired.
	 */
	public synchronized void start() throws MiException
	{
		if(!m_Started)
		{
			if(m_Thread == null)
			{
				m_Thread = new Thread(this);
				m_Thread.start();
			}
			m_Started = true;
		}
	}
	
	/**
	 * Stops the active object.
	 * This will stop the active thread.
	 * @param timeoutMs Period in miliseconds to wait for active thread to terminate. 
	 * Use zero to indicate no wait.
	 */
	public synchronized void stop(int timeoutMs)
	{
		if(m_Started)
		{
			if(m_Thread != null)
			{
				m_RunFlag = false;
				m_Thread.interrupt();
				try
				{
					if(timeoutMs > 0)
					{
						wait(timeoutMs);
					}
				} catch (InterruptedException ex){}
				m_Thread = null;
			}
			m_Started = false;
		}
		notifyAll();
	}

	public void run()
	{
		m_RunFlag = true;
		while(m_RunFlag)
		{
			try
			{
				if(!taskEntry())
				{
					m_RunFlag = false;
				}
			}
			catch(InterruptedException e)
			{
				m_RunFlag = false;
			}
		}
		
		synchronized(this)
		{
			m_Thread = null;
			m_Started = false;
			notifyAll();
		}
	}

	/**
	 * Waits for the client active thread to terminate, with timeout.
	 */
	public synchronized void waitForTermination(int timeoutMs)
	{
		if(m_Thread != null)
		{
			try
			{
				wait(timeoutMs);
			} catch (InterruptedException ex){}
		}
	}
	
	/**
	 * Waits for the client active thread to terminate, no timeout.
	 */
	public synchronized void waitForTermination()
	{
		try
		{
			while(m_Thread != null)
			{
				wait();
			}
		} catch (InterruptedException ex){}
	}
	
	/**
	 * This method represents a single iteration of the main loop.
	 * Concrete classes must implement this method. 
	 * This method will be invoked by the active thread repeatedly until stop is invoked.
	 * If this method can block its implementation should provide a way to interrupt the thread.
	 * The main loop will continue to invoke this method as long as it returns true.
	 * If the method returns false, the loop will terminate.
	 */
	public abstract boolean taskEntry() throws InterruptedException;
	
	/**
	 * Active thread.
	 */
	private Thread m_Thread;

	protected volatile boolean m_RunFlag;

	protected volatile boolean m_Started;

}

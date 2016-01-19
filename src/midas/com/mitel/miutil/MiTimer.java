/*
 * jTimer.java
 *
 * Created on October 26, 2003, 4:51 PM
 */

package com.mitel.miutil;

/**
 *
 * @author  Hai Vu
 */
public class MiTimer implements Runnable {
    
    private Thread m_OwnerThread;
    private int m_Delay;
    private Thread m_Thread = null;
	
    /** Creates a new instance of jTimer */
    public MiTimer(int delay) 
	{
        m_OwnerThread = Thread. currentThread();
        m_Delay = delay;
    }
    
    public void start()
    {
        m_Thread = new Thread( this );
		m_Thread.start();
    }
    
    public void stop()
    {
		if( null != m_Thread )
		{
			m_Thread.interrupt();
			m_Thread = null;
		}
    }
    
	public void run()
	{
		try
		{
			Thread.sleep(m_Delay);
			m_OwnerThread.interrupt();
		}
		catch( Exception e ){}
	}
	
}

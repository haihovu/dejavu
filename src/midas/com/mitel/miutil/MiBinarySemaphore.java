/*
 * jQueue.java
 *
 * Created on October 26, 2003, 11:51 AM
 */

package com.mitel.miutil;

/**
 *
 * @author  Hai Vu
 */
public class MiBinarySemaphore {
    
	private boolean m_full = true;
	
    /** @link dependency */
    /*# MiTimer lnkjTimer; */
    
    /** Creates a new instance of jQueue */
    public MiBinarySemaphore(boolean full) 
    {
		m_full = full;
    }
    
	/**
	 * This method waits for the semaphore to be full (with a timeout) and then empties it and return.
	 */	
    public void take(int timeout) throws java.lang.InterruptedException
    {
        synchronized( this )
        {
			MiTimer timer = new MiTimer( timeout );
            timer. start();
            while( !m_full )
            {
                wait();
				if( m_full )
				{
					// Semaphore is full exit the wait state
					break;
				}
            }
			// NOw empty the semaphore
			m_full = false;
			timer. stop();
        }
    }
    
	/**
	 * This method waits for the semaphore to be full (no timeout) and then empties it and return.
	 */	
    public void take() throws java.lang.InterruptedException
    {
        synchronized( this )
        {
            while( !m_full )
            {
                wait();
				if( m_full )
				{
					// Semaphore is full exit the wait state
					break;
				}
            }
			// NOw empty the semaphore
			m_full = false;
        }
    }
    
	public synchronized void give()
	{
		m_full = true;
		this.notifyAll();
	}
	
	public synchronized void emptyit()
	{
		m_full = false;
	}
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Like a background task but can be monitored and restarted (by a task manager) if premature termination is detected.
 * Concrete implementation should override (implement) the abstract method mainLoop (as opposed to the method
 * run). When started a worker thread is spawned, which then executes the mainLoop method.
 * @author haiv
 */
public abstract class DjvTaskSupervised implements Runnable
{
	/**
	 * Listener interface for receiving asynchronous progress/status notifications regarding supervised tasks.
	 */
	public interface Listener
	{
		/**
		 * A task had started, i.e. its worker thread is up and running.
		 * @param task The target task
		 */
		public void started(DjvTaskSupervised task);
		/**
		 * A task had terminated, i.e. its worker thread existed.
		 * @param task The target task
		 */
		public void terminated(DjvTaskSupervised task);
		/**
		 * A task had been requested to stop.
		 * @param task The target task.
		 */
		public void stop(DjvTaskSupervised task);
	}
	private Thread bgThread;
	protected final String taskName;
	private boolean runflag;
	private boolean started;
	private final Set<Listener> listeners = new HashSet<Listener>(16);
	/**
	 * Creates a new supervised task, not yet started.
	 * @param name The name of the task.
	 */
	public DjvTaskSupervised(String name)
	{
		taskName = name;
	}
	
	/**
	 * Retrieves the name of this task.
	 * @return The task name.
	 */
	public String getName()
	{
		return taskName;
	}
	
	/**
	 * Adds a listener for this task.
	 * @param listener The listener to be added.
	 * @return This object.
	 */
	public DjvTaskSupervised addListener(Listener listener)
	{
		synchronized(listeners)
		{
			if(listener != null)
			{
				listeners.add(listener);
			}
		}
		return this;
	}
	
	/**
	 * Removes a listener from this task.
	 * @param listener The listener to be removed.
	 * @return This object.
	 */
	public DjvTaskSupervised removeListener(Listener listener)
	{
		synchronized(listeners)
		{
			listeners.remove(listener);
		}
		return this;
	}
	
	/**
	 * Starts/Restart the task.
	 * @param taskManager An optional task manager which perform supervision functionality.
	 * If specified this task will automatically register with the task manager once the worker
	 * thread is up and running, and will deregister when the worker thread terminates.
	 * @param timeoutMs Timeout to wait for the worker thread to start running.
	 * @return True if the worker thread had started running, false otherwise.
	 * @throws InterruptedException
	 */
	public boolean start(DjvTaskManager taskManager, long timeoutMs) throws InterruptedException
	{
		long ts = System.currentTimeMillis();
		if(taskManager != null)
		{
			addListener(taskManager);
		}
		stop(timeoutMs);
		long remain = timeoutMs - (System.currentTimeMillis() - ts);
		synchronized(this)
		{
			runflag = true;
			started = false;
			bgThread = new Thread(this, taskName);
			bgThread.start();
			// Wait for thread to start
			while(!started)
			{
				if(remain > 0)
				{
					wait(remain);
					remain = timeoutMs - (System.currentTimeMillis() - ts);
				}
				else
				{
					break;
				}
			}
			return started;
		}
	}
	
	/**
	 * Stops the task.
	 * @param timeoutMs Timeout to wait for the task to terminate. Positive integers
	 * represents the number of milliseconds to wait, zero means wait forever, negative
	 * values means no wait.
	 * @throws InterruptedException
	 */
	public void stop(long timeoutMs) throws InterruptedException
	{
		Thread existing = null;
		List<Listener> ls;
		synchronized(listeners)
		{
			ls = new ArrayList<Listener>(listeners);
		}
		for(Listener l : ls)
		{
			l.stop(this);
		}
		synchronized(this)
		{
			if(bgThread != null)
			{
				existing = bgThread;
				bgThread = null;
			}
			runflag = false;
		}
		if(existing != null)
		{
			existing.interrupt();
			if(timeoutMs > -1)
			{
				existing.join(timeoutMs);
			}
		}
	}

	@Override
	public String toString()
	{
		return "MiTaskSupervised." + getName();
	}
	
	@Override
	public void run()
	{
		synchronized(this)
		{
			started = true;
			notifyAll();
		}
		try
		{
			List<Listener> ls;
			synchronized(listeners)
			{
				ls = new ArrayList<Listener>(listeners);
			}
			for(Listener l : ls)
			{
				l.started(this);
			}
			mainLoop();
		}
		catch(RuntimeException e)
		{
			DjvSystem.logError(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
		}
		finally
		{
			synchronized(this)
			{
				if(Thread.currentThread() == bgThread)
				{
					bgThread = null;
				}
			}
			List<Listener> ls;
			synchronized(listeners)
			{
				ls = new ArrayList<Listener>(listeners);
			}
			for(Listener l : ls)
			{
				try
				{
					l.terminated(this);
				}
				catch(RuntimeException e)
				{
					DjvSystem.logError(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
				}
			}
		}
	}
	
	/**
	 * Retrieves the current worker thread for this task.
	 * @return The current worker thread for this task, or null if the task had been stopped or had terminated somehow.
	 */
	public Thread getThread()
	{
		synchronized(this)
		{
			return bgThread;
		}
	}
	
	/**
	 * Retrieves the run flag for the task, this indicates that the task was started.
	 * This flag is set to true by the start method, and set to false by the stop method.
	 * Main loops should monitor this flag and terminate when set to false.
	 * Note that this doesn't mean the worker thread is currently running.
	 * @return True if the task was started, false otherwise.
	 */
	public boolean getRunFlag()
	{
		synchronized(this)
		{
			return runflag;
		}
	}
	
	/**
	 * This is the main loop for the supervised task. Concrete supervised task implementation
	 * should override this method and provide their custom logic.
	 */
	public abstract void mainLoop();
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dejavu.util;

/**
 * Generic background task, i.e. using a worker thread to execute some logic.
 * Typical usage simply extend this class and provide the run method.
 * @author haiv
 */
public abstract class DjvBackgroundTask implements Runnable
{
	private final Thread workerThread;
	private volatile boolean runFlag;

	/**
	 * Default constructor, creates an un-named background task.
	 */
	public DjvBackgroundTask()
	{
		workerThread = new Thread(this);
	}

	/**
	 * Creates a background task with a named thread.
	 * @param taskName The name to be given to the background task.
	 */
	public DjvBackgroundTask(String taskName)
	{
		workerThread = new Thread(this, taskName);
	}

	/**
	 * Retrieves the name of the task (i.e. name of worker thread)
	 * @return The name of the worker thread. Non-null.
	 */
	public String getTaskName()
	{
		return workerThread.getName();
	}

	/**
	 * Starts up the background task, i.e. starts the worker thread.
	 * Should be invoked only once, though I don't think repeated invocation hurts.
	 * @return This object.
	 */
	public DjvBackgroundTask start()
	{
		runFlag = true;
		workerThread.start();
		return this;
	}

	/**
	 * Attempts to stop the background task by interrupting the worker thread.
	 * May be invoked multiply.
	 */
	public void stop()
	{
		runFlag = false;
		workerThread.interrupt();
	}

	/**
	 * Determines whether the background task is still running, i.e. the worker thread is alive.
	 * @return True if the background task is still running, false if it had completed or stopped somehow.
	 */
	public boolean isRunning()
	{
		return workerThread.isAlive();
	}

	/**
	 * Determines whether the worker thread had received the interrupt event,
	 * this doesn't mean that the worker thread stops.
	 * @return True if the worker thread has received the interrupt event.
	 */
	public boolean isInterrupted()
	{
		return workerThread.isInterrupted();
	}

	/**
	 * Retrieves the run flag, which is set to true when the start method is invoked,
	 * and set to false when the stop method is invoked.
	 * @return True if the run flag is set, false otherwise.
	 */
	public boolean getRunFlag()
	{
		return runFlag;
	}

	/**
	 * Sets the run flag for this task.
	 * @param value New value with which to set the run flag
	 * @return This object.
	 */
	public DjvBackgroundTask setRunFlag(boolean value)
	{
		runFlag = value;
		return this;
	}

	/**
	 * Retrieves the worker thread.
	 * @return The worker thread. Non-null.
	 */
	public Thread getThread()
	{
		return workerThread;
	}
}

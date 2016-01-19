/* Generated by Together */

package com.mitel.mifsm;
import com.mitel.miutil.MiBackgroundTask;
import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements a timer manager which allows clients to register their FSM events to be fired
 * once, after some period of time.
 * @author haiv
 */
public class FsmTimer
{
	/**
	 * The timer event record
	 */
	private static class EventTimer
	{
		private EventTimer(FsmEvent evt, int timeoutMs)
		{
			event = evt;
			periodMs = timeoutMs;
		}

		public final FsmEvent event;
		private int periodMs;
	}

	/**
	 * Creates an FSM timer with a default resolution of 1 second
	 */
	public FsmTimer()
	{
		m_TimeResolutionMs = 1000;
	}

	/**
	 * Creates an FSM timer.
	 * @param timeResolution The resolution of the timer, in milliseconds.
	 */
	public FsmTimer(long timeResolution)
	{
		m_TimeResolutionMs = timeResolution;
	}

	/**
	 * Starts the FSM timer. May be invoked multiple times.
	 * @return This object.
	 */
	public FsmTimer start()
	{
		synchronized(this)
		{
			if(workerTask != null)
			{
				// Already started
				return this;
			}
			workerTask = new WorkerTask().start();
		}
		return this;
	}

	/**
	 * Stops the FSM timer. The timer may be restarted subsequently.
	 */
	public void stop()
	{
		synchronized(this)
		{
			if(workerTask == null)
			{
				// Already stopped
				return;
			}
			workerTask.stop();
		}
	}

	/**
	 * Adds an event to be fired later.
	 * @param evt The FSM event to be fired
	 * @param timeout The timeout, in milliseconds, to wait before firing the specified event
	 * @return The integer ID of the event with which the client may cancel the event
	 * prior to it being fired.
	 * @throws FsmException
	 */
	public Integer addEvent(FsmEvent evt, int timeout) throws FsmException
	{
		synchronized(this)
		{
			if(workerTask != null)
			{
				Integer key = new Integer(m_TimerKey++);
				m_TimerMap.put(key, new EventTimer(evt, timeout));
				return key;
			}
			throw new FsmException("Timer Shutdown");
		}
	}

	/**
	 * Removes a previously registered event, in effect cancelling it.
	 * @param timerKey The integer ID of the target event, as returned from addEvent().
	 */
	public synchronized void removeEvent(Integer timerKey)
	{
		m_TimerMap.remove(timerKey);
	}

	private final Map<Integer, EventTimer> m_TimerMap = new TreeMap<Integer, EventTimer>();
	private int m_TimerKey = 0;
	private MiBackgroundTask workerTask;

	/**
	 * Time resolution in milliseconds
	 */
	private final long m_TimeResolutionMs;
	
	/**
	 * Background task for handling event firing logic.
	 */
	private class WorkerTask extends MiBackgroundTask
	{
		private WorkerTask()
		{
			super();
		}

		@Override
		@SuppressWarnings("SleepWhileHoldingLock")
		public void run()
		{
			try
			{
				while(getRunFlag())
				{
					try
					{
						Thread.sleep(m_TimeResolutionMs);
						synchronized(FsmTimer.this)
						{
							Iterator iter = m_TimerMap.values().iterator();
							while(iter.hasNext() && getRunFlag())
							{
								EventTimer evtTo = (EventTimer)iter.next();

								if(evtTo.periodMs > 0)
								{
									evtTo.periodMs -= m_TimeResolutionMs;
									if(evtTo.periodMs <= 0)
									{
										evtTo.event.getContext().getFsmDomain().dispatchEvent(evtTo.event, true);
										iter.remove();
									}
								}
							}
						}
					}
					catch(RuntimeException e)
					{
						MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
					}
				}
			}
			catch(InterruptedException e)
			{
			}
			finally
			{
				synchronized(FsmTimer.this)
				{
					m_TimerMap.clear();
					if(workerTask == this)
					{
						workerTask = null;
					}
				}
			}
		}
	}
}

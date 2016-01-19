/* Generated by Together */

package com.mitel.miutil;

import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>The base implementation of a watchdog system.
 * In this system, there is the concept of one or more watchdogs that monitors the status of a number of components.
 * The components to be monitored are registered with their respective watchdogs with the following attributes:
 * <ul>
 * <li>Name - Name of the component.</li>
 * <li>Period - Intervals that the component must report status to the watchdog.</li>
 * <li>Response - Instruction as to what to do if the component fails, this is a piece of runnable logic.</li>
 * </ul>
 * </p>
 * <p>The monitored components must report their status with their watchdogs at regular intervals (as specified in the registration process).
 * The watchdogs periodically check to see if all their monitored components have reported their status at the indicated intervals.
 * If a component fails to report its status in a timely manner (the watchdog gives the components double the registered intervals to report their status),
 * the watchdog will execute the response that was registered with this component.
 * This way the watchdog leaves it to the monitored components to determine what is to be done in cases of failures.
 * </p>
 */
public class MiWatchDog extends TimerTask
{
	/**
	 * Component status. Used for status reporting.
	 */
	public enum Status
	{
		WD_NORMAL, WD_FAILED
	}

	/**
	 * Creates a new watchdog instance.
	 * @param name Name of the watchdog.
	 * @param capacity The capacity of the watchdog, i.e. how many monitors it can manage.
	 */
	public MiWatchDog(String name, int capacity)
	{
		m_MonitorRepository = new MonitorInfo[capacity];
		m_MaxIndex = 0;
		m_WdName = name;
	}
	
	/**
	 * Starts the watchdog.
	 * @param timeResolution How often does the watchdog check the monitored components, in milliseconds.
	 */
	public synchronized void start(int timeResolution)
	{
		if((timeResolution > 0)&&(null == m_Timer))
		{
			m_TimeResolution = timeResolution;
			m_Timer = new Timer(m_WdName);
			m_Timer.scheduleAtFixedRate(this, 0, timeResolution);
		}
	}
	
	/**
	 * Stops the watchdog. Note that due to its implementation, once stopped a watchdog cannot be restarted.
	 */
	public synchronized void stop()
	{
		if(null != m_Timer)
		{
			cancel();
			m_Timer.cancel();
		}
	}
	
	/**
	 * Reports the status of a component.
	 * @param key The key identifying the status of the component.
	 * @param status The status of the component. If the status reported is WD_FAILED, the failure response will be executed.
	 * @return Whether the status reporting was successful.
	 */
	public boolean reportStatus(int key, Status status)
	{
		if((key > -1)&&(key < m_MaxIndex))
		{
			MonitorInfo monitor = m_MonitorRepository[key];
			if(null != monitor)
			{
				if(MiSystem.diagnosticEnabled())
				{
					MiSystem.logInfo(MiLogMsg.Category.DESIGN,
						"Reporting " + status + " for " + monitor);
				}
		
				synchronized(monitor)
				{
					if(status == Status.WD_NORMAL)
					{
						monitor.m_Counter = monitor.m_Period;
						monitor.m_OnProbation = false;
					}
					else if(status == Status.WD_FAILED)
					{
						executeResponse(monitor);
					}
					return true;
				}
			}
			else
			{
				MiSystem.logWarning(MiLogMsg.Category.DESIGN,
					"No monitor for key: " + key);
			}
		}
		else
		{
			MiSystem.logWarning(MiLogMsg.Category.DESIGN,
				"Invalid key: " + key);
		}
		
		return false;
	}

	/**
	 * Registers a component with the watchdog for monitoring.
	 * 
	 * 
	 * @param name Name of the component.
	 * @param wdPeriod Intervals of status report, in milliseconds.
	 * @param failureResponse The failureResponse in case of component failure. If specified, the watchdog will execute the given logic if a failure is detected.
	 * @return An integer key to allow the client to report its status later.
	 */
	public synchronized int registerComponent(String name, int wdPeriod, Runnable failureResponse)
	{
		for(int i = 0; i < m_MaxIndex; ++i)
		{
			if(m_MonitorRepository[i] == null)
			{
				m_MonitorRepository[i] = new MonitorInfo(name, wdPeriod, failureResponse);
				return i;
			}
		}
		
		if(m_MaxIndex < m_MonitorRepository.length)
		{
			int index = m_MaxIndex++;
			m_MonitorRepository[index] = new MonitorInfo(name, wdPeriod, failureResponse);
			
			return index;
		}
		
		MiSystem.logError(MiLogMsg.Category.DESIGN,
			"Monitor capacity (" + m_MonitorRepository.length + ") exceeded while registering " + name);
		
		return -1;
	}

	/**
	 * Deregisters a component with the watchdog.
	 *
	 * @param key 
	 */
	public synchronized void deregisterComponent(int key)
	{
		if((key > -1)&&(key < m_MaxIndex))
		{
			if(m_MonitorRepository[key] != null)
			{
				MiSystem.logWarning(MiLogMsg.Category.DESIGN,
					"Deregistering " + m_MonitorRepository[key]);
				
				m_MonitorRepository[key] = null;
			}
		}
	}
	
	@SuppressWarnings("NestedAssignment")
	public void run()
	{
		synchronized(this)
		{
			for(int i = 0; i < m_MaxIndex; ++i)
			{
				MonitorInfo monitor = m_MonitorRepository[i];
				if(null != monitor)
				{
					synchronized(monitor)
					{
						if((monitor.m_Counter -= m_TimeResolution) > 0)
						{
							// This component is OK, go to the next
							continue;
						}
						
						// Component missed report
						if(!monitor.m_OnProbation)
						{
							// Give the component one more chance to report its status
							monitor.m_OnProbation = true;
							monitor.m_Counter = monitor.m_Period;
							
							if(MiSystem.getLogLevel() > 1)
							{
								MiSystem.logInfo(MiLogMsg.Category.DESIGN,
									"Placing " + monitor + " on probation");
							}
						}
						else
						{
							MiSystem.logWarning(MiLogMsg.Category.DESIGN,
								"Failure detected in component " + monitor);

							// Stop monitoring this particular component
							m_MonitorRepository[i] = null;
							executeResponse(monitor);
						}
					}
				}
			}
		}
	}

	@Override
	public String toString()
	{
		return m_WdName;
	}

	private void executeResponse(MonitorInfo monitor)
	{
		if(monitor.m_Response != null)
		{
			new Thread(monitor.m_Response, "wd" + monitor.m_Name).start();
		}
	}
	
	private static class MonitorInfo
	{
		final int m_Period;
		final String m_Name;
		final Runnable m_Response;
		int m_Counter;
		boolean m_OnProbation;
		
		MonitorInfo(String name, int period, Runnable response)
		{
			m_Name = name;
			m_Period = period;
			m_Response = response;
		}

		public String toString()
		{
			StringBuilder retValue = new StringBuilder(128).append("(Component");
			retValue.append(" name=").append(m_Name);
			retValue.append(" period=").append(m_Period);
			retValue.append(" counter=").append(m_Counter);
			retValue.append(" probation=").append(m_OnProbation);
			return retValue.append(")").toString();
		}
	}

	/**
	 * The repository of registered components.
	 */
	private volatile MonitorInfo[] m_MonitorRepository;
	/**
	 * The maximum index into the m_MonitorRepository array indicating the upper limit of registered components.
	 */
	private volatile int m_MaxIndex;
	/**
	 * Timer used to drive the watchdog monitoring process.
	 */
	private Timer m_Timer;
	/**
	 * Name of the watchdog, note that there may be multiple instances of the watchdogs running in an application.
	 */
	private String m_WdName;
	/**
	 * Resolution of the watchdog timer, in milliseconds.
	 */
	private int m_TimeResolution;
}

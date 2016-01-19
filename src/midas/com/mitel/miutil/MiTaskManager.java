/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mitel.miutil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * To be used with MiTaskSupervised. This provides task monitoring/supervision functionality.
 * All registered tasks are periodically examined to see if they had terminated without
 * being stopped by the application, if so the affected tasks will be restarted.
 * @author haiv
 */
public class MiTaskManager extends MiBackgroundTask implements MiTaskSupervised.Listener
{
	private final Map<String, MiTaskSupervised> taskRepository = new HashMap<String, MiTaskSupervised>(128);
	private final long monitorPeriodMs;
	/**
	 * Creates a new task manager.
	 * @param periodMs Monitor period, in milliseconds.
	 */
	public MiTaskManager(long periodMs)
	{
		monitorPeriodMs = periodMs;
	}

	/**
	 * Registers a task to be monitored.
	 * @param task The task to be monitored.
	 * @return
	 */
	public MiTaskManager registerTask(MiTaskSupervised task)
	{
		task.getClass(); // Null check
		synchronized(taskRepository)
		{
			MiTaskSupervised existing = taskRepository.get(task.getName());
			if((existing != null)&&(existing != task))
			{
				MiSystem.logWarning(MiLogMsg.Category.DESIGN, "Registering " + task + " on top of " + existing);
			}
			taskRepository.put(task.getName(), task);
			taskRepository.notifyAll();
		}
		task.addListener(this);
		return this;
	}
	
	/**
	 * De-registers a task.
	 * @param task The task to be monitored.
	 * @return
	 */
	public MiTaskManager deregisterTask(MiTaskSupervised task)
	{
		task.getClass(); // Null check
		synchronized(taskRepository)
		{
			taskRepository.remove(task.getName());
		}
		return this;
	}

	@Override
	public void stop()
	{
		synchronized(taskRepository)
		{
			super.stop();
			taskRepository.notifyAll();
		}
	}
	
	@SuppressWarnings("SleepWhileInLoop")
	public void run()
	{
		try
		{
			while(getRunFlag())
			{
				List<MiTaskSupervised> tasks;
				synchronized(taskRepository)
				{
					if(getRunFlag())
					{
						if(taskRepository.isEmpty())
						{
							taskRepository.wait();
						}
					}
					tasks = new ArrayList<MiTaskSupervised>(taskRepository.values());
				}
				for(MiTaskSupervised task : tasks)
				{
					if(!getRunFlag())
					{
						break;
					}
					// Restart any task that is supposed to be running but isn't
					boolean restart = false;
					synchronized(task)
					{
						Thread thread = task.getThread();
						if(task.getRunFlag()&&((thread == null)||(!thread.isAlive())))
						{
							restart = true;
						}
					}
					if(restart)
					{
						MiSystem.logWarning(MiLogMsg.Category.DESIGN, task + " was found terminated, attempt to restart");
						if(!task.start(this, 10000))
						{
							MiSystem.logWarning(MiLogMsg.Category.DESIGN, "Failed to restart " + task);
						}
					}
				}
				synchronized(taskRepository)
				{
					if(getRunFlag())
					{
						taskRepository.wait(monitorPeriodMs);
					}
				}
			}
		}
		catch(RuntimeException e)
		{
			MiSystem.logError(MiLogMsg.Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
		catch(InterruptedException ex)
		{
		}
	}

	@Override
	public void started(MiTaskSupervised task)
	{
		registerTask(task);
	}

	@Override
	public void terminated(MiTaskSupervised task)
	{
	}

	public void stop(MiTaskSupervised task)
	{
		deregisterTask(task);
	}
}

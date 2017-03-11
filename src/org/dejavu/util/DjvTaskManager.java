/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * To be used with DjvTaskSupervised. This provides task monitoring/supervision
 * functionality. All registered tasks are periodically examined to see if they
 * had terminated without being stopped by the application, if so the affected
 * tasks will be restarted.
 *
 * @author haiv
 */
public class DjvTaskManager extends DjvBackgroundTask implements DjvTaskSupervised.Listener {

	private final Map<String, DjvTaskSupervised> taskRepository = new HashMap<>(128);
	private final long monitorPeriodMs;

	/**
	 * Creates a new task manager.
	 *
	 * @param periodMs Monitor period, in milliseconds.
	 */
	public DjvTaskManager(long periodMs) {
		monitorPeriodMs = periodMs;
	}

	/**
	 * Registers a task to be monitored.
	 *
	 * @param task The task to be monitored.
	 * @return
	 */
	public DjvTaskManager registerTask(DjvTaskSupervised task) {
		task.getClass(); // Null check
		synchronized (taskRepository) {
			DjvTaskSupervised existing = taskRepository.get(task.getName());
			if ((existing != null) && (existing != task)) {
				DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, "Registering " + task + " on top of " + existing);
			}
			taskRepository.put(task.getName(), task);
			taskRepository.notifyAll();
		}
		task.addListener(this);
		return this;
	}

	/**
	 * De-registers a task.
	 *
	 * @param task The task to be monitored.
	 * @return
	 */
	public DjvTaskManager deregisterTask(DjvTaskSupervised task) {
		task.getClass(); // Null check
		synchronized (taskRepository) {
			taskRepository.remove(task.getName());
		}
		return this;
	}

	@Override
	public void stop() {
		synchronized (taskRepository) {
			super.stop();
			taskRepository.notifyAll();
		}
	}

	@SuppressWarnings("SleepWhileInLoop")
	@Override
	public void run() {
		try {
			while (getRunFlag()) {
				List<DjvTaskSupervised> tasks;
				synchronized (taskRepository) {
					if (getRunFlag()) {
						if (taskRepository.isEmpty()) {
							taskRepository.wait();
						}
					}
					tasks = new ArrayList<>(taskRepository.values());
				}
				for (DjvTaskSupervised task : tasks) {
					if (!getRunFlag()) {
						break;
					}
					// Restart any task that is supposed to be running but isn't
					boolean restart = false;
					synchronized (task) {
						Thread thread = task.getThread();
						if (task.getRunFlag() && ((thread == null) || (!thread.isAlive()))) {
							restart = true;
						}
					}
					if (restart) {
						DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, task + " was found terminated, attempt to restart");
						if (!task.start(this, 10000)) {
							DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, "Failed to restart " + task);
						}
					}
				}
				synchronized (taskRepository) {
					if (getRunFlag()) {
						taskRepository.wait(monitorPeriodMs);
					}
				}
			}
		} catch (RuntimeException e) {
			DjvSystem.logError(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
		} catch (InterruptedException ex) {
		}
	}

	@Override
	public void started(DjvTaskSupervised task) {
		registerTask(task);
	}

	@Override
	public void terminated(DjvTaskSupervised task) {
	}

	@Override
	public void stop(DjvTaskSupervised task) {
		deregisterTask(task);
	}
}

/* Generated by Together */
package org.dejavu.fsm;

import org.dejavu.util.DjvExceptionUtil;
import org.dejavu.util.DjvLogMsg.Category;
import org.dejavu.util.DjvQueue;
import org.dejavu.util.DjvSystem;
import org.dejavu.util.DjvWatchDog;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

/**
 * Represents a single FSM domain. Each FSM domain has a single FSM dispatch
 * thread and a transition table, a predefined number of states, and a set of
 * predefined events. Users of the FSM framework extend this class to add their
 * transition table, states, and events.
 */
public abstract class FsmDomain implements Runnable {

	/**
	 * Allows an instance of an FSM domain to be registered.
	 *
	 * @param newDomain The domain to be registered.
	 */
	public static void registerDomain(FsmDomain newDomain) {
		synchronized (gDomainMap) {
			gDomainMap.put(newDomain.getDomainName(), newDomain);
		}
	}

	/**
	 * Locates a pre-registered FSM domain.
	 *
	 * @param domainName The name of the desired FSM domain.
	 * @return The requested FSM domain or null if none matching the given name
	 * was previously registered.
	 */
	public static FsmDomain locateDomain(String domainName) {
		synchronized (gDomainMap) {
			return gDomainMap.get(domainName);
		}
	}

	/**
	 * Creates a new FSM domain.
	 *
	 * @param name The name of the new domain.
	 */
	public FsmDomain(String name) {
		m_Name = name;
	}

	/**
	 * Gets the name of this domain instance.
	 *
	 * @return The name of this FSM domain. Not null.
	 */
	public String getDomainName() {
		return m_Name != null ? m_Name : "";
	}

	/**
	 * Starts this FSM domain.
	 *
	 * @param watchdog Optional watchdog with which to register. Specify null if
	 * no watchdog monitoring is required.
	 * @param wdPeriod Intervals between watchdog status report, i.e. maximum
	 * time from the point of failure occurring and some response taken.
	 * @param failureResponse Optional watchdog failure response. I.e. what to
	 * do if a failure of this FSM domain is detected.
	 */
	public synchronized void start(DjvWatchDog watchdog, int wdPeriod, Runnable failureResponse) {
		if (!m_Started) {
			if (null != watchdog) {
				m_Watchdog = watchdog;

				// We don't register the given failure response with the watchdog.
				// Instead an intermediate failure response is registered which
				// generates a little more diagnostic info prior to invoking the
				// client supplied response.
				m_WdFailureResponse = failureResponse;
				m_WdKey = m_Watchdog.registerComponent(getDomainName(), wdPeriod, new Runnable() {
					@Override
					public void run() {
						// Dump the stack trace for the dispatching thread, this
						// might provide some explanation as to why the watchdog
						// failure might have occured.
						Thread dispatchThread;
						synchronized (FsmDomain.this) {
							dispatchThread = m_DispatchingThread;// Thread safety
						}

						if (null != dispatchThread) {
							StringBuilder traceMsg = new StringBuilder(256).append("Thread ");
							traceMsg.append(dispatchThread.getName());
							traceMsg.append(" may be stuck at: ");
							StackTraceElement[] traces = dispatchThread.getStackTrace();
							for (int i = 0; i < traces.length; ++i) {
								if (i == 0) {
									traceMsg.append(traces[i].toString());
								} else {
									traceMsg.append("->").append(traces[i].toString());
								}
							}
							DjvSystem.logWarning(Category.DESIGN, traceMsg.toString());
						}

						// And finally execute the response supplied by the client
						// And do it in a thread-safe and dead-lock-safe manner.
						Runnable clientResponse;
						synchronized (FsmDomain.this) {
							clientResponse = m_WdFailureResponse; // Thread-safety
						}

						if (null != clientResponse) {
							clientResponse.run();
						}
					}
				});

				if (-1 < m_WdKey) {
					// We want to report the status a little faster than what is registered
					int reportPeriod = (int) (wdPeriod * 0.75);
					if (reportPeriod < 1) {
						reportPeriod = 1;
					}
					m_WdStatEvtGen = new StatusEventGenerator(m_StatusEvent, reportPeriod);
				}
			}

			// Create and start the dispatch thread
			Thread domThread = new Thread(this, getDomainName());
			domThread.setPriority(Thread.currentThread().getPriority() + 2);
			domThread.start();

			m_DispatchingThread = domThread;

			m_Started = true;
		}
	}

	/**
	 * Stops the dispatch thread for this domain. Sub classes to define how the
	 * domain is started. Guaranteed to be non-blocking.
	 */
	public void stop() {
		DjvSystem.logInfo(Category.DESIGN, getDomainName() + " stop requested by user");
		new Thread(() -> {
			synchronized (FsmDomain.this) {
				m_Running = false;
				m_Started = false;
				
				if (null != m_WdStatEvtGen) {
					m_WdStatEvtGen.dispose();
					m_WdStatEvtGen = null;
				}
				
				if (null != m_Watchdog) {
					if (-1 < m_WdKey) {
						m_Watchdog.deregisterComponent(m_WdKey);
						m_WdKey = -1;
					}
					
					m_Watchdog = null;
				}
				
				if (m_DispatchingThread != null) {
					m_DispatchingThread.interrupt();
				}
			}
		}).start();
	}

	/**
	 * Dispatches the event dispatch thread to deliver the event to the target
	 * FSM context.
	 *
	 * @param event - The FSM event to dispatch (note that the target context is
	 * embedded in the event) queued - Whether to queue the event to be
	 * processed by the event dispatch thread later, or deliver the event now
	 * using the calling thread.
	 * @param queued Flag indicating whether the event is to be queued to be
	 * processed by a background thread (true), or processed immediately using
	 * the calling thread.
	 * @return true if the event had been dispatched, false otherwise.
	 */
	public boolean dispatchEvent(FsmEvent event, boolean queued) {
		try {
			if (queued) {
				synchronized (this) {
					if (m_DispatchingThread == null) {
						DjvSystem.logWarning(Category.DESIGN, "FSM domain " + getDomainName()
								+ " is already closed, event " + event + " ignored");
						return false;
					}
				}

				if (m_EventQueue.sendMsg(event, 0)) {
					return true;
				}
				DjvSystem.logWarning(Category.DESIGN, "Failed to send " + event);
			} else {
				return handleEvent(event);
			}
		} catch (InterruptedException e) {
			DjvSystem.logError(Category.DESIGN, "Unexpected uninterrupted event");
		}
		return false;
	}

	/**
	 * Returns the initial state for FSM context in this domain.
	 *
	 * @return The initial state of FSM context in this domain
	 */
	public synchronized FsmState getInitialState() {
		return m_InitialState;
	}

	/**
	 * Given a state ID, locates the associated state.
	 *
	 * @param id - Integer ID of the desired state.
	 * @return The state matching the given ID, or null if none was found.
	 */
	public FsmState locateState(int id) {
		synchronized(this) {
			if ((id > -1) && (id < m_StateMap.length)) {
				return m_StateMap[id];
			}
		}
		return null;
	}

	@Override
	public void run() {
		DjvSystem.logInfo(Category.DESIGN,
				"Thread " + Thread.currentThread().getName() + " Started");

		try {
			m_Running = true;
			try {
				while (m_Running) {
					FsmEvent event = (FsmEvent) m_EventQueue.receiveMsg();
					if (null != event) {
						handleEvent(event);
					}
				}
			} catch (RuntimeException e) {
				DjvSystem.logError(Category.DESIGN,
						Thread.currentThread() + " encountered " + DjvExceptionUtil.simpleTrace(e));
			} catch (InterruptedException e) {
				DjvSystem.logWarning(Category.DESIGN,
						Thread.currentThread() + " interrupted");
			}
		} finally {
			synchronized (this) {
				m_DispatchingThread = null;
				notifyAll();
			}
		}

		DjvSystem.logInfo(Category.DESIGN,
				"Thread " + Thread.currentThread().getName() + " Terminated");
	}

	/**
	 * Adds a state transition to the transition table. Typically used by
	 * sub-classes.
	 *
	 * @param eventId The event ID that triggers the transition
	 * @param fromState The initial state
	 * @param toState The end state
	 * @param guard Optional guard, if specified will enable/disable the
	 * transition based on some custom condition
	 * @param action Optional action to execute before hitting the end state
	 * @throws FsmException
	 */
	protected synchronized void addTransition(int eventId, FsmState fromState, FsmState toState, FsmAction guard, FsmAction action) throws FsmException {
		FsmTransition newTransition = new FsmTransition(eventId, fromState, toState, action, guard);
		m_TransitionMap.setTransition(newTransition);
	}

	/**
	 * Adds a state to the state table. Typically used by sub-classes.
	 *
	 * @param newState The state to be added.
	 * @param initialState Whether the given state is 'the' initial state.
	 */
	protected final synchronized void addState(FsmState newState, boolean initialState) {
		if (initialState) {
			if (null == m_InitialState) {
				m_InitialState = newState;
			} else {
				DjvSystem.logWarning(Category.DESIGN,
						"Attempting to set " + newState + " as initial state, but an initial state already exists: " + m_InitialState);
			}
		}

		if (null == m_StateMap[newState.getId()]) {
			m_StateMap[newState.getId()] = newState;
		} else {
			DjvSystem.logError(Category.DESIGN,
					"Collision between existing " + m_StateMap[newState.getId()] + " and new " + newState);
		}
	}

	/**
	 * Delivers an event to the target FSM context (embedded inside the event).
	 *
	 * @param event The event to be delivered.
	 * @return True if the event had been successfully executed with the target
	 * FSM context, false otherwise.
	 */
	protected boolean handleEvent(FsmEvent event) {
		if (event.getContext() != null) {
			FsmState currState = event.getContext().getCurrentState();
			if (null != currState) {
				FsmTransition trans = m_TransitionMap.getTransition(currState, event.getId());

				if (null != trans) {
					trans.execute(event);
					return true;
				} else {
					DjvSystem.logWarning(Category.DESIGN,
							new StringBuilder("Failed to locate transition from ").append(currState.toString())
									.append(" with event ").append(event.toString()).toString());
				}
			} else {
				DjvSystem.logWarning(Category.DESIGN,
						"Context " + event.getContext() + " has no current state");
			}
		} else if (event == m_StatusEvent) {
			DjvWatchDog wd;
			int key;

			// Always thread-safe
			synchronized (this) {
				wd = m_Watchdog;
				key = m_WdKey;
			}

			if (wd != null) {
				if (!wd.reportStatus(key, DjvWatchDog.Status.WD_NORMAL)) {
					DjvSystem.logWarning(Category.DESIGN,
							"Failed to report status to watchdog");
				}
			}
		} else {
			DjvSystem.logError(Category.DESIGN,
					new StringBuilder("Event ").append(event.toString())
							.append(" has NULL context").toString());
		}

		return false;
	}

	/**
	 * @associates FsmState
	 * @label State map
	 * @link aggregation
	 * @supplierCardinality 0..*
	 */
	private final FsmState[] m_StateMap = new FsmState[2048];

	/** @link aggregation
	 * @supplierCardinality 1 */
	private final FsmTransitionTable m_TransitionMap = new FsmTransitionTable();
	/**
	 * The main thread that drives (dispatching events) this FSM domain.
	 */
	@SuppressWarnings("ProtectedField")
	protected Thread m_DispatchingThread;

	/**
	 * @label Initial state
	 * @supplierCardinality 1
	 */
	private FsmState m_InitialState = null;
	private volatile boolean m_Running;

	/**
	 * @supplierCardinality 1
	 */
	private final DjvQueue m_EventQueue = new DjvQueue(1000);
	private static final AbstractMap<String, FsmDomain> gDomainMap = new HashMap<String, FsmDomain>();

	/**
	 * For use with watchdog monitoring.
	 */
	private final FsmEvent m_StatusEvent = new FsmEvent() {
		@Override
		public FsmContext getContext() {
			return null;
		}

		@Override
		public int getId() {
			return -1;
		}

		@Override
		public String toString() {
			return "(FsmStatusEvent)";
		}
	};

	/**
	 * Determines whether this FSM domain had been started.
	 *
	 * @return True if started, false otherwise.
	 */
	public boolean isStarted() {
		synchronized (this) {
			return m_Started;
		}
	}

	/**
	 * Generator of status events. If the FSM domain is operational, the events
	 * will be processed, at which time the status is reported to the watchdog.
	 */
	private class StatusEventGenerator extends TimerTask {

		private final FsmEvent m_WdEvent;

		private final Timer m_WdTimer;

		/**
		 * Create a new status event generator.
		 *
		 * @param wdEvent The status event to be dispatched periodically
		 * @param period The period, in milliseconds
		 */
		@SuppressWarnings("LeakingThisInConstructor")
		StatusEventGenerator(FsmEvent wdEvent, int period) {
			m_WdEvent = wdEvent;
			m_WdTimer = new Timer(getDomainName() + "WD");
			m_WdTimer.scheduleAtFixedRate(this, 0, period);
		}
		/**
		 * Disposes of the timer, releasing all allocated resources.
		 */
		void dispose() {
			cancel();
			m_WdTimer.cancel();
		}

		@Override
		public void run() {
			dispatchEvent(m_WdEvent, true);
		}
	}

	private DjvWatchDog m_Watchdog;

	private int m_WdKey = -1;

	private boolean m_Started;

	private final String m_Name;

	private StatusEventGenerator m_WdStatEvtGen;

	/**
	 * Watchdog failure response specified by the client of this FSM domain.
	 */
	private Runnable m_WdFailureResponse;
}
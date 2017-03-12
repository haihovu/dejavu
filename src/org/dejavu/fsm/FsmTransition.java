/* Generated by Together */
package org.dejavu.fsm;

import org.dejavu.util.DjvLogMsg.Category;
import org.dejavu.util.DjvSystem;

/**
 * Represents all state transitions. Programmable in terms of begin/end states,
 * guards, and actions.
 */
public class FsmTransition {

	/**
	 * Initialization constructor.
	 *
	 * @param triggerEventId The event that triggers the transition
	 * @param fromState The begin state from which the transition is to occur
	 * @param toState The end state to which the transition will take place
	 * @param action The optional action to execute during the state transition
	 * (null if no action is required)
	 * @param guard The optional guard to determines whether the transition can
	 * take place (null if no guard is required)
	 */
	public FsmTransition(int triggerEventId, FsmState fromState, FsmState toState, FsmAction action, FsmAction guard) {
		eventId = triggerEventId;
		this.action = action;
		this.guard = guard;
		this.fromState = fromState;
		this.toState = toState;
	}

	/**
	 * Executes the transition, the following steps are taken: - Check the
	 * guard, if not successful then abort - If the guard passes (or not
	 * present), execute any exit function on the begin state - Execute the
	 * action if exists - Execute the entry function on the end state if exists
	 *
	 * @param event The event to be executed.
	 * @return True if the event was executed, false otherwise.
	 * @throws java.lang.InterruptedException User interruption
	 */
	public boolean execute(FsmEvent event) throws InterruptedException {
		// Execute guard if one is specified...
		if ((null == guard) || (guard.handleEvent(event))) {
			// Execute the exit action of the current state
			fromState.executeExit(event);

			// Transition to the new state
			event.getContext().setCurrentState(toState);

			// Execute the transition action
			if (null != action) {
				action.handleEvent(event);
			}

			// Execute the entry action of the new state
			toState.executeEntry(event);

			return true;
		} else {
			DjvSystem.logWarning(Category.DESIGN,
					"Failed the guard check");
		}
		return false;
	}

	@Override
	public String toString() {
		return "FsmTransition{" + "eventId=" + eventId + ", fromState=" + fromState + ", toState=" + toState + ", action=" + action + ", guard=" + guard + '}';
	}

	/**
	 * Trigger event ID
	 */
	public final int eventId;

	/**
	 * Begin state
	 */
	public final FsmState fromState;

	/**
	 * End state
	 */
	public final FsmState toState;

	/**
	 * Action
	 */
	public final FsmAction action;

	/**
	 * Guard
	 */
	public final FsmAction guard;

	/** @link dependency */
	/*# FsmEvent lnkFsmEvent; */
}

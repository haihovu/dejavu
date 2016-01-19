/* Generated by Together */

package com.mitel.mifsm;

import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;

/**
 * Represents all state transitions. Programmable in terms of begin/end states, guards, and actions. 
 */
public class FsmTransition
{
	/**
	 * Initialization constructor.
	 * @param triggerEventId The event that triggers the transition
	 * @param fromState The begin state from which the transition is to occur
	 * @param toState The end state to which the transition will take place
	 * @param action The optional action to execute during the state transition (null if no action is required)
	 * @param guard The optional guard to determines whether the transition can take place (null if no guard is required)
	 */
	public FsmTransition(int triggerEventId, FsmState fromState, FsmState toState, FsmAction action, FsmAction guard)
	{
		m_EventId = triggerEventId;
		m_Action = action;
		m_Guard = guard;
		m_FromState = fromState;
		m_ToState = toState;
	}

	/**
	 * Executes the transition, the following steps are taken:
	 * - Check the guard, if not successful then abort
	 * - If the guard passes (or not present), execute any exit function on the begin state
	 * - Execute the action if exists
	 * - Execute the entry function on the end state if exists 
	 * @param event The event to be executed.
	 * @return True if the event was executed, false otherwise.
	 */
	public boolean execute(FsmEvent event)
	{
		// Execute guard if one is specified...
		if((null == m_Guard)||(m_Guard.handleEvent(event)))
		{
			// Execute the exit action of the current state
			m_FromState.executeExit(event);

			// Transition to the new state
			event.getContext().setCurrentState(m_ToState);

			// Execute the transition action
			if(null != m_Action)
			{
				m_Action.handleEvent(event);
			}

			// Execute the entry action of the new state
			m_ToState.executeEntry(event);

			return true;
		}
		else
		{
			MiSystem.logWarning(Category.DESIGN, 
				"Failed the guard check");
		}
		return false;
	}

	/**
	 * Gets the begin state of the transition.
	 * @return The begin state.
	 */
    public FsmState getFromState()
    {
        return m_FromState;
    }

	/**
	 * Gets the end state of the transition.
	 * @return The to state
	 */
    public FsmState getToState()
    {
        return m_ToState;
    }

	/**
	 * Gets the trigger event associated with this transition. 
	 * @return The trigger event ID
	 */
    public int getEventId()
    {
        return m_EventId;
    }

	@Override
	public String toString()
	{
        StringBuilder retValue = new StringBuilder("(FsmTransition")
			.append(" from=").append(m_FromState)
			.append(" to=").append(m_ToState).append(")");

        return retValue.toString();
	}

	/**
	 * Trigger event ID 
	 */
    private final int m_EventId;

	/**
	 * Begin state
	 */
	private final FsmState m_FromState;

	/**
	 * End state
	 */
	private final FsmState m_ToState;

	/**
	 * Action
	 */
	private final FsmAction m_Action;

	/**
	 * Guard
	 */
	private final FsmAction m_Guard;

	/** @link dependency */
    /*# FsmEvent lnkFsmEvent; */
}

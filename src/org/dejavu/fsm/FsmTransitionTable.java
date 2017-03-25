/* Generated by Together */
package org.dejavu.fsm;

import org.dejavu.util.DjvLogMsg;
import org.dejavu.util.DjvSystem;

/**
 * Look-up table for FSM transitions. This is actually a two dimensional map
 * scheme, where each state has a transition map keyed on trigger events, and
 * another map is used to locate the transition map based on the begin state.
 */
public class FsmTransitionTable {

	/**
	 * Given a begin state and a trigger event, returns a state transition, or
	 * null if none exists for this state/event combination.
	 *
	 * @param fromState
	 * @param triggerEvent
	 * @return The desired transition, or null if none matching the given
	 * criteria was found.
	 */
	public FsmTransition getTransition(FsmState fromState, int triggerEvent) {
		TransitionMap transitionMap;
		synchronized (stateTransitionMap) {
			transitionMap = stateTransitionMap[fromState.id];
		}
		if (null != transitionMap) {
			return transitionMap.locateTransition(triggerEvent);
		}
		return null;
	}

	/**
	 * Adds a new transition to the lookup table.
	 *
	 * @param newTransition The new transition record to be added.
	 * @throws FsmException
	 */
	public void setTransition(FsmTransition newTransition) throws FsmException {
		TransitionMap transitionMap;
		synchronized (stateTransitionMap) {
			transitionMap = stateTransitionMap[newTransition.fromState.id];
			if (null == transitionMap) {
				// The desired state was not in the map, add a new entry for it and retry
				transitionMap = new TransitionMap();
				stateTransitionMap[newTransition.fromState.id] = transitionMap;
			}
		}
		// Found the state-event table for the desired state
		// Now  add the new transition.
		transitionMap.addTransition(newTransition);
	}

	/**
	 * Map for managing transition lookup indices.
	 */
	private final TransitionMap[] stateTransitionMap = new TransitionMap[2048];

	/**
	 * Map for managing transition lookup indices for individual state, each
	 * state will have one of these maps.
	 */
	private class TransitionMap {

		/**
		 * Default constructor
		 */
		private TransitionMap() {
		}

		/**
		 * Locates a transition based on the trigger event ID. Returns null if
		 * there is no transition associated with the given event ID.
		 *
		 * @param eventId The ID of the event.
		 * @return The desired transition, or null if none matching the given
		 * criteria was found.
		 */
		public FsmTransition locateTransition(int eventId) {
			synchronized (transitions) {
				if (eventId < transitions.length) {
					return transitions[eventId];
				}
			}
			return null;
		}

		/**
		 * Adds a new transition to the map.
		 *
		 * @param transition The transition record to be added.
		 */
		public void addTransition(FsmTransition transition) {
			synchronized (transitions) {
				int id = transition.eventId;
				if (id < transitions.length) {
					if(transitions[id] != null) {
						DjvSystem.logWarning(DjvLogMsg.Category.DESIGN,
							"Transition " + transitions[id] + " already exists, overwrite with " + transition);
					}
					transitions[id] = transition;
				}
			}
		}

		/**
		 * State transition map keyed on trigger event ID's.
		 */
		private final FsmTransition[] transitions = new FsmTransition[1024];
	}
}

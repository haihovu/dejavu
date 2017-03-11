/* Generated by Together */
package org.dejavu.fsm;

/**
 * Interface for all concrete FSM events to implement.
 */
public interface FsmEvent {

	/**
	 * Gets the target context associated with this event.
	 * @return The context. Not null.
	 */
	FsmContext getContext();

	/**
	 * Gets the ID of the event.
	 * @return The event's ID.
	 */
	int getId();

	/** @link dependency */
	/*# FsmContext lnkFsmContext; */
}
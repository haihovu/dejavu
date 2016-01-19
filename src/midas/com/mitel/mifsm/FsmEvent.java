/* Generated by Together */

package com.mitel.mifsm;

/**
 * Interface for all concrete FSM events to implement. 
 */
public interface FsmEvent
{
	/**
	 * Gets the target context associated with this event. 
	 */
	FsmContext getContext();

	/**
	 * Gets the ID of the event. 
	 */
	int getId();

	/** @link dependency */
    /*# FsmContext lnkFsmContext; */
}

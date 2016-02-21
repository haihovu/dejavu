package org.dejavu.activefx;

import org.dejavu.mifsm.FsmAction;
import org.dejavu.mifsm.FsmState;

/**
 * Customised state for connections.
 * Not sure why this is needed.
 * @author haiv
 */
class AfxConnectionState extends FsmState
{
	AfxConnectionState(String name, FsmAction entryAction, FsmAction exitAction)
	{
		super(name, entryAction, exitAction);
	}
}


package org.dejavu.activefx;

import org.dejavu.fsm.FsmAction;
import org.dejavu.fsm.FsmState;

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


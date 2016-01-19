package com.mitel.activefx;

import com.mitel.mifsm.FsmAction;
import com.mitel.mifsm.FsmState;

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


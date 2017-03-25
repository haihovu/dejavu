/* Generated by Together */
package org.dejavu.activefx;

import org.dejavu.fsm.FsmAction;
import org.dejavu.fsm.FsmEvent;
import org.dejavu.fsm.FsmDomain;
import org.dejavu.fsm.FsmException;
import org.dejavu.util.DjvLogMsg.Category;
import org.dejavu.util.DjvSystem;

/**
 * The FSM domain for the Active FX framework.
 */
class AfxConnectionFsmDomain extends FsmDomain {

	/**
	 * Creates a new Active FX FSM domain.
	 *
	 * @param name Then name of the new instance of Active FX FSM domain.
	 * @throws FsmException
	 */
	AfxConnectionFsmDomain(String name) throws FsmException {
		super(name + "Fsm");

		addState(AFX_CLOSED, true);
		addState(AFX_CLOSING, false);
		addState(AFX_OPENNING, false);
		addState(AFX_IDLE, false);
		addState(AFX_READING, false);
		addState(AFX_WRITING, false);

		addTransition(AfxFsmEvent.OPEN, AFX_CLOSED, AFX_OPENNING, null, methodInitiateOpen);
		addTransition(AfxFsmEvent.CLOSE, AFX_OPENNING, AFX_CLOSING, null, methodAbortHandshake);
		addTransition(AfxFsmEvent.HANDSHAKE, AFX_OPENNING, AFX_OPENNING, null, methodHandleHandshake);
		addTransition(AfxFsmEvent.CLOSE, AFX_CLOSED, AFX_CLOSED, null, null);
		addTransition(AfxFsmEvent.OPEN_COMPLETE, AFX_OPENNING, AFX_IDLE, null, methodOpenComplete);
		addTransition(AfxFsmEvent.OPEN_FAILURE, AFX_IDLE, AFX_CLOSING, null, methodOpenFailed);
		addTransition(AfxFsmEvent.READ, AFX_IDLE, AFX_READING, methodReadGuard, methodInitiateRead);
		addTransition(AfxFsmEvent.WRITE, AFX_IDLE, AFX_WRITING, methodWriteGuard, methodInitiateWrite);
		addTransition(AfxFsmEvent.READ_COMPLETE, AFX_READING, AFX_IDLE, null, methodReadComplete);
		addTransition(AfxFsmEvent.WRITE_COMPLETE, AFX_WRITING, AFX_IDLE, null, methodWriteComplete);
		addTransition(AfxFsmEvent.OPEN_FAILURE, AFX_OPENNING, AFX_CLOSING, null, methodOpenFailed);
		addTransition(AfxFsmEvent.READ_FAILURE, AFX_READING, AFX_IDLE, null, methodReadFailed);
		addTransition(AfxFsmEvent.WRITE_FAILURE, AFX_WRITING, AFX_IDLE, null, methodWriteFailed);
		addTransition(AfxFsmEvent.CLOSE, AFX_IDLE, AFX_CLOSING, null, null);
		addTransition(AfxFsmEvent.CLOSE, AFX_READING, AFX_CLOSING, null, methodReadFailed);
		addTransition(AfxFsmEvent.CLOSE, AFX_WRITING, AFX_CLOSING, null, methodWriteFailed);
		addTransition(AfxFsmEvent.WRITE, AFX_READING, AFX_READ_WRITE, methodWriteGuard, methodInitiateWrite);
		addTransition(AfxFsmEvent.READ, AFX_WRITING, AFX_READ_WRITE, methodReadGuard, methodInitiateRead);
		addTransition(AfxFsmEvent.READ_COMPLETE, AFX_READ_WRITE, AFX_WRITING, null, methodReadComplete);
		addTransition(AfxFsmEvent.WRITE_COMPLETE, AFX_READ_WRITE, AFX_READING, null, methodWriteComplete);
		addTransition(AfxFsmEvent.READ_FAILURE, AFX_READ_WRITE, AFX_WRITING, null, methodReadFailed);
		addTransition(AfxFsmEvent.WRITE_FAILURE, AFX_READ_WRITE, AFX_READING, null, methodWriteFailed);
		addTransition(AfxFsmEvent.CLOSE, AFX_READ_WRITE, AFX_IDLE, null, methodReadWriteFailed);
		addTransition(AfxFsmEvent.CLOSE_COMPLETE, AFX_CLOSING, AFX_CLOSED, null, null);
		addTransition(AfxFsmEvent.CLOSE, AFX_CLOSING, AFX_CLOSING, null, null);
		addTransition(AfxFsmEvent.CONNECT, AFX_CLOSED, AFX_OPENNING, null, methodInitiateConnect);
	}

	@Override
	public boolean dispatchEvent(FsmEvent event, boolean queued) throws InterruptedException {
		if (null != dispatchingThread) {
			return super.dispatchEvent(event, queued);
		}

		DjvSystem.logWarning(Category.DESIGN, "Had been shutdown");
		return false;
	}

	private static final HandleHandshake methodHandleHandshake = new HandleHandshake();
	private static final AbortHandshake methodAbortHandshake = new AbortHandshake();
	private static final ReadGuard methodReadGuard = new ReadGuard();
	private static final WriteGuard methodWriteGuard = new WriteGuard();
	private static final InitiateClose methodInitiateClose = new InitiateClose();
	private static final InitiateOpen methodInitiateOpen = new InitiateOpen();
	private static final InitiateRead methodInitiateRead = new InitiateRead();
	private static final InitiateWrite methodInitiateWrite = new InitiateWrite();
	private static final OpenComplete methodOpenComplete = new OpenComplete();
	private static final ReadComplete methodReadComplete = new ReadComplete();
	private static final WriteComplete methodWriteComplete = new WriteComplete();
	private static final OpenFailed methodOpenFailed = new OpenFailed();
	private static final ReadFailed methodReadFailed = new ReadFailed();
	private static final WriteFailed methodWriteFailed = new WriteFailed();
	private static final ReadWriteFailed methodReadWriteFailed = new ReadWriteFailed();
	private static final InitiateConnect methodInitiateConnect = new InitiateConnect();

	public static final AfxConnectionState AFX_CLOSED = new AfxConnectionState("AFX_CLOSED", null, null);
	public static final AfxConnectionState AFX_CLOSING = new AfxConnectionState("AFX_CLOSING", methodInitiateClose, null);
	public static final AfxConnectionState AFX_OPENNING = new AfxConnectionState("AFX_OPENNING", null, null);
	public static final AfxConnectionState AFX_IDLE = new AfxConnectionState("AFX_IDLE", null, null);
	public static final AfxConnectionState AFX_READING = new AfxConnectionState("AFX_READING", null, null);
	public static final AfxConnectionState AFX_WRITING = new AfxConnectionState("AFX_WRITING", null, null);
	public static final AfxConnectionState AFX_READ_WRITE = new AfxConnectionState("AFX_READ_WRITE", null, null);

	private static class ReadGuard implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			return thisConnection.readGuard(event);
		}
	}

	private static class WriteGuard implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			return thisConnection.writeGuard(event);
		}
	}

	private static class InitiateOpen implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) throws InterruptedException {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.initiateOpen(event);
			return true;
		}
	}

	private static class HandleHandshake implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) throws InterruptedException {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.handleHandshake(event);
			return true;
		}
	}

	private static class AbortHandshake implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.abortHandshake(event);
			return true;
		}
	}

	private static class OpenComplete implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.openComplete(event);
			return true;
		}
	}

	private static class InitiateClose implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) throws InterruptedException {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.initiateClose(event);
			return true;
		}
	}

	private static class InitiateRead implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) throws InterruptedException {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.initiateRead(event);
			return true;
		}
	}

	private static class InitiateWrite implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.initiateWrite(event);
			return true;
		}
	}

	private static class ReadComplete implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.readComplete(event);
			return true;
		}
	}

	private static class ReadFailed implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.readFailed(event);
			return true;
		}
	}

	private static class OpenFailed implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			if (thisConnection != null) {
				thisConnection.openFailed(event);
			}
			return true;
		}
	}

	private static class WriteComplete implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.writeComplete(event);
			return true;
		}
	}

	private static class WriteFailed implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.writeFailed(event);
			return true;
		}
	}

	private static class ReadWriteFailed implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.readWriteFailed(event);
			return true;
		}
	}

	private static class InitiateConnect implements FsmAction {

		@Override
		public boolean handleEvent(FsmEvent event) throws InterruptedException {
			AfxConnection thisConnection = (AfxConnection) event.getContext();
			thisConnection.initiateConnect(event);
			return true;
		}
	}

}

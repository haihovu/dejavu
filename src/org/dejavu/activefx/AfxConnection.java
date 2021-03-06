/* Generated by Together */
package org.dejavu.activefx;

import java.nio.ByteBuffer;

import org.dejavu.fsm.FsmContext;
import org.dejavu.fsm.FsmDomain;
import org.dejavu.fsm.FsmEvent;
import org.dejavu.fsm.FsmState;
import org.dejavu.util.DjvExceptionUtil;
import org.dejavu.util.DjvLogMsg.Category;
import org.dejavu.util.DjvSystem;
import java.nio.channels.SelectableChannel;

/**
 * The abstract interface for all Active FX connection types.
 */
public abstract class AfxConnection implements FsmContext {

	/**
	 * Creates a new connection.
	 *
	 * @param domain The associated Active FX domain. All connection must be
	 * associated with a domain.
	 */
	public AfxConnection(AfxDomain domain) {
		this.domain = domain;
		setCurrentState(this.domain.getInitialState());
	}

	/**
	 * Initiates the connection request to the server.
	 *
	 * @param remoteIpAddr IP address or host name of the server
	 * @param remoteIpPort Server port.
	 * @param handler The event handler to receive acknowledgements.
	 * @return True if the request had been initiated, false if the request
	 * failed to start some how.
	 * @throws java.lang.InterruptedException
	 */
	public boolean open(String remoteIpAddr, int remoteIpPort, AfxEventHandler handler) throws InterruptedException {
		return domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.OPEN, this, remoteIpAddr, remoteIpPort, handler), false);
	}

	/**
	 * Open a connection for receiving data.
	 *
	 * @param localPort The port for local binding.
	 * @param handler The event handler to receive acknowledgements.
	 * @return True if the request had been initiated, false if the request
	 * failed to start some how.
	 * @throws java.lang.InterruptedException
	 */
	public boolean open(int localPort, AfxEventHandler handler) throws InterruptedException {
		return domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.OPEN, this, localPort, handler), false);
	}

	/**
	 * Opens the connection with an existing channel.
	 *
	 * @param channel The channel to be used. Must be non-blocking.
	 * @param handler The event handler to receive acknowledgements.
	 * @return True if the request had been initiated, false if the request
	 * failed to start some how.
	 * @throws java.lang.InterruptedException
	 */
	public boolean open(SelectableChannel channel, AfxEventHandler handler) throws InterruptedException {
		return domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.OPEN, this, channel, handler), true);
	}

	/**
	 * Closes the connection.
	 */
	public void close() {
		try {
			domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.CLOSE, this), true);
		} catch (InterruptedException ex) {
		}
	}

	/**
	 * Initiates reading of a message from the connection. Only one read request
	 * will be accepted at any one time. Should only be invoked after the
	 * previous read request had completed.
	 *
	 * @param buffer The buffer with which to read message into.
	 * @param handler The event handler to receive the read aknowledgement.
	 * @return True if the request had been initiated, false if the request
	 * failed to start some how.
	 * @throws java.lang.InterruptedException
	 */
	public boolean read(ByteBuffer buffer, AfxEventHandler handler) throws InterruptedException {
		return domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.READ, this, buffer, handler), true);
	}

	/**
	 * Initiates writing of a message to the connection. Only one write request
	 * will be accepted at any one time. Should only be invoked after the
	 * previous write request had completed.
	 *
	 * @param buffer The buffer containing the message write.
	 * @param handler The event handler to receive the write aknowledgement.
	 * @return True if the message had been queued to be sent, false means the
	 * request was rejected, the message will not be sent.
	 * @throws java.lang.InterruptedException
	 */
	public boolean write(ByteBuffer buffer, AfxEventHandler handler) throws InterruptedException {
		return domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.WRITE, this, buffer, handler), true);
	}

	/**
	 * Initiates the registering mode on the channel
	 *
	 * @param sockChannel
	 * @param handler
	 * @return True if the request had been initiated, false if the request
	 * failed to start some how.
	 * @throws java.lang.InterruptedException
	 */
	public boolean connect(SelectableChannel sockChannel, AfxEventHandler handler) throws InterruptedException {
		return domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.CONNECT, this, sockChannel, handler), true);
	}

	/**
	 * Retrieves the connection type.
	 *
	 * @return The connection type, one of:
	 * <ul>
	 * <li>AFX_CONNECTION_TCP</li>
	 * <li>AFX_CONNECTION_UDP</li>
	 * <li>AFX_CONNECTION_TLS</li>
	 * </ul>
	 */
	public abstract int getConnectionType();

	public abstract boolean isOpen();

	public abstract int getReceiveBufferSize();

	public abstract int getSendBufferSize();

	public abstract void setReceiveBufferSize(int newSize);

	public abstract void setSendBufferSize(int newSize);

	@Override
	public FsmDomain getFsmDomain() {
		return domain.getFsmDomain();
	}

	@Override
	public FsmState getCurrentState() {
		return m_CurrentState;
	}

	@Override
	public final void setCurrentState(FsmState newState) {
		m_CurrentState = newState;
	}

	abstract void initiateOpen(FsmEvent evt) throws InterruptedException;

	abstract void initiateClose(FsmEvent evt) throws InterruptedException;

	abstract void initiateRead(FsmEvent evt) throws InterruptedException;

	abstract void initiateWrite(FsmEvent evt);

	abstract void handleHandshake(FsmEvent evt) throws InterruptedException;

	abstract void abortHandshake(FsmEvent evt);

	abstract void openComplete(FsmEvent evt);

	abstract void openFailed(FsmEvent evt);

	abstract void readComplete(FsmEvent evt);

	abstract void readFailed(FsmEvent evt);

	abstract void writeComplete(FsmEvent evt);

	abstract void writeFailed(FsmEvent evt);

	abstract void readWriteFailed(FsmEvent evt);

	abstract void initiateConnect(FsmEvent evt) throws InterruptedException;

	@SuppressWarnings("NestedAssignment")
	synchronized boolean readGuard(FsmEvent evt) {
		try {
			AfxFsmEvent event = (AfxFsmEvent) evt;
			if (null != (readBuffer = event.getBuffer())) {
				readBuffer.clear();
				readEventHandler = event.getEventHandler();
				return true;
			}
		} catch (Exception e) {
			DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
			readBuffer = null;
		}

		return false;
	}

	@SuppressWarnings("NestedAssignment")
	boolean writeGuard(FsmEvent evt) {
		try {
			AfxFsmEvent event = (AfxFsmEvent) evt;
			if (null != (m_WriteBuffer = event.getBuffer())) {
				writeEventHandler = event.getEventHandler();
				return true;
			}
		} catch (Exception e) {
			DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
			m_WriteBuffer = null;
		}

		return false;
	}

	public final static int AFX_CONNECTION_TCP = 1;
	public final static int AFX_CONNECTION_UDP = 2;
	public final static int AFX_CONNECTION_TLS = 3;

	@SuppressWarnings("ProtectedField")
	protected ByteBuffer readBuffer = null;
	@SuppressWarnings("ProtectedField")
	protected ByteBuffer m_WriteBuffer = null;

	/**
	 * The domain governing this connection
	 */
	@SuppressWarnings("ProtectedField")
	protected final AfxDomain domain;

	/**
	 * Handler of connection events
	 */
	@SuppressWarnings("ProtectedField")
	protected AfxEventHandler connectionventHandler = null;

	/**
	 * Handler of read events
	 */
	@SuppressWarnings("ProtectedField")
	protected AfxEventHandler readEventHandler = null;

	/**
	 * Handler of write events
	 */
	@SuppressWarnings("ProtectedField")
	protected AfxEventHandler writeEventHandler = null;

	/**
	 * Current state of the connection
	 */
	@SuppressWarnings("ProtectedField")
	protected FsmState m_CurrentState;
}

/* Generated by Together */
package org.dejavu.activefx;

import org.dejavu.util.DjvSystem;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.dejavu.fsm.FsmEvent;
import org.dejavu.util.DjvExceptionUtil;
import org.dejavu.util.DjvLogMsg.Category;
import java.nio.channels.SelectableChannel;
import java.io.IOException;

/**
 * An AFX connection for handling TCP/IP channels
 */
public class AfxConnectionTcp extends AfxConnection implements ReactorEventHandler {

	private static volatile int gNumMsgsWrittenForAllChannels;
	private static volatile long gFirstWriteTs;
	private static volatile long gLastWriteTs;
	private volatile int numMsgsWrittenForThisChannel;

	/**
	 * Creates a new connection instance
	 *
	 * @param domain The AFX domain driving the state of this connection.
	 */
	public AfxConnectionTcp(AfxDomain domain) {
		super(domain);
	}

	@Override
	public int getConnectionType() {
		return AfxConnection.AFX_CONNECTION_TCP;
	}

	@Override
	public synchronized boolean isOpen() {
		return channel != null ? this.channel.socket().isOutputShutdown() : false;
	}

	@Override
	public void onAccept() {
		domain.deregisterHandler(this, SelectionKey.OP_ACCEPT);
	}

	/**
	 * Retrieves the application-wide message write rate
	 *
	 * @return The number of messages written per hour since the start of the
	 * application.
	 */
	public static int getMsgsRatePerHour() {
		long delta = (gLastWriteTs - gFirstWriteTs);
		if (delta > 0) {
			return (int) (((float) gNumMsgsWrittenForAllChannels / (float) delta) * 1000.0 * 3600.0);
		}
		return 0;
	}

	@Override
	public synchronized void onWrite() throws InterruptedException {
		if (!reactorWriteEnabled || channel == null) {
			// This does happen from time to time
			return;
		}

		try {
			if (null != m_WriteBuffer) {
				if (-1 < channel.write(m_WriteBuffer)) {
					if (m_WriteBuffer.remaining() == 0) {
						gLastWriteTs = System.currentTimeMillis();
						++gNumMsgsWrittenForAllChannels;
						++numMsgsWrittenForThisChannel;

						// Wrote the entire buffer, don't need write event any more ...
						disableReactorWrite();

						// ... null out the write buffer ...
						m_WriteBuffer = null;

						// ... and trigger the transition on event WRITE_COMPLETE
						domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.WRITE_COMPLETE, this), true);
					}
				} else {
					DjvSystem.logError(Category.DESIGN, "Write on " + this + " failed on " + channel);

					// Write failed, don't need write event any more ...
					disableReactorWrite();

					// ... null out the write buffer ...
					m_WriteBuffer = null;

					// ... and trigger the transition on event WRITE_FAILURE
					domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.WRITE_FAILURE, this), true);
				}
			} else {
				// No buffer, don't need write event any more ...
				DjvSystem.logError(Category.DESIGN, "No write buffer");

				disableReactorWrite();
			}
		} catch (IOException e) {
			DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));

			// Write failed, don't need write event any more ...
			disableReactorWrite();

			// ... null out the write buffer ...
			m_WriteBuffer = null;

			// ... And trigger the transition on event FAILURE
			domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.WRITE_FAILURE, this), true);
		}
	}

	@Override
	public synchronized void onConnect() throws InterruptedException {
		String cause;

		if (channel == null) {
			DjvSystem.logError(Category.DESIGN, "Connect failed - channel is not open");
			cause = "Channel is not open.";
		} else if (channel.isConnectionPending()) {
			try {
				if (channel.finishConnect()) {
					if (DjvSystem.diagnosticEnabled()) {
						DjvSystem.logInfo(Category.DESIGN, this + " finished connecting");
					}
					domain.deregisterHandler(this, SelectionKey.OP_CONNECT);
					domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.OPEN_COMPLETE, this), true);

					return;
				} else {
					if (DjvSystem.diagnosticEnabled()) {
						DjvSystem.logInfo(Category.DESIGN, this + " still waiting for connect completion");
					}
					// Connection not yet finish (this is a non-blocking channel), keep checking
					return;
				}
			} catch (IOException e) {
				if (DjvSystem.diagnosticEnabled()) {
					DjvSystem.logWarning(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
				}
				cause = e.getMessage();
			}
		} else if (!channel.isConnected()) {
			cause = channel + " not in connect pending state.";

			if (DjvSystem.diagnosticEnabled()) {
				DjvSystem.logInfo(Category.DESIGN, cause);
			}
		} else {
			if (DjvSystem.diagnosticEnabled()) {
				DjvSystem.logInfo(Category.DESIGN, this + " already connected, go");
			}
			// Channel is connected
			domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.OPEN_COMPLETE, this), true);
			return;
		}

		domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.OPEN_FAILURE, this, cause), true);
	}

	@Override
	public synchronized void onDisconnect() throws InterruptedException {
		domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.CLOSE, this), false);
	}

	@Override
	public synchronized void onRead() throws InterruptedException {
		try {
			if ((readBuffer != null) && (readBuffer.hasRemaining())) {
				if (null != channel) {
					if (-1 < channel.read(readBuffer)) {
						if (readBuffer.remaining() == 0) {
							// ... And trigger the transition on event READ_COMPLETE
							domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.READ_COMPLETE, this), true);
						}
					} else {
						// Probably end-of-stream, i.e. the other side closed its writer ...
						// ... trigger the transition on event READ_FAILURE
						throw new IOException("Read on " + this + " returned -1, probably end of stream.");
					}
				} else {
					throw new IOException("Channel is NULL, probably in the middle of shutting down");
				}
			} else {
				// Not ready to accept more read event, stop the reading process ...
				disableReactorRead();
			}
		} catch (java.io.IOException e) {
			DjvSystem.logWarning(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));

			// Read failed, stop the reading process ...
			disableReactorRead();
			domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.READ_FAILURE, this), true);
		}
	}

	@Override
	public int getReceiveBufferSize() {
		try {
			synchronized(this) {
				if(channel != null) {
					return channel.socket().getReceiveBufferSize();
				}
			}
		} catch (SocketException ex) {
			DjvSystem.logWarning(Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
		} catch (RuntimeException e) {
			DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
		}
		return -1;
	}

	@Override
	public int getSendBufferSize() {
		try {
			synchronized(this) {
				if(channel != null) {
					return channel.socket().getSendBufferSize();
				}
			}
		} catch (SocketException ex) {
			DjvSystem.logWarning(Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
		} catch (RuntimeException e) {
			DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
		}
		return -1;
	}

	@Override
	public void setReceiveBufferSize(int newSize) {
		try {
			synchronized(this) {
				if(channel != null) {
					channel.socket().setReceiveBufferSize(newSize);
				}
			}
		} catch (SocketException ex) {
			DjvSystem.logWarning(Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
		} catch (RuntimeException e) {
			DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
		}
	}

	@Override
	public void setSendBufferSize(int newSize) {
		try {
			synchronized(this) {
				if(channel != null) {
					channel.socket().setSendBufferSize(newSize);
				}
			}
		} catch (SocketException ex) {
			DjvSystem.logWarning(Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
		} catch (RuntimeException e) {
			DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
		}
	}

	@Override
	public SelectableChannel getHandle() {
		synchronized(this) {
			return channel;
		}
	}

	@Override
	public String toString() {
		synchronized(this) {
			return "{channel:" + (channel != null ? channel.toString() : "null") + ", currentState:" + getCurrentState() + ", currentInterest:" + domain.getInterestOps(this) + ", currentReady:" + domain.getReadyOps(this) + ", numMsgsWritten:" + numMsgsWrittenForThisChannel + "}";
		}
	}

	@Override
	public void handleHandshake(FsmEvent evt) throws InterruptedException {
		DjvSystem.logInfo(Category.DESIGN, "Don't handle this event in this class");
	}

	@Override
	public void abortHandshake(FsmEvent evt) {
		DjvSystem.logInfo(Category.DESIGN, "Don't handle this event in this class");
	}

	@Override
	@SuppressWarnings("NestedAssignment")
	public synchronized void initiateOpen(FsmEvent evt) throws InterruptedException {
		AfxFsmEvent afxEvent = (AfxFsmEvent) evt;
		try {
			connectionventHandler = afxEvent.getEventHandler();

			// Close any existing channel
			if (channel != null) {
				channel.close();
			}

			if (null == (channel = (SocketChannel) afxEvent.channel)) {
				channel = SocketChannel.open();

				channel.configureBlocking(false);

				if (channel.connect(new java.net.InetSocketAddress(afxEvent.ipAddr, afxEvent.ipPort))) {
					DjvSystem.logWarning(Category.DESIGN, "Unexpected non-blocking connect returning true");
				}
			} else {
				channel.configureBlocking(false);
			}

			// Tells reactor to notify of connect events
			domain.registerHandler(this, SelectionKey.OP_CONNECT);
		} catch (IOException e) {
			DjvSystem.logWarning(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
			domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.OPEN_FAILURE,
				this, e.getMessage()), true);
		}
	}

	@Override
	public synchronized void initiateClose(FsmEvent evt) throws InterruptedException {
		if (domain == null) {
			return;
		}

		domain.removeHandler(this);
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
				DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
			} finally {
				channel = null;
			}

			domain.dispatchEvent(new AfxFsmEvent(AfxFsmEvent.CLOSE_COMPLETE, this), true);

			if (connectionventHandler != null) {
				connectionventHandler.closed();
			}
		}
	}

	@Override
	public void initiateRead(FsmEvent evt) throws InterruptedException{
		// Tells the reactor to notify me of read events
		enableReactorRead();
	}

	@Override
	public void initiateWrite(FsmEvent evt) {
		// Tells reactor to notify of write events
		enableReactorWrite();
		if (gFirstWriteTs == 0) {
			gFirstWriteTs = System.currentTimeMillis();
		}
	}

	@Override
	public synchronized void initiateConnect(FsmEvent evt) throws InterruptedException {
		AfxFsmEvent afxEvent = (AfxFsmEvent) evt;
		if (connectionventHandler == null) {
			connectionventHandler = afxEvent.getEventHandler();
		}
		SelectableChannel chan = afxEvent.channel;
		if (chan instanceof SocketChannel) {
			channel = (SocketChannel) chan;
			if (channel.isConnected()) {
				onConnect();
			} else {
				enableReactorConnect();
			}
		} else {
			DjvSystem.logError(Category.DESIGN, "Wrong channel type " + chan + " connection probably pooched");
		}
	}

	@Override
	public synchronized void openComplete(FsmEvent evt) {
		if (connectionventHandler != null) {
			connectionventHandler.openCompleted();
		}
	}

	@Override
	public synchronized void openFailed(FsmEvent evt) {
		AfxFsmEvent afxEvt = (AfxFsmEvent) evt;

		if (connectionventHandler != null) {
			connectionventHandler.openFailed(afxEvt != null ? afxEvt.getCause() : "unknown");
		}

		// No more open related event will come from this connection
		connectionventHandler = null;
	}

	@Override
	public synchronized void readComplete(FsmEvent evt) {
		if (readEventHandler != null) {
			readBuffer.flip();
			readEventHandler.readCompleted(readBuffer);
		}

		readBuffer = null;
	}

	@Override
	public synchronized void readFailed(FsmEvent evt) {
		if (readEventHandler != null) {
			readEventHandler.readFailed();
		}
	}

	@Override
	public synchronized void writeComplete(FsmEvent evt) {
		if (null != writeEventHandler) {
			writeEventHandler.writeCompleted();
		}
	}

	@Override
	public synchronized void writeFailed(FsmEvent evt) {
		if (null != writeEventHandler) {
			writeEventHandler.writeFailed();
		}
	}

	@Override
	public synchronized void readWriteFailed(FsmEvent evt) {
		if (readEventHandler != null) {
			readEventHandler.readFailed();
		}

		if (null != writeEventHandler) {
			writeEventHandler.writeFailed();
		}
	}

	protected synchronized void disableReactorRead() {
		if (reactorReadEnabled) {
			domain.deregisterHandler(this, SelectionKey.OP_READ);
			reactorReadEnabled = false;
		}
	}

	protected synchronized void disableReactorWrite() {
		if (reactorWriteEnabled) {
			domain.deregisterHandler(this, SelectionKey.OP_WRITE);
			reactorWriteEnabled = false;
		}
	}

	protected synchronized void disableReactorConnect() {
		if (reactorConnectEnabled) {
			domain.deregisterHandler(this, SelectionKey.OP_CONNECT);
			reactorConnectEnabled = false;
		}
	}

	protected synchronized void enableReactorRead() {
		if (!reactorReadEnabled) {
			domain.registerHandler(this, SelectionKey.OP_READ);
			reactorReadEnabled = true;
		}
	}

	protected synchronized void enableReactorWrite() {
		if (!reactorWriteEnabled) {
			domain.registerHandler(this, SelectionKey.OP_WRITE);
			reactorWriteEnabled = true;
		} else {
			DjvSystem.logWarning(Category.DESIGN, "Already write enabled");
		}
	}

	/**
	 * Enables the reactor connect event. Not thread-safe
	 */
	protected void enableReactorConnect() {
		if (!reactorConnectEnabled) {
			domain.registerHandler(this, SelectionKey.OP_CONNECT);
			reactorConnectEnabled = true;
		}
	}

	@SuppressWarnings("ProtectedField")
	protected SocketChannel channel = null;

	/** @link dependency */
	/*# AfxFsmEvent lnkAfxFsmEvent; */
	@SuppressWarnings("ProtectedField")
	protected boolean reactorReadEnabled;
	@SuppressWarnings("ProtectedField")
	protected boolean reactorWriteEnabled;
	@SuppressWarnings("ProtectedField")
	protected boolean reactorConnectEnabled;
}

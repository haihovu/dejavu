/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.activefx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dejavu.fsm.FsmException;
import org.dejavu.util.DjvBackgroundTask;
import org.dejavu.util.DjvExceptionUtil;
import org.dejavu.util.DjvLogMsg;
import org.dejavu.util.DjvSystem;

/**
 *
 * @author Hai Vu
 */
public class AfxTcpTester {
	final AfxAcceptor acceptor;
	private final TestContext context = new TestContext();
	private static final AfxDomain gDomain;
	private boolean done;
	
	private class DataConsumer {
		private final AfxEventHandler handler = new AfxEventAdaptor() {
			@Override
			public void readFailed() {
				super.readFailed(); 
				connection.close();
			}

			@Override
			public void readCompleted(ByteBuffer returnedBuffer) {
				super.readCompleted(returnedBuffer);
				try {
					initiateRead();
				} catch (InterruptedException ex) {
					connection.close();
				}
			}
		};
		
		private final AfxConnection connection;
		private final ByteBuffer buf = ByteBuffer.allocate(512);
		
		private DataConsumer(AfxConnection conn) {
			super();
			connection = conn;
			context.increment();
		}
		
		private void initiateRead() throws InterruptedException {
			DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, "Initiate read on " + connection);
			buf.rewind();
			if(!connection.read(buf, handler)) {
				DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, "Failed to initiate read, terminate");
			}
		}
	}
	
	private class DataProducer extends DjvBackgroundTask {
		private final AfxEventHandler handler = new AfxEventAdaptor() {
			@Override
			public void closed() {
				super.closed();
				stop();
			}
			
			@Override
			public void writeCompleted() {
				super.writeCompleted();
				synchronized(DataProducer.this) {
					if(--maxWrite > 0) {
						initiateWrite();
					} else {
						stop();
					}
				}
			}
		};
		
		private final AfxConnection connection;
		private final ByteBuffer buf;
		private int maxWrite;
		private final byte [] data = new byte[1024];
		
		private DataProducer(AfxConnection connection, int maxWrites) {
			super("Produce");
			this.connection = connection;
			buf = ByteBuffer.allocate(data.length);
			this.maxWrite = maxWrites;
		}
		
		private void initiateWrite() {
			new Thread(() -> {
				try {
					DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, "Initiate write on " + connection);
					for(int i = 0; i < data.length; ++i) {
						data[i] = (byte)(Math.random() * 256.0);
					}
					
					buf.rewind();
					buf.put(data);
					buf.rewind();

					connection.write(buf, handler);
				} catch(InterruptedException e) {
					stop();
				}
			}).start();
		}
		
		@Override
		@SuppressWarnings("SleepWhileInLoop")
		public void run() {
			try {
				initiateWrite();
				int counter = 1000;
				while(getRunFlag() && ((--counter) > 0)) {
					Thread.sleep(1000);
				}
			} catch (InterruptedException ex) {
			} finally {
				connection.close();
				context.decrement();
			}
		}
	}

	private class TestContext {
		private int testerCount;
		void increment() {
			synchronized(this) {
				++testerCount;
			}
		}
		void decrement() {
			synchronized(this) {
				if(--testerCount <=0) {
					stopTest();
				}
			}
		}
	}
	
	/**
	 * Acceptor class
	 */
	private class Acceptor extends AfxEventAdaptor {
		Acceptor() {
			super();
		}
		/**
		 * Initiates the accept process
		 */
		private void accept() {
			try {
				acceptor.open("127.0.0.1", 12345, this);
				acceptor.accept();
			} catch (AfxException|IOException ex) {
				DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
			}
		}

		@Override
		public void acceptCompleted(AfxConnection newConnection) {
			super.acceptCompleted(newConnection);
			try {
				new DataConsumer(newConnection).initiateRead();
			} catch (InterruptedException ex) {
				stopTest();
			}
		}

		@Override
		public void acceptCompleted(SelectableChannel newChannel) {
			super.acceptCompleted(newChannel);
			try {
				AfxConnectionTcp tcp = new AfxConnectionTcp(gDomain);
				tcp.connect(newChannel, new AfxEventAdaptor(){
					@Override
					public void openCompleted() {
						DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, tcp + " opened, start reading");
						try {
							new DataConsumer(tcp).initiateRead();
						} catch (InterruptedException ex) {
							stopTest();
						}
					}
				});
			} catch (InterruptedException ex) {
				stopTest();
			}
		}
	}
	public AfxTcpTester(AfxAcceptor acceptor) {
		this.acceptor = acceptor;
	}
	public AfxTcpTester() {
		this.acceptor = new AfxAcceptor(gDomain);
	}
	
	private void stopTest() {
		synchronized(this) {
			done = true;
			this.notifyAll();
		}
	}
	/**
	 * Conducts the test
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("SleepWhileInLoop")
	void test() throws InterruptedException {
		new Acceptor().accept();
		try {
			for(int i = 0; i < 10; ++i) {
				AfxConnection connector = new AfxConnectionTcp(gDomain);
				connector.open("127.0.0.1", 12345, new AfxEventAdaptor(){
					@Override
					public void openCompleted() {
						super.openCompleted();
						new DataProducer(connector, 128).start();
					}
				});
			}
			int countdown = 1000;
			while(countdown-- > 0) {
				synchronized(this) {
					if(!done) {
						this.wait(1000);
					} else {
						break;
					}
				}
			}
			Thread.sleep(1000);
		} finally {
			DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, "Closing acceptor");
			acceptor.close();
		}
	}
	public static void main(String[] args) {
		try {
			DjvSystem.setLogLevel(2);
			gDomain.start(1024, 5);
			AfxTcpTester tester = new AfxTcpTester();
			tester.test();
		} catch (InterruptedException ex) {
		} catch (IOException ex) {
			Logger.getLogger(AfxTcpTester.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			gDomain.stop();
		}
	}
	static {
		AfxDomain tmp = null;
		try {
			tmp = new AfxDomain("Tester");
		} catch (FsmException ex) {
			DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
		}
		gDomain = tmp;
	}
}
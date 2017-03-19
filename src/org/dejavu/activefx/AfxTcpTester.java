/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.activefx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	private final TestContext context = new TestContext();
	private static final Map<Integer, AfxDomain> gDomain = new HashMap<>(6);
	private boolean done;
	private final Set<AfxConnection> consumers = new HashSet<>();
	
	private static int writeCounter;
	private static int readCounter;
	
	private class DataConsumer {
		private final AfxEventHandler handler = new AfxEventAdaptor() {
			@Override
			public void readFailed() {
				super.readFailed(); 
				connection.close();
				done();
			}

			@Override
			public void readCompleted(ByteBuffer returnedBuffer) {
				super.readCompleted(returnedBuffer);
				synchronized(AfxTcpTester.class) {
					++readCounter;
				}
				initiateRead();
			}
		};

		@Override
		public String toString() {
			return "DataConsumer:{" + " connection=" + connection + '}';
		}
		
		private void done() {
			synchronized(consumers) {
				consumers.remove(connection);
				if(consumers.isEmpty()) {
					DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, "No more consumer");
				} else {
					DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, consumers.size() + " consumers left");
				}
			}
		}
		private final AfxConnection connection;
		private final ByteBuffer buf = ByteBuffer.allocate(512);
		
		private DataConsumer(AfxConnection conn) {
			super();
			connection = conn;
			synchronized(consumers) {
				consumers.add(conn);
			}
			context.increment();
		}
		
		private void initiateRead() {
			// Use another thread to bring out any thread-safety issues.
			new Thread(()->{
				try {
					buf.rewind();
					if(!connection.read(buf, handler)) {
						DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, "Failed to initiate read, terminate");
						connection.close();
						done();
					}
				} catch (InterruptedException ex) {
					connection.close();
					done();
				}
			}).start();
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
			public String toString() {
				return "DataProducer:{connection:" + connection + '}';
			}
			
			@Override
			public void writeCompleted() {
				super.writeCompleted();
				synchronized(AfxTcpTester.class) {
					++writeCounter;
				}
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
			// Use another thread to bring out any thread-safety issues.
			new Thread(() -> {
				try {
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
				DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, testerCount + " producer(s) left");
			}
		}
	}
	
	/**
	 * Acceptor class
	 */
	private class Acceptor extends AfxEventAdaptor {
		private final int port;
		private final AfxAcceptor acceptor;
		private final AfxDomain domain;
		Acceptor(int port, AfxDomain domain) {
			super();
			this.port = port;
			this.acceptor = new AfxAcceptor(domain);
			this.domain = domain;
		}
		/**
		 * Initiates the accept process
		 * @return 
		 */
		private Acceptor accept() {
			try {
				acceptor.open("127.0.0.1", port, this);
				acceptor.accept();
			} catch (AfxException|IOException ex) {
				DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
			}
			return this;
		}
		private void close() {
			acceptor.close();
		}
		@Override
		public String toString() {
			return "Acceptor{acceptor:" + acceptor + '}';
		}

		@Override
		public void acceptCompleted(AfxConnection newConnection) {
			super.acceptCompleted(newConnection);
			new DataConsumer(newConnection).initiateRead();
		}

		@Override
		public void acceptCompleted(SelectableChannel newChannel) {
			super.acceptCompleted(newChannel);
			try {
				AfxConnectionTcp tcp = new AfxConnectionTcp(domain);
				tcp.connect(newChannel, new AfxEventAdaptor(){
					@Override
					public void openCompleted() {
						DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, tcp + " opened, start reading");
						new DataConsumer(tcp).initiateRead();
					}
					@Override
					public String toString() {
						return "receiver:{connection:" + tcp + "}";
					}
				});
			} catch (InterruptedException ex) {
				stopTest();
			}
		}
	}
	/**
	 * Creates a test instance.
	 */
	public AfxTcpTester() {
	}
	/**
	 * Halts the test process.
	 */
	private void stopTest() {
		synchronized(this) {
			done = true;
			this.notifyAll();
		}
	}
	/**
	 * Conducts the test
	 * @throws InterruptedException 
	 * @throws org.dejavu.fsm.FsmException 
	 * @throws java.io.IOException 
	 */
	@SuppressWarnings("SleepWhileInLoop")
	void test() throws InterruptedException, FsmException, IOException {
		int[] ports = new int[]{12344, 12345, 12346};
		List<Acceptor> acceptors = new LinkedList<>();
		try {
			for(int port : ports) {
				AfxDomain domain = new AfxDomain("Test" + port);
				domain.start(512, 5);
				synchronized(gDomain) {
					gDomain.put(port, domain);
				}
				acceptors.add(new Acceptor(port, domain).accept());
				try {
					for(int i = 0; i < 64; ++i) {
						AfxConnection connector = new AfxConnectionTcp(domain);
						connector.open("127.0.0.1", port, new AfxEventAdaptor(){
							@Override
							public void openCompleted() {
								super.openCompleted();
								new DataProducer(connector, 256).start();
							}
							@Override
							public String toString() {
								return "Connector:{" + connector + "}";
							}
						});
					}
				} finally {
					DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, "Closing acceptor");
					synchronized(AfxTcpTester.class) {
						DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, writeCounter + " msgs written, " + readCounter + " msgs read");
					}
				}
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
			// The producers are probably completed, give the consumers sometime to complete
			Thread.sleep(60000);
		} finally {
			acceptors.forEach((acceptor) -> {
				acceptor.close();
			});
		}
	}
	public static void main(String[] args) {
		try {
			DjvSystem.setLogLevel(2);
			AfxTcpTester tester = new AfxTcpTester();
			tester.test();
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
		} catch (IOException | FsmException ex) {
			DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
		} finally {
			synchronized(gDomain) {
				gDomain.values().forEach((domain) -> {
					domain.stop();
				});
			}
		}
	}
}

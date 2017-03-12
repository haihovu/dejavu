/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.activefx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
public class AfxUdpTester {
	final AfxConnectionUdp receivor;
	final AfxConnectionUdp connector;
	private static final AfxDomain gDomain;
	private final byte [] data = new byte[1024];
	private boolean dataReady;
	private boolean done;
	
	private class DataConsumer {
		private final AfxEventHandler handler = new AfxEventAdaptor() {
			@Override
			public void readCompleted(ByteBuffer returnedBuffer) {
				new Thread(() -> {
					try {
						super.readCompleted(returnedBuffer);
						byte[] returnData = returnedBuffer.array();
						synchronized(data) {
							for(int i = 0; i <  returnData.length; ++i) {
								if(returnData[i] != data[i]) {
									DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, "Data check failed");
								}
							}
							dataReady = false;
							data.notify();
						}
						initiateRead();
					} catch (InterruptedException ex) {
						stopTest();
					}
				}).start();
			}
		};
		private final AfxConnection connection;
		private final ByteBuffer buf = ByteBuffer.allocate(1024);
		private DataConsumer(AfxConnection conn) {
			super();
			connection = conn;
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
				synchronized(AfxUdpTester.this) {
					dataReady = true;
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
		
		private DataProducer(AfxConnection connection, int maxWrites) {
			super("Produce");
			this.connection = connection;
			buf = ByteBuffer.allocate(data.length);
			this.maxWrite = maxWrites;
		}
		
		private void initiateWrite() {
			new Thread(() -> {
				try {
					int safety = 10;
					synchronized(data) {
						while(dataReady && (--safety > 0)) {
							data.wait(1000);
						}
						DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, "Initiate write on " + connection);
						for(int i = 0; i < data.length; ++i) {
							data[i] = (byte)(Math.random() * 256.0);
						}
						buf.rewind();
						buf.put(data);
						buf.rewind();
					}
					connection.write(buf, handler);
				} catch(InterruptedException e) {
					stopTest();
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
				stopTest();
			}
		}
	}
	
	public AfxUdpTester() {
		this.receivor = new AfxConnectionUdp(gDomain, 12344);
		this.connector = new AfxConnectionUdp(gDomain, 12344);
	}
	
	private void stopTest() {
		synchronized(this) {
			done = true;
			this.notifyAll();
		}
	}
	
	@SuppressWarnings("SleepWhileInLoop")
	void test() throws InterruptedException {
		try {
			receivor.open(null, new AfxEventAdaptor(){
				@Override
				public void closed() {
					super.closed(); 
					stopTest();
				}

				@Override
				public void openCompleted() {
					super.openCompleted();
					try {
						new DataConsumer(receivor).initiateRead();
						connector.connect(new InetSocketAddress("127.0.0.1", 12344), new AfxEventAdaptor(){
							@Override
							public void openCompleted() {
								super.openCompleted();
								new DataProducer(connector, 2048).start();
							}
						});
					} catch (InterruptedException ex) {
						stopTest();
					} catch (IOException ex) {
						DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
						stopTest();
					}
				}
			});
			
			int countdown = 100;
			while(countdown-- > 0) {
				synchronized(this) {
					if(!done) {
						this.wait(1000);
					} else {
						break;
					}
				}
			}
		} catch (RuntimeException ex) {
			DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
		} finally {
			receivor.close();
			connector.close();
		}
	}
	public static void main(String[] args) {
		try {
			DjvSystem.setLogLevel(2);
			gDomain.start(10, 5);
			AfxUdpTester tester = new AfxUdpTester();
			tester.test();
		} catch (InterruptedException ex) {
		} catch (IOException ex) {
			Logger.getLogger(AfxUdpTester.class.getName()).log(Level.SEVERE, null, ex);
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

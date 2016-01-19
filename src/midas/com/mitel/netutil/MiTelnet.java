/*
 * MiTelnet.java
 *
 * Created on April 23, 2004, 8:48 PM
 */

package com.mitel.netutil;

import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.mitel.miutil.MiQueue;
import com.mitel.miutil.MiSystem;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLSocket;



/**
 *
 * @author  Hai Vu
 */
public class MiTelnet
{
	
	public static class TelnetCommand
	{
		public static final int IAC = 255;		/* interpret as command: */
		public static final int DONT = 254;	/* you are not to use option */
		public static final int DO = 253;		/* please, you use option */
		public static final int WONT = 252;	/* I won't use option */
		public static final int WILL = 251;	/* I will use option */
		public static final int SB = 250;		/* interpret as subnegotiation */
		public static final int GA = 249;		/* you may reverse the line */
		public static final int EL = 248;		/* erase the current line */
		public static final int EC = 247;		/* erase the current character */
		public static final int AYT = 246;		/* are you there */
		public static final int AO = 245;		/* abort output--but let prog finish */
		public static final int IP = 244;		/* interrupt process--permanently */
		public static final int BREAK = 243;	/* break */
		public static final int DM = 242;		/* data mark--for connect. cleaning */
		public static final int NOP = 241;		/* nop */
		public static final int SE = 240;		/* end sub negotiation */
		public static final int EOR = 239;     /* end of record (transparent mode) */
		public static final int ABORT = 238;	/* Abort process */
		public static final int SUSP = 237;	/* Suspend process */
		public static final int xEOF = 236;	/* End of file: EOF is already used... */
		public static final int SYNCH = 242;	/* for telfunc calls */

	}
	
	public static class TelnetOption
	{
		public static final int TELOPT_BINARY = 0;			/* 8-bit data path */
		public static final int TELOPT_ECHO = 1;			/* echo */
		public static final int TELOPT_RCP = 2;				/* prepare to reconnect */
		public static final int TELOPT_SGA = 3;				/* suppress go ahead */
		public static final int TELOPT_NAMS = 4;			/* approximate message size */
		public static final int TELOPT_STATUS = 5;			/* give status */
		public static final int TELOPT_TM = 6;				/* timing mark */
		public static final int TELOPT_RCTE = 7;			/* remote controlled transmission and echo */
		public static final int TELOPT_NAOL = 8;			/* negotiate about output line width */
		public static final int TELOPT_NAOP = 9;			/* negotiate about output page size */
		public static final int TELOPT_NAOCRD = 10;			/* negotiate about CR disposition */
		public static final int TELOPT_NAOHTS = 11;			/* negotiate about horizontal tabstops */
		public static final int TELOPT_NAOHTD = 12;			/* negotiate about horizontal tab disposition */
		public static final int TELOPT_NAOFFD = 13;			/* negotiate about formfeed disposition */
		public static final int TELOPT_NAOVTS = 14;			/* negotiate about vertical tab stops */
		public static final int TELOPT_NAOVTD = 15;			/* negotiate about vertical tab disposition */
		public static final int TELOPT_NAOLFD = 16;			/* negotiate about output LF disposition */
		public static final int TELOPT_XASCII = 17;			/* extended ascic character set */
		public static final int TELOPT_LOGOUT = 18;			/* force logout */
		public static final int TELOPT_BM = 19;				/* byte macro */
		public static final int TELOPT_DET = 20;			/* data entry terminal */
		public static final int TELOPT_SUPDUP = 21;			/* supdup protocol */
		public static final int TELOPT_SUPDUPOUTPUT = 22;	/* supdup output */
		public static final int TELOPT_SNDLOC = 23;			/* send location */
		public static final int TELOPT_TTYPE = 24;			/* terminal type */
		public static final int TELOPT_EOR = 25;			/* end or record */
		public static final int TELOPT_TUID = 26;			/* TACACS user identification */
		public static final int TELOPT_OUTMRK = 27;			/* output marking */
		public static final int TELOPT_TTYLOC = 28;			/* terminal location number */
		public static final int TELOPT_3270REGIME = 29;		/* 3270 regime */
		public static final int TELOPT_X3PAD = 30;			/* X.3 PAD */
		public static final int TELOPT_NAWS = 31;			/* window size */
		public static final int TELOPT_TSPEED = 32;			/* terminal speed */
		public static final int TELOPT_LFLOW = 33;			/* remote flow control */
		public static final int TELOPT_LINEMODE = 34;		/* Linemode option */
		public static final int TELOPT_XDISPLOC = 35;		/* X Display Location */
		public static final int TELOPT_ENVIRON = 36;		/* Environment variables */
		public static final int TELOPT_AUTHENTICATION = 37;	/* Authenticate */
		public static final int TELOPT_ENCRYPT = 38;		/* Encryption option */
		public static final int TELOPT_NEW_ENVIRON = 39;	/* New - Environment variables */
		public static final int TELOPT_TN3270E = 40;		/* TN3270E [RFC1647] */
		public static final int TELOPT_XAUTH = 41;			/* XAUTH [Earhart] */
		public static final int TELOPT_CHARSET = 42;		/* CHARSET [RFC2066] */
		public static final int TELOPT_RSP = 43;			/* Telnet Remote Serial Port (RSP) [Barnes] */
		public static final int TELOPT_CPCO = 44;			/* Com Port Control Option [RFC2217] */
		public static final int TELOPT_SLE = 45;			/* Telnet Suppress Local Echo [Atmar] */
		public static final int TELOPT_START_TLS = 46;		/* TLS(SSL) option  - lee*/		
	}
	
	public static class TlsSubOption
	{
		public static final int TLS_FOLLOWS = 1;
	}
	
	public static class MiTelnetException extends Exception
	{
		private static final long serialVersionUID = 1L;
		
		public MiTelnetException(String description)
		{
			super(description);
		}
		
		@Override
		public String toString()
		{
			return super.getMessage();
		}
	}
	
	private static class hTelnetEngine implements Runnable
	{
		
		private java.io.Reader m_Reader;
		
		private char[] m_Data = new char[64];
				
		private MiQueue m_Queue = new MiQueue(1000);
		
		private final Thread m_ThisThread = new Thread(this, "TelnetEngine");
		
		private java.io.InputStream m_Input;
		
		private java.io.OutputStream m_Output;
		
		private volatile boolean m_Runflag = true;
		
		private volatile boolean m_Started = false;
		
		private boolean m_ProcessingIAC = false;
		
		private boolean m_IacSubNegotiation = false;
		
		private int m_OptionCount = 0;
		
		private char m_PendingCommand = 0;
		
		private char m_PendingOption = 0;
		
		private boolean m_WaitingForTlsFollow = false;
		
		private boolean m_WaitToStartTls = false;
		
		private Socket m_Socket;
		
		private javax.net.ssl.SSLSocket m_sSocket;
		
		private String m_HostName;
		
		private int m_Port;
		
		private boolean m_TlsEnabled = false;
		
		private boolean m_Ready = false;
		
		private boolean m_WaitingForCmd = false;
		
		private final CharBuffer m_inputTextBuffer = CharBuffer.allocate(1024);;
		
		/**
		 * Creates a new hTelnetEngine object.
		 * @param host
		 * @param port
		 * @param tls
		 * @throws IOException
		 */
		hTelnetEngine(String host, int port, boolean tls) throws IOException
		{
			m_Socket = new Socket( host, port );
			m_Socket.setSoTimeout(4000);
			m_HostName = host;
			m_Port = port;
			m_TlsEnabled = tls;
			m_Input = m_Socket.getInputStream();
			m_Output = m_Socket.getOutputStream();
			m_Reader = new InputStreamReader(m_Input, Charset.forName("ISO-8859-1"));
		}
		
		@Override
		@SuppressWarnings("NestedAssignment")
		public void run()
		{
			MiSystem.logInfo(Category.DESIGN, Thread.currentThread() + " started");

			while( m_Runflag )
			{
				try
				{
					int dataSize = 0;
					if(-1 < (dataSize = m_Reader.read(m_Data)))
					{
						String inputStr = filterIac(m_Data, dataSize);
						if(null != inputStr)
						{
							m_Queue.sendMsg(inputStr, 60000);
						}
					}
					else
					{
						System.out.println("INFO: " + getClass().getName() + ".run() - Read returns -1, terminate thread");
						break;
					}
				}
				catch(MiTelnetException ex)
				{
					MiSystem.logWarning(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
					break;
				}
				catch(SocketTimeoutException e)
				{
					continue;
				}
				catch(IOException ex)
				{
					MiSystem.logWarning(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
					break;
				}
				catch(InterruptedException e )
				{
					break;
				}
				catch(RuntimeException e )
				{
					MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
					break;
				}
			}
			m_Runflag = false;
			try
			{
				synchronized(this)
				{
					if(null != m_sSocket)
						m_sSocket.close();
					else if(m_Socket != null)
						m_Socket.close();

					m_sSocket = null;
					m_Socket = null;
				}
			}
			catch(IOException e)
			{
			}
		}
		
		public boolean isRunning()
		{
			return m_Runflag && m_Started;
		}
		
		/**
		 * Synchronuous, may be invoked from any thread.
		 */
		public void stop()
		{
			m_Runflag = false;
			m_ThisThread.interrupt();
			
			// Spawn a new thread to execute potentially blocking calls.
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						synchronized(this)
						{
							if(null != m_Reader)
							{
								m_Reader.close();
							}
							if(null != m_Input)
							{
								m_Input.close();
							}
							if(null != m_Output)
							{
								m_Output.close();
							}
						}
					}
					catch(IOException ex)
					{
						MiSystem.logWarning(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
					}
				}
			}).start();
		}
		
		public String getLine(int timeout)
		{
			try
			{
				return (String)m_Queue.receiveMsg(timeout);
			}
			catch(InterruptedException e)
			{
				MiSystem.logInfo(Category.DESIGN, "Thread " + Thread.currentThread().getName() + " interrupted");
			}
			return null;
		}
		
		/**
		 * Not thread-safe
		 * @param option
		 * @return
		 * @throws com.mitel.netutil.MiTelnet.MiTelnetException
		 */
		private boolean handleSb(char option) throws MiTelnetException
		{
			if(option == TelnetOption.TELOPT_START_TLS)
			{
				m_WaitingForTlsFollow = true;
				synchronized(this) {
					++m_OptionCount; /* Take into account of the TLS_FOLLOW option */
				}
				return true;
			}
			else if( m_WaitingForTlsFollow )
			{
				if((char)TlsSubOption.TLS_FOLLOWS != option)
				{
					throw new MiTelnetException("Did not get TLS_FOLLOWS. TLS Negotiation failed");
				}
				setWaitToStartTls(true);
			}
			else
			{
				throw new MiTelnetException("BAD SB. TLS Negotiation failed");
			}
			m_WaitingForTlsFollow = false;
			return false;
		}
		
		/**
		 * Not thread-safe
		 * @param option
		 * @return
		 * @throws java.io.IOException
		 */
		private boolean handleWill(char option) throws IOException
		{
			switch(option)
			{
				case TelnetOption.TELOPT_ECHO:
				{
					byte[] options = new byte[1];
					options[0] = TelnetOption.TELOPT_ECHO;
					sendIAC((byte)TelnetCommand.DO,options);
					break;
				}
				
				case TelnetOption.TELOPT_START_TLS:
				{
					// Just following the standard
					byte[] options = new byte[1];
					options[0] = TelnetOption.TELOPT_START_TLS;
					sendIAC((byte)TelnetCommand.DONT,options);
					break;
				}
				
				default:
				{
					byte[] options = new byte[1];
					options[0] = (byte)option;
					sendIAC((byte)TelnetCommand.DONT,options);
					break;
				}
			}
			return false;
		}
		
		private void sendIAC(byte cmd, byte[] options) throws IOException
		{
			byte[] output;
			if( null != options )
			{
				output = new byte[2+options.length];
				output[0] = (byte)TelnetCommand.IAC;
				output[1] = cmd;
				for(int i = 0;i < options.length; ++i)
				{
					output[2+i] = options[i];
				}
			}
			else
			{
				output = new byte[2];
				output[0] = (byte)TelnetCommand.IAC;
				output[1] = cmd;
			}
			m_Output.write(output);
			m_Output.flush();
		}
		
		/**
		 * Not thread-safe
		 * @param option
		 * @return
		 * @throws java.io.IOException
		 */
		private boolean handleDo(char option) throws IOException
		{
			switch(option)
			{
				case TelnetOption.TELOPT_START_TLS:
				{
					if(m_TlsEnabled)
					{
						// Only ACK if TLS is requested by client
						byte[] options = new byte[1];
						options[0] = TelnetOption.TELOPT_START_TLS;
						sendIAC((byte)TelnetCommand.WILL,options);
						
						options = new byte[2];
						options[0] = TelnetOption.TELOPT_START_TLS;
						options[1] = TlsSubOption.TLS_FOLLOWS;
						sendIAC((byte)TelnetCommand.SB, options);
						
						sendIAC((byte)TelnetCommand.SE, null);
					}
					else
					{
						// TLS is not required, NACK it
						byte[] options = new byte[1];
						options[0] = (byte)option;
						sendIAC((byte)TelnetCommand.WONT,options);
					}
					break;
				}
				
				default:
				{
					// This takes care of all unsupported commands
					byte[] options = new byte[1];
					options[0] = (byte)option;
					sendIAC((byte)TelnetCommand.WONT,options);
					break;
				}
			}
			return false;
		}
		
		private synchronized String filterIac(char[] rawBytes, int numBytes) throws IOException, MiTelnet.MiTelnetException
		{
			m_inputTextBuffer.clear();
			for(int i = 0; (null != rawBytes)&&((i < numBytes)&&(i < rawBytes.length)); ++i)
			{
				if(rawBytes[i] == (char)TelnetCommand.IAC)
				{
					if( m_ProcessingIAC )
					{
						// Escaping IAC
						m_ProcessingIAC = false;
						m_WaitingForCmd = false;
						//retValue.append(TelnetCommand.IAC);
						m_inputTextBuffer.put((char)TelnetCommand.IAC);
						continue;
					}
					else
					{
						m_ProcessingIAC = true;
						m_WaitingForCmd = true;
					}
				}
				else if( m_ProcessingIAC )
				{
					if( m_WaitingForCmd )
					{
						m_WaitingForCmd = false;
						m_PendingCommand = rawBytes[i];
						m_OptionCount = getOptionCount(m_PendingCommand);
						
						switch(m_PendingCommand)
						{
							case (char)TelnetCommand.SE:
							{
								m_IacSubNegotiation = false;
								if( isWaitToStartTls() )
								{
									setWaitToStartTls(false);
									try
									{
										negotiateTls();
										m_Ready = true;
									}
									catch(Exception e)
									{
										throw new MiTelnet.MiTelnetException("Failed to negotiate TLS");
									}
								}
								break;
							}
							
							default:
								// Do nothing
								break;
						}
						if( 0 >= m_OptionCount )
						{
							m_ProcessingIAC = false;
						}
					}
					// Continue to process IAC until no more option is expected
					else if(m_OptionCount > 0)
					{
						--m_OptionCount;
						switch(m_PendingCommand)
						{
							case (char)TelnetCommand.WILL:
							{
								m_IacSubNegotiation = handleWill(rawBytes[i]);
								break;
							}
							case (char)TelnetCommand.DO:
							{
								m_IacSubNegotiation = handleDo(rawBytes[i]);
								break;
							}
							case (char)TelnetCommand.SB:
							{
								m_IacSubNegotiation = handleSb(rawBytes[i]);
								break;
							}
							default:
							{
								m_IacSubNegotiation = false;
								System.out.println("WARNING: " +getClass().getName() + ".filterIac() - Received unsupported command " + (int)m_PendingCommand);
							}
						}
						
						// Finish IAC processing
						if(!m_IacSubNegotiation)
						{
							m_OptionCount = 0;
							m_ProcessingIAC = false;
						}
					}
					else
					{
						m_ProcessingIAC = false;
					}
				}
				else
				{
					m_inputTextBuffer.put(rawBytes[i]);
					if( !m_TlsEnabled )
					{
						// If we are not doing secured telnet then
						// any character received from the server that is
						// not IAC can be considered a sign that telnet
						// service is ready for use
						m_Ready = true;
					}
				}
			}

			m_inputTextBuffer.flip();
			return m_inputTextBuffer.toString();
		}
		
		private static int getOptionCount(char cmd)
		{
			switch( cmd )
			{
				case (char)MiTelnet.TelnetCommand.SE:
					return 0;
					
				default:
					return 1;
			}
		}
		
		private synchronized void negotiateTls() throws IOException
		{
			System.out.println("DEBUG: " + getClass().getName() + ".negotiateTls()\n\tNegotiating TLS");
			
			m_sSocket = (SSLSocket)MiSocketSslFactory.getInstance().getClientSocketFactory().createSocket(m_Socket, m_HostName, m_Port, true);
			if(null != m_sSocket)
			{
				m_sSocket.startHandshake();
				m_Input = m_sSocket.getInputStream();
				m_Output = m_sSocket.getOutputStream();
				m_Reader = new InputStreamReader(m_Input, Charset.forName("ISO-8859-1"));
			}
			
			System.out.println("DEBUG: " + getClass().getName() + ".negotiateTls()\n\tDone negotiating TLS");
		}
		
		/**
		 * Writes a string to the telnet session. This method automatically appends a new
		 * line at the end of the string.
		 * This method only writes if the telnet session is in ready state, otherwise it
		 * returns false right away.
		 * @param command The string to write to the telnet session.
		 * @throws java.io.IOException 
		 * @return True if the string was written, false otherwise.
		 */
		public boolean sendCommand(java.lang.String command) throws IOException
		{
			if(!isRunning())
			{
				throw new IOException("Telnet session " + this + " is closed");
			}
			synchronized(this)
			{
				if(m_Ready)
				{
					m_Output.write(command.getBytes());
					m_Output.write("\r\n".getBytes());
					m_Output.flush();
					return true;
				}
			}
			MiSystem.logWarning(Category.DESIGN, "Telnet session not ready");
			return false;
		}
		
		private hTelnetEngine start()
		{
			m_Started = true;
			m_ThisThread.setPriority(Thread.currentThread().getPriority()+1);
			m_ThisThread.start();
			return this;
		}
		
		@Override
		public String toString()
		{
			StringBuilder retValue = new StringBuilder(256);
			retValue.append('(').append(getClass().getName());
			synchronized(this) {
				retValue.append("(Ready ").append(this.m_Ready).append(')');
			}
			retValue.append("(Host ").append(m_HostName).append(')');
			retValue.append("(Port ").append(m_Port).append(')');
			retValue.append(')');
			return retValue.toString();
		}

		/**
		 * Determines whether this engine is waiting to start
		 * @return True - waiting, false - not
		 */
		private synchronized boolean isWaitToStartTls()
		{
			return m_WaitToStartTls;
		}

		/**
		 * Specifies whether this engine is waiting to start
		 * @param newValue The new wait to start value
		 * @return This object
		 */
		private synchronized Runnable setWaitToStartTls(boolean newValue)
		{
			this.m_WaitToStartTls = newValue;
			return this;
		}
		
	}
	
	private hTelnetEngine m_TelnetEngine;
	
	private static final Pattern m_cmdPromptPattern = Pattern.compile("^\\->\\s*$", Pattern.MULTILINE);
	
	/** Creates a new instance of MiTelnet and start up a reader thread
	 * @param hostName
	 * @param portNumber
	 * @param tls
	 * @throws IOException
	 */
	public MiTelnet(java.lang.String hostName, int portNumber, boolean tls) throws IOException
	{
		m_TelnetEngine = new hTelnetEngine(hostName, portNumber, tls).start();
	}
	
	public static void testTelnetSession(String host, int port, String id, String password) throws MiTelnetException
	{
		try
		{
			MiTelnet myTelnet = new MiTelnet(host, port, true/*TLS*/);
			try
			{
				myTelnet.login( id, password );
				System.out.println( "TELNET: " + myTelnet.executeCommand( "i" ));
			}
			catch(MiTelnet.MiTelnetException e)
			{
				System.out.println( "ERROR: telnet login failed due to " + e.getMessage());
			}
			finally
			{
				myTelnet.close();
			}
		}
		catch(IOException ex)
		{
			throw new MiTelnet.MiTelnetException(ex.toString());
		}
	}
	
	public synchronized void close()
	{
		if( null != m_TelnetEngine )
		{
			m_TelnetEngine.stop();
			m_TelnetEngine = null;
		}
	}
	
	/**
	 * Returns one input line from the telnet session.
	 * @param timeoutMs Timeout to wait for the line.
	 * @return The next input line from the telnet session, or null if none is available.
	 */
	private String readOneLine(int timeoutMs)
	{
		hTelnetEngine telnetEngine;
		synchronized(this)
		{
			telnetEngine = m_TelnetEngine;
		}
		if(null != telnetEngine)
		{
			String line = telnetEngine.getLine(timeoutMs);
			return line;
		}
		return null;
	}
	
	/**
	 * Initiates a telnet session and attempts to log in using the supplied user ID and
	 * password.
	 * @param userId Login user ID
	 * @param password Login password for the userId
	 * @throws com.mitel.netutil.MiTelnet.MiTelnetException 
	 */
	@SuppressWarnings("NestedAssignment")
	public void login(java.lang.String userId, java.lang.String password) throws MiTelnetException
	{
		MiSystem.logInfo(Category.DESIGN, "Started");
		try
		{
			writeOneLine("");
			writeOneLine("");

			String response;
			Pattern plogin = Pattern.compile(".*login:.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			Matcher mlogin;
			while(( null != ( response = readOneLine(4000)))&&(!(mlogin = plogin.matcher(response)).find()))
			{
				System.out.print( response );
				System.out.flush();
			}
			if( null != response )
			{
				System.out.print( response );
				System.out.flush();
			}
			else
			{
				throw new MiTelnet.MiTelnetException(ResourceBundle.getBundle("com/mitel/netutil/resource").getString("noLoginPromptError"));
			}

			writeOneLine( userId );

			Pattern ppswd = Pattern.compile(".*password:.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			Matcher mpswd;
			while(( null != ( response = readOneLine(4000)))&&(!(mpswd = ppswd.matcher(response)).find()))
			{
				System.out.print( response );
				System.out.flush();
			}
			if( null != response )
			{
				System.out.print( response );
				System.out.flush();
			}
			else
			{
				throw new MiTelnet.MiTelnetException(ResourceBundle.getBundle("com/mitel/netutil/resource").getString("noPasswordPromptError"));
			}

			writeOneLine( password );
			writeOneLine( "" );

			while( null != ( response = readOneLine(4000)))
			{
				System.out.print( response );
				System.out.flush();
				Pattern pprompt = Pattern.compile(".*\\->.*", Pattern.DOTALL);
				Matcher mprompt = pprompt.matcher(response);
				if(mprompt.find())
				{
					MiSystem.logInfo(Category.DESIGN, "Completed successfully");
					return;
				}
			}
		}
		catch(IOException ex)
		{
			throw new MiTelnet.MiTelnetException(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/netutil/resource").getString("telnetFailureWarning"), 
				new Object[] {ex}));
		}
		catch(InterruptedException ex)
		{
			throw new MiTelnet.MiTelnetException(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/netutil/resource").getString("telnetFailureWarning"), 
				new Object[] {ex}));
		}
		throw new MiTelnet.MiTelnetException(ResourceBundle.getBundle("com/mitel/netutil/resource").getString("timeoutWaitingForCommandPromptError"));
	}
	
	/**
	 * Writes a single line to the telnet session. A timeout of 4 seconds is used.
	 * @param line The line of text to write.
	 * @throws InterruptedException 
	 * @throws IOException
	 */
	@SuppressWarnings("SleepWhileHoldingLock")
	private void writeOneLine(java.lang.String line) throws IOException, InterruptedException
	{
		hTelnetEngine telnetEngine;
		synchronized(this)
		{
			telnetEngine = m_TelnetEngine;
		}

		// Retries up to 40 times with sleep of 100ms until line is written.
		int safetyBreak = 200;
		while((null != telnetEngine)&&(!telnetEngine.sendCommand(line)))
		{
			if( --safetyBreak < 0 )
			{
				throw new IOException("Safety break triggered");
			}
			Thread.sleep(100);
		}
	}
	
	@SuppressWarnings("NestedAssignment")
	public String executeCommand(java.lang.String cmd)
	{
		StringBuilder retValue = new StringBuilder(256);
		try
		{
			writeOneLine(cmd);
			String oneLine;

			// Some commands have a slow response
			while( null != ( oneLine = readOneLine( 4000 ))) // In some cases, commands such as tt take a long time to generate output
			{
				retValue.append(oneLine);

				// Look for the tell tale sign of the end of a command execution
				Matcher m = m_cmdPromptPattern.matcher(oneLine);
				if(m.find())
				{
					break;
				}
			}
		}
		catch(IOException ex)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
		catch(InterruptedException ex)
		{
		}
		return retValue.toString();
	}

	@Override
	public String toString()
	{
		synchronized(this) {
			if(null != m_TelnetEngine) {
				return "(MiTelnet" + m_TelnetEngine.toString() + ")";
			}
		}
		return "(MiTelnet NULL)";
	}
	
}

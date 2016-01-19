/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mitel.ssh;

import com.mitel.miutil.MiBackgroundTask;
import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg;
import com.mitel.miutil.MiSystem;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.connection.Channel;
import com.sshtools.j2ssh.connection.ChannelEventListener;
import com.sshtools.j2ssh.session.SessionChannelClient;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages a single ssh shell session.
 * @author haiv
 */
public class MiSshShell
{
	private final SessionChannelClient m_Channel;
	private final OutputStreamWriter m_StdOut;
	private final InputStreamReader m_StdIn;
	private static final String gNewLine = "\n";
	/**
	 * Pattern for detecting PWD environment variable.
	 */
	private static final Pattern s_PwdPattern = Pattern.compile("^PWD=(.*)");

	/**
	 * How long to wait on a read before we assume no more data is forthcoming.
	 */
	private long m_ReadTimeoutMs = 1000;

	/**
	 * The encoding of the SSH session.
	 */
	private final String m_Encoding;
	/**
	 * List of new (unread) input lines.
	 */
	private final LinkedList<String> m_InputLines = new LinkedList<>();
	/**
	 * Input reader task
	 */
	private MiBackgroundTask m_StdInReader;
	
	/**
	 * Local listener for channel event.
	 */
	private class MyChannelListener implements ChannelEventListener
	{
		@Override
		public void onChannelOpen(Channel arg0)
		{
		}

		@Override
		public void onChannelEOF(Channel arg0)
		{
		}

		@Override
		public void onChannelClose(Channel arg0)
		{
		}

		@Override
		public void onDataReceived(Channel arg0, byte[] arg1)
		{
			synchronized(MiSshShell.this)
			{
				MiSshShell.this.notifyAll();
			}
		}

		@Override
		public void onDataSent(Channel arg0, byte[] arg1)
		{
		}
	}
	
	/**
	 * Background task for reading input text from the SSH shell.
	 */
	private class BgTaskInputReader extends MiBackgroundTask
	{
		private final char[] inputBuffer = new char[1024];
		private final Reader inputReader;

		/**
		 * Creates a new background task for reading input.
		 * @param name Name of the task
		 * @param reader The reader to be used for reading data
		 */
		private BgTaskInputReader(String name, Reader reader)
		{
			super(name);
			inputReader = reader;
		}

		@Override
		public void run()
		{
			try
			{
				StringBuilder stringBuf = new StringBuilder(4098);
				while(getRunFlag())
				{
					synchronized(MiSshShell.this)
					{
						if(inputReader.ready())
						{
							int bytesRead = inputReader.read(this.inputBuffer);
							if(bytesRead < 1)
							{
								break;
							}

							stringBuf.append(this.inputBuffer, 0, bytesRead);
							continue;
						}
						else if(stringBuf.length() < 1)
						{
							// Since we don't have anything to be processed,
							// wait for a specified amount of time for more data
							long ts = System.currentTimeMillis();
							long timeout = m_ReadTimeoutMs;
							long remain = timeout;
							while(remain > 0)
							{
								MiSshShell.this.wait(remain);
								if(inputReader.ready())
								{
									// There is more data, stop waiting and continue reading.
									break;
								}
								remain = timeout - (System.currentTimeMillis() - ts);
							}
							continue;
						}
						else
						{
							// Since we don't have anything to be processed,
							// wait for a specified amount of time for more data
							long ts = System.currentTimeMillis();
							long timeout = m_ReadTimeoutMs;
							long remain = timeout;
							while(remain > 0)
							{
								MiSshShell.this.wait(remain);
								if(inputReader.ready())
								{
									// There is more data, stop waiting and continue reading.
									break;
								}
								remain = timeout - (System.currentTimeMillis() - ts);
							}
						}
					}

					// Now extract all lines from the data read in previously.
					int bufSize = stringBuf.length();
					if(bufSize < 1)
					{
						// Didn't read in anything.
						continue;
					}
					
					// Now parse all lines and stuff them in m_InputLines.
					String leftOver = null;
					synchronized(m_InputLines) {
						int cur = 0;
						while(cur < bufSize) {
							int nl = stringBuf.indexOf("\n", cur);
							if(nl > 0) {
								m_InputLines.add(stringBuf.substring(cur, nl));
								cur = nl + 1;
							} else if(nl < 0) {
								leftOver = stringBuf.substring(cur, bufSize);
								break;
							} else {
								++cur;
							}
						}
						m_InputLines.notifyAll();
					}
					stringBuf = leftOver != null ? new StringBuilder(4098).append(leftOver) : new StringBuilder(4098);
				}

				// Grab any left-over
				if(stringBuf.length() > 0)
				{
					synchronized(m_InputLines)
					{
						m_InputLines.add(stringBuf.toString());
						m_InputLines.notifyAll();
					}
				}
			}
			catch(InterruptedException | IOException ex) {
			}
		}
	}
	
	/**
	 * Creates a new shell channel from an SSH client session.
	 * @param sshClient The SSH client from which the new shell channel is to be created.
	 * @param encoding The SSH shell's encoding, e.g. "UTF-8", ...
	 * @throws java.io.IOException
	 */
	public MiSshShell(SshClient sshClient, String encoding) throws IOException
	{
		// Create a new session channel from which the shell is started.
		m_Channel = sshClient.openSessionChannel(new MyChannelListener());

		// Found this out on the web since there is no documentation with this what so ever.
		m_Channel.requestPseudoTerminal("vt100", 80, 24, 0, 0, "");

		m_Channel.startShell();
		
		// Grab all input/output streams.
		m_Encoding = encoding;
		m_StdOut = new OutputStreamWriter(m_Channel.getOutputStream(), m_Encoding);
		m_StdIn = new InputStreamReader(m_Channel.getInputStream(), m_Encoding);
	}

	/**
	 * Executes a single command on the shell. This is equivalent to typing in the command, followed by ENTER.
	 * @param cmd The command to be executed.
	 * @throws java.io.IOException
	 */
	public void execute(String cmd) throws IOException
	{
		if(cmd != null)
		{
			m_StdOut.write(cmd);
		}
		m_StdOut.write(gNewLine);
		
		// This is needed otherwise the output may be buffered for sometimes
		m_StdOut.flush();
	}

	/**
	 * Attempts to discover the command-line prompt string, for the current directory.
	 * @param authInfo The authentication info record with which to manage the prompts.
	 * @return The command-line prompt, null if not able to determine the command-line prompt.
	 * @throws java.io.IOException
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("NestedAssignment")
	public String discoverCmdLinePrompt(MiSshAuthInfo authInfo) throws IOException, InterruptedException
	{
		String line;
		String lastLine = null;
		long readTimeout;
		String pwd = null;
		String prompt = null;
		synchronized(this)
		{
			/*This needs to be a bit longer than the read timeout*/
			readTimeout = m_ReadTimeoutMs + 500;
		}
		
		execute("env");
		
		while((line = readLine(readTimeout)) != null)
		{
			Matcher m = s_PwdPattern.matcher(line);
			if(m.find())
			{
				pwd = m.group(1);
				prompt = authInfo.getCmdlinePrompt(pwd);
				break;
			}
		}
		if(prompt != null)
		{
			readTimeout = 100;
		}
		while((line = readLine(readTimeout)) != null)
		{
			lastLine = line;
		}
		if(prompt == null)
		{
			prompt = lastLine;
			authInfo.setCmdlinePrompt(pwd, prompt);
		}
		return prompt;
	}

	/**
	 * Type some sequence of characters into the shell, <b>not</b> followed by ENTER.
	 * @param seq The sequence of characters to be typed in.
	 * @throws java.io.IOException
	 */
	public void type(String seq) throws IOException
	{
		if(seq != null)
		{
			m_StdOut.write(seq);
		}
		
		// This is needed otherwise the output may be buffered for sometimes
		m_StdOut.flush();
	}

	@SuppressWarnings("NestedAssignment")
	public static void main(String[] args)
	{
		MiSshClient client = new MiSshClient();
		PasswordAuthenticationClient auth = new PasswordAuthenticationClient();
		auth.setUsername("root");
		auth.setPassword("root");
		try {
			// Newer PPC Linux systems will need to allow Diffie-Hellman group 1 SHA-1 key exchange algorithm.
			client.connectAndAuthenticate(auth, "10.38.72.19");
			try {
				MiSshShell sh = client.openShell("UTF-8");
				try {
					sh.start(1000);
					sh.type("ps axu\n");
					String line;
					while((line = sh.readLine(2000)) != null) {
						System.out.println(line);
					}
				}
				catch(InterruptedException ex) {
				} finally {
					sh.dispose();
				}
			} finally {
				client.disconnect();
			}
		}
		catch(IOException ex) {
			MiSystem.logWarning(MiLogMsg.Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
	}

	/**
	 * Retrieves a single line from the shell.
	 * The shell must be started prior to invoking this.
	 * @param waitMs Period of time in milliseconds to wait for a line to be available.
	 * Zero means no wait.
	 * @return A line or null if no new line is available.
	 * @throws InterruptedException
	 */
	public String readLine(long waitMs) throws InterruptedException
	{
		synchronized(m_InputLines)
		{
			if((m_InputLines.isEmpty())&&(waitMs > 0))
			{
				long ts = System.currentTimeMillis();
				long remain = waitMs;
				while((remain > 0)&&(m_InputLines.isEmpty()))
				{
					m_InputLines.wait(remain);
					remain = waitMs - (System.currentTimeMillis() - ts);
				}
			}

			if(m_InputLines.isEmpty())
				return null;

			return m_InputLines.removeFirst();
		}
	}
	
	/**
	 * Starts the reader thread that retrieves lines from the shell.
	 * Start may only be invoked once, subsequent invocations are ignored.
	 * Once the reader thread is stopped either via the disposed method or due to some other interruption,
	 * the shell cannot be restarted and should be discarded.
	 * @param readTimeoutMs How long to wait on the read-side before determining no more data is forthcoming.
	 * @return This object.
	 */
	public MiSshShell start(long readTimeoutMs)
	{
		synchronized(this)
		{
			if(m_StdInReader == null)
			{
				m_StdInReader = new BgTaskInputReader("StdInReader", m_StdIn).start();
			}
			m_ReadTimeoutMs = readTimeoutMs;
		}
		
		return this;
	}
	
	/**
	 * Closes down the shell and its associated session channel. Releases all resources.
	 * This shell session should now be discarded (it cannot be restarted).
	 */
	public void dispose()
	{
		synchronized(this)
		{
			if(m_StdInReader != null)
			{
				m_StdInReader.stop();
				m_StdInReader = null;
			}
		}

		try
		{
			m_StdOut.close();
		}
		catch(IOException e)
		{
		}

		try
		{
			m_StdIn.close();
		}
		catch(IOException e)
		{
		}

		try
		{
			m_Channel.close();
		}
		catch(IOException e)
		{
		}
	}
}

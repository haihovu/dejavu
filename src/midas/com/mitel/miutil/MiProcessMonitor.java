/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mitel.miutil;

import java.io.*;

/**
 * Task for monitoring the status of a child process
 * @author haiv
 */
public class MiProcessMonitor extends MiBackgroundTask
{
	/**
	 * Monitor task for an input stream to this program, i.e. output stream from the child process
	 * @author haiv
	 */
	private static class TaskInputStreamMonitor implements Runnable
	{
		/**
		 * The input stream from which to monitor output from the child process
		 */
		private final InputStream istream;
		/**
		 * Name of the owner of the input stream, i.e. the name of the child process.
		 * Used for diagnostic purposes
		 */
		private final String owner;
		/**
		 * Worker thread used to run the monitoring logic
		 */
		private final Thread worker;
		/**
		 * Flag indicating the run status of this monitor task
		 */
		private boolean running;
		/**
		 * Save the output to a string
		 */
		private final StringBuilder mOutput = new StringBuilder(10240);
		/**
		 * Creates a new monitor task
		 * @param streamOwner The name of the owner process of the input stream 
		 * @param stream The input stream to be monitored
		 */
		private TaskInputStreamMonitor(String streamOwner, InputStream stream)
		{
			owner = streamOwner;
			istream = stream;
			worker = new Thread(this);
		}
		
		/**
		 * Starts the monitor task
		 * @return This object
		 */
		private MiProcessMonitor.TaskInputStreamMonitor start()
		{
			synchronized(this)
			{
				running = true;
			}
			worker.start();
			return this;
		}

		/**
		 * Stops the monitor task
		 */
		private void stop()
		{
			synchronized(this)
			{
				running = false;
			}
			try
			{
				istream.close();
			}
			catch(IOException ex)
			{
			}
			worker.interrupt();
		}
		
		/**
		 * Determines whether the task is running
		 * @return True if the task is running, false otherwise.
		 */
		private boolean isRunning()
		{
			synchronized(this)
			{
				return running;
			}
		}
		
		/**
		 * Return the captured output from executing a command.
		 * 
		 * @return a string representing the output of a command.
		 */
		public String getOutput()
		{
			return mOutput.toString();
		}
		
		/**
		 * Waits for the stream monitor to complete
		 * @param timeoutMs Optional timeout to wait, zero or less means no wait
		 * @return True if the stream monitor has completed, false otherwise.
		 * @throws InterruptedException
		 */
		@SuppressWarnings("WaitWhileNotSynced")
		public boolean waitForCompletion(long timeoutMs) throws InterruptedException
		{
			if(timeoutMs > 0)
			{
				long ts = System.currentTimeMillis();
				long remain = timeoutMs;
				synchronized(this)
				{
					while(isRunning() && remain > 0)
					{
						this.wait(remain);
						remain = timeoutMs - (System.currentTimeMillis() - ts);
					}
				}
			}
			return !isRunning();
		}
		
		@Override
		@SuppressWarnings({"NestedAssignment", "SleepWhileInLoop"})
		public void run()
		{
			byte[] buf = new byte[2048];
			try
			{
				int bytesBuffered = 0;
				StringBuilder bufferedString = null;
				boolean eof = false;
				while(isRunning() && (!eof))
				{
					int avail = istream.available();
					// Read when: there are data to be read OR when there is nothing to parse
					if((avail > 0)||(bytesBuffered == 0)) {
						int bytes = istream.read(buf);
						if(bytes > 0)
						{
							bytesBuffered += bytes;
							if(bufferedString == null) {
								bufferedString = new StringBuilder(4096);
							}
							String newStr = new String(buf, 0, bytes);
							bufferedString.append(newStr);
							mOutput.append(newStr);
							continue;
						}
						else
						{
							System.out.println("DEBUG - " + owner + " EOF");
							eof = true;
						}
					}
					if((bytesBuffered > 0)&&(bufferedString != null)) {
						// Read in the data as text, one line at a time
						BufferedReader r = new BufferedReader(new StringReader(bufferedString.toString()));
						String line;
						while((line = r.readLine()) != null)
						{
							System.out.println(owner + " - " + line);
						}
						System.out.flush();
						bytesBuffered = 0;
						bufferedString = null;
					}
				}
			}
			catch (IOException e)
			{
			}
			catch (RuntimeException e)
			{
			}
			finally
			{
				try
				{
					istream.close();
				}
				catch(IOException e)
				{
				}
				
				synchronized(this)
				{
					running = false;
					this.notifyAll();
				}
			}
		}
	}
	
	private final Process child;
	private final MiProcessMonitor.TaskInputStreamMonitor stdoutMon;
	private final MiProcessMonitor.TaskInputStreamMonitor stderrMon;
	
	private volatile int exitVal = -1;
	
	/**
	 * Creates a new task for monitoring a process
	 * @param childProc The process to be monitored
	 * @param name Name of the process (for diagnostic purposes)
	 * @param monitorStderr Flag indicating whether to monitor the stderr of the child process
	 */
	public MiProcessMonitor(String name, Process childProc, boolean monitorStderr)
	{
		super("Monitor-" + name);
		child = childProc;
		if(monitorStderr)
		{
			stderrMon = new TaskInputStreamMonitor(name + ".stderr", childProc.getErrorStream());
		}
		else
		{
			stderrMon = null;
		}
		stdoutMon = new TaskInputStreamMonitor(name + ".stdout", childProc.getInputStream());
	}

	@Override
	public MiBackgroundTask start()
	{
		super.start();
		if(stderrMon != null)
		{
			stderrMon.start();
		}
		stdoutMon.start();
		return this;
	}

	/**
	 * Waits for the monitored process to terminate 
	 * @param timeoutMs Optional timeout to wait for the process to complete, zero or less means no wait.
	 * @return The exit value of the monitored process,
	 * -1 if the process is still running.
	 * @throws InterruptedException 
	 */
	public int waitForCompletion(long timeoutMs) throws InterruptedException
	{
		if(MiSystem.diagnosticEnabled())
		{
			MiSystem.logInfo(MiLogMsg.Category.DESIGN, Thread.currentThread() + " wait for completion " + timeoutMs + "ms");
		}
		long ts = System.currentTimeMillis();
		long remainMs = timeoutMs;
		synchronized(this)
		{
			while(getRunFlag() && (remainMs > 0))
			{
				this.wait(5000);
				remainMs = timeoutMs - (System.currentTimeMillis() - ts);
			}
		}
		if(MiSystem.diagnosticEnabled())
		{
			MiSystem.logInfo(MiLogMsg.Category.DESIGN, Thread.currentThread() + " done wait for completion " + timeoutMs + "ms");
		}
		return exitVal;
	}

	/**
	 * Retrieves the output stream of this program that represents the stdin of the monitored process
	 * @return The output stream representing the stdin of the monitored process
	 */
	public OutputStream getStdIn()
	{
		return child.getOutputStream();
	}

	/**
	 * Retrieves the input stream of this program that represents the stdout of the monitored process
	 * @return The input stream representing the stdout of the monitored process
	 */
	public InputStream getStdOut()
	{
		return child.getInputStream();
	}

	/**
	 * Retrieves the input stream of this program that represents the stderr of the monitored process
	 * @return The input stream representing the stderr of the monitored process
	 */
	public InputStream getStdErr()
	{
		return child.getErrorStream();
	}
	
	/**
	 * Return the error captured from running the command. 
	 * 
	 * @return a string representing the captured error output from running
	 * the command. 
	 */
	public String getStdErrStr()
	{
		return stderrMon.getOutput();
	}
	
	/**
	 * Return the output captured from running the command. 
	 * 
	 * @return a string representing the captured output from running
	 * the command.
	 */
	public String getStdOutStr()
	{
		return stdoutMon.getOutput();
	}
	
	@Override
	public void run()
	{
		try
		{
			/*
			 * This task basically runs until the monitored process exits
			 */
			exitVal = child.waitFor();
			if(exitVal != 0)
			{
				System.out.println(Thread.currentThread().getName() + " terminated Abnormally");
			}
			else
			{
				System.out.println(Thread.currentThread().getName() + " terminated Normally");
			}
			if(MiSystem.diagnosticEnabled())
			{
				MiSystem.logInfo(MiLogMsg.Category.DESIGN, Thread.currentThread().getName() + " waiting for stderr/stdout monitors to terminate");
			}
			if(stderrMon != null)
			{
				stderrMon.waitForCompletion(500);
			}
			stdoutMon.waitForCompletion(500);
			if(MiSystem.diagnosticEnabled())
			{
				MiSystem.logInfo(MiLogMsg.Category.DESIGN, Thread.currentThread().getName() + " DONE waiting for stderr/stdout monitors to terminate");
			}
		}
		catch (InterruptedException e)
		{
			System.out.println(Thread.currentThread().getName() + " stopped");
		}
		finally
		{
			if(stderrMon != null)
			{
				stderrMon.stop();
			}
			stdoutMon.stop();
			synchronized(this)
			{
				setRunFlag(false);
				this.notifyAll();
				if(MiSystem.diagnosticEnabled())
				{
					MiSystem.logInfo(MiLogMsg.Category.DESIGN, Thread.currentThread() + " terminates, notifies all");
				}
			}
		}
	}
}
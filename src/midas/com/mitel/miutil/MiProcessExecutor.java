/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mitel.miutil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A utility for executing a shell command using a child process  
 * @author haiv
 */
public class MiProcessExecutor
{
	/**
	 * Name of the process, for diagnostic purposes
	 */
	private final String name;
	
	/**
	 * The command and arguments to execute on the shell
	 */
	private final String[] cmds;
	
	/**
	 * Monitor for the child process
	 */
	private MiProcessMonitor procMon;
	
	/**
	 * The exit code of the executed process
	 */
	private int exitCode = -1;

	/**
	 * Creates a new process to execute a specific command
	 * @param name The name of the command, for diagnostic purposes
	 * @param cmds The command plus zero or more arguments
	 */
	public MiProcessExecutor(String name, String... cmds)
	{
		this.name = name;
		this.cmds = cmds;
	}

	/**
	 * Retrieves the input stream of this program that represents the stdout of the child process
	 * @return The input stream representing the stdout of the child process, or null if the child process is not running
	 */
	public InputStream getStdOut()
	{
		synchronized(this)
		{
			if(procMon != null)
			{
				return procMon.getStdOut();
			}
		}
		return null;
	}

	/**
	 * Retrieve the output string captured when running the command. This method
	 * should be called after the command has been executed.
	 * 
	 * @return the captured output from running the command.
	 */
	public String getStdOutStr()
	{
		return procMon.getStdOutStr();
	}
	
	/**
	 * Retrieves the input stream of this program that represents the stderr of the child process
	 * @return The input stream representing the stderr of the child process, or null if the child process is not running
	 */
	public InputStream getStdErr()
	{
		synchronized(this)
		{
			if(procMon != null)
			{
				return procMon.getStdErr();
			}
		}
		return null;
	}
	
	/**
	 * Retrieve the error output string captured when running the command. This method
	 * should be called after the command has been executed.
	 * 
	 * @return the captured error output from running the command.
	 */
	public String getStdErrStr()
	{
		return procMon.getStdErrStr();
	}

	/**
	 * Retrieves the output stream of this program that represents the stdin of the child process
	 * @return The output stream representing the stdin of the child process, or null if the child process is not running
	 */
	public OutputStream getStdIn()
	{
		synchronized(this)
		{
			if(procMon != null)
			{
				return procMon.getStdIn();
			}
		}
		return null;
	}
	
	/**
	 * Launches the child process with the given command
	 * @param monitorStderr Flag indicating whether to monitor the stderr of the child process
	 * @return True if the process had been launched, false otherwise.
	 * @throws IOException 
	 */
	public boolean launchProcess(boolean monitorStderr) throws IOException
	{
		synchronized(this)
		{
			if(procMon == null)
			{
        		StringBuilder cmdLine = new StringBuilder(1024);
        		for(String cmd : cmds)
        		{
        			cmdLine.append(cmd).append(' ');
        		}
		
        		procMon = initiateProcess(name, cmdLine.toString(), monitorStderr);
        		if(procMon != null)
        		{
        			return true;
        		}
			}
			else
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getExitCode()
	{
		return exitCode;
	}
	
	/**
	 * Retrieves the child process monitor
	 * @return The child process monitor, or null if the child process is not running
	 */
	private MiProcessMonitor getProcMon()
	{
		synchronized(this)
		{
			return procMon;
		}
	}
	
	/**
	 * Waits for the child process to terminate
	 * @param timeoutMs Number of milliseconds to wait
	 * @return True if the child process had terminated, false otherwise
	 * @throws InterruptedException
	 */
	public boolean waitForCompletion(long timeoutMs) throws InterruptedException
	{
		MiProcessMonitor mon = getProcMon();
		if (mon != null)
		{
			exitCode = mon.waitForCompletion(timeoutMs);
			if (exitCode == -1)
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Creates the child process, starts and monitors it.
	 * @param name Name of the process, for diagnostic purposes
	 * @param cmdline The command line to be executed by the child process
	 * @param monitorStderr Flag indicating whether to monitor the stderr of the child process
	 * @return The process monitor, not null.
	 * @throws IOException
	 */
	private MiProcessMonitor initiateProcess(String name, String cmdline, boolean monitorStderr) throws IOException
	{
		System.out.println("Calling: " + name);

		Runtime rt = Runtime.getRuntime();
		Map<String, String> envMap = System.getenv();
		String[] envs = new String[envMap.size()];
		int i = 0;
		for(Entry<String, String> en : envMap.entrySet())
		{
			String key = en.getKey();
			StringBuilder value = new StringBuilder(1024).append(en.getValue());
			if(key.equals("PATH"))
			{
				value.append(File.pathSeparator).append("/sbin").append(File.pathSeparator).append("/bin").append(File.pathSeparator).append("/usr/bin");
			}
			envs[i++] = key + '=' + value;
		}
		Process proc = rt.exec(cmdline, envs);
		MiBackgroundTask mon = new MiProcessMonitor(name, proc, monitorStderr).start();
		return (MiProcessMonitor)mon;
	}
}
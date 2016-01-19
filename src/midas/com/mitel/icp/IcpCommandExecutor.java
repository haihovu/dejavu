package com.mitel.icp;

import com.mitel.guiutil.MiProgressDialog;
import com.mitel.netutil.MiTelnet;
import com.mitel.netutil.TelnetExecutor.TelnetException;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;




/**
 * Based on TelnetExecutor but with GUI components and ICP specific stuff
 * @author Hai Vu
 */
public class IcpCommandExecutor implements com.mitel.guiutil.MiProgressDialog.MiProgressListener, ActionListener
{
	
	private final Frame m_mainFrame;
	private final Map<IcpProcessor.ProcessorName, MiTelnet> m_telnetSessions = new HashMap<IcpProcessor.ProcessorName, MiTelnet>(4);
	private final IcpDescriptor m_icpDesc;
	private MiProgressDialog m_progressDialog;
	
	private Thread m_executionThread;
	
	private volatile boolean m_runFlag;
	
	/**
	 * Creates new form iNquisitor
	 * @param mainFrame
	 * @param icp
	 */
	public IcpCommandExecutor(Frame mainFrame, IcpDescriptor icp)
	{
		m_icpDesc = icp;
		m_mainFrame = mainFrame;
	}
	
	/**
	 * For internal use with the MiProgressDialog dialog
	 */	
	public void onCancel(javax.swing.JDialog progressDialog)
	{
		progressDialog.dispose();
		m_runFlag = false;
		synchronized(m_telnetSessions) {
			if(null != m_executionThread)
				m_executionThread.interrupt();
		}
	}
	
	private String executeOneCommand(SysCmd cmd, ActionListener listener) throws TelnetException
	{
		MiTelnet telnetSession = locateTelnetSession(cmd.m_selectedProcessor);
		if(null == telnetSession)
		{
			IcpProcessor proc = m_icpDesc.locateProcessor(cmd.m_selectedProcessor);
			if(null != proc)
			{
				IcpProcessor.ProcessorName name;
				synchronized(proc)
				{
					name = proc.getName();
				}
				try
				{
					telnetSession = openTelnet(name, m_icpDesc, 60000);
					addTelnetSession(name, telnetSession);
				}
				catch(InterruptedException ex)
				{
					throw new TelnetException("Telnet session to " + cmd.m_selectedProcessor + " was interrupted");
				}
			}
			else
			{
				throw new TelnetException("Can't find processor " + cmd.m_selectedProcessor);
			}
		}

		if(null != telnetSession)
		{
			try
			{
				return new com.mitel.netutil.TelnetExecutor(telnetSession).executeCommand(cmd2cmdargs(cmd), listener);
			}
			catch(TelnetException e)
			{
				removeTelnetSession(telnetSession);
				throw e;
			}
		}
		else
		{
			throw new TelnetException("Failed to open telnet session");
		}
	}
	
	/**
	 * Execute a single command on a telnet session, returning the result.
	 * @param cmd
	 * @return 
	 */
	public String executeCommand(SysCmd cmd)
	{
		StringBuilder output = new StringBuilder(256);
		try
		{
			synchronized(m_telnetSessions) {
				m_executionThread = Thread.currentThread();

				m_progressDialog = MiProgressDialog.safeInstantiate(m_mainFrame, ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("cmdExecutionLabel"), 0, this);
				if(m_progressDialog != null) {
					m_progressDialog.start();
					m_progressDialog.setOptionalText(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("executingLabel"),new Object[]{cmd2cmdargs(cmd)}));
				}
			}
			output.append(executeOneCommand(cmd, this));
			synchronized(m_telnetSessions) {
				if(m_progressDialog != null) {
					m_progressDialog.dismiss();
					m_progressDialog = null;
				}
			}
		}
		catch(TelnetException e)
		{
			synchronized(m_telnetSessions) {
				if(null != m_progressDialog)
				{
					m_progressDialog.dismiss();
					m_progressDialog = null;
				}
			}
		}
		return output.toString();
	}
	
	/**
	 * Execute a series of commands on a telnet session, returning the result.
	 * @param cmds
	 * @return 
	 */
	public String executeCommands(List cmds)
	{
		StringBuilder retValue = new StringBuilder(256);
		Iterator iter = cmds.iterator();
		synchronized(m_telnetSessions) {
			m_executionThread = Thread.currentThread();

			m_progressDialog = MiProgressDialog.safeInstantiate(m_mainFrame, ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("cmdExecutionLabel"), 0, this);
			if(m_progressDialog != null) {
				m_progressDialog.start();
			}
		}
		try
		{
			int count = 0;
			m_runFlag = true;
			while(m_runFlag &&(iter.hasNext()))
			{
				SysCmd cmd = (SysCmd)iter.next();
				synchronized(m_telnetSessions) {
					if(m_progressDialog != null) {
						m_progressDialog.setProgress(((++count)*100)/cmds.size());
						m_progressDialog.setOptionalText(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("executingLabel"),new Object[] {cmd2cmdargs(cmd)}));
					}
				}
				if(count > 1)
					retValue.append("\n");

				String output = executeOneCommand(cmd, null);
				if(null != output)
					retValue.append(output);
			}
			synchronized(m_telnetSessions) {
				if(m_progressDialog != null) {
					m_progressDialog.dispose();
					m_progressDialog = null;
				}
			}
		}
		catch(TelnetException e)
		{
			synchronized(m_telnetSessions) {
				if(null != m_progressDialog) {
					m_progressDialog.dispose();
				}

				m_progressDialog = null;
			}
		}
		return retValue.toString();
	}
	
	/**
	 * For internal use with the TelnetExecutor to report progress.
	 * @param actionEvent 
	 */
	public void actionPerformed(java.awt.event.ActionEvent actionEvent)
	{
		synchronized(m_telnetSessions) {
			if(null != m_progressDialog)
			{
				m_progressDialog.setProgress(actionEvent.getID());
			}
		}
	}
	
	/**
	 * Opens a new telnet session.
	 * @param processorName
	 * @param icp
	 * @param timeoutMs
	 * @return
	 * @throws java.lang.InterruptedException
	 */
	private MiTelnet openTelnet(IcpProcessor.ProcessorName processorName, IcpDescriptor icp, int timeoutMs) throws InterruptedException
	{
		IcpTelnetClient telnet = new IcpTelnetClient(m_mainFrame, icp, processorName);
		Thread workerThread = new Thread(telnet);
		workerThread.start();
		workerThread.join(timeoutMs);
		if(null == telnet.getSession())
		{
			telnet.abort();
			throw new InterruptedException("Telnet Failed");
		}
		return telnet.getSession();
	}
	
	private static String cmd2cmdargs(SysCmd cmd)
	{
		StringBuilder cmdArgs = new StringBuilder(512).append(cmd.m_cmdName);

		if((cmd.m_args != null)&&(cmd.m_args.size() > 0))
		{
			Iterator iter = cmd.m_args.iterator();
			int argcount = 0;
			while(iter.hasNext())
			{
				if(0 == argcount++)
				{
					cmdArgs.append(' ').append(iter.next());
				}
				else
				{
					cmdArgs.append(',').append(iter.next());
				}
			}
		}
		return cmdArgs.toString();
	}
	
	/**
	 * Method close must be invoked when done with the executor. Otherwise telnet
	 * sessions will be left opened.
	 */	
	public void close()
	{
		List<MiTelnet> sessions;
		synchronized(m_telnetSessions)
		{
			sessions = new ArrayList<MiTelnet>(m_telnetSessions.values());
			m_telnetSessions.clear();
		}
		for(MiTelnet telnetSession : sessions)
		{
			telnetSession.close();
		}
	}
	
	private MiTelnet locateTelnetSession(IcpProcessor.ProcessorName processorName)
	{
		synchronized(m_telnetSessions)
		{
			return m_telnetSessions.get(processorName);
		}
	}
	
	private void removeTelnetSession(MiTelnet session)
	{
		session.close();
		synchronized(m_telnetSessions)
		{
			m_telnetSessions.values().remove(session);
		}
	}
	
	private void addTelnetSession(IcpProcessor.ProcessorName processorName, MiTelnet session)
	{
		synchronized(m_telnetSessions)
		{
			if(m_telnetSessions.containsKey(processorName))
			{
				m_telnetSessions.remove(processorName);
			}

			m_telnetSessions.put(processorName, session);
		}
	}
	
}


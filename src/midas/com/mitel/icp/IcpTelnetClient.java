package com.mitel.icp;

import com.mitel.guiutil.MiGuiUtil;
import com.mitel.guiutil.MiProgressDialog;
import com.mitel.netutil.MiTelnet;
import java.awt.Frame;
import java.util.ResourceBundle;
import javax.swing.JDialog;
import javax.swing.JOptionPane;




public class IcpTelnetClient implements Runnable, com.mitel.guiutil.MiProgressDialog.MiProgressListener
{
	private MiTelnet m_telnetSession = null;
	private final Frame m_parentFrame;
	private final IcpDescriptor m_icpDescriptor;
	private final IcpProcessor.ProcessorName m_processorName;
	private JDialog m_loginDialog;
	private IcpTelnetClient m_thisObject = this;
	
	private Thread m_workerThread;
	
	public IcpTelnetClient(Frame parentFrame, IcpDescriptor icp, IcpProcessor.ProcessorName processorName)
	{
		m_parentFrame = parentFrame;
		m_icpDescriptor = icp;
		m_processorName = processorName;
	}
	
	@Override
	public void run()
	{
		synchronized(this) {
			m_workerThread = Thread.currentThread();
		}
		MiProgressDialog progressDialog = null;
		try
		{
			if(null != m_icpDescriptor)
			{
				while(true)
				{
					// Prompt for user input
					if(!m_icpDescriptor.getValidated(m_processorName))
					{
						if(!(new IcpUserInfoValidator(m_parentFrame, m_icpDescriptor).challengeUserInfo(m_processorName)))
						{
							showMsgDialog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed2Label"), JOptionPane.ERROR_MESSAGE);
							break;
						}
					}

					progressDialog = MiProgressDialog.safeInstantiate(m_parentFrame, "Telnet", 0, m_thisObject);
					if(progressDialog != null) {
						progressDialog.start();
					}
					try
					{
						// Now try to open a telnet session and login
						if((m_icpDescriptor.getUserId(m_processorName) != null)&&(m_icpDescriptor.getPassword(m_processorName) != null))
						{
							if(progressDialog != null) {
								progressDialog.setOptionalText(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("openTelnetLabel"));
							}
							if((null != m_icpDescriptor.getAddress(m_processorName))&&(m_icpDescriptor.getAddress(m_processorName).length()>0))
							{
								MiTelnet session = new MiTelnet(m_icpDescriptor.getAddress(m_processorName), 2002, true/*TLS*/);
								synchronized(this)
								{
									m_telnetSession = session;
								}
								if(progressDialog != null) {
									progressDialog.setProgress(40);
									progressDialog.setOptionalText(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("logingOnLabel"));
								}
								try
								{
									session.login(m_icpDescriptor.getUserId(m_processorName), m_icpDescriptor.getPassword(m_processorName));
									if(progressDialog != null) {
										progressDialog.setProgress(100);
										progressDialog.setOptionalText(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("doneLabel"));
									}
									m_icpDescriptor.setValidated(m_processorName, true);

									// SUCCESS Path
									break;
								}
								catch(MiTelnet.MiTelnetException e)
								{
									resetTelnetSession();
									if(progressDialog != null) {
										progressDialog.setProgress(100);
										progressDialog.setOptionalText(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed2Label"));
									}
									showMsgDialog(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed1Label"), new Object[] {e.getMessage()}), JOptionPane.ERROR_MESSAGE);
									m_icpDescriptor.setValidated(m_processorName, false);
								}
							}
							else
							{
								throw new java.net.UnknownHostException("NULL/Empty host name for processor " + m_processorName + " of " + m_icpDescriptor);
							}
						}
						else
						{
							m_icpDescriptor.setValidated(m_processorName, false);
						}
					}
					catch(java.net.UnknownHostException e)
					{
						showMsgDialog(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unknownHostWarning"), new Object[] {m_icpDescriptor.getAddress(m_processorName), m_processorName}), JOptionPane.ERROR_MESSAGE);
						resetTelnetSession();
					}
					catch(java.io.IOException e)
					{
						showMsgDialog(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/netutil/resource").getString("telnetFailureWarning")
							, new Object[] {e.getMessage()}), JOptionPane.ERROR_MESSAGE);
						resetTelnetSession();
					}
					break;
				}
			}
		}
		finally
		{
			if(null != progressDialog)
			{
				progressDialog.dispose();
			}
			synchronized(this)
			{
				m_workerThread = null;
			}
		}
	}
	
	public synchronized MiTelnet getSession()
	{
		return this.m_telnetSession;
	}
	
	/**
	 * Activate a message dialog displaying some specified text. The argument type is one of:
	 * JOptionPane.ERROR_MESSAGE, INFORMATION_MESSAGE, PLAIN_MESSAGE, QUESTION_MESSAGE, or WARNING_MESSAGE
	 * @param message
	 * @param type
	 */
	private void showMsgDialog(java.lang.String message, int type)
	{
		MiGuiUtil.showMessageDialog(m_parentFrame, message, "Telnet", type);
	}
	
	@Override
	public synchronized void onCancel(JDialog progressDialog)
	{
		if(null != m_telnetSession)
		{
			m_telnetSession.close();
			m_telnetSession = null;
			if(null != m_workerThread)
			{
				m_workerThread.interrupt();
			}
		}
		if(null != progressDialog)
			progressDialog.dispose();
	}
	
	public synchronized void abort()
	{
		if(null != m_telnetSession)
		{
			m_telnetSession.close();
			m_telnetSession = null;
			if(null != m_workerThread)
			{
				m_workerThread.interrupt();
			}
		}
	}
	
	private synchronized void resetTelnetSession()
	{
		if(null != m_telnetSession)
		{
			m_telnetSession.close();
			m_telnetSession = null;
		}
	}
	
}


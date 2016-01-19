package com.mitel.icp;

import java.awt.Frame;
import javax.swing.*;
import com.mitel.guiutil.*;
import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import com.mitel.netutil.FtpClient;

/**
 * Create an FTP session to a particular ICP.
 */
public class IcpFtpClient implements Runnable, IcpType.IcpTypeListener, MiProgressDialog.MiProgressListener
{
	
	private FtpClient m_ftpSession = null;
	
	private final int m_timeoutMs;
	
	private Thread m_myThread;
	
	private boolean m_retry = false;
	
	/**
	 * The parent frame is used to center any dialogs.
	 */	
	private final Frame m_parentFrame;
	
	private final IcpDescriptor m_icpDescriptor;
	private final IcpProcessor.ProcessorName m_processorName;
	private final MiProgressDialog m_progressDialog;
	
	/**
	 * Creates a new IcpFtpClient object
	 *
	 * @param parentFrame
	 * @param icp
	 * @param processorName
	 * @param timeoutMs
	 */
	@SuppressWarnings("LeakingThisInConstructor")
	public IcpFtpClient(Frame parentFrame, IcpDescriptor icp, IcpProcessor.ProcessorName processorName, int timeoutMs)
	{
		m_parentFrame = parentFrame;
		m_icpDescriptor = icp;
		m_processorName = processorName;
		m_timeoutMs = timeoutMs;
		if(null != m_parentFrame)
		{
			m_progressDialog = MiProgressDialog.safeInstantiate(m_parentFrame,"FTP - Progress", 0, this);
			if(m_progressDialog != null) {
				MiGuiUtil.centerComponent(m_progressDialog, parentFrame);
			}
		}
		else
		{
			m_progressDialog = null;
		}
	}

	@Override
	public void run()
	{
		m_myThread = Thread.currentThread();
		
		// Haven't figured out how to determine success/failure of FTP login.
		// so request user info always.
		// Currently, only the RTC allows FTP access
		if(!new IcpUserInfoValidator(m_parentFrame, m_icpDescriptor).challengeUserInfo(m_processorName))
		{
			System.out.println("ERROR: " + getClass().getName() + ".run() - challengeUserInfo() failed");
		}
		else
		{
			try
			{
				IcpType icpApi = IcpTypeRepository.getInstance().locateIcpType(m_icpDescriptor.getType());
				if(icpApi != null) {
					icpApi.enableFtp(m_icpDescriptor, this);
				}
				m_ftpSession = new FtpClient(m_icpDescriptor.getAddress(m_processorName), m_icpDescriptor.getUserId(m_processorName), m_icpDescriptor.getPassword(m_processorName), 5000);
				m_icpDescriptor.setValidated(m_processorName, true);
			}
			catch(Exception e)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
				
				if(null != m_ftpSession)
					m_ftpSession.close();
				
				m_ftpSession = null;
				
				try{m_icpDescriptor.setValidated(IcpProcessor.ProcessorName.RTC, false);}catch(Exception e2){}
			}
		}
		m_myThread = null;
	}
	
	public FtpClient getSession()
	{
		return m_ftpSession;
	}
	
	public void close()
	{
		m_retry = true;
		
		if(null != m_myThread)
			m_myThread.interrupt();

		if(null != m_progressDialog)
		{
			m_progressDialog.dismiss();
		}
	}
		
	public java.awt.Frame getFrame()
	{
		return m_parentFrame;
	}
	
	public void onActivateCscLog(java.lang.String response, boolean success, int percentDone)
	{
	}
	
	public void onDeactivateCscLog(java.lang.String response, boolean success, int percentDone)
	{
	}
	
	public void onDisableCscDebug(java.lang.String response, boolean success, int percentDone)
	{
	}
	
	public void onEnableCscDebug(java.lang.String response, boolean success, int percentDone)
	{
	}
	
	public void onEnableFtp(java.lang.String response, boolean success, int percentDone)
	{
		class ProgressUpdater implements Runnable
		{
			private String m_Response;
			private boolean m_Success;
			private int m_Percent;
			ProgressUpdater(java.lang.String response, boolean success, int percentDone)
			{
				m_Response = response;
				m_Success = success;
				m_Percent = percentDone;
			}
			
			public void run()
			{
				if((m_Success)&&(m_Percent < 100))
				{
					m_progressDialog.setVisible(true);
					m_progressDialog.setProgress(m_Percent);
					m_progressDialog.setOptionalText(m_Response);

				}
				else
				{
					m_progressDialog.setVisible(false);
				}
			}
		}
		if(null != m_progressDialog)
		{
			SwingUtilities.invokeLater(new ProgressUpdater(response, success, percentDone));
		}
	}
	
	public void onGetDateTime(java.lang.String response, com.mitel.miutil.MiDateTime dateTime, boolean success, int percentDone)
	{
	}
	
	public void onPrintCscLog(java.lang.String response, boolean success, int percentDone)
	{
	}
	
	public void onPrintCscMap(java.lang.String response, boolean success, int percentDone)
	{
	}
	
	public void onPrintRscMap(java.lang.String response, boolean success, int percentDone)
	{
	}
	
	public void onCancel(javax.swing.JDialog progressDialog)
	{
		if(null != m_progressDialog)
			progressDialog.setVisible(false);
	}
}


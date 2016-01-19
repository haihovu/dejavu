package com.mitel.icp;

import com.mitel.guiutil.LogInDialog.LogInEvent;
import com.mitel.miutil.MiLogMsg;
import com.mitel.miutil.MiSystem;
import java.awt.Frame;
import javax.swing.JDialog;



/**
 * Opens the Login dialog to allow users to enter user ID and password.
 */
public class IcpUserInfoValidator
{
	private final Frame m_parentFrame;
	private final IcpDescriptor m_icpDescriptor;
	
	/**
	 * Parent frame info is used to center the login dialog
	 * @param parentFrame
	 * @param icp
	 */
	public IcpUserInfoValidator(Frame parentFrame, IcpDescriptor icp)
	{
		m_parentFrame = parentFrame;
		m_icpDescriptor = icp;
	}
	
	/**
	 * Prompts the user to enter the user ID and password.
	 * This method blocks until the user enter the requested information.
	 * @param processorName
	 * @return
	 */
	@SuppressWarnings("null")
	public boolean challengeUserInfo(IcpProcessor.ProcessorName processorName)
	{
		UserInfoListener listener = new UserInfoListener();
		JDialog loginDialog = null;
		try
		{
			if(!m_icpDescriptor.getValidated(processorName))
			{
				IcpProcessor proc = m_icpDescriptor.locateProcessor(processorName);
				if(null != proc)
				{
					IcpProcessor.ProcessorName procName;
					synchronized(proc)
					{
						procName = proc.getName();
					}
					// Only validate if not already done so
					loginDialog = com.mitel.guiutil.LogInDialog.safeInstantiate(m_parentFrame,
						m_icpDescriptor.getName(), procName, m_icpDescriptor.getUserId(procName),
						m_icpDescriptor.getPassword(procName), listener);
					if(loginDialog != null) {
						loginDialog.setVisible(true);

						listener.waitForAccept();
						if(listener.isAccepted())
						{
							synchronized(proc) {
								proc.m_userId = listener.getRtcUserId();
								proc.m_password = listener.getRtcPassword();
							}
							return true;
						}
					} else {
						MiSystem.logWarning(MiLogMsg.Category.DESIGN, "No listener specified");
					}
				} else {
					// No processor
					MiSystem.logWarning(MiLogMsg.Category.DESIGN, "Processor " + processorName + " not found in " + m_icpDescriptor);
				}
			} else {
				// Already validated
				return true;
			}
		}
		catch(InterruptedException ex)
		{
			loginDialog.dispose();
		}
		return false;
	}
	
	/**
	 * Concrete log in listener.
	 */
	private class UserInfoListener implements com.mitel.guiutil.LogInDialog.iLogInListener
	{
		
		private String m_rtcUserId;
		private String m_rtcPassword;
		private boolean responded;
		private boolean m_accepted;
		
		private UserInfoListener()
		{
		}
		
		@Override
		public void handleCancelEvent(LogInEvent evt)
		{
			synchronized(this)
			{
				if( null != evt.logInDialog )
				{
					evt.logInDialog.dispose();
				}
				responded = true;
				this.notifyAll();
			}
		}
		
		@Override
		public void handleLoginEvent(LogInEvent evt)
		{
			synchronized(this)
			{
				if( null != evt.logInDialog )
				{
					evt.logInDialog.dispose();
				}

				m_accepted = true;

				m_rtcUserId = evt.newUserId;
				m_rtcPassword = evt.newPassword;
				responded = true;
				this.notifyAll();
			}
		}
		
		public void waitForAccept() throws java.lang.InterruptedException
		{
			synchronized(this)
			{
				while(!responded)
				{
					this.wait();
				}
			}
		}

		/**
		 * @return the m_rtcUserId
		 */
		public String getRtcUserId()
		{
			synchronized(this)
			{
				return m_rtcUserId;
			}
		}

		/**
		 * @return the m_rtcPassword
		 */
		public String getRtcPassword()
		{
			synchronized(this)
			{
				return m_rtcPassword;
			}
		}

		/**
		 * @return the m_accepted
		 */
		public boolean isAccepted()
		{
			synchronized(this)
			{
				return m_accepted;
			}
		}
	}
	
}


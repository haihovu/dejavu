/*
 * FtpProgressMonitor.java
 *
 * Created on June 22, 2004, 11:51 PM
 */

package com.mitel.guiutil;

import java.awt.Frame;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.SwingUtilities;
import com.mitel.netutil.FtpClient.FtpEvent;

/**
 *
 * @author  Hai Vu
 */
public class FtpProgressMonitor implements com.mitel.guiutil.MiProgressDialog.MiProgressListener, com.mitel.netutil.FtpClient.iFtpListener
{
	
	private final Frame m_parentFrame;
	private final MiProgressDialog m_progressDialog;
	private final String m_fileName;
	
	/** Creates a new instance of FtpProgressMonitor
	 * @param parentFrame
	 * @param action
	 * @param fileName
	 */
	@SuppressWarnings("LeakingThisInConstructor")
	public FtpProgressMonitor(Frame parentFrame, String action, String fileName)
	{
		m_parentFrame = parentFrame;
		m_fileName = fileName;
		m_progressDialog = MiProgressDialog.safeInstantiate(m_parentFrame, MessageFormat.format(ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui").getString("ftpProgressTitle"), new Object[] {action, fileName}), 0, this);
		if(m_progressDialog != null) {
			m_progressDialog.start();
		}
	}
	
	public void onDownload(FtpEvent evt)
	{
		if(evt.m_totalBytes > 0)
		{
			if(m_progressDialog != null) {
				m_progressDialog.setProgress((int)((100*evt.m_bytesRead)/evt.m_totalBytes));
			}
		}
		if(m_progressDialog != null) {
			m_progressDialog.setOptionalText(MessageFormat.format(ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui").getString("byteProgressLabel"), new Object[] {m_fileName, Long.toString(evt.m_bytesRead), Long.toString(evt.m_totalBytes)}));
		}
		if(evt.m_completed)
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if(m_progressDialog != null) {
						m_progressDialog.dispose();
					}
				}
			});
	}
	
	public void onUpload(FtpEvent evt)
	{
		if(evt.m_totalBytes > 0)
		{
			if(m_progressDialog != null) {
				m_progressDialog.setProgress((int)((100*evt.m_bytesRead)/evt.m_totalBytes));
			}
		}
		if(m_progressDialog != null) {
			m_progressDialog.setOptionalText(MessageFormat.format(ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui").getString("byteProgressLabel"), new Object[] {m_fileName, Long.toString(evt.m_bytesRead), Long.toString(evt.m_totalBytes)}));
		}
		if(evt.m_completed)
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if(m_progressDialog != null) {
						m_progressDialog.dispose();
					}
				}
			});
	}
	
	public void onCancel(javax.swing.JDialog progressDialog)
	{
		progressDialog.dispose();
	}
	
}

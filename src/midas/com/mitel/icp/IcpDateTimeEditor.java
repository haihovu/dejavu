/*
 * IcpDateTimeEditor.java
 *
 * Created on July 13, 2004, 8:54 AM
 */

package com.mitel.icp;

import java.awt.Frame;
import java.util.ResourceBundle;
import javax.swing.*;
import com.mitel.guiutil.MiProgressDialog;
import com.mitel.miutil.MiDateTime;

/**
 *
 * @author  haiv
 */
public class IcpDateTimeEditor implements ArgEditor
{
	private static final IcpDateTimeEditor m_singleton = new IcpDateTimeEditor();
	
	/** Creates a new instance of IcpDateTimeEditor */
	@SuppressWarnings("LeakingThisInConstructor")
	public IcpDateTimeEditor()
	{
		ArgEditorRepository.getInstance().registerEditor(MiArg.ARG_TYPE_DATETIME, this);
	}

	@Override
	public String editArg(Frame parentFrame, java.lang.String origArg, IcpDescriptor icp)
	{
		class LocalDateTimeListener implements com.mitel.guiutil.MiDateTimeDialog.DateTimeListener
		{
			private MiDateTime m_dateTime = null;
			
			private LocalDateTimeListener()
			{
			}

			@Override
			public void onCommit(MiDateTime dateTime, JDialog source)
			{
				synchronized(LocalDateTimeListener.this)
				{
					m_dateTime = dateTime;
				}
				source.dispose();
			}

			@Override
			public void onCancel(JDialog source)
			{
				source.dispose();
			}

			synchronized MiDateTime getDateTime()
			{
				return m_dateTime;
			}
		}
		
		MiDateTime localDate = null;
		if(null != icp)
		{
			IcpType icpApi = IcpTypeRepository.getInstance().locateIcpType(icp.getType());
			if(null != icpApi)
			{
				localDate = icpApi.getDateTime(icp, new IcpApiListener(parentFrame));
			}
		}
		
		if(null == localDate)
		{
			localDate = new MiDateTime();
		}
		
		LocalDateTimeListener loclis = new LocalDateTimeListener();
		new com.mitel.guiutil.MiDateTimeDialog(parentFrame, ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui").getString("dateTimeEditorLabel"), localDate, loclis).setVisible(true);
		if(loclis.getDateTime() != null)
		{
			StringBuilder retValue = new StringBuilder(256);
			retValue.append("\"").append(String.valueOf(loclis.getDateTime().m_year));
			retValue.append("\",\"").append(String.valueOf(loclis.getDateTime().m_month+1));
			retValue.append("\",\"").append(String.valueOf(loclis.getDateTime().m_day+1));
			retValue.append("\",\"1"); // Day of week not used
			retValue.append("\",\"").append(String.valueOf(loclis.getDateTime().m_hour));
			retValue.append("\",\"").append(String.valueOf(loclis.getDateTime().m_minute));
			retValue.append("\",\"").append(String.valueOf(loclis.getDateTime().m_second)).append("\"");
			return retValue.toString();
		}
		
		return "";
	}
	
	public static IcpDateTimeEditor getInstance()
	{
		return m_singleton;
	}
	
	private static class IcpApiListener implements IcpType.IcpTypeListener, com.mitel.guiutil.MiProgressDialog.MiProgressListener
	{
		private final Frame m_parentFrame;
		private MiProgressDialog m_progress;
		
		private IcpApiListener(Frame parentFrame)
		{
			m_parentFrame = parentFrame;
		}
		
		public void onCancel(JDialog progressDialog)
		{
			progressDialog.dispose();
			synchronized(this) {
				m_progress = null;
			}
		}
		
		public Frame getFrame()
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
		
		@SuppressWarnings("CallToThreadYield")
		public void onGetDateTime(java.lang.String response, MiDateTime dateTime, boolean success, int percentDone)
		{
			if(!success)
			{
				JOptionPane.showMessageDialog(m_parentFrame, response, ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("getSystemTimeLabel"), JOptionPane.ERROR_MESSAGE);
			}
			synchronized(this) {
				if(null == m_progress)
				{
					m_progress = MiProgressDialog.safeInstantiate(m_parentFrame, ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("getSystemTimeLabel"), 0, this);
				}
				if(null != m_progress)
				{
					m_progress.start();
					m_progress.setOptionalText(response);
					m_progress.setProgress(percentDone);
					if(100 == percentDone)
					{
						m_progress.dispose();
					}
				}
			}
			Thread.yield();
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
		
		public void onEnableFtp(java.lang.String response, boolean success, int percentDone)
		{
		}
		
	}
	
}

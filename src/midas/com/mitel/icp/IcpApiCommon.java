/*
 * IcpApi3300.java
 *
 * Created on April 28, 2004, 10:53 AM
 */

package com.mitel.icp;

import com.mitel.miutil.MiDateTime;
import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import com.mitel.netutil.MiTelnet;
import com.mitel.netutil.MiTelnet.MiTelnetException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This represets the SX200ICP (no web server), and many legacy systems, including
 * SX200ICP 1.1, ICP3300 4.0--.
 * @author haiv
 */
abstract class IcpApiCommon implements IcpType
{
	
	private final IcpCancelRequestRunnable m_cancelRequestRunnable = new IcpCancelRequestRunnable(this);
	
	private static final Map<String, Integer> m_monthStringMap = new TreeMap<String, Integer>();
	
	static 
	{
		m_monthStringMap.put("January", 1);
		m_monthStringMap.put("February", 2);
		m_monthStringMap.put("March", 3);
		m_monthStringMap.put("April", 4);
		m_monthStringMap.put("May", 5);
		m_monthStringMap.put("June", 6);
		m_monthStringMap.put("July", 7);
		m_monthStringMap.put("August", 8);
		m_monthStringMap.put("September", 9);
		m_monthStringMap.put("October", 10);
		m_monthStringMap.put("November", 11);
		m_monthStringMap.put("December", 12);
	}
	
	// Default type is SX200ICP
	/** Creates a new instance of IcpApi3300 */
	IcpApiCommon()
	{
	}
	
	@SuppressWarnings("NoopMethodInAbstractClass")
	public void cancelRequest()
	{
		// Not much we can do here
	}
	
	public void printRscMap(int levelOfDetails, IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		boolean userValidationRetry = false;
		try
		{
			while(true)
			{
				if(!userValidationRetry)
				{
					userValidationRetry = true;
					if(!new IcpUserInfoValidator(listener.getFrame(),icpDesc).challengeUserInfo(IcpProcessor.ProcessorName.RTC))
					{
						listener.onPrintRscMap(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("requesstCancelledLabel"), false, 100 );
						break; // Cancelled, getout
					}
				}
				else
				{
					listener.onPrintRscMap( "ERROR: Failed to log in", false, 100 );
					break;
				}
				
				listener.onPrintRscMap(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("connectingLabel"), true, 15);
				MiTelnet telnetSession = new MiTelnet(icpDesc.getAddress(IcpProcessor.ProcessorName.RTC), 2002, true/*TLS*/);
				listener.onPrintRscMap(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("logingOnLabel"), true, 30);
				try
				{
					telnetSession.login(icpDesc.getUserId(IcpProcessor.ProcessorName.RTC), icpDesc.getPassword(IcpProcessor.ProcessorName.RTC));
					icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, true);
					listener.onPrintRscMap(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("executingLabel"), new Object[]{"PrintRSCMap"}), true, 70);
					String icpOutput = telnetSession.executeCommand("PrintRSCMap");
					icpOutput += "(Done)";

					listener.onPrintRscMap( icpOutput, true, 100 );
					break;
				}
				catch(MiTelnetException e)
				{
					listener.onPrintRscMap(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed2Label"), false, 100 );
					telnetSession.close();
					telnetSession = null;
					icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, false);
				}
				finally
				{
					if(telnetSession != null) {
						telnetSession.close();
					}
				}
			}
		}
		catch( Exception e )
		{
			listener.onPrintRscMap(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unexpectedFailureLabel"),
				new Object[] {e.getMessage()}), false, 100);
		}
	}
	
	@SuppressWarnings("NoopMethodInAbstractClass")
	public void enableFtp(IcpDescriptor icpDesc, IcpTypeListener listener)
	{
	}
	
	public void activateCscLog(int queueSize, IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		try
		{

			if(!new IcpUserInfoValidator(listener.getFrame(),icpDesc).challengeUserInfo(IcpProcessor.ProcessorName.RTC))
			{
				listener.onActivateCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("requesstCancelledLabel"), false, 100 );
				return;
			}

			listener.onActivateCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("connectingLabel"), true, 15);
			MiTelnet telnetSession = new MiTelnet(icpDesc.getAddress(IcpProcessor.ProcessorName.RTC), 2002, true/*TLS*/);
			listener.onActivateCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("logingOnLabel"), true, 30);
			try
			{
				telnetSession.login(icpDesc.getUserId(IcpProcessor.ProcessorName.RTC), icpDesc.getPassword(IcpProcessor.ProcessorName.RTC));
				listener.onActivateCscLog(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("executingLabel"), new Object[]{"ActivateCSCLog"}), true, 70);
				String icpOutput = telnetSession.executeCommand("ActivateCSCLog " + queueSize);
				icpOutput += "(Done)";
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, true);

				listener.onActivateCscLog( icpOutput, true, 100 );
			}
			catch(MiTelnetException e)
			{
				listener.onActivateCscLog( MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed1Label"), new Object[] {e.getMessage()}), false, 100 );
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, false);
			}
			finally
			{
				telnetSession.close();
			}
		}
		catch( Exception e )
		{
			listener.onActivateCscLog(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unexpectedFailureLabel"), new Object[] {e.getMessage()}), false, 100);
		}
	}
	
	public void deactivateCscLog(IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		try
		{
			if(!new IcpUserInfoValidator(listener.getFrame(),icpDesc).challengeUserInfo(IcpProcessor.ProcessorName.RTC))
			{
				listener.onDeactivateCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("requesstCancelledLabel"), false, 100 );
				return;
			}
			listener.onDeactivateCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("connectingLabel"), true, 15);
			MiTelnet telnetSession = new MiTelnet(icpDesc.getAddress(IcpProcessor.ProcessorName.RTC), 2002, true/*TLS*/);
			listener.onDeactivateCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("logingOnLabel"), true, 30);
			try
			{
				telnetSession.login(icpDesc.getUserId(IcpProcessor.ProcessorName.RTC), icpDesc.getPassword(IcpProcessor.ProcessorName.RTC));
				listener.onDeactivateCscLog(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("executingLabel"), new Object[]{"DeactivateCSCLog"}), true, 75);
				String icpOutput = telnetSession.executeCommand("DeactivateCSCLog");
				icpOutput += "(Done)";
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, true);

				listener.onDeactivateCscLog( icpOutput, true, 100 );
			}
			catch(MiTelnetException e)
			{
				listener.onDeactivateCscLog(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed1Label"), new Object[] {e.getMessage()}), false, 100 );
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, false);
			}
			finally
			{
				telnetSession.close();
			}
		}
		catch( Exception e )
		{
			listener.onDeactivateCscLog(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unexpectedFailureLabel"), new Object[] {e.getMessage()}), false, 100);
		}
	}
	
	public void printCscLog(int levelOfDetails, IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		try
		{
			if(!new IcpUserInfoValidator(listener.getFrame(),icpDesc).challengeUserInfo(IcpProcessor.ProcessorName.RTC))
			{
				listener.onPrintCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("requesstCancelledLabel"), false, 100 );
				return;
			}

			listener.onPrintCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("connectingLabel"), true, 20);
			MiTelnet telnetSession = new MiTelnet(icpDesc.getAddress(IcpProcessor.ProcessorName.RTC), 2002, true/*TLS*/);
			listener.onPrintCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("logingOnLabel"), true, 40);
			try
			{
				telnetSession.login(icpDesc.getUserId(IcpProcessor.ProcessorName.RTC), icpDesc.getPassword(IcpProcessor.ProcessorName.RTC));
				listener.onPrintCscLog(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("executingLabel"), new Object[]{"PrintCSCLog"}), true, 60);
				String logOutput = telnetSession.executeCommand("PrintCSCLog " + String.valueOf(levelOfDetails));

				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, true);

				// Search for first open paren
				char[] buff = logOutput.toCharArray();
				int index = -1;
				for( int i = 0; i < buff.length; ++i )
				{
					if( buff[i] == '(' )
					{
						index = i;
						break;
					}
				}

				// Found at a first open paren
				if( -1 != index )
				{
					logOutput = logOutput.substring(index);

					// If the first open paren does not correlate to CscLog
					// then add it...
					listener.onPrintCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("correctingOutputLabel"), true, 80);
					Pattern p = Pattern.compile( "^\\(CscLog\\s*\\(.*", Pattern.DOTALL );
					Matcher m = p.matcher(logOutput);
					if( !m.find())
					{
						logOutput = ("(CscLog" + logOutput);

						// Search for last close paren
						buff = logOutput.toCharArray();
						index = -1;
						for( int i = 0; i < buff.length; ++i )
						{
							if( buff[i] == ')' )
							{
								index = i;
							}
						}

						// ...and the corresponding close paren
						if( -1 != index )
						{
							logOutput = logOutput.substring(0, index);
							logOutput += ")";
						}
					}
				}

				listener.onPrintCscLog( logOutput, true, 100 );
			}
			catch(MiTelnetException e)
			{
				listener.onPrintCscLog(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed1Label"), new Object[] {e.getMessage()}), false, 100 );
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, false);
			}
			finally
			{
				telnetSession.close();
			}
		}
		catch( Exception e )
		{
			listener.onPrintCscLog(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unexpectedFailureLabel"), new Object[] {e.getMessage()}), false, 100);
		}
	}

	@Override
	public Runnable getRequestCancelRunnable()
	{
		return m_cancelRequestRunnable;
	}
	
	public void printCscMap(int levelOfDetails, IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		try
		{
			if(!new IcpUserInfoValidator(listener.getFrame(),icpDesc).challengeUserInfo(IcpProcessor.ProcessorName.RTC))
			{
				listener.onPrintCscMap(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("requesstCancelledLabel"), false, 100 );
				return;
			}

			listener.onPrintCscMap(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("connectingLabel"), true, 20);
			MiTelnet telnetSession = new MiTelnet(icpDesc.getAddress(IcpProcessor.ProcessorName.RTC), 2002, true/*TLS*/);
			listener.onPrintCscMap(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("logingOnLabel"), true, 40);
			try
			{
				telnetSession.login(icpDesc.getUserId(IcpProcessor.ProcessorName.RTC), icpDesc.getPassword(IcpProcessor.ProcessorName.RTC));
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, true);
				listener.onPrintCscMap(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("executingLabel"), new Object[]{"PrintCSCMap"}), true, 60);
				listener.onPrintCscMap(telnetSession.executeCommand("PrintCSCMap " + String.valueOf(levelOfDetails)), true, 100);
			}
			catch(MiTelnetException e)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
				listener.onPrintCscMap(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed1Label"), new Object[] {e.getMessage()}), false, 100);
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, false);
			}
			finally
			{
				telnetSession.close();
			}
		}
		catch(IOException ex)
		{
			listener.onPrintCscMap(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unexpectedFailureLabel"), 
				new Object[] {ex.toString()}), false, 100);
		}
	}

	@Override
	public void enableCscDebug(IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		try
		{
			if(!new IcpUserInfoValidator(listener.getFrame(),icpDesc).challengeUserInfo(IcpProcessor.ProcessorName.RTC))
			{
				listener.onEnableCscDebug(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("requesstCancelledLabel"), false, 100 );
				return;
			}

			listener.onEnableCscDebug( ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("connectingLabel"), true, 20);
			MiTelnet telnetSession = new MiTelnet(icpDesc.getAddress(IcpProcessor.ProcessorName.RTC), 2002, true/*TLS*/);
			listener.onEnableCscDebug( ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("logingOnLabel"), true, 40);
			try
			{
				telnetSession.login(icpDesc.getUserId(IcpProcessor.ProcessorName.RTC), icpDesc.getPassword(IcpProcessor.ProcessorName.RTC));
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, true);
				listener.onEnableCscDebug(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("executingLabel"), new Object[]{"EnableCSCDebug"}), true, 60);
				listener.onEnableCscDebug( telnetSession.executeCommand("EnableCSCDebug"), true, 100);
			}
			catch(MiTelnetException e)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
				listener.onEnableCscDebug(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed1Label"), new Object[] {e.getMessage()}), false, 100);
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, false);
			}
			finally
			{
				telnetSession.close();
			}
		}
		catch( Exception e )
		{
			listener.onEnableCscDebug(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unexpectedFailureLabel"), new Object[] {e.getMessage()}), false, 100);
		}
	}

	@Override
	public void disableCscDebug(IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		try
		{
			if(!new IcpUserInfoValidator(listener.getFrame(),icpDesc).challengeUserInfo(IcpProcessor.ProcessorName.RTC))
			{
				listener.onDisableCscDebug(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("requesstCancelledLabel"), false, 100 );
				return;
			}

			listener.onDisableCscDebug( ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("connectingLabel"), true, 20);
			MiTelnet telnetSession = new MiTelnet(icpDesc.getAddress(IcpProcessor.ProcessorName.RTC), 2002, true/*TLS*/);
			listener.onDisableCscDebug( ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("logingOnLabel"), true, 40);
			try
			{
				telnetSession.login(icpDesc.getUserId(IcpProcessor.ProcessorName.RTC), icpDesc.getPassword(IcpProcessor.ProcessorName.RTC));
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, true);
				listener.onDisableCscDebug(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("executingLabel"), new Object[]{"DisableCSCDebug"}), true, 60);
				listener.onDisableCscDebug( telnetSession.executeCommand("DisableCSCDebug"), true, 100);
			}
			catch(MiTelnetException e)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
				listener.onDisableCscDebug(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed1Label"), new Object[] {e.getMessage()}), false, 100);
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, false);
			}
			finally
			{
				telnetSession.close();
			}
		}
		catch( Exception e )
		{
			listener.onDisableCscDebug(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unexpectedFailureLabel"), new Object[] {e.getMessage()}), false, 100);
		}
	}

	@Override
	public MiDateTime getDateTime(IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		MiTelnet telnetSession;
		try
		{
			if(!new IcpUserInfoValidator(listener != null ? listener.getFrame() : null, icpDesc).challengeUserInfo(IcpProcessor.ProcessorName.RTC))
			{
				return null;
			}
			if(null != listener)
			{
				listener.onGetDateTime(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("openTelnetLabel"), null, true, 10);
			}
			telnetSession = new MiTelnet(icpDesc.getAddress(IcpProcessor.ProcessorName.RTC), 2002, true/*TLS*/);
			try
			{
				if(null != listener)
				{
					listener.onGetDateTime(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("logingOnLabel"), null, true, 20);
				}
				telnetSession.login(icpDesc.getUserId(IcpProcessor.ProcessorName.RTC), icpDesc.getPassword(IcpProcessor.ProcessorName.RTC));
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, true);
				if(null != listener)
				{
					listener.onGetDateTime(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("executingLabel"), new Object[] {"GetDateTime"}), null, true, 80);
				}
				String dateTimeResult = telnetSession.executeCommand("GetDateTime");
				
				if(null != listener)
				{
					listener.onGetDateTime(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("doneLabel"), null, true, 100);
				}
				return parseDateTime(dateTimeResult);
			}
			catch(MiTelnetException e)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
				if(null != listener)
				{
					listener.onGetDateTime(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed1Label"), new Object[] {e.getMessage()}), null, false, 100);
				}
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, false);
			}
			finally
			{
				telnetSession.close();
			}
		}
		catch(IOException ex)
		{
			MiSystem.logWarning(Category.DESIGN, "Failed to establish telnet session due to " + ex);
			if(null != listener)
			{
				listener.onGetDateTime(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unexpectedFailureLabel"), new Object[] {ex.getMessage()}), null, false, 100);
			}
		}
		catch(RuntimeException e)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
			if(null != listener)
			{
				listener.onGetDateTime(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unexpectedFailureLabel"), new Object[] {e.getMessage()}), null, false, 100);
			}
		}
		
		return null;
	}

	private static int convertMonthFromString(String month)
	{
		if(m_monthStringMap.containsKey(month))
		{
			return (m_monthStringMap.get(month));
		}
		throw new RuntimeException("Invalid month " + month);
	}
	
	private static MiDateTime parseDateTime(String dateTime)
	{
		try
		{
			Pattern p = Pattern.compile("([1-9][0-9]+)\\-([A-Z][a-z]+)\\-([0-9]+)\\s+([0-9]+):([0-9]+):([0-9]+)", Pattern.DOTALL | Pattern.MULTILINE);
			Matcher m = p.matcher(dateTime);
			if(m.find())
			{
				int year = Integer.parseInt(m.group(1));
				int month = convertMonthFromString(m.group(2))-1;
				int day = Integer.parseInt(m.group(3))-1;
				int hour = Integer.parseInt(m.group(4));
				int min = Integer.parseInt(m.group(5));
				int sec = Integer.parseInt(m.group(6));

				return new MiDateTime(year, month, day, hour, min, sec);
			}
		}
		catch(RuntimeException e)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
		return null;
	}	
}

/*
 * IcpApi3300.java
 *
 * Created on April 28, 2004, 10:53 AM
 */

package com.mitel.icp;

import com.mitel.netutil.MiTelnet;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * This represents the SX200ICP (no web server), and many legacy systems, including
 * SX200ICP 1.1, ICP3300 4.0--.
 * @author haiv
 */
public class IcpApi200 extends IcpApiCommon implements IcpType
{	
	private final IcpTypeInfo typeInfo;
	
	// Default type is SX200ICP
	private final String typeName;

	/** Creates a new instance of IcpApi3300
	 * @param icpType
	 */
	@SuppressWarnings("LeakingThisInConstructor")
	public IcpApi200(String icpType)
	{
		typeName = icpType;
		typeInfo = new IcpTypeInfo(icpType, IcpTypeInfo.IcpClass.MCD_Mxe);
		IcpTypeRepository.getInstance().registerIcpType(this);
	}

	@Override
	public IcpTypeInfo getTypeInfo()
	{
		return typeInfo;
	}
	
	@Override
	public void cancelRequest()
	{
		// Not much we can do here
	}	

	@Override
	public String compIdToString(java.lang.String compId)
	{
		final String[] compNames =
		{
			"CSC",
			"RSC",
			"TDM_SWITCHING_SP",
			"E2T_SP",
			"E2T_PROXY_SP",
			"HDLC_SP",
			"EC_PROXY_SP",
			"PPP_SP",
			"IP_PHONE_SP",
			"IP_SWITCHING_SP",
			"STP_SP",
			"DSP_SP",
			"NETSYNC_SP",
			"CIM_SP",
			"CHUB_SP",
			"FIM_SP",
			"HUB_SP",
			"ONS_SP",
			"LS_SP",
			"MOH_SP",
			"PAGER_SP",
			"T1PRA_SP",
			"MODEM_SP",
			"IP_TRUNKING_SP",
			"ASOCK_CTLR",
			"SX200_IP_SP"
		};
		if( compId.matches("[0-9]+"))
		{
			int index = Integer.parseInt(compId);
			if( index < compNames.length )
			{
				return compNames[index];
			}
		}
		return compId;
	}
	
	@Override
	public String toString()
	{
		return "<Type>" + this.typeInfo + "</Type>";
	}
	
	@Override
	public void enableFtp(IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		try
		{

			if(!new IcpUserInfoValidator(listener.getFrame(),icpDesc).challengeUserInfo(IcpProcessor.ProcessorName.RTC))
			{
				listener.onEnableFtp(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("requesstCancelledLabel"), false, 100 );
				return;
			}

			listener.onEnableFtp(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("connectingLabel"), true, 15);
			MiTelnet telnetSession = new MiTelnet(icpDesc.getAddress(IcpProcessor.ProcessorName.RTC), 2002, true/*TLS*/);
			listener.onEnableFtp(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("logingOnLabel"), true, 30);
			try
			{
				telnetSession.login(icpDesc.getUserId(IcpProcessor.ProcessorName.RTC), icpDesc.getPassword(IcpProcessor.ProcessorName.RTC));
				listener.onEnableFtp(MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("executingLabel"), new Object[]{"ActivateCSCLog"}), true, 70);
				String icpOutput = telnetSession.executeCommand("startFtpd");
				icpOutput += "(Done)";
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, true);

				listener.onEnableFtp( icpOutput, true, 100 );
			}
			catch(MiTelnet.MiTelnetException e)
			{
				listener.onEnableFtp( MessageFormat.format(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("loginFailed1Label"), new Object[] {e.getMessage()}), false, 100 );
				icpDesc.setValidated(IcpProcessor.ProcessorName.RTC, false);
			}
			finally
			{
				telnetSession.close();
			}
		}
		catch(IOException ex)
		{
			listener.onEnableFtp(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unexpectedFailureLabel"),
				new Object[] {ex.getMessage()}), false, 100);
		}
		catch(RuntimeException e)
		{
			listener.onEnableFtp(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("unexpectedFailureLabel"),
				new Object[] {e.getMessage()}), false, 100);
		}
	}
	
}

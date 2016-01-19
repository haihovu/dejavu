/*
 * IcpApi3300.java
 *
 * Created on April 28, 2004, 10:53 AM
 */

package com.mitel.icp;

/**
 * This represents the SX200ICP (no web server), and many legacy systems, including
 * SX200ICP 1.1, ICP3300 4.0--.
 * @author haiv
 */
class IcpApi3300_r40 extends IcpApiCommon implements IcpType
{	
	private final IcpTypeInfo typeInfo;
	
	// Default type is SX200ICP
	private final String typeName;

	/** Creates a new instance of IcpApi3300
	 * @param icpType
	 * @param icpClass 
	 */
	@SuppressWarnings("LeakingThisInConstructor")
	IcpApi3300_r40(String icpType, IcpTypeInfo.IcpClass icpClass)
	{
		typeName = icpType;
		typeInfo = new IcpTypeInfo(icpType, icpClass);
		IcpTypeRepository.getInstance().registerIcpType(this);
	}

	@Override
	public IcpTypeInfo getTypeInfo()
	{
		return typeInfo;
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
}

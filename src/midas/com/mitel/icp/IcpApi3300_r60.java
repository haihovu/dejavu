/*
 * IcpApi3300.java
 *
 * Created on April 28, 2004, 10:53 AM
 */

package com.mitel.icp;

import com.mitel.httputil.HttpListener;

/**
 *
 * @author  haiv
 */
class IcpApi3300_r60 extends IcpApi3300_r50 implements IcpType, HttpListener
{	
	/** Creates a new instance of IcpApi3300
	 * @param typeName Name of the type
	 * @param icpClass Class of this ICP type. 
	 */
	IcpApi3300_r60(String typeName, IcpTypeInfo.IcpClass icpClass)
	{
		super(typeName, icpClass);
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
			"DSP_SP",
			"NETSYNC_SP",
			"CIM_SP",
			"CHUB_SP",
			"HUB_SP",
			"ONS_SP",
			"LS_SP",
			"MOH_SP",
			"PAGER_SP",
			"T1PRA_SP",
			"MODEM_SP",
			"ASOCK_CTLR",
			"FPGA_SP",
			"MILAP_SP",
			"TONEDETCOORD_SP",
			"QFIM_SP",
			"T1D4_SP",
			"T1BRA_SP",
			"FIM_SP",
			"A2T_SP"
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
	
}

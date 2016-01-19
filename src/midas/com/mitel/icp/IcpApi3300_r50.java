/*
 * IcpApi3300_r50.java
 *
 * Created on April 28, 2004, 10:53 AM
 */

package com.mitel.icp;

import com.mitel.httputil.HttpClient;
import com.mitel.httputil.HttpListener;
import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author  haiv
 */
class IcpApi3300_r50 extends IcpApiCommon implements IcpType, HttpListener
{
	
	private HttpClient m_HttpClient;
	
	private IcpTypeListener m_listener;
	
	/**
	 * Command ID used with invocation to sendHttpRequest()
	 */
	protected static final int CSC_HTTP_ACTIVATE_LOG = 1;
	/**
	 * Command ID used with invocation to sendHttpRequest()
	 */
	protected static final int CSC_HTTP_DEACTIVATE_LOG = 2;
	/**
	 * Command ID used with invocation to sendHttpRequest()
	 */
	protected static final int CSC_HTTP_PRINT_LOG = 3;
	/**
	 * Command ID used with invocation to sendHttpRequest()
	 */
	protected static final int CSC_HTTP_PRINT_MAP = 4;
	
	private static final String cscHttpCommandActivateLog = "GET /cscactivatelog.asp\n\n";
	private static final String cscHttpCommandDeactivateLog = "GET /cscdeactivatelog.asp\n\n";
	private static final String cscHttpCommandPrintMap = "GET /cscprintmap.asp";
	private static final String cscHttpCommandPrintLog = "GET /cscprintlog.asp";
	private final IcpTypeInfo typeInfo;

	/** Creates a new instance of IcpApi3300_r50
	 * @param typeName Name of the type
	 * @param icpClass ICP class
	 */
	@SuppressWarnings("LeakingThisInConstructor")
	IcpApi3300_r50(String typeName, IcpTypeInfo.IcpClass icpClass)
	{
		typeInfo = new IcpTypeInfo(typeName, icpClass);
		IcpTypeRepository.getInstance().registerIcpType(this);
	}
	
	@Override
	public void activateCscLog(int queueSize, IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		synchronized( this )
		{
			//queueSize is currently ignored
			m_listener = listener;
			m_listener.onActivateCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("sendingRequestLabel"), true, 30);
			sendHttpRequest( icpDesc, CSC_HTTP_ACTIVATE_LOG, cscHttpCommandActivateLog );
		}
	}
	
	@Override
	public void deactivateCscLog(IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		synchronized( this )
		{
			m_listener = listener;
			m_listener.onDeactivateCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("sendingRequestLabel"), true, 30);
			sendHttpRequest( icpDesc, CSC_HTTP_DEACTIVATE_LOG, cscHttpCommandDeactivateLog );
		}
	}
	
	@Override
	public void printCscLog(int levelOfDetails, IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		synchronized( this )
		{
			m_listener = listener;
			m_listener.onPrintCscLog(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("sendingRequestLabel"), true, 30);
			sendHttpRequest( icpDesc, CSC_HTTP_PRINT_LOG, cscHttpCommandPrintLog + "?levelOfDetails=" + String.valueOf(levelOfDetails) + "\n\n" );
		}
	}
	
	@Override
	public void printCscMap(int levelOfDetails, IcpDescriptor icpDesc, IcpTypeListener listener)
	{
		synchronized( this )
		{
			m_listener = listener;
			m_listener.onPrintCscMap(ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("sendingRequestLabel"), true, 30);
			sendHttpRequest( icpDesc, CSC_HTTP_PRINT_MAP, cscHttpCommandPrintMap + "?levelOfDetails=" + String.valueOf(levelOfDetails) + "\n\n" );
		}
	}
	
	/**
	 * Sends HTTP command to the server and returns the HTTP reply string.
	 * @param icpDesc
	 * @param cmdId
	 * @param httpCmd 
	 */
	private synchronized void sendHttpRequest(IcpDescriptor icpDesc, int cmdId, java.lang.String httpCmd)
	{
		// Start up a new secured client to send request
		try
		{
			m_HttpClient = new HttpClient(icpDesc.getAddress(IcpProcessor.ProcessorName.RTC), httpCmd, cmdId, true /*SSL*/, false /*HTTP1.1*/, true /*Send cmd*/, this );
			m_HttpClient. start();
		}
		catch(Exception e)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
	}
	
	@Override
	public void httpResponse(String httpData, int cmdId)
	{
		synchronized( this )
		{
			// NULL out the HTTP client
			m_HttpClient = null;

			switch( cmdId )
			{
				case CSC_HTTP_ACTIVATE_LOG:
					if( null != this.m_listener )
					{
						Pattern p = Pattern.compile(".*\\(Done\\).*", Pattern.DOTALL);
						Matcher m = p.matcher(httpData);
						if(m.find())
							this.m_listener.onActivateCscLog(httpData, true, 100);
						else
							this.m_listener.onActivateCscLog(httpData, false, 100);
					}
					else
					{
						System.out.println( "ERROR: " + getClass().getName() + ".httpResponse() - No listener" );
					}
					break;

				case CSC_HTTP_DEACTIVATE_LOG:
					if( null != this.m_listener )
					{
						Pattern p = Pattern.compile(".*\\(Done\\).*", Pattern.DOTALL);
						Matcher m = p.matcher(httpData);
						if(m.find())
							this.m_listener.onDeactivateCscLog(httpData, true, 100);
						else
							this.m_listener.onDeactivateCscLog(httpData, false, 100);
					}
					else
					{
						System.out.println( "ERROR: " + getClass().getName() + ".httpResponse() - No listener" );
					}
					break;

				case CSC_HTTP_PRINT_LOG:
					if( null != this.m_listener )
					{
						this.m_listener.onPrintCscLog(httpData, true, 100);
					}
					else
					{
						System.out.println( "ERROR: " + getClass().getName() + ".httpResponse() - No listener" );
					}
					break;

				case CSC_HTTP_PRINT_MAP:
					if( null != this.m_listener )
					{
						this.m_listener.onPrintCscMap(httpData, true, 100);
					}
					else
					{
						System.out.println( "ERROR: " + getClass().getName() + ".httpResponse() - No listener" );
					}
					break;

				default:
					// Do nothing
					break;
			}
			m_listener = null; // Done, don't need this anymore
		}
	}
	
	@Override
	public void httpError(java.lang.String errorDescription, int cmdId)
	{
		synchronized( this )
		{
			m_HttpClient = null;
			switch( cmdId )
			{
				case CSC_HTTP_ACTIVATE_LOG:
					if( null != this.m_listener )
					{
						this.m_listener.onActivateCscLog("ERROR: "+errorDescription, false, 100);
					}
					break;

				case CSC_HTTP_DEACTIVATE_LOG:
					if( null != this.m_listener )
					{
						this.m_listener.onDeactivateCscLog("ERROR: "+errorDescription, false, 100);
					}
					break;

				case CSC_HTTP_PRINT_LOG:
					if( null != this.m_listener )
					{
						this.m_listener.onPrintCscLog("ERROR: "+errorDescription, false, 100);
					}
					break;

				case CSC_HTTP_PRINT_MAP:
					if( null != this.m_listener )
					{
						this.m_listener.onPrintCscMap("ERROR: "+errorDescription, false, 100);
					}
					break;

				default:
					// Do nothing
					break;
			}
			m_listener = null;
		}
	}
	
	@Override
	public IcpTypeInfo getTypeInfo()
	{
		return typeInfo;
	}
	
	@Override
	public synchronized void cancelRequest()
	{
		if( null != m_HttpClient )
		{
			m_HttpClient.close();
			m_HttpClient = null;
			m_listener = null;
		}
	}	
	
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
			"ASOCK_CTLR",
			"TRUNKING_SP_MGR",
			"FPGA_SP",
			"MILAP_SP",
			"TONEDETCOORD_SP",
			"QFIM_SP",
			"T1D4_SP",
			"T1BRA_SP"
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

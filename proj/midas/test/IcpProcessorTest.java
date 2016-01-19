/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.mitel.icp.IcpProcessor;
import com.mitel.miutil.MiParameter;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 *
 * @author haiv
 */
public class IcpProcessorTest extends TestCase
{
	private static final Logger gLogger = Logger.getLogger(IcpProcessorTest.class.getName());
	
	public IcpProcessorTest(String testName)
	{
		super(testName);
	}
	
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
	// TODO add test methods here. The name must begin with 'test'. For example:
	// public void testHello() {}
	@SuppressWarnings("CallToThreadDumpStack")
	public void testConfigParam()
	{
		gLogger.setLevel(Level.ALL);
		try
		{
			IcpProcessor icp = new IcpProcessor(IcpProcessor.ProcessorName.RTC, 123);
			MiParameter p = icp.getConfigParameter();
			gLogger.log(Level.INFO, "1. {0} -> {1}", new Object[]{icp, p});
			IcpProcessor copy = new IcpProcessor(p);
			if(!icp.equals(copy))
			{
				fail(icp + " != " + copy);
			}
			
			icp.m_address = "123.123.123.123";
			icp.m_userId = "mixml";
			icp.m_password = "";
			p = icp.getConfigParameter();
			gLogger.log(Level.INFO, "2. {0} -> {1}", new Object[]{icp, p});
			copy = new IcpProcessor(p);
			if(!icp.equals(copy))
			{
				fail(icp + " != " + copy);
			}
			
			icp.m_password = "hdjrhtHGJH$#@";
			p = icp.getConfigParameter();
			gLogger.log(Level.INFO, "3. {0} -> {1}", new Object[]{icp, p});
			copy = new IcpProcessor(p);
			if(!icp.equals(copy))
			{
				fail(icp + " != " + copy);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail("Encountered unexpected " + e);
		}
	}
}

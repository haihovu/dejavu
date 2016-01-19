/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.mitel.icp.IcpDescriptor;
import com.mitel.icp.IcpProcessor;
import com.mitel.icp.IcpService;
import com.mitel.miutil.MiParameter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 *
 * @author haiv
 */
public class IcpDescriptorTest extends TestCase
{
	private static final Logger gLogger = Logger.getLogger(IcpDescriptorTest.class.getName());
	
	public IcpDescriptorTest(String testName)
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
	public void testSerialization()
	{
		IcpDescriptor icp = new IcpDescriptor("ABC", "1.2.9", "###", "123.123.123.123", "000.000.000.000", IcpService.ICP_TYPE_3300LiCS);
		icp.locateProcessor(IcpProcessor.ProcessorName.RTC).setUserId("mixml").setPassword("fjhskjdfhKJHGFISAF%$^%$%");
		MiParameter p = icp.getConfigParameter();
		gLogger.log(Level.INFO, "{0} -> {1}", new Object[]{icp, p});
		IcpDescriptor copy = new IcpDescriptor(p);
		if(!icp.equals(copy))
		{
			fail(icp + " != " + copy);
		}
		List<IcpProcessor> procs = icp.getProcessorList();
		for(IcpProcessor proc : procs)
		{
			IcpProcessor cproc = copy.locateProcessor(proc.getName());
			if((cproc == null)||(!cproc.equals(proc)))
			{
				fail(proc + " != " + cproc);
			}
		}
		procs = copy.getProcessorList();
		for(IcpProcessor proc : procs)
		{
			IcpProcessor cproc = icp.locateProcessor(proc.getName());
			if((cproc == null)||(!cproc.equals(proc)))
			{
				fail(proc + " != " + cproc);
			}
		}
		if(!icp.getMiNetVersion().equals(copy.getMiNetVersion()))
		{
			fail(icp.getMiNetVersion() + " != " + copy.getMiNetVersion());
		}
		if(!icp.getRegistrationCode().equals(copy.getRegistrationCode()))
		{
			fail(icp.getRegistrationCode() + " != " + copy.getRegistrationCode());
		}
		if(!icp.getType().equals(copy.getType()))
		{
			fail(icp.getType() + " != " + copy.getType());
		}
	}
}

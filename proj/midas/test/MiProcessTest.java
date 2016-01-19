/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.mitel.icp.SysCmd;
import com.mitel.miutil.MiProcessExecutor;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 * Tests for MiProcessExecutor and MiProcessMonitor
 * @author haiv
 */
public class MiProcessTest extends TestCase
{
	private static final Logger gLogger = Logger.getLogger(MiProcessTest.class.getName());
	
	public MiProcessTest(String testName)
	{
		super(testName);
	}
	
	// TODO add test methods here. The name must begin with 'test'. For example:
	// public void testHello() {}
	public void testMiProcessExecutor()
	{
		String cmd = "ifconfig";
		if(System.getProperty("os.name").matches(".*[Ww][Ii][Nn][Dd][Oo][Ww][Ss].*"))
		{
			cmd = "ipconfig";
		}
		try
		{
			MiProcessExecutor exec = new MiProcessExecutor("test", cmd);
			exec.launchProcess(true);
			exec.waitForCompletion(10000);
			System.out.println("stderr=" + exec.getStdErrStr());
			System.out.println("stdout=" + exec.getStdOutStr());
		}
		catch(InterruptedException ex)
		{
			fail("Interrupted");
		}
		catch(IOException ex)
		{
			fail(ex.getMessage());
		}
	}
}

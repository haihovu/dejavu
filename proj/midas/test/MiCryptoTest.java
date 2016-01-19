/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.mitel.miutil.MiCrypto;
import com.mitel.miutil.MiException;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test for MiCrypto
 *
 * @author haiv
 */
public class MiCryptoTest extends TestCase {

	private static final Logger gLogger = Logger.getLogger(MiCryptoTest.class.getName());

	public MiCryptoTest(String testName) {
		super(testName);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	// TODO add test methods here. The name must begin with 'test'. For example:
	// public void testHello() {}

	@SuppressWarnings("ResultOfObjectAllocationIgnored")
	public void testEncryptDecrypt() {
		String orig = "This is a Test payload";
		String password = "This@Is1GreatPasswordForYou!";
		byte[] salt1 = new byte[]{(byte) 0x12, (byte) 0x32, (byte) 0xff, (byte) 0x52, (byte) 0x34, (byte) 0x87, (byte) 0x99, (byte) 0x01};
		int iteration1 = 123;
		byte[] salt2 = new byte[]{(byte) 0xa0, (byte) 0xff, (byte) 0x10, (byte) 0xd1, (byte) 0xbf, (byte) 0x00, (byte) 0x12, (byte) 0xc2};
		int iteration2 = 1234;
		try {
			com.mitel.miutil.MiCrypto crypt = new com.mitel.miutil.MiCrypto(password);
			String encrypted = crypt.in(orig.getBytes(), salt1, iteration1);
			String decrypted = new String(crypt.out(encrypted, salt1, iteration1));
			gLogger.log(Level.INFO, "Orig = ''{0}'', encrypted = ''{1}'', decrypted = ''{2}''", new Object[]{orig, encrypted, decrypted});
			if(!orig.equals(decrypted)) {
				Assert.fail("Orig = '" + orig + "', decrypted ='" + decrypted + "'");
			}
			try {
				new com.mitel.miutil.MiCrypto(password).out(encrypted, salt2, iteration1);
				// This is unexpected
				Assert.fail("Was able to decrypt with different salt");
			}
			catch(MiException ex) {
				// This is expected
			}
			try {
				new com.mitel.miutil.MiCrypto(MiCrypto.KeyAlgorithm.PBEWithMD5AndDES, password).out(encrypted, salt1, iteration2);
				// This is unexpected
				Assert.fail("Was able to decrypt with different interation");
			}
			catch(MiException ex) {
				// This is expected
			}
			try {
				new com.mitel.miutil.MiCrypto(password).out(encrypted, salt2, iteration2);
				// This is unexpected
				Assert.fail("Was able to decrypt with different salt and interation");
			}
			catch(MiException ex) {
				// This is expected
			}
			try {
				new com.mitel.miutil.MiCrypto(MiCrypto.KeyAlgorithm.PBKDF2WithHmacSHA1, password).out(encrypted, salt1, iteration1);
				// This is unexpected
				Assert.fail("Was able to decrypt with different algorithm");
			}
			catch(MiException ex) {
				// This is expected
			}
			try {
				new com.mitel.miutil.MiCrypto(null);
				Assert.fail("Null password unexpectedly allowed");
			}
			catch(NullPointerException e) {
				// expected
			}
		} catch(Exception ex) {
			gLogger.log(Level.SEVERE, null, ex);
			Assert.fail("Encountered unexpected " + ex);
		}
	}
}

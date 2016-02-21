/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.netutil;

import org.dejavu.util.DjvException;
import org.dejavu.util.DjvExceptionUtil;
import org.dejavu.util.DjvLogMsg;
import org.dejavu.util.DjvSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import org.apache.commons.codec.binary.Base64;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

/**
 * This is a manager of a keystore used for maintaining trust certificates,
 * private/public keys (with certificates, one to be used for SSL client authentication),
 * plus zero or more secret keys for encryption.
 */
public class DjvCertManager {
	private static DjvCertManager defaultMgr;
	private static final Object gLock = new Object();
	
	private final KeyStore keyStore;
	private final String clientKeyAlias;
	private final char[] clientKeyPass;
	/**
	 * JCEKS is the most secured keystore implementation that came with Java as is.
	 * This used to be tightly controlled, but I think they've loosen the export
	 * laws in the US now regarding encryption topics.
	 */
	private static final String gSupportedStoreType = "JCEKS";
	/**
	 * Creates a certificate manager instance
	 * @param keyStore The keystore file against which certificates and keys are managed
	 * @param storePass The password to access the keystore content
	 * @param clientAlias Alias to a client key pair
	 * @param clientPass Password to the aforementioned client key pair
	 * @throws DjvException 
	 */
	private DjvCertManager(File keyStore, String storePass, String clientAlias, String clientPass) throws DjvException {
		this.keyStore = DjvCertStore.loadStore(keyStore, storePass, gSupportedStoreType);
		clientKeyAlias = clientAlias;
		clientKeyPass = clientPass.toCharArray();
	}
	
	/**
	 * Retrieves the managed keystore
	 * @return The keystore, not null.
	 */
	public KeyStore getKeyStore() {
		return keyStore;
	}
	
	/**
	 * Retrieves the client certificate (used for SSL authentication)
	 * @return The client certificate, or null if no client certificate is located
	 * inside the managed keystore.
	 * @throws KeyStoreException 
	 */
	public Certificate getClientCertificate() throws KeyStoreException {
		return keyStore.getCertificate(clientKeyAlias);
	}
	/**
	 * Retrieves the client's private key
	 * @return The client's private key, or null if no key can be located.
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException 
	 */
	public Key getClientKey() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		return keyStore.getKey(clientKeyAlias, clientKeyPass);
	}
	
	/**
	 * Retrieves the client certificate (used for SSL authentication), in PEM format
	 * @return The client certificate in PEM format, or empty string if no client
	 * certificate is located inside the managed keystore. Never null.
	 * @throws KeyStoreException
	 * @throws CertificateEncodingException  
	 */
	public String getClientCertificateAsPEM() throws KeyStoreException, CertificateEncodingException {
		Certificate cert = getClientCertificate();
		if(cert != null) {
			return certToPem(cert);
		}
		return "";
	}
	
	/**
	 * Extracts a PEM representation of a certificate
	 * @param cert The certificate to be processed
	 * @return The PEM format of the given certificate, not null.
	 * @throws CertificateEncodingException 
	 */
	public static String certToPem(Certificate cert) throws CertificateEncodingException {
		cert.getClass(); // Null check
		return "-----BEGIN CERTIFICATE-----\n" + new String(new Base64(64).encode(cert.getEncoded())) + "-----END CERTIFICATE-----";
	}
	
	public static Certificate pemToCert(String pem) throws CertificateException {
		String x = pem.replaceAll("-----BEGIN CERTIFICATE-----", "").replaceAll("-----END CERTIFICATE-----", "");
		byte[] c = Base64.decodeBase64(x);
		return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(c));
	}
	
	@SuppressWarnings("UseSpecificCatch")
	public static void main(String[] args) {
		DjvSystem.setLogLevel(2);
		try {
			CertAndKeyGen ckg = new CertAndKeyGen("RSA", "SHA1WithRSA");
			ckg.generate(1024);
			X500Name name = new X500Name("Hai Vu", "R&D", "Mitel", "Kanata", "ON", "CA");
			X509Certificate cert = ckg.getSelfCertificate(name, 24 * 3600/*Seconds*/);
			String pem = certToPem(cert);
			Certificate c = pemToCert(pem);
			if(cert.equals(c)) {
				DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, "Success: " + cert + "->" + pem + "->" + c);
			} else {
				DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, "Failed: " + cert + "->" + pem + "->" + c);
			}
		}
		catch(Exception ex) {
			DjvSystem.logError(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
		}
	}
	
	/**
	 * Retrieves the default SSL client socket factory backed by this certificate manager
	 * @return The default SSL client socket factory, not null.
	 * @throws SSLException 
	 */
	public static SSLSocketFactory getClientSocketFactory() throws SSLException {
		return DjvSocketSslFactory.getInstance().getClientSocketFactory();
	}
	
	/**
	 * Initializes the default SSL context and factory for creating client sockets,
	 * using a particular keystore. This keystore can also be used for storing secret
	 * keys and other certificates.
	 * @param keystore The keystore file
	 * @param storePass The password to access the content of the keystore file
	 * @param clientKeyAlias The alias to the client public/private key (with certificate)
	 * @param clientKeyPass The password to access the aforementioned client public/private key.
	 * @return A certificate manager containing the given keystore (loaded).
	 * @throws DjvException
	 * @throws SSLException 
	 */
	public static DjvCertManager initializeSslCertificate(File keystore, String storePass, String clientKeyAlias, String clientKeyPass) throws DjvException, SSLException {
		// Initializes the SSL context with the given key/trust information
		DjvSocketSslFactory.loadTrustStore(keystore, storePass, clientKeyPass.toCharArray(), gSupportedStoreType, (X509Certificate[] par1) -> true);
		synchronized(gLock) {
			defaultMgr = new DjvCertManager(keystore, storePass, clientKeyAlias, clientKeyPass);
			return defaultMgr;
		}
	}
	/**
	 * Retrieves the certificate manager created in a previous call to initializeSslCertificate().
	 * @return The default certificate manager, or null if none was created, yet.
	 */
	public static DjvCertManager getDefaultCertManager() {
		synchronized(gLock) {
			return defaultMgr;
		}
	}
}

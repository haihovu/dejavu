/*
 * MiSocketSsl.java
 *
 * Created on April 26, 2004, 5:42 PM
 */
package org.dejavu.netutil;

import org.dejavu.util.DjvException;
import org.dejavu.util.DjvExceptionUtil;
import org.dejavu.util.DjvLogMsg.Category;
import org.dejavu.util.DjvSystem;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

/**
 * <p>This code is the culmination of many hours of reading many badly written
 * and disjointed partial documents found on the web and text books, and just
 * pure hacking (trial and errors, mostly errors) on the subject SSL in
 * Java.</p>
 *
 * <p>This represents the repository of SSL socket factories (a factory of
 * factories if you will) and is implemented as a singleton.</p>
 *
 * @author haiv
 */
public class DjvSocketSslFactory {

	/**
	 * Our SSL context
	 */
	private final SSLContext m_SslContext;
	/**
	 * This is a singleton
	 */
	private static DjvSocketSslFactory m_Singleton;
	/**
	 * Used for protecting access to static attributes such as the singleton
	 * reference.
	 */
	private static final Object s_StaticLock = new Object();

	/**
	 * Creates a new instance of MiSocketSslFactory for one-way authentication
	 *
	 * @param storeFile An optional store file with which to load the key/trust
	 * store data. If null then an empty key/trust store is created and used.
	 * @param storePass The password for use with the key/trust store
	 * @param storeType Optional store type to use, if null then the default
	 * type will be used. This must match the type of the key/trust store
	 * specified by storeFile, if exists. E.g. JCEKS.
	 * @param listener Asynchronous listener interface for handling trust
	 * negotiation events.
	 * @throws SSLException
	 */
	public DjvSocketSslFactory(File storeFile, String storePass, String storeType, DjvSslTrustListener listener) throws SSLException {
		this(storeFile, storePass, null, storeType, listener);
	}

	/**
	 * Creates a new instance of MiSocketSslFactory
	 *
	 * @param storeFile An optional store file with which to load the key/trust
	 * store data. If null then an empty key/trust store is created and used.
	 * @param storePass The password for use with the key/trust store
	 * @param keyPass Optional password to a keypair entry for two-way
	 * authentication. Normally only one-way authentication is required, so this
	 * can be null in those cases.
	 * @param storeType Optional store type to use, if null then the default
	 * type will be used. This must match the type of the key/trust store
	 * specified by storeFile, if exists. E.g. JCEKS.
	 * @param listener Asynchronous listener interface for handling trust
	 * negotiation events.
	 * @throws SSLException
	 */
	public DjvSocketSslFactory(File storeFile, String storePass, char[] keyPass, String storeType, DjvSslTrustListener listener) throws SSLException {
		try {
			// Acquire the SSL context and claim it as ours
			m_SslContext = SSLContext.getInstance("SSL");
			if(null == m_SslContext) {
				throw new SSLException("Failed to acquire SSL context");
			}

			KeyManager[] keyMgrs = null;
			if((keyPass != null) && (keyPass.length > 0)) {
				KeyManagerFactory keyMgrFactory = KeyManagerFactory.getInstance("SunX509");
				keyMgrFactory.init(DjvCertStore.loadStore(storeFile, storePass, storeType), keyPass);
				keyMgrs = keyMgrFactory.getKeyManagers();
			}

			// Build up a trust manager array (of one) for our context
			TrustManager[] trustmgr = new TrustManager[1];
			trustmgr[0] = new MiTrustManager(storeFile, storePass, storeType, listener);

			// Initialize our SSL context with the trust managers built up previously
			m_SslContext.init(keyMgrs, trustmgr, null);
		}
		catch(NoSuchAlgorithmException e) {
			throw new SSLException(e);
		}
		catch(KeyManagementException e) {
			throw new SSLException(e);
		}
		catch(DjvException ex) {
			throw new SSLException(ex);
		}
		catch(UnrecoverableKeyException ex) {
			throw new SSLException(ex);
		}
		catch(KeyStoreException ex) {
			throw new SSLException(ex);
		}
	}

	/**
	 * Creates a new instance of MiSocketSslFactory for one-way authentication
	 *
	 * @param storePath A URL from which to load the key/trust store data. Must
	 * not be null.
	 * @param storePass The password for use with the key/trust store
	 * @param storeType Optional store type to use, if null then the default
	 * type will be used. This must match the type of the key/trust store
	 * specified by storeFile, if exists.
	 * @param listener Asynchronous listener interface for handling trust
	 * negotiation events.
	 * @throws SSLException
	 */
	public DjvSocketSslFactory(URL storePath, String storePass, String storeType, DjvSslTrustListener listener) throws SSLException {
		this(storePath, storePass, null, storeType, listener);
	}

	/**
	 * Creates a new instance of MiSocketSslFactory
	 *
	 * @param storePath A URL from which to load the key/trust store data. Must
	 * not be null.
	 * @param storePass The password for use with the key/trust store
	 * @param keyPass Optional password to a keypair entry for two-way
	 * authentication. Normally only one-way authentication is required, so this
	 * can be null in those cases.
	 * @param storeType Optional store type to use, if null then the default
	 * type will be used. This must match the type of the key/trust store
	 * specified by storeFile, if exists.
	 * @param listener Asynchronous listener interface for handling trust
	 * negotiation events.
	 * @throws SSLException
	 */
	public DjvSocketSslFactory(URL storePath, String storePass, char[] keyPass, String storeType, DjvSslTrustListener listener) throws SSLException {
		try {
			// Acquire the SSL context and claim it as ours
			m_SslContext = SSLContext.getInstance("SSL");
			if(null == m_SslContext) {
				throw new SSLException("Failed to acquire SSL context");
			}

			KeyManager[] keyMgrs = null;
			if((keyPass != null) && (keyPass.length > 0)) {
				KeyManagerFactory keyMgrFactory = KeyManagerFactory.getInstance("SunX509");
				keyMgrFactory.init(DjvCertStore.loadStore(storePath, storePass, storeType), keyPass);
				keyMgrs = keyMgrFactory.getKeyManagers();
			}

			// Build up a trust manager array (of one) for our context
			TrustManager[] trustMgrs = new TrustManager[1];
			trustMgrs[0] = new MiTrustManager(storePath, storePass, storeType, listener);

			// Initialize our SSL context with the trust managers built up previously
			m_SslContext.init(keyMgrs, trustMgrs, null);
		}
		catch(KeyManagementException ex) {
			throw new SSLException(ex);
		}
		catch(NoSuchAlgorithmException ex) {
			throw new SSLException(ex);
		}
		catch(DjvException ex) {
			throw new SSLException(ex);
		}
		catch(KeyStoreException ex) {
			throw new SSLException(ex);
		}
		catch(UnrecoverableKeyException ex) {
			throw new SSLException(ex);
		}
	}

	public static void main(String[] args) {
		DjvSystem.setLogLevel(2);
		for(Provider provider : Security.getProviders()) {
			DjvSystem.logInfo(Category.DESIGN, "Found provider " + provider);
		}
		DjvSystem.logInfo(Category.DESIGN, "Default trust manager algorith " + TrustManagerFactory.getDefaultAlgorithm());
		DjvSystem.logInfo(Category.DESIGN, "Default key manager algorith " + KeyManagerFactory.getDefaultAlgorithm());
	}

	/**
	 * Retrieves the SSL context of this factory.
	 *
	 * @return The SSL context, not null.
	 */
	public SSLContext getSslContext() {
		return m_SslContext;
	}

	/**
	 * Retrieves the server socket factory.
	 *
	 * @return The SSL server socket factory, not null.
	 */
	public SSLServerSocketFactory getServerSocketFactory() {
		return m_SslContext.getServerSocketFactory();
	}

	/**
	 * Retrieves the client socket factory.
	 *
	 * @return The SSL client socket factory, not null.
	 */
	public SSLSocketFactory getClientSocketFactory() {
		return m_SslContext.getSocketFactory();
	}

	/**
	 * Loads a key/trust store into the singleton of this module. This should be
	 * invoked prior to using any other functionality.
	 *
	 * @param storeFile An optional store file with which to load the key/trust
	 * store data. If null then an empty key/trust store is created and used.
	 * @param keyStorePass The password for use with the key/trust store
	 * @param storeType Optional store type to use, if null then the default
	 * type will be used. This must match the type of the key/trust store
	 * specified by storeFile, if exists.
	 * @param listener Asynchronous listener interface for handling trust
	 * negotiation events.
	 * @throws SSLException
	 */
	public static void loadTrustStore(File storeFile, String keyStorePass, String storeType, DjvSslTrustListener listener) throws SSLException {
		loadTrustStore(storeFile, keyStorePass, null, storeType, listener);
	}

	/**
	 * Loads a key/trust store into the singleton of this module. This should be
	 * invoked prior to using any other functionality.
	 *
	 * @param storeFile An optional store file with which to load the key/trust
	 * store data. If null then an empty key/trust store is created and used.
	 * @param keyStorePass The password for use with the key/trust store
	 * @param keypass Optional keypass for the public certificate to be used.
	 * @param storeType Optional store type to use, if null then the default
	 * type will be used. This must match the type of the key/trust store
	 * specified by storeFile, if exists.
	 * @param listener Asynchronous listener interface for handling trust
	 * negotiation events.
	 * @throws SSLException
	 */
	public static void loadTrustStore(File storeFile, String keyStorePass, char[] keypass, String storeType, DjvSslTrustListener listener) throws SSLException {
		synchronized(s_StaticLock) {
			if(m_Singleton == null) {
				m_Singleton = new DjvSocketSslFactory(storeFile, keyStorePass, keypass, storeType, listener);
			}
		}
	}

	/**
	 * Retrieves the singleton instance of this module.
	 *
	 * @return The singleton instance
	 * @throws SSLException
	 */
	public static DjvSocketSslFactory getInstance() throws SSLException {
		synchronized(s_StaticLock) {
			if(m_Singleton == null) {
				// Use default trust store
				m_Singleton = new DjvSocketSslFactory(DjvSocketSslFactory.class.getClassLoader().getResource("truststore"), "9x2F7ggYA81", null, "JCEKS", null/*no listener*/);
			}
			return m_Singleton;
		}
	}

	/**
	 * Custom implementation of an X509 trust manager.
	 */
	private static class MiTrustManager implements X509TrustManager {

		private X509TrustManager m_RealTrustMgr;

		/**
		 * Creates a new trust manager instance.
		 *
		 * @param storeFile An optional store file with which to load the
		 * key/trust store data. If null then an empty key/trust store is
		 * created and used.
		 * @param storePass The password for use with the key/trust store
		 * @param storeType Optional store type to use, if null then the default
		 * type will be used. This must match the type of the key/trust store
		 * specified by storeFile, if exists.
		 * @param listener Asynchronous listener interface for handling trust
		 * negotiation events.
		 * @throws SSLException
		 */
		private MiTrustManager(File storeFile, String storePass, String storeType, DjvSslTrustListener listener)
			throws SSLException {
			m_StoreUrl = null;
			m_Listener = listener;
			m_StoreFile = storeFile;
			m_StorePass = storePass;
			try {
				// Grab the built-in SUN X509 trust manager factory
				TrustManagerFactory myFactory = TrustManagerFactory.getInstance("SunX509");
				m_KeyStore = DjvCertStore.createStore(m_StoreFile, m_StorePass, storeType);

				// Initialize the trust manager factory with our keystore
				myFactory.init(m_KeyStore);

				// Now acquire a trust manager from the factory
				m_RealTrustMgr = (X509TrustManager) myFactory.getTrustManagers()[0];
			}
			catch(Exception e) {
				throw new SSLException("Failed to create trust manager: " + e.getMessage());
			}
		}

		/**
		 * Creates a new trust manager instance.
		 *
		 * @param storePath A URL from which to load the key/trust store data.
		 * Must not be null.
		 * @param storePass The password for use with the key/trust store
		 * @param storeType Optional store type to use, if null then the default
		 * type will be used. This must match the type of the key/trust store
		 * specified by storePath.
		 * @param listener Asynchronous listener interface for handling trust
		 * negotiation events.
		 * @throws SSLException
		 */
		private MiTrustManager(URL storePath, String storePass, String storeType, DjvSslTrustListener listener)
			throws SSLException {
			storePath.getClass();
			m_StoreFile = null;
			m_Listener = listener;
			m_StoreUrl = storePath;
			m_StorePass = storePass;
			try {
				// Grab the built-in SUN X509 trust manager factory
				TrustManagerFactory myFactory = TrustManagerFactory.getInstance("SunX509");

				InputStream iStream = m_StoreUrl.openStream();
				try {
					m_KeyStore = DjvCertStore.createStore(iStream, m_StorePass, storeType);
				} finally {
					iStream.close();
				}

				// Initialize the trust manager factory with our keystore
				myFactory.init(m_KeyStore);

				// Now acquire a trust manager from the factory
				m_RealTrustMgr = (X509TrustManager) myFactory.getTrustManagers()[0];
			}
			catch(Exception e) {
				throw new SSLException("Failed to create trust manager: " + e.getMessage());
			}
		}

		@Override
		public void checkClientTrusted(X509Certificate[] par1, String par2) throws CertificateException {
			if(null == m_RealTrustMgr) {
				throw new CertificateException("No trust manager");
			}

			m_RealTrustMgr.checkClientTrusted(par1, par2);
		}

		@Override
		public void checkServerTrusted(X509Certificate[] par1, String par2) throws CertificateException {
			if(null == m_RealTrustMgr) {
				throw new CertificateException("No trust manager");
			}
			try {
				m_RealTrustMgr.checkServerTrusted(par1, par2);
			}
			catch(CertificateException e) {
				if(m_Listener != null) {
					if(m_Listener.challengeTrust(par1)) {
						try {
							// Stuff the new certificate into the trust store
							KeyStore.TrustedCertificateEntry newEntry = new KeyStore.TrustedCertificateEntry(par1[0]);
							m_KeyStore.setEntry(par1[0].getIssuerX500Principal().getName(), newEntry, null);

							// Re initialize the trust manager
							TrustManagerFactory myFactory = TrustManagerFactory.getInstance("SunX509");
							myFactory.init(m_KeyStore);
							m_RealTrustMgr = (X509TrustManager) myFactory.getTrustManagers()[0];

							// And save the store if we can
							if(null != m_StoreFile) {
								DjvCertStore.saveStore(m_KeyStore, m_StoreFile, m_StorePass);
							}
						}
						catch(Exception ex) {
							DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
							throw new CertificateException(ex);
						}
						return;
					}
				}
				throw e;
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			if(null == m_RealTrustMgr) {
				return null;
			}
			return m_RealTrustMgr.getAcceptedIssuers();
		}
		private final DjvSslTrustListener m_Listener;
		/**
		 * USed with stores that have read/write access.
		 */
		private final File m_StoreFile;
		/**
		 * Used with read-only store.
		 */
		private final URL m_StoreUrl;
		private final KeyStore m_KeyStore;
		private final String m_StorePass;
	}
}

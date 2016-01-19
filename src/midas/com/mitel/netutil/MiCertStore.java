/*
 * MiCertStore.java
 *
 * Created on December 4, 2006, 8:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.mitel.netutil;

import com.mitel.miutil.MiException;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Simplifies certificate management.
 * @author haiv
 */
public class MiCertStore
{

	/** Creates a new instance of MiCertStore */
	private MiCertStore()
	{
	}

	/**
	 * Loads a key store from a key store file of type JCEKS.
	 * @param storeFile The key store file to load the key store from.
	 * @param storePass The key store password.
	 * @return The newly loaded key store.
	 * @throws com.mitel.miutil.MiException If the key store cannot be loaded for any reason.
	 */
	public static KeyStore loadStore(File storeFile, String storePass) throws MiException
	{
		return loadStore(storeFile, storePass, "JCEKS");
	}

	/**
	 * Creates a key store of type JCEKS, and load it up with information from a key store file.
	 * @param storeFile The file for loading the key store information. If null then an
	 * empty trust store will be created.
	 * @param storePass Password for the key store.
	 * @return The newly created key store. Not null.
	 * @throws java.security.KeyStoreException
	 * @throws java.security.NoSuchAlgorithmException
	 * @throws java.security.cert.CertificateException
	 * @throws java.io.IOException
	 */
	public static KeyStore createStore(File storeFile, String storePass) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
	{
		return createStore(storeFile, storePass, "JCEKS");
	}

	/**
	 * Creates a key store, and load it up with information from an input stream.
	 * @param iStream The input stream from which to load key store information.
	 * @param storePass The password to the key store.
	 * @return The newly created key store. Not null.
	 * @throws java.security.KeyStoreException
	 * @throws java.io.IOException
	 * @throws java.security.NoSuchAlgorithmException
	 * @throws java.security.cert.CertificateException
	 */
	public static KeyStore createStore(InputStream iStream, String storePass) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
	{
		return createStore(iStream, storePass, "JCEKS");
	}

	/**
	 * Creates an empty store of type JCEKS.
	 * @return The newly created key store. Not null.
	 * @throws java.security.KeyStoreException
	 * @throws java.io.IOException
	 * @throws java.security.NoSuchAlgorithmException 
	 * @throws java.security.cert.CertificateException
	 */
	public static KeyStore createStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
	{
		return createStore("JCEKS");
	}

	/**
	 * Loads a key store from a key store file.
	 * @param storeFile The key store file to load the key store from.
	 * @param storePass The key store password.
	 * @param storeType The optional store type to create, if null then the default will be used.
	 * @return The newly loaded key store.
	 * @throws com.mitel.miutil.MiException If the key store cannot be loaded for any reason.
	 */
	public static KeyStore loadStore(File storeFile, String storePass, String storeType) throws MiException
	{
		try
		{
			InputStream iStream = new FileInputStream(storeFile);
			try
			{
				if(storeType == null)
				{
					if(MiSystem.diagnosticEnabled()) {
						MiSystem.logInfo(Category.DESIGN, "Create a keystore using default keystore type " + KeyStore.getDefaultType());
					}
				}
				KeyStore retValue = KeyStore.getInstance(storeType != null ? storeType : KeyStore.getDefaultType());
				retValue.load(iStream, storePass.toCharArray());
				return retValue;
			}
			catch(Exception ex)
			{
				throw new MiException(ex);
			}
			finally
			{
				try
				{
					iStream.close();
				}
				catch(IOException ex)
				{
				}
			}
		}
		catch(FileNotFoundException ex)
		{
			throw new MiException(ex);
		}
	}

	/**
	 * Loads a key store from a key store file.
	 * @param keystore The URL to the key store to be loaded.
	 * @param storePass The key store password.
	 * @param storeType The optional store type to create, if null then the default will be used.
	 * @return The newly loaded key store.
	 * @throws com.mitel.miutil.MiException If the key store cannot be loaded for any reason.
	 */
	public static KeyStore loadStore(URL keystore, String storePass, String storeType) throws MiException
	{
		try
		{
			InputStream iStream = keystore.openStream();
			try
			{
				if(storeType == null)
				{
					if(MiSystem.diagnosticEnabled()) {
						MiSystem.logInfo(Category.DESIGN, "Create a keystore using default keystore type " + KeyStore.getDefaultType());
					}
				}
				KeyStore retValue = KeyStore.getInstance(storeType != null ? storeType : KeyStore.getDefaultType());
				retValue.load(iStream, storePass.toCharArray());
				return retValue;
			}
			catch(Exception ex)
			{
				throw new MiException(ex);
			}
			finally
			{
				try
				{
					iStream.close();
				}
				catch(IOException ex)
				{
				}
			}
		}
		catch(IOException ex) {
			throw new MiException(ex);
		}
	}

	/**
	 * Creates a key store, and load it up with information from a key store file.
	 * @param storeFile The file for loading the key store information. If null then an
	 * empty trust store will be created.
	 * @param storePass Password for the key store.
	 * @param storeType The optional store type to create, if null then the default will be used.
	 * @return The newly created key store. Not null.
	 * @throws java.security.KeyStoreException
	 * @throws java.security.NoSuchAlgorithmException
	 * @throws java.security.cert.CertificateException
	 * @throws java.io.IOException
	 */
	public static KeyStore createStore(File storeFile, String storePass, String storeType) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
	{
		if(storeType == null)
		{
			if(MiSystem.diagnosticEnabled()) {
				MiSystem.logInfo(Category.DESIGN, "Create a keystore using default keystore type " + KeyStore.getDefaultType());
			}
		}
		KeyStore retValue = KeyStore.getInstance(storeType != null ? storeType : KeyStore.getDefaultType());
		boolean succeeded = false;
		try
		{
			if(storeFile != null)
			{
				InputStream iStream = new FileInputStream(storeFile);
				try {
					retValue.load(iStream, storePass.toCharArray());
					succeeded = true;
				} finally {
					iStream.close();
				}
			}
			else if(MiSystem.diagnosticEnabled())
			{
				MiSystem.logInfo(Category.DESIGN, "No store file given, will create an empty store");
			}
		}
		catch(IOException ex)
		{
			// Create a new empty store
			MiSystem.logInfo(Category.MAINTENANCE, "Failed to load key store " 
				+ storeFile + " will create an empty store");
		}
		if(!succeeded)
		{
			retValue.load(null, storePass.toCharArray());
		}
		return retValue;
	}

	/**
	 * Creates a key store, and load it up with information from an input stream.
	 * @param iStream The input stream from which to load key store information.
	 * @param storePass The password to the key store.
	 * @param storeType The optional store type to create, if null then the default will be used.
	 * @return The newly created key store. Not null.
	 * @throws java.security.KeyStoreException
	 * @throws java.io.IOException
	 * @throws java.security.NoSuchAlgorithmException
	 * @throws java.security.cert.CertificateException
	 */
	public static KeyStore createStore(InputStream iStream, String storePass, String storeType) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
	{
		if(storeType == null)
		{
			if(MiSystem.diagnosticEnabled()) {
				MiSystem.logInfo(Category.DESIGN, "Create a keystore using default keystore type " + KeyStore.getDefaultType());
			}
		}
		KeyStore retValue = KeyStore.getInstance(storeType != null ? storeType : KeyStore.getDefaultType());
		retValue.load(iStream, storePass.toCharArray());
		return retValue;
	}

	/**
	 * Creates an empty store.
	 * @param storeType The optional store type to create, if null then the default will be used.
	 * @return The newly created key store. Not null.
	 * @throws java.security.KeyStoreException
	 * @throws java.io.IOException
	 * @throws java.security.NoSuchAlgorithmException 
	 * @throws java.security.cert.CertificateException
	 */
	public static KeyStore createStore(String storeType) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
	{
		if(storeType == null)
		{
			if(MiSystem.diagnosticEnabled()) {
				MiSystem.logInfo(Category.DESIGN, "Create a keystore using default keystore type " + KeyStore.getDefaultType());
			}
		}
		KeyStore retValue = KeyStore.getInstance(storeType != null ? storeType : KeyStore.getDefaultType());
		retValue.load(null);
		return retValue;
	}

	/**
	 * Saves the content of the given key store into a key store file.
	 * @param store The key store whose content is to be saved.
	 * @param storeFile The key store file to save the content.
	 * @param storePass The password for the key store.
	 * @throws java.security.KeyStoreException
	 * @throws java.io.IOException
	 * @throws java.security.NoSuchAlgorithmException
	 * @throws java.security.cert.CertificateException
	 */
	public static void saveStore(KeyStore store, File storeFile, String storePass) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
	{
		OutputStream oStream = new FileOutputStream(storeFile);
		try {
			saveStore(store, oStream, storePass);
		} finally {
			oStream.close();
		}
	}

	/**
	 * Saves the content of the given key store into an output stream.
	 * @param store The key store whose content is to be saved.
	 * @param oStream The output stream into which to save the key store content.
	 * @param storePass The password for the key store.
	 * @throws java.security.KeyStoreException
	 * @throws java.io.IOException
	 * @throws java.security.NoSuchAlgorithmException
	 * @throws java.security.cert.CertificateException
	 */
	public static void saveStore(KeyStore store, OutputStream oStream, String storePass) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
	{
		store.store(oStream, storePass.toCharArray());
	}
}

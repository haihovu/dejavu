/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.util;

import org.dejavu.util.DjvLogMsg.Category;
import org.dejavu.netutil.DjvCertStore;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.spec.KeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

/**
 * Utility class dealing with encryption functionality for MiDAS.
 *
 * @author haiv
 */
public class DjvCrypto {
	private static final Pattern gSaltPattern = Pattern.compile("^salt=(.+)$");
	private static final Pattern gIvPattern = Pattern.compile("^iv\\s+=(.+)$");
	private static final Pattern gKeyPattern = Pattern.compile("^key=(.+)$");
	/**
	 * Supported password-based key algorithm
	 */
	public static enum KeyAlgorithm {
		PBEWithMD5AndDES,
		PBKDF2WithHmacSHA1,
		PBKDF2WithHmacSHA256;
	}
	/**
	 * The encryption information
	 */
	public static class EncryptionInfo {
		public final char [] password;
		public final byte [] key;
		private byte [] salt;
		private byte [] iv;
		private int iters;
		private byte [] encryptedData;

		/**
		 * Retrieves the salt value
		 * @return The salt value, null means no salt.
		 */
		@SuppressWarnings("ReturnOfCollectionOrArrayField")
		public byte[] getSalt() {
			return salt;
		}

		/**
		 * Specifies the salt value.
		 * @param salt The salt value, null means no salt.
		 * @return This object
		 */
		@SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
		public EncryptionInfo setSalt(byte[] salt) {
			synchronized(this) {
				this.salt = salt;
			}
			return this;
		}

		/**
		 * Retrieves the iteration count value
		 * @return The iteration count value, must be greater than zero.
		 */
		public int getIters() {
			return iters;
		}

		/**
		 * Specifies the iteration count value.
		 * @param iters The iteration count value, must be greater than zero.
		 * @return This object
		 */
		public EncryptionInfo setIters(int iters) {
			synchronized(this) {
				this.iters = iters;
			}
			return this;
		}

		/**
		 * Retrieves the initialisation vector.
		 * @return The initialisation vector, or null if one is not specified.
		 */
		@SuppressWarnings("ReturnOfCollectionOrArrayField")
		public byte[] getIv() {
			synchronized(this) {
				return iv;
			}
		}

		/**
		 * Specifies the initialisation vector.
		 * @param iv The initialisation vector, or null if one is not specified.
		 * @return This object
		 */
		@SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
		public EncryptionInfo setIv(byte[] iv) {
			synchronized(this) {
				this.iv = iv;
			}
			return this;
		}

		/**
		 * Retrieves the encrypted data.
		 * @return The encrypted data, or null if one is not specified.
		 */
		@SuppressWarnings("ReturnOfCollectionOrArrayField")
		public byte[] getEncryptedData() {
			synchronized(this) {
				return encryptedData;
			}
		}

		/**
		 * Specifies the encrypted data.
		 * @param encryptedData The encrypted data, or null if one is not specified.
		 * @return This object
		 */
		@SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
		public EncryptionInfo setEncryptedData(byte[] encryptedData) {
			synchronized(this) {
				this.encryptedData = encryptedData;
			}
			return this;
		}
		/**
		 * Creates a new piece of encryption information.
		 * @param key The key associated with this encryption info.
		 */
		public EncryptionInfo(byte [] key) {
			super();
			this.key = key;
			password = null;
		}
		/**
		 * Creates a new piece of encryption information.
		 * @param password The password associated with this encryption info.
		 */
		public EncryptionInfo(char [] password) {
			super();
			this.key = null;
			this.password = password;
		}
		
		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder(512).append("{");
			synchronized(this) {
				if(password != null) {
					ret.append("password:'").append(password).append("',");
				}
				if(key != null) {
					ret.append("key:'").append(bytesToString(key)).append("',");
				}
				if(salt != null) {
					ret.append("salt:'").append(bytesToString(salt)).append("',");
				}
				if(iv != null) {
					ret.append("iv:'").append(bytesToString(iv)).append("',");
				}
				if(iters > 0) {
					ret.append("iters:'").append(iters).append("',");
				}
			}
			return ret.append("}").toString();
		}
	}
	
	/**
	 * The secret key
	 */
	private final Key key;
	/**
	 * The secret key factory
	 */
	private final SecretKeyFactory keyFactory;
	/**
	 * The password
	 */
	private final char [] password;
	/**
	 * The encryption algorithm, e.g. AES, ...
	 */
	private final String algorithm;
	/**
	 * The optional key length, -1 if not specified.
	 */
	private final int keyLen;
	/**
	 * The string 'PBEWithMD5AndDES', in UTF-8 encoding.
	 */
	private static final String PBEWithMD5AndDES = "PBEWithMD5AndDES";
	/**
	 * The string 'PBKDF2WithHmacSHA1', in UTF-8 encoding.
	 */
	private static final String PBKDF2WithHmacSHA1 = "PBKDF2WithHmacSHA1";
	/**
	 * The string 'PBKDF2WithHmacSHA256', in UTF-8 encoding.
	 */
	private static final String PBKDF2WithHmacSHA256 = "PBKDF2WithHmacSHA256";
	/**
	 * The string 'AES', in UTF-8 encoding.
	 */
	private static final String AES = "AES";
	/**
	 * The selected cipher transformation
	 */
	private final String cipherTransformation;

	/**
	 * Creates a crypto object for PBEWithMD5AndDES password-based encryption.
	 * This is not as secured as the newer PBKDF2WithHmacSHA1 or PBKDF2WithHmacSHA256
	 * @param password The password used for encryption/decryption.
	 * @throws DjvException
	 */
	@SuppressWarnings("UseSpecificCatch")
	public DjvCrypto(String password) throws DjvException {
		password.getClass(); // Null check
		try {
			// Generate a secrete key from the password
			keyFactory = SecretKeyFactory.getInstance(PBEWithMD5AndDES);
			key = null;
			this.password = password.toCharArray();
			algorithm = null;
			keyLen = -1;
			cipherTransformation = PBEWithMD5AndDES;
		}
		catch(Exception ex) {
			throw new DjvException(ex);
		}
	}

	/**
	 * Creates a crypto object for password-based encryption.
	 *
	 * @param alg Secret key algorithm
	 * @param password The password used for encryption/decryption.
	 * @throws DjvException
	 */
	@SuppressWarnings("UseSpecificCatch")
	public DjvCrypto(KeyAlgorithm alg, String password) throws DjvException {
		password.getClass(); // Null check
		this.password = password.toCharArray();
		try {
			// Generate a secrete key from the password
			switch(alg) {
				case PBEWithMD5AndDES:
					cipherTransformation = PBEWithMD5AndDES;
					keyFactory = SecretKeyFactory.getInstance(PBEWithMD5AndDES);
					key = null;
					algorithm = null;
					keyLen = -1;
					break;
					
				case PBKDF2WithHmacSHA1:
					// AES/CBC/PKCS5Padding
					cipherTransformation = "AES/CBC/PKCS5Padding";
					keyFactory = SecretKeyFactory.getInstance(PBKDF2WithHmacSHA1);
					key = null;
					algorithm = AES;
					keyLen = 128;
					break;	
					
				case PBKDF2WithHmacSHA256:
				default:
					/*
					// AES/EBC/PKCS5Padding
					b = new byte[]{0x41,0x45,0x53,0x2f,0x45,0x42,0x43,0x2f,0x50,0x4b,0x43,0x53,0x35,0x50,0x61,0x64,0x64,0x69,0x6e,0x67};
					*/
					// AES/CBC/PKCS5Padding
					cipherTransformation = "AES/CBC/PKCS5Padding";
					keyFactory = SecretKeyFactory.getInstance(PBKDF2WithHmacSHA256);
					key = null;
					algorithm = AES;
					keyLen = 256;
					break;	
			}
		}
		catch(Exception ex) {
			throw new DjvException(ex);
		}
	}

	/**
	 * Creates a crypto object for secret key-based encryption
	 *
	 * @param keystore The keystore file containing a secret key with AES algorithm
	 * (128-bit key)
	 * @param keyAlias The alias
	 * @param storePass The store pass
	 * @param keyPass The key pass
	 * @throws DjvException
	 */
	@SuppressWarnings("UseSpecificCatch")
	public DjvCrypto(File keystore, String keyAlias, String storePass, String keyPass) throws DjvException {
		try {
			keystore.getClass();
			keyAlias.getClass();
			storePass.getClass();
			keyPass.getClass();
			KeyStore ct = DjvCertStore.loadStore(keystore, storePass);
			Key k = ct.getKey(keyAlias, keyPass.toCharArray());
			if(k == null) {
				throw new DjvException("Requested key not found");
			}
			password = null;
			keyFactory = null;
			key = new SecretKeySpec(k.getEncoded(), AES);
			cipherTransformation = AES; // Select AES algorithm
			algorithm = null;
			keyLen = -1;
		}
		catch(Exception ex) {
			throw new DjvException(ex);
		}
	}

	/**
	 * Creates a crypto object for secret key-based encryption
	 *
	 * @param keystore The keystore containing a secret key with AES algorithm (128-bit
	 * key)
	 * @param keyAlias The secret key alias
	 * @param keyPass The secret key password
	 * @throws DjvException
	 */
	@SuppressWarnings("UseSpecificCatch")
	public DjvCrypto(KeyStore keystore, String keyAlias, String keyPass) throws DjvException {
		try {
			keystore.getClass();
			keyAlias.getClass();
			keyPass.getClass();
			Key k = keystore.getKey(keyAlias, keyPass.toCharArray());
			if(k == null) {
				throw new DjvException("Requested key not found");
			}
			password = null;
			keyFactory = null;
			key = new SecretKeySpec(k.getEncoded(), AES);
			cipherTransformation = AES; // Select AES algorithm
			algorithm = null;
			keyLen = -1;
		}
		catch(Exception ex) {
			throw new DjvException(ex);
		}
	}

	/**
	 * Encrypts an input buffer and then present the result in a Base64 string.
	 * This is used with secret-key-based encryption, for password-based encryption,
	 * use the method with the salt value.
	 * @param input The byte array containing information to be encrypted.
	 * @return Encrypted information
	 * @throws DjvException
	 */
	@SuppressWarnings("UseSpecificCatch")
	public String in(byte[] input) throws DjvException {
		try {
			// Get the appropriate cipher
			Cipher cipher = Cipher.getInstance(cipherTransformation);
			synchronized(cipher) {
				if(key == null) {
					throw new DjvException("No key specified");
				}
				cipher.init(Cipher.ENCRYPT_MODE, key);

				// The base64 encoder is from the PostgreSQL driver.
				byte[] encoded = cipher.doFinal(input);
				return Base64.encodeBase64String(encoded);
			}
		}
		catch(Exception e) {
			DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
			throw new DjvException(e);
		}
	}

	/**
	 * Decrypts an input Base64 string into the original byte array data.
	 * This is used with secret-key-based encryption, for password-based use
	 * the method with salt value.
	 * @param txt The base64 text to be decrypted.
	 * @return The unencrypted byte array associated with the given encrypted
	 * text. Non-null.
	 * @throws DjvException
	 */
	@SuppressWarnings("UseSpecificCatch")
	public byte[] out(String txt) throws DjvException {
		try {
			// Get the appropriate cipher
			Cipher cipher = Cipher.getInstance(cipherTransformation);
			synchronized(cipher) {
				if(key != null) {
					cipher.init(Cipher.DECRYPT_MODE, key);
				}
				else {
					throw new DjvException("No key specified");
				}

				// The base64 encoder is from the PostgreSQL driver.
				return cipher.doFinal(Base64.decodeBase64(txt));
			}
		}
		catch(Exception e) {
			throw new DjvException(e);
		}
	}

	/**
	 * Encrypts an input buffer and then present the result in a Base64 string.
	 * This is used with password-based encryption, for secret-key-based encryption
	 * use the method without the salt value.
	 * @param input The byte array containing information to be encrypted.
	 * @param salt The Salt value for use with key gen
	 * @param iters The iteration count value for use with key gen
	 * @return Encrypted information
	 * @throws DjvException
	 */
	@SuppressWarnings("UseSpecificCatch")
	public String in(byte[] input, byte [] salt, int iters) throws DjvException {
		try {
			// Get the appropriate cipher
			Cipher cipher = Cipher.getInstance(cipherTransformation);
			synchronized(cipher) {
				if(password == null) {
					throw new DjvException("No password specified");
				}
				KeySpec kSpec = keyLen > 0 ? new PBEKeySpec(password, salt, iters, keyLen) : new PBEKeySpec(password, salt, iters);
				if(algorithm == null) {
					// Probably the MD5/DES encryption technique
					PBEParameterSpec spec = new PBEParameterSpec(salt, iters);
					cipher.init(Cipher.ENCRYPT_MODE, keyFactory.generateSecret(kSpec), spec);
				} else {
					cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyFactory.generateSecret(kSpec).getEncoded(), algorithm));
				}

				// The base64 encoder is from the PostgreSQL driver.
				byte[] encoded = cipher.doFinal(input);
				return Base64.encodeBase64String(encoded);
			}
		}
		catch(Exception e) {
			DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
			throw new DjvException(e);
		}
	}

	/**
	 * Decrypts an input Base64 string into the original byte array data.
	 * This is used with password-based encryption, for secret-key-based encryption
	 * use the method without the salt value.
	 * @param txt The base64 text to be decrypted.
	 * @param salt The Salt value for use with key gen
	 * @param iters The iteration count value for use with key gen
	 * @return The unencrypted byte array associated with the given encrypted
	 * text. Non-null.
	 * @throws DjvException
	 */
	@SuppressWarnings("UseSpecificCatch")
	public byte[] out(String txt, byte [] salt, int iters) throws DjvException {
		try {
			// Get the appropriate cipher
			Cipher cipher = Cipher.getInstance(cipherTransformation);
			if(password != null) {
				KeySpec kSpec = keyLen > 0 ? new PBEKeySpec(password, salt, iters, keyLen) : new PBEKeySpec(password, salt, iters);
				if(algorithm != null) {
					cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyFactory.generateSecret(kSpec).getEncoded(), algorithm));
				} else {
					// Probably the MD5/DES encryption technique
					PBEParameterSpec spec = new PBEParameterSpec(salt, iters);
					cipher.init(Cipher.DECRYPT_MODE, keyFactory.generateSecret(kSpec), spec);
				}
			} else {
				throw new DjvException("No key specified");
			}

			// The base64 encoder is from the PostgreSQL driver.
			return cipher.doFinal(Base64.decodeBase64(txt));
		} catch(Exception e) {
			throw new DjvException(e);
		}
	}

	/**
	 * Encrypts data from an input stream then sends it to an output stream.
	 * This is meant to be used for secret-key-based encryption, for password-based
	 * encryption, use the method with the salt value instead.
	 * @param in The unencrypted input stream.
	 * @param out The encrypted output stream
	 * @return The 16-byte randomly generated initialisation vector.
	 * @throws DjvException 
	 */
	@SuppressWarnings("UseSpecificCatch")
	public byte [] enc(InputStream in, OutputStream out) throws DjvException {
		try {
			byte[] ret;
			Cipher cipher = Cipher.getInstance(cipherTransformation);
			if(key == null) {
				throw new DjvException("No key specified");
			}
			cipher.init(Cipher.ENCRYPT_MODE, key);
			ret = cipher.getIV();
			encryptStream(in, out, cipher);
			return ret;
		}
		catch(Exception ex) {
			throw new DjvException(ex);
		}
	}
	
	/**
	 * Encrypts data from an input stream then sends it to an output stream.
	 * This is meant to be used for password-based encryption, for secret-key-based
	 * encryption, use the method without the salt value instead.
	 * @param in The unencrypted input stream.
	 * @param out The encrypted output stream
	 * @param salt The Salt value for use with key gen
	 * @param iters The iteration count value for use with key gen
	 * @return The 16-byte randomly generated initialisation vector.
	 * @throws DjvException 
	 */
	@SuppressWarnings("UseSpecificCatch")
	public byte [] enc(InputStream in, OutputStream out, byte [] salt, int iters) throws DjvException {
		try {
			byte[] ret;
			Cipher cipher = Cipher.getInstance(cipherTransformation);
			if(password == null) {
				throw new DjvException("No password specified");
			}
			KeySpec kSpec = keyLen > 0 ? new PBEKeySpec(password, salt, iters, keyLen) : new PBEKeySpec(password, salt, iters);
			if(algorithm != null) {
				cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyFactory.generateSecret(kSpec).getEncoded(), algorithm));
			} else {
				// Probably the MD5/DES encryption technique
				PBEParameterSpec spec = new PBEParameterSpec(salt, iters);
				cipher.init(Cipher.ENCRYPT_MODE, keyFactory.generateSecret(kSpec), spec);
			}
			ret = cipher.getIV();
			encryptStream(in, out, cipher);
			return ret;
		}
		catch(Exception ex) {
			throw new DjvException(ex);
		}
	}
	/**
	 * Encrypts data from an input stream and writes the result to an output stream.
	 * @param in The input stream from which to read clear-text data
	 * @param out The output stream to which to write encrypted data
	 * @param cipher The cipher to be used, already configured.
	 * @throws IOException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException 
	 */
	private static void encryptStream(InputStream in, OutputStream out, Cipher cipher) throws IOException, IllegalBlockSizeException, BadPaddingException {
		byte[] encryptedData;
		byte[] clearTextData = new byte[4096];
		int bytes = in.read(clearTextData);
		while(bytes > 0) {
			encryptedData = cipher.update(clearTextData, 0, bytes);
			if(encryptedData != null) {
				out.write(encryptedData);
			}
			bytes = in.read(clearTextData);
		}
		encryptedData = cipher.doFinal();
		if(encryptedData != null) {
			out.write(encryptedData);
		}
	}
	
	/**
	 * Decrypts data from an input stream into an output stream, e.g. from one file to another.
	 * This is meant to be used for secret-key-based encryption, for password-based
	 * encryption, use the method with the salt value instead.
	 * @param in The encrypted input stream.
	 * @param out The unencrypted output stream
	 * @param iv The initialisation vector to use in the decryption process.
	 * @throws DjvException 
	 */
	@SuppressWarnings("UseSpecificCatch")
	public void dec(InputStream in, OutputStream out, byte [] iv) throws DjvException {
		try {
			Cipher cipher = Cipher.getInstance(cipherTransformation);
			if(key == null) {
				throw new DjvException("No key specified");
			}
			cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
			decryptStream(in, out, cipher);
		}
		catch(Exception ex) {
			throw new DjvException(ex);
		}
	}
	
	/**
	 * Decrypts data from an input stream into an output stream, e.g. from one file to another.
	 * This is meant to be used for password-based encryption, for secret-key-based
	 * encryption, use the method without the salt value instead.
	 * @param in The encrypted input stream.
	 * @param out The unencrypted output stream
	 * @param salt The Salt value for use with key gen
	 * @param iters The iteration count value for use with key gen
	 * @param iv The initialisation vector to use in the decryption process.
	 * @throws DjvException 
	 */
	@SuppressWarnings("UseSpecificCatch")
	public void dec(InputStream in, OutputStream out, byte [] salt, int iters, byte [] iv) throws DjvException {
		try {
			Cipher cipher = Cipher.getInstance(cipherTransformation);
			if(password == null) {
				throw new DjvException("No password specified");
			}
			KeySpec kSpec = keyLen > 0 ? new PBEKeySpec(password, salt, iters, keyLen) : new PBEKeySpec(password, salt, iters);
			if(algorithm != null) {
				cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyFactory.generateSecret(kSpec).getEncoded(), algorithm), new IvParameterSpec(iv));
			} else {
				cipher.init(Cipher.DECRYPT_MODE, keyFactory.generateSecret(kSpec), new IvParameterSpec(iv));
			}
			decryptStream(in, out, cipher);
		}
		catch(Exception ex) {
			throw new DjvException(ex);
		}
	}
	/**
	 * Decrypts data from an input stream and write the result to an output stream.
	 * @param in The input stream containing encrypted data
	 * @param out The output stream to which to write decrypted data
	 * @param cipher The cipher used for decryption, already configured.
	 * @throws IOException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException 
	 */
	private static void decryptStream(InputStream in, OutputStream out, Cipher cipher) throws IOException, IllegalBlockSizeException, BadPaddingException {
		byte[] encrypted = new byte[4096];
		int bytes = in.read(encrypted);
		byte[] decrypted;
		while(bytes > 0) {
			decrypted = cipher.update(encrypted, 0, bytes);
			if(decrypted != null) {
				out.write(decrypted);
			} else {
				DjvSystem.logWarning(Category.DESIGN, "Update returned null");
			}
			bytes = in.read(encrypted);
		}
		decrypted = cipher.doFinal();
		if(decrypted != null) {
			out.write(decrypted);
		} else {
			DjvSystem.logWarning(Category.DESIGN, "DoFinal returned null");
		}
	}
	
	/**
	 * Retrieves the encryption parameters, e.g. key, salt, and IV, from a file
	 * that was output using OpenSSL with a password. Requires OpenSSL to be installed on the host
	 * and added to the execution path.
	 * @param encrypted The OpenSSL-output file
	 * @param password The password with which the file was output
	 * @return The encryption information.
	 * @throws org.dejavu.util.DjvException
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("NestedAssignment")
	public static EncryptionInfo getEncryptedDataFromOpenSsl(File encrypted, char [] password) throws DjvException, InterruptedException {
		try {
			ProcessBuilder cmd = new ProcessBuilder(new String[]{"openssl", "enc", "-aes-256-cbc", "-d", "-pass", "pass:" + String.valueOf(password), "-P", "-in", encrypted.toString()});
			Process proc = cmd.start();
			try {
				InputStream is = proc.getInputStream();
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String line;
					byte [] salt = null;
					byte [] iv = null;
					byte [] key = null;
					while ((line = reader.readLine()) != null) {
						if(salt == null) {
							Matcher m = gSaltPattern.matcher(line);
							if(m.find()) {
								salt = stringToBytes(m.group(1));
								continue;
							}
						}
						if(iv == null) {
							Matcher m = gIvPattern.matcher(line);
							if(m.find()) {
								iv = stringToBytes(m.group(1));
								continue;
							}
						}
						if(key == null) {
							Matcher m = gKeyPattern.matcher(line);
							if(m.find()) {
								key = stringToBytes(m.group(1));
								continue;
							}
						}
						if((iv != null)&&(salt != null)&&(key != null)) {
							break;
						}
					}
					return new EncryptionInfo(key).setSalt(salt).setIv(iv);
				} finally {
					is.close();
				}
			} finally {
				proc.waitFor();
			}
		} catch (IOException ex) {
			throw new DjvException(ex);
		}
	}
	/**
	 * Decrypts a file (that was output with OpenSSL) using OPenSSL.
	 * Requires OpenSSL to be installed on the host and added to the execution path.
	 * @param encrypted The output file.
	 * @param decrypted The file containing output information.
	 * @param password The password to be used for decryption
	 * @throws org.dejavu.util.DjvException
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("NestedAssignment")
	public static void decryptOpenSslFile(File encrypted, File decrypted, char [] password) throws DjvException, InterruptedException {
		try {
			ProcessBuilder cmd = new ProcessBuilder(new String[]{"openssl", "enc", "-aes-256-cbc", "-d", "-pass", "pass:" + String.valueOf(password), "-out", decrypted.toString(), "-in", encrypted.toString()});
			Process proc = cmd.start();
			try {
				InputStream is = proc.getInputStream();
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String line;
					while ((line = reader.readLine()) != null) {
						System.out.println(line);
					}
				} finally {
					is.close();
				}
			} finally {
				proc.waitFor();
			}
		} catch (IOException ex) {
			throw new DjvException(ex);
		}
	}
	
	/**
	 * Encrypts a file using OPenSSL. Requires OpenSSL to be installed on the host
	 * and added to the execution path.
	 * @param plainText The plain text file to be output.
	 * @param encrypted The file containing the resulting output data.
	 * @param password The password to be used for encryption
	 * @throws org.dejavu.util.DjvException
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("NestedAssignment")
	public static void encryptFileOpenSsl(File plainText, File encrypted, char [] password) throws DjvException, InterruptedException {
		try {
			ProcessBuilder cmd = new ProcessBuilder(new String[]{"openssl", "enc", "-aes-256-cbc", "-salt", "-pass", "pass:" + String.valueOf(password), "-out", encrypted.toString(), "-in", plainText.toString()});
			Process proc = cmd.start();
			try {
				InputStream is = proc.getInputStream();
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String line;
					while ((line = reader.readLine()) != null) {
						System.out.println(line);
					}
				} finally {
					is.close();
				}
			} finally {
				proc.waitFor();
			}
		} catch (IOException ex) {
			throw new DjvException(ex);
		}
	}
	/**
	 * Test program
	 * @param args 
	 */
	public static void main(String[] args) {
		DjvSystem.setLogLevel(2);
		try {
			int iters = 0xffff;
			String password = "password";
			File orig = new File("build.xml");
			File enc1 = new File("enc1.xml");
			File enc2 = new File("enc2.xml");
			File dec1 = new File("dec1.xml");
			File dec2 = new File("dec2.xml");
			File dec3 = new File("dec3.xml");
			byte[] iv = null;
			byte[] salt = new byte[]{(byte)0x01, (byte)0x23, (byte)0xf3, (byte)0xcc, (byte)0xb5, (byte)0x0a, (byte)0x55, (byte)0xb7};
			DjvCrypto crypto = new DjvCrypto(KeyAlgorithm.PBKDF2WithHmacSHA256, password);
			{
				InputStream is = new FileInputStream(orig);
				try {
					OutputStream os = new FileOutputStream(enc1);
					try {
						iv = crypto.enc(is, os, salt, iters);
					} finally {
						os.close();
					}
				} finally {
					is.close();
				}
			}
			crypto = new DjvCrypto(KeyAlgorithm.PBKDF2WithHmacSHA256, password);
			{
				InputStream is = new FileInputStream(enc1);
				try {
					OutputStream os = new FileOutputStream(dec1);
					try {
						crypto.dec(is, os, salt, iters, iv);
					} finally {
						os.close();
					}
				} finally {
					is.close();
				}
			}
			encryptFileOpenSsl(orig, enc2, password.toCharArray());
			EncryptionInfo encInfo = getEncryptedDataFromOpenSsl(enc2, password.toCharArray());
			try {
				DjvSystem.logInfo(Category.DESIGN, "Found " + encInfo + " from " + enc2);
				crypto = new DjvCrypto(KeyAlgorithm.PBKDF2WithHmacSHA256, password);
				{
					InputStream input = new FileInputStream(enc2);
					try {
						OutputStream output = new FileOutputStream(dec2);
						try {
							crypto.dec(input, output, encInfo.getSalt(), encInfo.getIters(), encInfo.getIv());
						} finally {
							output.close();
						}
					} finally {
						input.close();
					}
				}
			} catch(DjvException ex) {
				DjvSystem.logWarning(Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
			}
			decryptOpenSslFile(enc2, dec3, password.toCharArray());
		}
		catch(IOException | DjvException ex) {
			DjvSystem.logError(Category.DESIGN, DjvExceptionUtil.simpleTrace(ex));
		}
		catch(InterruptedException ex) {
		}
	}
	/**
	 * Converts a byte array into equivalent, human-readable string.
	 * @param data The byte array to be converted
	 * @return The string representation of the given byte array
	 */
	private static String bytesToString(byte [] data) {
		StringBuilder ret = new StringBuilder(1024);
		if(data != null) {
		boolean comma = false;
			for(byte x : data) {
				if(!comma) {
					comma = true;
				} else {
					ret.append(',');
				}
				ret.append(String.format("0x%02x", x));
			}
		}
		return ret.toString();
	}
	/**
	 * Converts a hex string into the equivalent byte array. E.g.
	 * "AB1C66" -> [0xab, 0x1c, 0x66]
	 * If a string containing non numerical values is given, some runtime exception
	 * (most probably NumberFormatException) will be thrown.
	 * @param str The string containing hex byte values to be converted
	 * @return The byte array associated with the given string.
	 */
	private static byte [] stringToBytes(String str) {
		int begin = 0;
		int end = begin + 2;
		int len = str.length();
		byte[] ret = new byte[len/2];
		int idx = 0;
		while(end <= len) {
			ret[idx++] = (byte)Integer.parseInt(str.substring(begin, end), 16);
			begin += 2;
			end += 2;
		}
		return ret;
	}
}

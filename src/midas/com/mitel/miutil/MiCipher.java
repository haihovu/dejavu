/*
 * MiCipher.java
 *
 * Created on September 1, 2006, 1:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.mitel.miutil;

import java.util.HashMap;
import java.util.Map;

/**
 * Class used for managing Mitel cipher information, for MiNET signaling purposes, not for encryption use.
 * @author haiv
 */
public enum MiCipher
{
	MICIPHER_NONE(0, "NONE"),
	MICIPHER_DES(1, "DES"),
	MICIPHER_3DES(2, "3DES"),
	MICIPHER_CAST(3, "CAST"),
	MICIPHER_RC4(4, "RC4"),
	MICIPHER_AES(5, "AES");
	
	private MiCipher(int id, String str)
	{
		m_Id = id;
		m_Str = str;
	}

	/**
	 * Retrieves the integer ID of the cipher, as specified by Mitel standard such as MiNET.
	 * @return The cipher's integer ID.
	 */
	public int intValue()
	{
		return m_Id;
	}
	
	@Override
	public String toString()
	{
		return m_Str;
	}
	
	/**
	 * Cipher ID as in MiNET spec.
	 */
	private final int m_Id;
	/**
	 * Human readable string.
	 */
	private final String m_Str;
	
	/**
	 * Converts a cipher value in its string form to the enumeration equivalent.
	 * @param cipherName The string to be converted.
	 * @return The associated cipher value,
	 * if an invalid string is given, MICIPHER_INVALID is returned.
	 */
	public static MiCipher stringToCipher(String cipherName)
	{
		MiCipher value = s_CipherNameMap.get(cipherName);
		
		return (value != null ? value : MICIPHER_NONE);
	}
	
	/**
	 * Converts a cipher value in its string form to the enumeration equivalent.
	 * @param value The string to be converted.
	 * @return The associated cipher value,
	 * if an invalid string is given, MICIPHER_INVALID is returned.
	 */
	public static MiCipher intToCipher(int value)
	{
		MiCipher ret = null;
		if((value > -1)&&(value < s_CipherLookup.length))
		{
			ret = s_CipherLookup[value];
		}
		return (ret != null ? ret : MICIPHER_NONE);
	}
	
	private static final Map<String, MiCipher> s_CipherNameMap = new HashMap<String, MiCipher>(16);
	private static final MiCipher[] s_CipherLookup = new MiCipher[10];
	
	static
	{
		s_CipherNameMap.put(MICIPHER_DES.toString(), MICIPHER_DES);
		s_CipherNameMap.put(MICIPHER_3DES.toString(), MICIPHER_3DES);
		s_CipherNameMap.put(MICIPHER_CAST.toString(), MICIPHER_CAST);
		s_CipherNameMap.put(MICIPHER_RC4.toString(), MICIPHER_RC4);
		s_CipherNameMap.put(MICIPHER_AES.toString(), MICIPHER_AES);
		
		s_CipherLookup[MICIPHER_DES.intValue()] = MICIPHER_DES;
		s_CipherLookup[MICIPHER_3DES.intValue()] = MICIPHER_3DES;
		s_CipherLookup[MICIPHER_CAST.intValue()] = MICIPHER_CAST;
		s_CipherLookup[MICIPHER_RC4.intValue()] = MICIPHER_RC4;
		s_CipherLookup[MICIPHER_AES.intValue()] = MICIPHER_AES;
	}
}

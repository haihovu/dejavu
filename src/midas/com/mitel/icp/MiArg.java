/*
 * MiArg.java
 *
 * Created on July 13, 2004, 1:49 PM
 */

package com.mitel.icp;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author  haiv
 */
public class MiArg
{
	
	public static final int ARG_TYPE_STRING = 0;
	
	public static final int ARG_TYPE_DATETIME = 1;
	
	public static final int ARG_TYPE_UNDEFINED = -11;
	
	private static Map<Integer, String> m_argTypeIntString = new TreeMap<Integer, String>();
	
	private static Map<String, Integer> m_argTypeStringInt = new TreeMap<String, Integer>();
	
	static 
	{
		m_argTypeIntString.put(new Integer(ARG_TYPE_STRING), "ARG_TYPE_STRING");
		m_argTypeIntString.put(new Integer(ARG_TYPE_DATETIME), "ARG_TYPE_DATETIME");
		
		Set<Entry<Integer, String>> entries = m_argTypeIntString.entrySet();
		Iterator<Entry<Integer, String>> iter = entries.iterator();
		while(iter.hasNext())
		{
			Entry<Integer, String> entry = iter.next();
			m_argTypeStringInt.put(entry.getValue(), entry.getKey());
		}
	}
	
	/** Creates a new instance of MiArg */
	public MiArg()
	{
	}
	
	public static String convertArgType2String(int argType) throws InvalidArgumentException
	{
		Integer type = new Integer(argType);
		if(m_argTypeIntString.containsKey(type))
		{
			return m_argTypeIntString.get(type);
		}
		throw new InvalidArgumentException("Invalid arg type " + argType);
	}
	
	public static int convertArgType2Int(String argType) throws InvalidArgumentException
	{
		if(m_argTypeStringInt.containsKey(argType))
		{
			return (m_argTypeStringInt.get(argType)).intValue();
		}
		throw new InvalidArgumentException("Invalid arg type " + argType);
	}
	
	public static class InvalidArgumentException extends Exception
	{
		private static final long serialVersionUID = 1L;
		
		public InvalidArgumentException(String desc)
		{
			super(desc);
		}
		
	}
	
}

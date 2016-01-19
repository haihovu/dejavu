/*
 * ArgEditorRepository.java
 *
 * Created on July 13, 2004, 8:44 AM
 */

package com.mitel.icp;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author  haiv
 */
public class ArgEditorRepository
{
	
	private Map<Integer, ArgEditor> m_argEditorMap = new TreeMap<Integer, ArgEditor>();
	
	private static ArgEditorRepository m_singleton = new ArgEditorRepository();
	
	/** Creates a new instance of ArgEditorRepository */
	public ArgEditorRepository()
	{
	}
	
	public ArgEditor locateEditor(int argType)
	{
		Integer key = new Integer(argType);
		if(m_argEditorMap.containsKey(key))
		{
			return (ArgEditor)m_argEditorMap.get(key);
		}
		return null;
	}
	
	public void registerEditor(int argType, ArgEditor editor)
	{
		m_argEditorMap.put(new Integer(argType), editor);
	}
	
	public static ArgEditorRepository getInstance()
	{
		return m_singleton;
	}
	
}

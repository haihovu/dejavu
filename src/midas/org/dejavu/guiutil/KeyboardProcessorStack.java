/*
 * KeyboardProcessorStack.java
 *
 * Created on May 5, 2004, 1:14 PM
 */

package org.dejavu.guiutil;

import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.util.LinkedList;

/**
 *
 * @author  haiv
 */
public class KeyboardProcessorStack
{
	
	private KeyEventPostProcessor m_CurrentActiveKeyProcessor = null;
	
	private LinkedList<KeyEventPostProcessor> m_KeyProcessorStack = new LinkedList<KeyEventPostProcessor>();
	
	private static KeyboardProcessorStack m_Singleton = new KeyboardProcessorStack();
	
	/** Creates a new instance of KeyboardProcessorStack */
	public KeyboardProcessorStack()
	{
	}
	
	/**
	 * Removes the top key processor from the stack
	 */
	public void popKeyProcessorStack()
	{
		if( null != m_CurrentActiveKeyProcessor )
		{
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(this.m_CurrentActiveKeyProcessor);
			m_CurrentActiveKeyProcessor = null;
		}
		if( !m_KeyProcessorStack.isEmpty())
		{
			m_CurrentActiveKeyProcessor = (KeyEventPostProcessor)m_KeyProcessorStack.removeLast();
			if( null != m_CurrentActiveKeyProcessor )
			{
				KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(m_CurrentActiveKeyProcessor);
			}
		}
		else
		{
			Thread.dumpStack();
			System.out.println( "ERROR: " + getClass().getName() + ".popKeyProcessorStack() - Over pop detected" );
		}
	}
	
	/**
	 * Removes a particular key processor from the stack
	 * @param keyProc The key processor to remove
	 */
	public void popKeyProcessorStack(KeyEventPostProcessor keyProc)
	{
		if( keyProc == m_CurrentActiveKeyProcessor )
		{
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(this.m_CurrentActiveKeyProcessor);
			m_CurrentActiveKeyProcessor = null;
			if( !m_KeyProcessorStack.isEmpty())
			{
				m_CurrentActiveKeyProcessor = (KeyEventPostProcessor)m_KeyProcessorStack.removeLast();
				if( null != m_CurrentActiveKeyProcessor )
				{
					KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(m_CurrentActiveKeyProcessor);
				}
			}
		}
		else if( !this.m_KeyProcessorStack.isEmpty())
		{
			m_KeyProcessorStack.remove(keyProc);
		}
		else
		{
			Thread.dumpStack();
			System.out.println( "ERROR: " + getClass().getName() + ".popKeyProcessorStack() - Over pop detected" );
		}
	}
	
	/**
	 * Inserts a new key processor at the top of the stack
	 * @param keyProc The key processor to insert
	 */
	public void pushKeyProcessorStack(KeyEventPostProcessor keyProc)
	{
		if( null != this.m_CurrentActiveKeyProcessor )
		{
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(this.m_CurrentActiveKeyProcessor);
		}
		m_KeyProcessorStack.add(this.m_CurrentActiveKeyProcessor);
		m_CurrentActiveKeyProcessor = null;
		if( null != keyProc )
		{
			m_CurrentActiveKeyProcessor = keyProc;
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(this.m_CurrentActiveKeyProcessor);
		}
	}
	
	/**
	 * Retrieves the KeyboardProcessorStack singleton
	 * @return The KeyboardProcessorStack singleton
	 */
	public static KeyboardProcessorStack getInstance()
	{
		return m_Singleton;
	}
	
}

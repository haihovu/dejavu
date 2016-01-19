/*
 * iCscFactory.java
 *
 * Created on April 1, 2004, 9:15 PM
 */

package com.mitel.miutil;

import javax.swing.tree.MutableTreeNode;

/**
 *
 * @author  Hai Vu
 */
public interface TreeObject
{
	/**
	 * Returns a tree model associated with this object.
	 */	
	public MutableTreeNode getTreeModel();
	
	/**
	 * Returns the string representation of the class name of the object.
	 */	
	public String getClassName();
	
}

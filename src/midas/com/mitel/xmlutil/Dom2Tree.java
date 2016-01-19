/*
 * iDom2Tree.java
 *
 * Created on October 14, 2003, 3:54 PM
 */

package com.mitel.xmlutil;

import org.w3c.dom.*;
import javax.swing.tree.*;

/**
 *
 * Interface to be implemented by components that can parse a DOM document into a Tree structure.
 * @author  haiv
 */
public interface Dom2Tree {
	
	/** Parses a DOM node structure into a tree node structure.
	 * @param domNode - The DOM document to parse
	 * @return The resultant MutableTreeNode structure, or null if document cannot be parsed.*/
	public MutableTreeNode parseDom( Node domNode );
}

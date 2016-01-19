/*
 * iDomParser.java
 *
 * Created on March 27, 2004, 5:17 PM
 */

package com.mitel.xmlutil;

import org.w3c.dom.Node;

/**
 * Interface for those components that can parse DOM documents for their own usage.
 * @author Hai Vu
 */
public interface DomParser {
    
    /**
     * Parses a DOM document. This must be implemented by concrete classes.
     * @param domDocument - The DOM document to parse
     * @return true if the document was successfully parsed, false otherwise.
     */    
    public boolean parseDomDocument(Node domDocument);
    
}

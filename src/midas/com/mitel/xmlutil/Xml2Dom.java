/*
 * httpxml2tree.java
 *
 * Created on August 11, 2003, 11:22 PM
 */
package com.mitel.xmlutil;

import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * This class utility provides methods for converting readers or strings that contain XML contents into DOM document structures.
 * @author haiv
 * @stereotype utility
 */
class Xml2Dom 
{
	private static DocumentBuilder s_DocBuilder;
	private static final Object s_GlobalLock = new Object();

	static
	{
		try
		{
			s_DocBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/** Parses the XML content from a reader into a DOM document.
	 * @param xmlSource - The reader from which XML text is retrieved and parsed.
	 * @return The DOM document parsed from the input data.
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document xmlReader2Dom(Reader xmlSource) throws SAXException, IOException
	{
		synchronized(s_GlobalLock)
		{
			if(s_DocBuilder != null)
			{
				InputSource src = new InputSource(xmlSource);
				return s_DocBuilder.parse(src);
			}
		}
		return null;
	}
	
	/** Parses the XML content from a string into a DOM document.
	 * @param xmlString - The string containing the XML text to be parsed.
	 * @return The DOM document parsed from the input data.
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document xmlString2Dom(java.lang.String xmlString) throws SAXException, IOException
	{
		if(null != xmlString)
		{
			synchronized(s_GlobalLock)
			{
				if(s_DocBuilder != null)
				{
					return s_DocBuilder.parse(new InputSource(new StringReader(xmlString)));
				}
			}
		}
		
		return null;
	}

	
}

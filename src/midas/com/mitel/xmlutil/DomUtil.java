/*
 * DomUtils.java
 *
 * Created on October 14, 2003, 12:11 PM
 */

package com.mitel.xmlutil;

import com.mitel.miutil.MiException;
import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import java.io.*;
import java.nio.charset.Charset;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/**
 *
 * Class utility that provides value-added methods for extracting information from DOM documents.
 * @author  haiv
 * @stereotype utility
 */
public class DomUtil
{	
	/**
	 * Enumeration of some commonly used DOM configuration parameters
	 */
	public static enum DomConfigParam
	{
		/**
		 * <i>canonical-form</i>
		 * <ul>
		 * <li>true [optional] Writes the document according to the rules specified in [Canonical XML].
		 * In addition to the behavior described in " canonical-form" [DOM Level 3 Core] ,
		 * setting this parameter to true will set the parameters "format-pretty-print",
		 * "discard-default-content", and "xml-declaration ", to false.
		 * Setting one of those parameters to true will set this parameter to false.
		 * Serializing an XML 1.1 document when "canonical-form" is true will generate a fatal error.</li>
		 * <li>false [required] (default) Do not canonicalize the output.</li></ul>
		 */
		CANONICAL_FORM("canonical-form"),
		/**
		 * <i>discard-default-content</i>
		 * <ul>
		 * <li>true [required] (default) Use the Attr.specified attribute to decide what attributes should be discarded.
		 * Note that some implementations might use whatever information available to the implementation
		 * (i.e. XML schema, DTD, the Attr.specified attribute, and so on) to determine what attributes and content to discard if this parameter is set to true.</li>
		 * <li>false [required]Keep all attributes and all content.</li>
		 * </ul>
		 */
		DISCARD_DEFAULT_CONTENT("discard-default-content"),
		/**
		 * <i>format-pretty-print</i>
		 * <ul>
		 * <li>true [optional] Formatting the output by adding whitespace to produce a pretty-printed,
		 * indented, human-readable form. The exact form of the transformations is not specified by this specification.
		 * Pretty-printing changes the content of the document and may affect the validity of the document,
		 * validating implementations should preserve validity.</li>
		 * <li>false [required] (default) Don't pretty-print the result.</li>
		 * </ul>
		 */
		FORMAT_PRETTY_PRINT("format-pretty-print"),
		/**
		 * <i>normalize-characters</i> - This parameter is equivalent to the one defined by DOMConfiguration in [DOM Level 3 Core].
		 * Unlike in the Core, the default value for this parameter is true. While DOM implementations are not required to
		 * support fully normalizing the characters in the document according to appendix E of [XML 1.1],
		 * this parameter must be activated by default if supported. 
		 */
		NORMALIZE_CHARACTERS("normalize-characters"),
		/**
		 * <i>xml-declaration</i>
		 * <ul>
		 * <li>true [required] (default) If a Document, Element, or Entity node is serialized,
		 * the XML declaration, or text declaration, should be included. The version
		 * (Document.xmlVersion if the document is a Level 3 document and the version is non-null,
		 * otherwise use the value "1.0"), and the output encoding (see LSSerializer.write for
		 * details on how to find the output encoding) are specified in the serialized XML declaration.</li>
		 * <li>false [required] Do not serialize the XML and text declarations.
		 * Report a "xml-declaration-needed" warning if this will cause problems
		 * (i.e. the serialized data is of an XML version other than [XML 1.0],
		 * or an encoding would be needed to be able to re-parse the serialized data).</li>
		 * </ul>
		 */
		XML_DECLARATION("xml-declaration");
		private DomConfigParam(String value)
		{
			this.value = value;
		}
		public final String value;
	}
	
	/** Given a DOM document retrieves the #text value from the first element matching a particular name.
	 * @param domDocument DOM document on which to perform the search
	 * @param elName Name of the desired element
	 * @return The #text value associated with the desired element, or null if none was found
	 */
	public static String getValueFromDom(Document domDocument, java.lang.String elName)
	{
		if((null != domDocument)&&(elName != null))
		{
			Element tmp = locateElement(domDocument, elName);
			return getLeafValue(tmp);
		}
		return null;
	}
	
    /** Takes a DOM node structure and returns the first #text value encountered, prefixed with hierarchical node names separated by dot '.'
     * @param aNode - The node from which to start the search.
     * @return Dot notation representing the #text value.
     */    
    public static String getLeafNameValue(Node aNode) 
	{
        if(aNode.getNodeName().equals("#text")) {
			return aNode.getNodeValue();
        }
		StringBuilder retValue = new StringBuilder(aNode. getNodeName());
        for( Node child = aNode. getFirstChild(); child != null; child = child.getNextSibling()) {
            String subNode = getLeafNameValue( child );
            if( null != subNode ) {
				retValue.append('=').append(subNode);
            }
        }
        return retValue.toString();
    }
    
	/**
	 * Given a DOM Node retrieves the first #text value encountered.
	 * @param aNode - The DOM node from which to start the search
	 * @return The first #text value found in the given node structure, or null if none was found. 
	 */
	public static String getLeafValue(Node aNode)
	{
		if(null != aNode)
		{
			if(aNode.getNodeName().equals("#text"))
			{
				return aNode.getNodeValue();
			}
			else
			{
				NodeList children = aNode.getChildNodes();
				for(int i = children.getLength() - 1; i > -1; --i)
				{
					String retValue = getLeafValue(children.item(i));
					if(null != retValue)
						return retValue;
				}
			}
		}
        return null;
	}
	
	/**
	 * Given a DOM document or a starting element, locates the first Element matching a certain name.
	 * @param doc The DOM document or element from which to start the search
	 * @param elName Name of the desired element
	 * @return the first Element matching the given name or null if no match was found.
	 */
	public static Element locateElement(Node doc, java.lang.String elName)
	{
		if(doc instanceof Document)
		{
			NodeList nodes = ((Document)doc).getElementsByTagName(elName);
			
			if(nodes.getLength() > 0)
				return (Element)nodes.item(0);
		}
		else if(doc instanceof Element)
		{
			if(doc.getNodeName().equals(elName))
				return (Element)doc;

			NodeList nodes = ((Element)doc).getElementsByTagName(elName);
			
			if(nodes.getLength() > 0)
				return (Element)nodes.item(0);
		}
		return null;
	}

	/**
	 * Deserializes an XML text string into the equivalent DOM structure.
	 * @param xmlText The XML text to be deserialized.
	 * @return The DOM document parsed from the test.
	 * @throws org.xml.sax.SAXException
	 * @throws java.io.IOException 
	 */
	public static Document deserializeDom(String xmlText) throws SAXException, IOException
	{
		return Xml2Dom.xmlString2Dom(xmlText);
	}

	/**
	 * Deserializes an XML text file into the equivalent DOM structure, using UTF-16 character set encoding.
	 * @param xmlFile The XML file to be deserialized.
	 * @return The DOM document parsed from the file.
	 * Null if the given file cannot be deserialized into an XML document.
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 * @throws org.xml.sax.SAXException 
	 */
	public static Document deserializeDom(File xmlFile) throws FileNotFoundException, IOException, SAXException
	{
		return deserializeDom(xmlFile, Charset.forName("UTF-16"));
	}

	/**
	 * Deserializes an XML text file into the equivalent DOM structure.
	 * @param xmlFile The XML file to be deserialized.
	 * @param charSet The character set with which to decode the text from the file.
	 * @return The DOM document parsed from the file.
	 * Null if the given file cannot be deserialized into an XML document.
	 * @throws java.io.IOException
	 * @throws org.xml.sax.SAXException 
	 */
	public static Document deserializeDom(File xmlFile, Charset charSet) throws IOException, SAXException
	{
		Document ret = null;
		
		InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), charSet);
		try
		{
			ret = deserializeDom(reader);
		}
		finally
		{
			reader.close();
		}
		return ret;
	}

	/**
	 * Deserializes the text from a reader into the equivalen DOM structure.
	 * @param reader The reader source of text to be deserialized.
	 * @return The DOM document parsed from the reader.
	 * @throws org.xml.sax.SAXException
	 * @throws java.io.IOException 
	 */
	public static Document deserializeDom(Reader reader) throws SAXException, IOException
	{
		return Xml2Dom.xmlReader2Dom(reader);
	}

	/**
	 * Retrieves the global DOM serializer for this application.
	 * @return The global LSSerializer instance.
	 */
	public static LSSerializer getSerializer()
	{
		synchronized(s_GlobalLock)
		{
			return s_DomSerializer;
		}
	}
	
	/**
	 * Uses the DOM LS serializer to serialize a DOM node structure into the equivalent XML String using UTF-16 encoding.
	 * @param aNode - The DOM node from with which to serialize.
	 * @return The XML string representing the DOM document. If there are problems, an empty string is returned. 
	 */
	public static String serializeDom(Node aNode)
	{
		synchronized(s_GlobalLock)
		{
			if((s_DomSerializer != null)&&(aNode != null))
			{
				return s_DomSerializer.writeToString(aNode);
			}
		}
		return "";
	}

	/**
	 * Uses the DOM LS serializer to serialize a DOM node structure into the equivalent XML String using UTF-16 encoding.
	 * Remove all newlines from the result.
	 * @param aNode - The DOM node from with which to serialize.
	 * @return The XML string representing the DOM document. If there are problems, an empty string is returned.
	 */
	public static String serializeDomNoNewLine(Node aNode)
	{
		synchronized(s_GlobalLock)
		{
			if((s_DomSerializer != null)&&(aNode != null))
			{
				String raw = s_DomSerializer.writeToString(aNode);
				String[] fragments = raw.split("\n");
				StringBuilder output = new StringBuilder(1024);
				for(String fragment : fragments)
				{
					output.append(fragment);
				}
				return output.toString();
			}
		}
		return "";
	}

	/**
	 * Uses the DOM LS serializer to serialize a DOM node structure into a file, using UTF-16 encoding.
	 * @param aNode - The DOM node from with which to serialize.
	 * @param aFile The file to save the XML text.
	 * @return True if the document was successfully saved, false otherwise.
	 */
	public static boolean serializeDom(Node aNode, File aFile)
	{
		return serializeDom(aNode, aFile, Charset.forName("UTF-16"));
	}

	/**
	 * Uses the DOM LS serializer to serialize a DOM node structure into a file.
	 * @param aNode - The DOM node from with which to serialize.
	 * @param aFile The file to save the XML text.
	 * @param charSet The character set to be used when encoding the XML string to the file.
	 * @return True if the document was successfully saved, false otherwise.
	 */
	public static boolean serializeDom(Node aNode, File aFile, Charset charSet)
	{
		OutputStreamWriter writer;
		try
		{
			writer = new OutputStreamWriter(new FileOutputStream(aFile), charSet);
			try
			{
				Transformer transform = TransformerFactory.newInstance().newTransformer();
				transform.setOutputProperty(OutputKeys.ENCODING, charSet.name());
				transform.transform(new DOMSource(aNode), new StreamResult(writer));
				return true;
			}
			finally
			{
				writer.close();
			}
		}
		catch(TransformerException ex)
		{
			MiSystem.logWarning(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
		catch(IOException ex)
		{
			MiSystem.logWarning(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
		return false;
	}

	/**
	 * Creates a new DOM Document with a name.
	 * @param rootElementName - The name of the root element of the newly created Document.
	 * @return The newly created Document or null if some problems are encountered. 
	 */
	public static Document createDocument(String rootElementName)
	{
		synchronized(s_GlobalLock)
		{
			if(s_DomImpl == null)
				return null;
			
			return s_DomImpl.createDocument(null, rootElementName, null);
		}
	}

	/**
	 * Creates a new LS serialzer that the power callers can customize for their specialized needs.
	 * @return The new serializer, non null.
	 * @throws MiException 
	 */
	public static LSSerializer createSerializer() throws MiException
	{
		return createSerializer(s_DomImpl);
	}
	
	private static final Object s_GlobalLock = new Object();
	
	/**
	 * Default DOM implmenentation 
	 */
	private static final DOMImplementation s_DomImpl;

	/**
	 * Default DOM serializer. 
	 */
	private static final LSSerializer s_DomSerializer;

	/**
	 * Creates a new LS serialzer instance.
	 * @param domImpl The DOM implementation from which the serializer is created
	 * @return The requested serializer instance, not null.
	 * @throws MiException 
	 */
	private static LSSerializer createSerializer(DOMImplementation domImpl) throws MiException
	{
		if(domImpl != null)
		{
			// Locate the DOMImplementationLS for LS version 3.0 - LS stands for Load & Save
			Object obj = domImpl.getFeature("LS", "3.0");
			if(obj instanceof DOMImplementationLS)
			{
				DOMImplementationLS ls = (DOMImplementationLS)obj;
				return ls.createLSSerializer();
			}
			throw new MiException("Failed to locate DOMImplementationLS for LS 3.0");
		}
		throw new MiException("No DOM implementation");
	}
	
	static
	{
		DOMImplementation domImpl = null;
		LSSerializer serializer = null;
		try
		{
			domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();

			// Locate the DOMImplementationLS for LS version 3.0 - LS stands for Load & Save
			serializer = createSerializer(domImpl);
		}
		catch(MiException ex)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
		catch(ParserConfigurationException ex)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
		catch(RuntimeException e)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
		s_DomSerializer = serializer;
		s_DomImpl = domImpl;
	}
}

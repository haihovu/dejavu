/*
 * hConfig.java
 *
 * Created on May 5, 2004, 7:36 PM
 */

package com.mitel.miutil;

import com.mitel.xmlutil.DomUtil;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;



/**
 * 
 * A container for configuration parameters. 
 * Typically used for saving the state of applications on exit so that they can be restored when restarted in the future.
 * Not thread safe.
 * @see MiParameter
 * @author Hai Vu
 */
public final class MiParameterCollection
{
	private final List<MiParameter> m_Parameters = new LinkedList<MiParameter>();

	/** Instantiation constructor. Creates a new configuration object and read its parameters from a file.
	 * @param configFile File from which parameters are read.
	 * @throws FileNotFoundException
	 * @throws SAXException
	 * @throws IOException  
	 */
	public MiParameterCollection(File configFile) throws FileNotFoundException, SAXException, IOException
	{
		if(null != configFile)
		{
			m_XmlDoc = DomUtil.deserializeDom(configFile);
			parseDoc(m_XmlDoc);
		}
		else
			throw new IOException("NULL config file");
	}
	
	/**
	 * Instantiation constructor. Creates a new configuration object and read its parameters from an input stream. 
	 * @param inStream The input stream from which parameters are read.
	 * @throws SAXException
	 * @throws IOException  
	 */
	public MiParameterCollection(InputStream inStream) throws SAXException, IOException
	{
		if(inStream != null)
		{
			// UTF-16 is the standard XML serializer/deserializer in Java
			InputStreamReader in = new InputStreamReader(inStream, Charset.forName("UTF-16"));
			try
			{
				m_XmlDoc = DomUtil.deserializeDom(in);
			}
			catch (IOException ex)
			{
				in. close();
				throw ex;
			}
			catch (SAXException ex)
			{
				in. close();
				throw ex;
			}
			in. close();
			parseDoc( m_XmlDoc );
			return;
		}
		throw new IOException("NULL Stream");
	}

	/**
	 * Instantiation constructor. Creates a new configuration object that takes in some existing parameter structure.
	 * @param aParameter A parameter representing the root of the collection.
	 */
	public MiParameterCollection(MiParameter aParameter)
	{
		aParameter.getClass(); // null check
		
		addParameter(aParameter);
		m_XmlDoc = DomUtil.createDocument(aParameter.getName());
		if(m_XmlDoc == null)
			return;

		Element top = m_XmlDoc.getDocumentElement();
		Iterator iter = aParameter.getSubParams().iterator();
		while(iter.hasNext())
		{
			MiParameter child = (MiParameter)iter.next();
			top.appendChild(child.exportParam(m_XmlDoc));
		}
	}	
	
	/**
	 *Returns a parameter identified by its name.
	 * @param parameterName - Name of the desired parameter.
	 * @return The requested parameter, or null if one cannot be located. 
	 */
	public MiParameter getParameter(java.lang.String parameterName)
	{
		Iterator iter = m_Parameters.iterator();
		while( iter.hasNext())
		{
			MiParameter aParam = ( MiParameter )iter.next();
			MiParameter retValue = aParam.getSubParam(parameterName);
			if( retValue != null )
			{
				return retValue;
			}
		}
		return null;
	}	
	
	/**
	 * Adds some parameters to the configugration object.
	 * @param aParameter The parameter to add.
	 */
	public void addParameter(MiParameter aParameter)
	{
		m_Parameters.add(aParameter);
	}

	/**
	 * Loads new parameters from a config file.
	 * @param configFile File from which the parameters are loaded.
	 * @throws SAXException
	 * @throws IOException  
	 */
	public void loadFile(File configFile) throws SAXException, IOException
	{
		Node doc = DomUtil.deserializeDom(configFile);
		parseDoc(doc);
	}

	/**
	 * Saves the configuration to file.
	 * @param configFile File to which this configuration object is saved.
	 * @throws IOException  
	 */
	public void saveFile(File configFile)throws IOException
	{
		if(null != m_XmlDoc)
		{
			// New fangled way of doing things
			DomUtil.serializeDom(m_XmlDoc, configFile);
		}
		else
		{
			FileWriter out = new FileWriter( configFile );
			try {
				// Legacy way of doing things
				Iterator iter = m_Parameters.iterator();
				while( iter.hasNext())
				{
					MiParameter aParam = ( MiParameter )iter.next();
					saveParameter(aParam, out);
				}
			} finally {
				out. close();
			}
		}
	}

	/**
	 * Given an XML node structure, extract configuration parameter from it and add the newly created parameters to the configuration object.
	 * @param doc The XML structure to process.
	 */
	private void parseDoc(Node doc)
	{
		// Ignore the top node
		if(null != doc)
		{
			for(Node child = doc.getFirstChild(); child != null; child = child.getNextSibling())
			{
				m_Parameters.add(new MiParameter(child));
			}
		}
	}

	/**
	 * Recursively writes parameters to a file. The resulting format is kept readable by following an indentation scheme.
	 * @param param The parameter structure starting from which to write out.
	 * @param out The file writer to which the data is written.
	 * @throws IOException  
	 */
	private void saveParameter(MiParameter param, FileWriter out) throws IOException
	{
		out.write( "<" + param.getName() + ">" );
		out.write( param.getValue());
		Iterator iter = param.getSubParams().iterator();
		while( iter.hasNext())
		{
			MiParameter aParam = ( MiParameter )iter.next();
			saveParameter(aParam, out);
		}
		out.write( "</" + param.getName() + ">" );
	}	

	public final Document m_XmlDoc;
}

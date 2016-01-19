package com.mitel.miutil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Represents configuration parameters. 
 * Structured as a tree, a parameter may be a single name/value pair or can be a container for other parameters.
 * Not thread safe.
 */
public class MiParameter
{
	
	private final List<MiParameter> m_SubParams;
	
	private final String m_Name;
	
	private final String m_Value;
	
	/**
	 * Creates a simple parameter consisting of a name and a value.
	 * @param name The name of the parameter.
	 * @param value The text value of the parameter.
	 */
	public MiParameter(java.lang.String name, java.lang.String value)
	{
		m_Name = name;
		m_Value = value;
		m_SubParams = new LinkedList<MiParameter>();
	}
	
	/**
	 * Creates a new parameter given its name, value, and optional sub-parameters. 
	 * @param name The name of the parameter.
	 * @param value The text value of the parameter.
	 * @param subParams The optional sub parameters.
	 */
	public MiParameter(String name, String value, List<MiParameter> subParams)
	{
		m_Name = name;
		m_Value = value;
		m_SubParams = subParams;
	}
	
	/**
	 * Creates a new parameter structure from an XML document. 
	 * @param doc An XML node to be parsed into the new parameter object.
	 */
	public MiParameter(Node doc)
	{
		m_Name = doc.getNodeName();
		m_Value = doc.getTextContent();
		m_SubParams = new LinkedList<MiParameter>();
		for( Node child = doc.getFirstChild(); child != null; child = child.getNextSibling())
		{
			parseChildNode(child);
		}
	}
	
	/**
	 * Exports the state of the parameter in XML format.
	 * @param ownerDoc The owner XML document from which the export XML fragment is to be created. 
	 * @return The XML element representing this parameter object.
	 */
	public Element exportParam(Document ownerDoc)
	{
		Element retValue = ownerDoc.createElement(getName());
		retValue.setTextContent(getValue());
		Iterator iter = m_SubParams.iterator();
		while(iter.hasNext())
		{
			MiParameter child = (MiParameter)iter.next();
			retValue.appendChild(child.exportParam(ownerDoc));
		}
		return retValue;
	}
	
	/**
	 * Retrieves the name of the parameter
	 * @return The name of the parameter. 
	 */
	public String getName()
	{
		return m_Name;
	}

	/**
	 * Retrieves the value of the parameter
	 * @return The value of the parameter
	 */
	public String getValue()
	{
		return m_Value;
	}

	/**
	 * Adds a sub-parameter.
	 * @param subParam A new sub parameter to add.
	 */
	public void addSubParam(MiParameter subParam)
	{
		m_SubParams.add(subParam);
	}
	
	/**
	 * Locates a sub-parameter by name, starting with the current parameter.
	 * @param subparamName Name of the desired parameter
	 * @return The requested parameter or null if none matching the given name was found. 
	 */
	public MiParameter getSubParam(String subparamName)
	{
		if(m_Name.equals(subparamName))
		{
			return this;
		}
		Iterator<MiParameter> iter = m_SubParams.iterator();
		while( iter.hasNext())
		{
			MiParameter aParam = iter.next();
			MiParameter retValue = aParam.getSubParam(subparamName);
			if( null != retValue )
			{
				return retValue;
			}
		}
		return null;
	}

	/**
	 * Retrieves an ArrayList that contains all sub-parameters.
	 * @return A copy of the sub parameter list. Never null.
	 */
	public List<MiParameter> getSubParams()
	{
		return new ArrayList<MiParameter>(m_SubParams);
	}	
	
	@Override
	public String toString()
	{
		StringBuilder retValue = new StringBuilder(128).append("<").append(m_Name).append(">").append(m_Value);
		Iterator iter = m_SubParams.iterator();
		while(iter.hasNext())
		{
			retValue.append(((MiParameter)iter.next()));
		}
		retValue.append("</").append(m_Name).append(">");
		return retValue.toString();
	}

	/**
	 * Given an XML node, this parses it into a MiParameter structure. 
	 * @param node The XML node to be parsed.
	 */
	private void parseChildNode(Node node)
	{
		if(node instanceof Element)
		{
			m_SubParams.add(new MiParameter(node));
		}
	}	
}


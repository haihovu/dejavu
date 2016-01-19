/*
 * MiConfig.java
 *
 * Created on October 18, 2004, 9:27 PM
 */

package com.mitel.miutil;
import java.util.*;
import java.util.Iterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



/**
 * Generic configuration policy. Simple flat arrays of name/value pairs.
 * @see com.mitel.miutil.MiAttributeDescriptor
 * @author Hai Vu
 */
public class MiConfig {
	
	/** Default constructor. Creates a new instance of MiConfig*/
	public MiConfig()
	{
	}

	/** Initialization constructor. Creates a new instance of MiConfig from a MiParameter structure
	 * @param param
	 */
	public MiConfig(MiParameter param)
	{
		List<MiParameter> params = param.getSubParams();
		if(null != params)
		{
			Iterator<MiParameter> iter = params.iterator();
			while(iter.hasNext())
			{
				MiParameter aParam = iter.next();
				MiAttributeDescriptor attDesc = MiAttributeDescriptor.locateAttribute(aParam.getName());
				if(null != attDesc)
					m_Attributes[attDesc.m_Id] = aParam.getValue();
				else
					MiSystem.logError(MiLogMsg.Category.DESIGN, "Could not locate attribute name for " + aParam.getName());
			}
		}
	}

	/**
	 * Copy constructor. 
	 * Creates a new instance of MiConfig from another MiConfig object
	 *
	 * @param aCopy
	 */
	public MiConfig(MiConfig aCopy)
	{
		for(int i = 0; i < m_Attributes.length; ++i)
			m_Attributes[i] = aCopy.m_Attributes[i];
	}

	/**
	 * Clones this MiConfig object. 
	 */
	@Override
	public Object clone()
	{
		return new MiConfig(this);
	}

	/**
	 * Retrieves the array of ALL attribute values from this configuration object. 
	 * The index into the array indicates the attribute ID (see MiConfigAttributeName), 
	 * while the value inside the array are the attribute values, in string format.
	 *
	 * @return
	 */
	@SuppressWarnings("ReturnOfCollectionOrArrayField")
	public Object[] getAttributes()
	{
		return m_Attributes;
	}

	/**
	 * Retrieves the value of an attribute in String format, identified by its descriptor.
	 * @param attDesc The descriptor of the desired attribute
	 * @return The value of the requested attribute, 
	 * or empty string if the given descriptor is invalid.
	 * Guarantee to return non-null value.
	 */
	public synchronized String getAttributeValueString(MiAttributeDescriptor attDesc)
	{
		return attDesc.objToString(getAttributeValue(attDesc));
	}

	/**
	 * Retrieves the value of an attribute, identified by its descriptor.
	 * @param attDesc The descriptor of the desired attribute
	 * @return The value of the requested attribute, or null if the given descriptor is invalid.
	 */
	public synchronized Object getAttributeValue(MiAttributeDescriptor attDesc)
	{
		try
		{
			return m_Attributes[attDesc.m_Id];
		}
		catch(RuntimeException e)
		{
			MiSystem.logError(MiLogMsg.Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
		return null;
	}

	/**
	 * Attempts to set the value of an attribute identified by its descriptor.
	 * @param attDesc The descriptor identifying the desired attribute
	 * @param value The value to set
	 */
	public synchronized void setAttributeValue(MiAttributeDescriptor attDesc, Object value)
	{
		if(attDesc != null) {
			try
			{
				m_Attributes[attDesc.m_Id] = value;
			}
			catch(RuntimeException e)
			{
				MiSystem.logError(MiLogMsg.Category.DESIGN, MiExceptionUtil.simpleTrace(e));
			}
		}
	}
	
	/**
	 * Updates a MiParameter structure with a snapshot of this MiConfig object.
	 * Typically used for saving/loading the states to/from files.
	 *
	 * @param outputParam
	 */
	public synchronized void getParam(MiParameter outputParam)
	{
		for(int i = 0; i < m_Attributes.length; ++i)
		{
			Object attValue = m_Attributes[i];
			if(attValue != null)
			{
				MiAttributeDescriptor attName = MiAttributeDescriptor.locateAttribute(i);
				if(null != attName)
				{
					outputParam.addSubParam(new MiParameter(attName.toString(), attValue.toString()));
				}
			}
		}
	}

	/**
	 * Adds the content of this config object to an existing XML element.
	 * Typically used for saving/loading the states to/from files.
	 * @param parentNode The parent XML element to which the content of this object is added.
	 */
	public synchronized void exportXml(Element parentNode)
	{
		if(parentNode == null)
			return;
		
		Document doc = parentNode.getOwnerDocument();
		
		if(doc == null)
			return;
		
		for(int i = 0; i < m_Attributes.length; ++i)
		{
			Object attValue = m_Attributes[i];
			if(attValue != null)
			{
				MiAttributeDescriptor attName = MiAttributeDescriptor.locateAttribute(i);
				if(null != attName)
				{
					Element el = doc.createElement(attName.toString());
					el.setTextContent(attValue.toString());
					parentNode.appendChild(el);
				}
			}
		}
	}

	/**
	 * Updates this MiConfig object with a MiParameter structure.
	 * Typically used for saving/loading the states to/from files.
	 *
	 * @param param
	 */
	public synchronized void setParam(MiParameter param)
	{
		List<MiParameter> params = param.getSubParams();
		if(null != params)
		{
			Iterator<MiParameter> iter = params.iterator();
			while(iter.hasNext())
			{
				MiParameter aParam = iter.next();
				MiAttributeDescriptor attName = MiAttributeDescriptor.locateAttribute(aParam.getName());
				if(null != attName)
				{
					setAttributeValue(attName, aParam.getValue());
				}
				else
				{
					MiSystem.logError(MiLogMsg.Category.DESIGN,
						"Failed to locate parameter name for " + aParam.getName());
				}
			}
		}
		else
		{
			MiSystem.logWarning(MiLogMsg.Category.DESIGN, "Parameter " + param + " contains no subnode");
		}
	}

	/**
	 * Updates this MiConfig object with the content of an XML node.
	 * Typically used for saving/loading the states to/from files.
	 * @param xmlContent The XML node containing the content from which the state of this object is updated.
	 */
	public synchronized void importXml(Node xmlContent)
	{
		if(xmlContent == null)
			return;

		NodeList nodes = xmlContent.getChildNodes();
		int numNodes = nodes.getLength();
		for(int i = 0; i < numNodes; ++i)
		{
			Element el = (Element)nodes.item(i);
			MiAttributeDescriptor attName = MiAttributeDescriptor.locateAttribute(el.getTagName());
			setAttributeValue(attName, el.getTextContent());
		}
	}

	/**
	 * Clears this configuration object of all attributes. 
	 */
	public synchronized void clear()
	{
		for(int i = 0; i < m_Attributes.length; ++i)
		{
			m_Attributes[i] = null;
		}
	}
	
	@Override
	public String toString()
	{
		StringBuilder retValue = new StringBuilder("(").append(getClass().getSimpleName());
		for(int i = 0; i < m_Attributes.length; ++i)
		{
			if(null != m_Attributes[i])
			{
				retValue.append(" Att").append(i).append("=\"").append(m_Attributes[i].toString()).append('"');
			}
		}
		return retValue.append(")").toString();
	}
	
	/**
	 * Map of all attributes contained in this config policy instance.
	 * The key is the Integer ID of the associated MiConfigAttributeName key.
	 */
	protected final Object[] m_Attributes = new Object[100];

	/** @link dependency */
    /*# MiParameter lnkMiParameter; */

	/** @link dependency */
    /*# MiAttributeDescriptor lnkMiAttributeDescriptor; */
}

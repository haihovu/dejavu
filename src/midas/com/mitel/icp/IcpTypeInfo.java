/*
 * IcpTypeInfo.java
 *
 * Created on June 16, 2004, 10:50 PM
 */

package com.mitel.icp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ICP type information, a type is a specific version of an ICP class.
 * ICP and MCD are used interchangeably in this context.
 * @author  Hai Vu
 */
public class IcpTypeInfo
{
	/**
	 * General classes of ICPs
	 */
	public static enum IcpClass
	{
		/**
		 * MCD hidden behind MBG
		 */
		MCD_MBG,
		/**
		 * x86 server without any TDM functionality
		 */
		MCD_Server,
		/**
		 * PPC platform wit E2T and RTC running on the same processor, the old 3300 lites (as well as the SX200).
		 */
		MCD_Mxe,
		/**
		 * PPC platform with external E2T processor, the old 3300 heavies.
		 */
		MCD_MxeExt,
		/**
		 * Hybrid x86 + PPC, where RTC is running in x86 (Atlas), plus E2T running in PPC.
		 */
		MCD_MxeServer,
		/**
		 * Unknown class
		 */
		UNKNOWN
	}
	@SuppressWarnings("SetReplaceableByEnumSet")
	private final Set<IcpProcessor.ProcessorName> m_processors = new HashSet<IcpProcessor.ProcessorName>(4);
	private final ReadWriteLock m_processorLock = new ReentrantReadWriteLock();
	private final String m_name;
	private final IcpClass icpClass;
	
	/** Creates a new instance of IcpTypeInfo
	 * @param typeName Name of the ICP type.
	 * @param icpClass The ICP class associated with this type. 
	 */
	public IcpTypeInfo(String typeName, IcpClass icpClass)
	{
		m_name = typeName;
		this.icpClass = icpClass;
		switch(icpClass)
		{
			case MCD_MBG:
				addProcessor(IcpProcessor.ProcessorName.RTC);
				break;
				
			case MCD_Mxe:
				addProcessor(IcpProcessor.ProcessorName.RTC);
				addProcessor(IcpProcessor.ProcessorName.E2T);
				break;
				
			case MCD_MxeExt:
				addProcessor(IcpProcessor.ProcessorName.RTC);
				addProcessor(IcpProcessor.ProcessorName.E2T);
				break;
				
			case MCD_MxeServer:
				addProcessor(IcpProcessor.ProcessorName.RTC);
				addProcessor(IcpProcessor.ProcessorName.E2T);
				break;
				
			case MCD_Server:
				addProcessor(IcpProcessor.ProcessorName.RTC);
				break;
		}
	}
	/**
	 * Retrieves the class of this ICP type
	 * @return The requested ICP class, not null.
	 */
	public IcpClass getIcpClass() {
		return icpClass != null ? icpClass : IcpClass.UNKNOWN;
	}
	
	@SuppressWarnings({"ReturnOfCollectionOrArrayField", "SetReplaceableByEnumSet"})
	public Set<IcpProcessor.ProcessorName> getProcessors()
	{
		Lock rlock = m_processorLock.readLock();
		if(rlock.tryLock())
		{
			try
			{
				return new HashSet<IcpProcessor.ProcessorName>(m_processors);
			}
			finally
			{
				rlock.unlock();
			}
		}
		return new HashSet<IcpProcessor.ProcessorName>(0);
	}
	
	private void addProcessor(IcpProcessor.ProcessorName processorName)
	{
		Lock wlock = m_processorLock.writeLock();
		if(wlock.tryLock())
		{
			try
			{
				m_processors.add(processorName);
			}
			finally
			{
				wlock.unlock();
			}
		}
	}
	
	/**
	 * Determines whether this ICP type has a particular processor.
	 * @param processorName The name of the desired processor, this may be a regular expression pattern.
	 * @return True if the requested process exists in this ICP type, false otherwise.
	 */
	public boolean hasProcessor(String processorName)
	{
		Lock rlock = m_processorLock.readLock();
		if(rlock.tryLock())
		{
			try
			{
				IcpProcessor.ProcessorName name = IcpProcessor.ProcessorName.string2Name(processorName);
				if((name != IcpProcessor.ProcessorName.UNKNOWN)&&(m_processors.contains(name)))
				{
					return true;
				}

				// Look for possible pattern
				Iterator<IcpProcessor.ProcessorName> iter = m_processors.iterator();
				while(iter.hasNext())
				{
					IcpProcessor.ProcessorName procName = iter.next();
					if(procName.value.matches(processorName))
						return true;
				}
			}
			finally
			{
				rlock.unlock();
			}
		}
		return false;
	}
	
	/**
	 * Determines whether this ICP type has a particular processor.
	 * @param processorName The name of the desired processor.
	 * @return True if the requested process exists in this ICP type, false otherwise.
	 */
	public boolean hasProcessor(IcpProcessor.ProcessorName processorName)
	{
		Lock rlock = m_processorLock.readLock();
		if(rlock.tryLock())
		{
			try
			{
				if(m_processors.contains(processorName))
					return true;
			}
			finally
			{
				rlock.unlock();
			}
		}
		return false;
	}
	
	/**
	 * Retrieve the ICP type name.
	 * @return The ICP type name, not null.
	 */
	public String getName()
	{
		return m_name != null ? m_name : "";
	}
	
	@Override
	public String toString()
	{
		StringBuffer retValue = new StringBuffer("<Name>").append(m_name).append("</Name>");
		if(m_processors.size() > 0)
		{
			retValue.append("<ProcessorList>");
		}
		Iterator iter = m_processors.iterator();
		while(iter.hasNext())
		{
			retValue.append("<Processor>").append(iter.next()).append("</Processor>");
		}
		if(m_processors.size() > 0)
		{
			retValue.append("</ProcessorList>");
		}
		return retValue.toString();
	}
	
}

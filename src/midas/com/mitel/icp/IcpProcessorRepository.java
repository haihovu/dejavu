/*
 * IcpProcessorRepository.java
 *
 * Created on June 20, 2004, 8:42 PM
 */

package com.mitel.icp;
import com.mitel.miutil.MiLogMsg;
import com.mitel.miutil.MiSystem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ICP processor types repository.
 * @author  Hai Vu
 */
public class IcpProcessorRepository
{
	
	private static final IcpProcessorRepository gSingleton = new IcpProcessorRepository();
	private final Map<IcpProcessor.ProcessorName, IcpProcessor> m_processors = new HashMap<IcpProcessor.ProcessorName, IcpProcessor>(4);
	private final ReadWriteLock processorLock = new ReentrantReadWriteLock();
	
	/** Creates a new instance of IcpProcessorRepository */
	public IcpProcessorRepository()
	{
		super();
	}
	
	/**
	 * Retrieves the singleton instance.
	 * @return The singleton instance, non-null.
	 */
	public static IcpProcessorRepository getInstance()
	{
		return gSingleton;
	}
	
	/**
	 * Retrieves a processor type.
	 * @param processorName Name of the desired processor type, this may be a regexp.
	 * @return The requested processor type record, or null if no match is found.
	 */
	public IcpProcessor locateProcessor(IcpProcessor.ProcessorName processorName)
	{
		Lock rlock = processorLock.readLock();
		if(rlock.tryLock())
		{
			try
			{
				return m_processors.get(processorName);
			}
			finally
			{
				rlock.unlock();
			}
		}
		return null;
	}
	
	/**
	 * Retrieves a processor type.
	 * @param processorName Name of the desired processor type, this may be a regexp.
	 * @return The requested processor type record, or null if no match is found.
	 */
	public IcpProcessor locateProcessor(String processorName)
	{
		Lock rlock = processorLock.readLock();
		if(rlock.tryLock())
		{
			try
			{
				IcpProcessor.ProcessorName name = IcpProcessor.ProcessorName.string2Name(processorName);
				if(name != IcpProcessor.ProcessorName.UNKNOWN)
				{
					return m_processors.get(name);
				}

				// Look for pattern
				Iterator<IcpProcessor> iter = m_processors.values().iterator();
				while(iter.hasNext())
				{
					IcpProcessor proc = iter.next();
					synchronized(proc)
					{
						if(proc.getName().value.matches(processorName))
						{
							return proc;
						}
					}
				}
			}
			finally
			{
				rlock.unlock();
			}
		}
		return null;
	}
	
	/**
	 * Adds a new processor type.
	 * @param processorName Name of the processor type
	 * @param proc The processor type record.
	 */
	public void addProcessor(IcpProcessor.ProcessorName processorName, IcpProcessor proc)
	{
		Lock wlock = processorLock.writeLock();
		if(wlock.tryLock())
		{
			try
			{
				MiSystem.logInfo(MiLogMsg.Category.DESIGN, "Adding ICP processor template " + proc);
				m_processors.put(processorName, proc);
			}
			finally
			{
				wlock.unlock();
			}
		}
	}
	
	static
	{
		// Only the following two ICP processor types exist.
		getInstance().addProcessor(IcpProcessor.ProcessorName.RTC, new IcpProcessor(IcpProcessor.ProcessorName.RTC, 2002));
		getInstance().addProcessor(IcpProcessor.ProcessorName.E2T, new IcpProcessor(IcpProcessor.ProcessorName.E2T, 2002));
	}
	
}

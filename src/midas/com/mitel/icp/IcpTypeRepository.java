/*
 * IcpTypeRepository.java
 *
 * Created on April 28, 2004, 11:53 AM
 */

package com.mitel.icp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Repository for all concrete ICP types. Concrete ICP type implementations
 * register with the repository at instantiation time.
 * @author haiv
 */
public class IcpTypeRepository
{
	private final Map<String, IcpType> m_IcpMap = new HashMap<String, IcpType>(16);
	private final ReadWriteLock m_IcpMapLock = new ReentrantReadWriteLock();
	private IcpType m_CurrentIcpApi;
	private static final IcpTypeRepository gSingleton = new IcpTypeRepository();
	
	/** Creates a new instance of IcpTypeRepository */
	private IcpTypeRepository()
	{
		super();
	}
	
	/**
	 * Locates an ICP type from the repository.
	 * @param anIcpType The name of the desired ICP type
	 * @return The requested ICP type record, or null if the desired type has not been registered in the repository.
	 */
	public IcpType locateIcpType(String anIcpType)
	{
		Lock rlock = m_IcpMapLock.readLock();
		if(rlock.tryLock())
		{
			try
			{
				if(null != anIcpType)
					return m_IcpMap.get(anIcpType);
			}
			finally
			{
				rlock.unlock();
			}
		}
		return null;
	}
	
	/**
	 * Registers a new ICP type with the repository.
	 * @param newType The new ICP type to be registered.
	 */
	public void registerIcpType(IcpType newType)
	{
		Lock wlock = m_IcpMapLock.writeLock();
		if(wlock.tryLock())
		{
			try
			{
				m_IcpMap.put(newType.getTypeInfo().getName(), newType);
				if( null == m_CurrentIcpApi )
				{
					m_CurrentIcpApi = newType;
				}
			}
			finally
			{
				wlock.unlock();
			}
		}
	}
	
	/**
	 * Removes an ICP type from the repository.
	 * @param type The type record to be deregistered from the repository.
	 */
	public void deregisterIcpType(IcpType type)
	{
		Lock wlock = m_IcpMapLock.writeLock();
		if(wlock.tryLock())
		{
			try
			{
				m_IcpMap.remove(type.getTypeInfo().getName());
			}
			finally
			{
				wlock.unlock();
			}
		}
	}	

	/**
	 * Retrieves the list of all registered ICP types, by names.
	 * @return The array representing all registered ICP type names.
	 */
	public Object[] getRegisteredTypes()
	{
		Lock rlock = m_IcpMapLock.readLock();
		if(rlock.tryLock())
		{
			try
			{
				return m_IcpMap.keySet().toArray();
			}
			finally
			{
				rlock.unlock();
			}
		}
		return new String[0];
	}

	/**
	 * Retrieves the singleton instance of the ICP type repository.
	 * @return The singleton instance. Not null.
	 */
	public static IcpTypeRepository getInstance()
	{
		return gSingleton;
	}	
}

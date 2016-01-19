/*
 * IcpRepositoryDefault.java
 *
 * Created on June 17, 2005, 4:10 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.mitel.icp;

import com.mitel.icp.IcpRepository.RepositoryObserver;
import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiParameter;
import com.mitel.miutil.MiParameterCollection;
import com.mitel.miutil.MiSystem;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.xml.sax.SAXException;

/**
 * A default implementation of the ICP repository.
 * This is used by the IcpRepository if no other concrete instance was registered.
 * @author haiv
 */
public class IcpRepositoryDefault extends IcpRepository 
{
	/**
	 * Creates a new instance of IcpRepositoryDefault 
	 */
	public IcpRepositoryDefault()
	{
		super();
		loadCfg();
	}
	
	@Override
	public void setIcpList(List<IcpDescriptor> icpList)
	{
		synchronized(m_savedIcps)
		{
			// Replicate the ICP list just passed in
			m_savedIcps.clear();
			for(IcpDescriptor icpDesc:icpList)
			{
				m_savedIcps.put(icpDesc.getName(), new IcpDescriptor(icpDesc));
			}
		}
		fireRepositoryUpdateEvent(icpList);
	}

	@Override
	public List<IcpDescriptor> getIcpList()
	{
		synchronized(m_savedIcps)
		{
			return new ArrayList<IcpDescriptor>(m_savedIcps.values());
		}
	}

	@Override
	public IcpDescriptor getIcp(String icpName)
	{
		if(null != icpName)
		{
			synchronized(m_savedIcps)
			{
				return m_savedIcps.get(icpName);
			}
		}
		return null;
	}

	@Override
	public void setIcp(IcpDescriptor newIcp)
	{
		IcpDescriptor existingIcp;
		Collection<IcpDescriptor> icps = null;
		synchronized(m_savedIcps)
		{
			existingIcp = m_savedIcps.get(newIcp.getName());
			if(existingIcp == null)
			{
				// A new ICP was added
				m_savedIcps.put(newIcp.getName(), newIcp);
				icps = m_savedIcps.values();
			}
		}
		if(null != existingIcp)
		{
			synchronized(existingIcp)
			{
				existingIcp.copy(newIcp);
				fireRepositoryUpdateEvent(existingIcp);
			}
		}
		else if(icps != null)
		{
			fireRepositoryUpdateEvent(icps);
		}
	}

	@Override
	public void removeIcp(String icpName) throws IcpException
	{
		IcpDescriptor icp;
		synchronized(m_savedIcps)
		{
			icp = m_savedIcps.remove(icpName);
		}
		fireRepositoryDeleteEvent(icp);
	}

	@Override
	public boolean isIcpInList(String icpName)
	{
		synchronized(m_savedIcps)
		{
			return m_savedIcps.containsKey(icpName);
		}
	}

	@Override
	public boolean isEmpty()
	{
		synchronized(m_savedIcps)
		{
			return m_savedIcps.isEmpty();
		}
	}

	@Override
	public void fireRepositoryUpdateEvent(Collection icpList)
	{
		synchronized(m_Observers)
		{
			for(RepositoryObserver observer:m_Observers)
			{
				observer.icpListUpdated(icpList);
			}
		}
		saveCfg();
	}

	@Override
	public void fireRepositoryUpdateEvent(IcpDescriptor icp)
	{
		synchronized(m_Observers)
		{
			for(RepositoryObserver observer:m_Observers)
			{
				observer.icpUpdated(icp);
			}
		}
		saveCfg();
	}

	@Override
	public void fireRepositoryDeleteEvent(IcpDescriptor icp) throws IcpException
	{
		synchronized(m_Observers)
		{
			for(RepositoryObserver observer:m_Observers)
			{
				observer.icpRemoved(icp);
			}
		}
		saveCfg();
	}

	@Override
	public void addObserver(RepositoryObserver observer)
	{
		synchronized(m_Observers)
		{
			m_Observers.add(observer);
		}
	}

	@Override
	public void removeObserver(RepositoryObserver observer)
	{
		synchronized(m_Observers)
		{
			m_Observers.remove(observer);
		}
	}

	private void loadCfg()
	{
		try
		{
			// Get ICP config
			MiParameter icps = new MiParameterCollection(new File(ICP_CONFIG_FILE)).getParameter("Icps");
			setConfigParameter(icps);
		}
		catch( IOException e )
		{
			MiSystem.logInfo(Category.DESIGN, "File " + ICP_CONFIG_FILE + " not found, use default.");
			try
			{
				// Get ICP config
				MiParameter icps = new MiParameterCollection (new File(ICP_CONFIG_FILE)).getParameter("Icps");
				setConfigParameter(icps);
			}
			catch(IOException e2)
			{
				MiSystem.logWarning(Category.DESIGN, "Default config file not found.");
			}
			catch(SAXException ex)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
			}
		}
		catch(SAXException ex)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
		catch(RuntimeException e)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
	}

	private void saveCfg()
	{
		try
		{			
			// Now save the ICP config parameter to a file
			MiParameter top = new MiParameter("IcpConfig", "");
			top.addSubParam(getConfigParameter());
			
			new MiParameterCollection(top).saveFile(new File(ICP_CONFIG_FILE));
		}
		catch( IOException e )
		{
			System.out.println("ERROR: " + getClass().getName() + ".saveCfg() - Failed to save ICP info due to " + e);
		}
	}

	/**
	 * Retrieves the config parameter for this object.
	 * @return The config parameter for this object.
	 */
	private MiParameter getConfigParameter()
	{
		MiParameter retValue = new MiParameter("Icps","");
		synchronized(m_savedIcps)
		{
			Collection<IcpDescriptor> icps = m_savedIcps.values();
			for(IcpDescriptor icp : icps)
			{
				retValue.addSubParam(icp.getConfigParameter());
			}
		}
		return retValue;
	}

	/**
	 * Updates the state of this object with a config parameter.
	 * @param param The parameter to set.
	 */
	private void setConfigParameter(MiParameter param)
	{
		if((null != param)&&(param.getName().equals("Icps")))
		{
			Collection<IcpDescriptor> savedIcps;
			synchronized(m_savedIcps)
			{
				m_savedIcps.clear();
				List<MiParameter> params = param.getSubParams();
				for(MiParameter aParam : params)
				{
					IcpDescriptor icp = new IcpDescriptor(aParam);
					m_savedIcps.put(icp.getName(), icp);
				}
				savedIcps = m_savedIcps.values();
			}
			fireRepositoryUpdateEvent(savedIcps);
		}
	}

	private final AbstractMap<String, IcpDescriptor> m_savedIcps = new HashMap<String, IcpDescriptor>(128);
	private final List<RepositoryObserver> m_Observers = new LinkedList<RepositoryObserver>();
	private static final String ICP_CONFIG_FILE = System.getProperty("user.home") + "/icp.cfg";
}

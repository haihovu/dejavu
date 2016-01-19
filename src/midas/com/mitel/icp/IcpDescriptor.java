package com.mitel.icp;

import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiParameter;
import com.mitel.miutil.MiSystem;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Descriptive attributes of individual ICP's, including things like ICP type, host
 * name, user ID, password, ...
 * Each ICP descriptor identifies a specific ICP.
 */
public class IcpDescriptor implements Comparable<IcpDescriptor>
{
	/**
	 * Arbitrary name for the ICP.
	 * Used for identification, not to be confused with host name.
	 */
	private String m_icpName;
	/**
	 * Registration code for IP devices.
	 */
	private String m_RegistrationCode = "###";
	/**
	 * For valid type see IcpService.
	 */
	private String icpTypeName = "Undefined";	
	private String m_MiNetVersion = "1.2.9";
	
	private static final Pattern gSerializedIcpPattern = Pattern.compile("([^,]+),([^,]+),([^,]+),([0-9\\.]*),([0-9\\.]*),([0-9\\.]*),([0-9\\.]*)");
	private static final Pattern gIpAddrPattern = Pattern.compile("([1-9][0-9]*\\.){3}([1-9][0-9]*)");
			
	/**
	 * Map of IcpProcessor objects keyed by their names
	 */	
	private final Map<IcpProcessor.ProcessorName, IcpProcessor> m_processors = new EnumMap<IcpProcessor.ProcessorName, IcpProcessor>(IcpProcessor.ProcessorName.class);
	
	/**
	 * Creates an uninitialized ICP descriptor.
	 */
	public IcpDescriptor()
	{
	}
	
	/**
	 * Creates a new ICP descriptor.
	 * 
	 * @param icpName The arbitrary name of the ICP, not to be confused with its host name.
	 * @param minetVer The highest MiNET version currently supported by the ICP.
	 * @param regCode The set registration/replacement code for this ICP.
	 * @param rtcAddress The address of the main processor of this ICP 
	 * (i.e. the RTC card or the hostname/address of the Linux call server)
	 * @param e2tAddress The optional E2T address of this ICP. Specify null if no E2T is available.
	 * @param icpType Type of this ICP, see IcpService for possible values.
	 * @param userId Optional system user on the ICP (for use with telnet)
	 * @param password Optional system user password (for use with telnet).
	 */
	public IcpDescriptor(String icpName, String minetVer, String regCode, String rtcAddress
		, String e2tAddress, String icpType, String userId, String password)
	{
		m_icpName = icpName;
		m_RegistrationCode = regCode;
		m_MiNetVersion = minetVer;
		
		setType(icpType);
		IcpProcessor rtc = locateProcessor(IcpProcessor.ProcessorName.RTC);
		if(null != rtc)
		{
			setAddress(rtc.getName(), rtcAddress);
		}

		if((null != e2tAddress)&&(hasProcessor(IcpProcessor.ProcessorName.E2T)))
		{
			setAddress(IcpProcessor.ProcessorName.E2T, e2tAddress);
		}

		if(rtc != null) {
			setUserId(rtc.getName(), userId);
			setPassword(rtc.getName(), password);
		}
	}

	/**
	 * Creates a new ICP descriptor.
	 * @param icpName The arbitrary name of the ICP, not to be confused with its host name.
	 * @param minetVer The highest MiNET version currently supported by the ICP.
	 * @param regCode The set registration/replacement code for this ICP.
	 * @param rtcAddress The address of the main processor of this ICP 
	 * (i.e. the RTC card or the hostname/address of the Linux call server)
	 * @param e2tAddress The optional E2T address of this ICP. Specify null if no E2T is available.
	 * @param icpType Type of this ICP, see IcpService for possible values.
	 */
	public IcpDescriptor(java.lang.String icpName, String minetVer, String regCode
		, java.lang.String rtcAddress, java.lang.String e2tAddress, java.lang.String icpType)
	{
		m_icpName = icpName;
		m_RegistrationCode = regCode;
		if(null != minetVer)
		{
			m_MiNetVersion = minetVer;
		}
		
		setType(icpType);
		IcpProcessor rtc = locateProcessor(IcpProcessor.ProcessorName.RTC);
		if(null != rtc)
		{
			setAddress(rtc.getName(), rtcAddress);
		}

		if((null != e2tAddress)&&(hasProcessor(IcpProcessor.ProcessorName.E2T)))
		{
			setAddress(IcpProcessor.ProcessorName.E2T, e2tAddress);
		}
	}
	
	/**
	 * Copy constructor, creates a duplicate of an original ICP descriptor.
	 * @param copy The original descriptor from which a duplicate is made.
	 */
	public IcpDescriptor(IcpDescriptor copy)
	{
		if( null != copy )
		{
			m_icpName = copy.m_icpName;
			icpTypeName = copy.icpTypeName;
			m_RegistrationCode = copy.m_RegistrationCode;
			m_MiNetVersion = copy.m_MiNetVersion;
			
			m_processors.clear();
			Iterator<IcpProcessor> iter = copy.m_processors.values().iterator();
			while(iter.hasNext())
			{
				IcpProcessor newproc = new IcpProcessor(iter.next());
				addProcessor(newproc);
			}
		}
	}
	
	/**
	 * Creates an ICP descriptor, and fill it with the data from a configuration object.
	 * @param config The configuration object from which initialization data is acquired.
	 */
	public IcpDescriptor(MiParameter config)
	{
		MiParameter type = config.getSubParam("IcpType");
		if(null != type)
		{
			setType(type.getValue());
		}
		
		MiParameter icpName = config.getSubParam("IcpName");
		if(null != icpName)
		{
			m_icpName = icpName.getValue();
		}
		
		MiParameter setRegCode = config.getSubParam("SetRegCode");
		if(null != setRegCode)
		{
			m_RegistrationCode = setRegCode.getValue();
		}
		
		MiParameter setMiNet = config.getSubParam("MiNetVersion");
		if(null != setMiNet)
		{
			m_MiNetVersion = setMiNet.getValue();
		}

		// DEPRECATED - only used for backward compatibility
		MiParameter hostAddress = config.getSubParam("HostAddress");
		if(null != hostAddress)
		{
			try
			{
				setAddress(IcpProcessor.ProcessorName.RTC, hostAddress.getValue());
			}
			catch(RuntimeException e)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
			}
		}
		
		MiParameter procs = config.getSubParam("IcpProcessorList");
		if(null != procs)
		{
			try
			{
				Iterator iter = procs.getSubParams().iterator();
				while(iter.hasNext())
				{
					MiParameter proc = (MiParameter)iter.next();
					addProcessor(new IcpProcessor(proc));
				}
				if(type != null) {
					icpTypeName = type.getValue();
				}
			}
			catch(RuntimeException e)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
			}
		}
	}	

	/**
	 * Copies the content of one ICP descriptor into another.
	 * @param copy The source ICP descriptor whose state will be copied into this one.
	 * @return This object.
	 */
	public IcpDescriptor copy(IcpDescriptor copy)
	{
		if((copy == null)||(copy == this))
			return this;

		// Synchronize the copy and this object separately to avoid dead-lock.
		String newName;
		String newType;
		String newRegCode;
		String newMiNet;
		List<IcpProcessor> newProcessors;
		synchronized(copy)
		{
			newName = copy.m_icpName;
			newType = copy.icpTypeName;
			newRegCode = copy.m_RegistrationCode;
			newMiNet = copy.m_MiNetVersion;
			newProcessors = copy.getProcessorList();
		}
		synchronized(this)
		{
			m_icpName = newName;
			icpTypeName = newType;
			m_RegistrationCode = newRegCode;
			m_MiNetVersion = newMiNet;
			m_processors.clear();
			for(IcpProcessor proc:newProcessors)
			{
				m_processors.put(proc.getName(), proc);
			}
		}
		return this;
	}	

	/**
	 * Retrieves the set registration/replacement code.
	 * @return The set registration/replacement code. Non-null.
	 */
	public String getRegistrationCode()
	{
		synchronized(this)
		{
			return (m_RegistrationCode != null?m_RegistrationCode:"");
		}
	}

	/**
	 * Sets the set registration/replacement code for this ICP.
	 * @param newCode The new registration/replacement code to set.
	 * @return This object 
	 */
	public IcpDescriptor setRegistrationCode(String newCode)
	{
		synchronized(this)
		{
			m_RegistrationCode = newCode;
		}
		return this;
	}

	/**
	 * Retrieves the configuration object for this ICP.
	 * The clients may do as they wish with the returned object.
	 * @return The configuration object containing the state of this ICP descriptor.
	 */
	public MiParameter getConfigParameter()
	{
		MiParameter retValue = new MiParameter("IcpDesc","");
		synchronized(this)
		{
			retValue.addSubParam(new MiParameter("IcpName",m_icpName));
			retValue.addSubParam(new MiParameter("SetRegCode",m_RegistrationCode));
			retValue.addSubParam(new MiParameter("MiNetVersion",m_MiNetVersion));
			retValue.addSubParam(new MiParameter("IcpType",icpTypeName));
			MiParameter procList = new MiParameter("IcpProcessorList", "");
			Collection<IcpProcessor> procs = m_processors.values();
			for(IcpProcessor proc : procs)
			{
				procList.addSubParam(proc.getConfigParameter());
			}
			retValue.addSubParam(procList);
		}
		return retValue;
	}
	
	/**
	 * Locates a processor in this ICP, depending on the type of the ICP there may be one or more processor(s) available.
	 * E.g. on a heavy ICP there are RTC and E2T processors, on the light ICPs and LiCS there is only the RTC (or its equivalent).
	 * For valid processor names, see IcpProcessor.
	 * Note that this only looks up a database, the IcpProcessorRepository, to get the
	 * data; this method does not contact the actual ICP.
	 * @param name The name of the desired processor.
	 * @return The processor record identified by the given name.
	 */
	public final IcpProcessor locateProcessor(IcpProcessor.ProcessorName name)
	{
		synchronized(this)
		{
			return m_processors.get(name);
		}
	}

	/**
	 * Determines whether the ICP described by this descriptor contains a particular processor.
	 * @param processorName The name of the queried processor, for valid processor names see IcpProcessor.
	 * @return True if the specified processor exists, false otherwise.
	 * Note that this only looks up a database, the IcpProcessorRepository, to get the
	 * data; this method does not contact the actual ICP.
	 */
	public final boolean hasProcessor(IcpProcessor.ProcessorName processorName)
	{
		return (null != locateProcessor(processorName));
	}

	/**
	 * Retrieves the list of processors available on this ICP.
	 * Note that this only looks up a database, the IcpProcessorRepository, to get the
	 * data; this method does not contact the actual ICP.
	 * @return The list of processor(s) associated with this ICP.
	 */
	public List<IcpProcessor> getProcessorList()
	{
		synchronized(this)
		{
			return new ArrayList<IcpProcessor>(m_processors.values());
		}
	}

	/**
	 * Retrieves the address of the specified processor inside the ICP (note that each ICP may have one or more processors with separate addresses).
	 * The ICP processor identifiers are defined in class IcpProcessor. 
	 * The identifiers are actually specified as regular expression so that RTC will match only "Rtc", but RTC_LITE with match both "Rtc" and "Lite".
	 * @param processorName The identifier of the desired processor, see IcpProcessor for possible identifier values.
	 * @return Returns the string representation of the address of the specified processor. This can be IP addresses or host names.
	 * If no processor matches the identifier given, an empty string is returned.
	 */
	public String getAddress(IcpProcessor.ProcessorName processorName)
	{
		synchronized(this)
		{
			IcpProcessor proc = locateProcessor(processorName);
			return (proc != null?proc.m_address:"");
		}
	}
	
	/**
	 * Retrieves the address of the main processor, in the case of the 3300 it's the RTC address, in the case of MCD, it's the main board's address.
	 * @return Returns the string representation of the address of the main processor of the ICP, or empty string if none can be found.
	 */
	public String getAddress()
	{
		synchronized(this)
		{
			IcpProcessor proc = locateProcessor(IcpProcessor.ProcessorName.RTC);
			return (proc != null ? proc.getAddress() : "");
		}
	}
	
	/**
	 * Sets the address for a particular processor on this ICP.
	 * @param processorName The name of the processor whose address is to be set. 
	 * See IcpProcessor for valid processor names.
	 * @param address The new address to set.
	 * @return This object
	 */
	public final IcpDescriptor setAddress(IcpProcessor.ProcessorName processorName, String address)
	{
		synchronized(this)
		{
			IcpProcessor proc = locateProcessor(processorName);
			if(null != proc)
			{
				proc.m_address = address;
			}
			else
			{
				MiSystem.logInfo(Category.DESIGN, new StringBuilder("Attempting to set ")
					.append(address).append(" to non-existent processor ")
					.append(processorName).append(" of ").toString());
			}
		}
		return this;
	}

	/**
	 * Retrieves the name of this ICP, just an arbitrary string used for ease of identification by human users,
	 * not to be confused with the ICP's host name.
	 * @return The name of this ICP, non-null.
	 */
	public String getName()
	{
		synchronized(this)
		{
			return (m_icpName != null?m_icpName:"");
		}
	}

	/**
	 * Sets the name of this ICP, just an arbitrary string used for ease of identification by human users,
	 * not to be confused with the ICP's host name.
	 * @param newName The new name of this ICP.
	 * @return This object.
	 */
	public IcpDescriptor setName(String newName)
	{
		synchronized(this)
		{
			m_icpName = newName;
		}
		return this;
	}

	/**
	 * Retrieves the highest version of MiNET supported by this ICP.
	 * @return The MiNET version. Non-null.
	 */
	public String getMiNetVersion()
	{
		synchronized(this)
		{
			return (m_MiNetVersion != null?m_MiNetVersion:"");
		}
	}
	
	/**
	 * Sets the highest version of MiNET supported by this ICP.
	 * @param newVersion The new MiNET version to set.
	 * @return This object
	 */
	public IcpDescriptor setMiNetVersion(String newVersion)
	{
		synchronized(this)
		{
			m_MiNetVersion = newVersion;
		}
		return this;
	}
	
	/**
	 * Retrieves the system user password for the particular processor on this ICP.
	 * @param processorName The name of processor against which the query is directed.
	 * @return The desired password, guaranteed to be non-null.
	 */
	public String getPassword(IcpProcessor.ProcessorName processorName)
	{
		synchronized(this)
		{
			IcpProcessor proc = locateProcessor(processorName);
			return (proc != null?(proc.m_password != null?proc.m_password:""):"");
		}
	}
	
	/**
	 * Sets the system user password for the particular processor on this ICP.
	 * @param processorName The name of processor against which the password is set.
	 * @param password The password to set.
	 * @return This object.
	 */
	public final IcpDescriptor setPassword(IcpProcessor.ProcessorName processorName, String password)
	{
		synchronized(this)
		{
			IcpProcessor proc = locateProcessor(processorName);
			if(null != proc)
			{
				proc.m_password = password;
			}
			else
			{
				MiSystem.logInfo(Category.DESIGN, "Cannot locate processor "
					+ processorName + " for " + toString());
			}
		}
		return this;
	}
	
	/**
	 * Retrieves the type of this ICP, see IcpProcessor for valid types.
	 * @return The type of this ICP, guaranteed to be non-null.
	 */
	public String getType()
	{
		synchronized(this)
		{
			return (icpTypeName != null?icpTypeName:"");
		}
	}

	/**
	 * Retrieves the type of this ICP, see IcpProcessor for valid types.
	 * @return The type of this ICP, guaranteed to be non-null.
	 */
	public IcpTypeInfo getTypeInfo()
	{
		IcpType typeRec = IcpTypeRepository.getInstance().locateIcpType(getType());
		synchronized(this) {
			return typeRec != null ? typeRec.getTypeInfo() : new IcpTypeInfo(icpTypeName, IcpTypeInfo.IcpClass.MCD_Server);
		}
	}

	/**
	 * Sets the type of this ICP, see IcpProcessor for valid types.
	 * @param type The type of the ICP.
	 * @return This object.
	 */
	public final IcpDescriptor setType(String type)
	{
		synchronized(this)
		{
			IcpType icpTypeRec = IcpTypeRepository.getInstance().locateIcpType(type);
			if(null != icpTypeRec)
			{
				IcpTypeInfo icpType = icpTypeRec.getTypeInfo();
				icpTypeName = icpType.getName();

				// Add any missing processors
				for(IcpProcessor.ProcessorName procName : icpType.getProcessors())
				{
					if(!hasProcessor(procName))
					{
						IcpProcessor template = IcpProcessorRepository.getInstance().locateProcessor(procName);
						if(template != null)
						{
							addProcessor(new IcpProcessor(template));
						}
						else
						{
							MiSystem.logError(Category.DESIGN, "Failed to locate ICP processor template for " + procName);
						}
					}
				}

				// Remove all unused processor
				Iterator<IcpProcessor> iter2 = m_processors.values().iterator();
				while(iter2.hasNext())
				{
					IcpProcessor proc = iter2.next();
					if(!icpType.hasProcessor(proc.getName()))
					{
						iter2.remove();
					}
				}
			}
			else
			{
				MiSystem.logWarning(Category.DESIGN, "ICP type '" + type + "' not found in repository");
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.getCompressedTrace());
			}
		}
		return this;
	}

	/**
	 * Retrieves the system user name on this ICP, each ICP has the concept of a
	 * system user who can log on via ESM or telnet to perform administrative tasks.
	 * It is conceivable that each processor on the ICP may have a different system
	 * user name/password but typically they are set the the same values.
	 * @param processorName The processor against which the query is directed.
	 * @return The string name of the system user, guaranteed to be non-null.
	 */
	public String getUserId(IcpProcessor.ProcessorName processorName)
	{
		IcpProcessor proc = locateProcessor(processorName);
		return (proc != null?(proc.m_userId != null?proc.m_userId:""):"");
	}

	/**
	 * Sets the system user name on this ICP, each ICP has the concept of a
	 * system user who can log on via ESM or telnet to perform administrative tasks.
	 * It is conceivable that each processor on the ICP may have a different system
	 * user name/password but typically they are set the the same values.
	 * @param processorName The processor against which the name is set.
	 * @param userId The system user name to set.
	 * @return This object
	 */
	public final IcpDescriptor setUserId(IcpProcessor.ProcessorName processorName, String userId)
	{
		if(userId != null)
		{
			IcpProcessor proc = locateProcessor(processorName);
			if(null != proc)
			{
				proc.m_userId = userId;
			}
			else
			{
				MiSystem.logWarning(Category.DESIGN, "Cannot locate processor "
					+ processorName + " for " + toString());
			}
		}
		return this;
	}	
	
	/**
	 * Retrieves the flag that indicates whether the user name/password had been validated on the specified processor.
	 * I.e. did we successfully log into the said processor on the ICP?
	 * @param processorName The processor against which the query is targeted.
	 * @return True if the user name/password had been validated on the specified processor of this ICP.
	 */
	public boolean getValidated(IcpProcessor.ProcessorName processorName)
	{
		IcpProcessor proc = locateProcessor(processorName);
		if(null != proc)
		{
			return proc.m_validated;
		}
		return false;
	}

	/**
	 * Set the flag that indicates whether the user name/password had been validated on the specified processor.
	 * I.e. did we successfully log into the said processor on the ICP?
	 * @param processorName The processor against which the flag is set.
	 * @param validated The flag to set.
	 * @return This object
	 */
	public IcpDescriptor setValidated(IcpProcessor.ProcessorName processorName, boolean validated)
	{
		IcpProcessor proc = locateProcessor(processorName);
		if(null != proc)
		{
			proc.m_validated = validated;
		}
		else
		{
			MiSystem.logInfo(Category.DESIGN, "Cannot locate processor " + processorName
				+ " for " + toString());
		}
		return this;
	}	
	
	/**
	 * Serializes the state of this ICP descriptor into a string (CSV format).
	 * @return The serialized string representing this ICP descriptor.
	 */
	public String serialized()
	{
		synchronized(this)
		{
			StringBuilder retValue = new StringBuilder(m_icpName).append(",")
				.append(icpTypeName).append(",").append(m_RegistrationCode);

			// The order of processors exported is RTC then E2T then LITE
			IcpProcessor proc = m_processors.get(IcpProcessor.ProcessorName.RTC);
			if(null != proc)
			{
				IcpProcessor Rtc = proc;
				retValue.append(",").append(Rtc.m_address);
			}
			else
			{
				retValue.append(",");
			}

			proc = m_processors.get(IcpProcessor.ProcessorName.E2T);
			if(null != proc)
			{
				IcpProcessor E2t = proc;
				retValue.append(",").append(E2t.m_address);
			}
			else
			{
				retValue.append(",");
			}

			retValue.append(",").append(m_MiNetVersion);
			return retValue.toString();
		}
	}

	@Override
	public String toString()
	{
		synchronized(this)
		{
			StringBuilder retValue = new StringBuilder(getName()).append(".").append(getType());
			Iterator iter = m_processors.values().iterator();
			while(iter.hasNext())
			{
				IcpProcessor proc = (IcpProcessor)iter.next();
				retValue.append(".").append(proc.toString());
			}
			return retValue.toString();
		}
	}	

	/**
	 * De-serializes a string representation of a serialized ICP descriptor back into an ICP descriptor object.
	 * @param serializedIcp The serialized string (CSV format).
	 * @return The IcpDescriptor object representing the specified serialized string, non-null.
	 * @throws IcpException
	 */
	public static IcpDescriptor deserialized(String serializedIcp) throws IcpException
	{
		Matcher m = gSerializedIcpPattern.matcher(serializedIcp);
		if(m.find())
		{
			String icpName = m.group(1);
			String icpType = m.group(2);
			String regCode = m.group(3);
			String rtc = m.group(4);
			String e2t = m.group(5);
			String lite = m.group(6);			
			String minetVer = m.group(7);

			String proc1 = "";
			String proc2 = "";

			Matcher mE2t = gIpAddrPattern.matcher(e2t);
			if(mE2t.find())
				proc2 = e2t;

			Matcher mRtc = gIpAddrPattern.matcher(rtc);

			Matcher mLite = gIpAddrPattern.matcher(lite);

			if(mRtc.find())
				proc1 = rtc;
			else if(mLite.find())
				proc1 = lite;
				
			return new IcpDescriptor(icpName, minetVer, regCode, proc1, proc2, icpType);
		}
		throw new IcpException("Invalid input: " + serializedIcp);
	}

	/**
	 * Adds a new processor to this ICP.
	 * @param proc The new processor to add.
	 */
	private void addProcessor(IcpProcessor proc)
	{
		synchronized(this)
		{
			m_processors.put(proc.getName(), proc);
		}
	}

	@Override
	public int hashCode()
	{
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof IcpDescriptor)
		{
			IcpDescriptor rhs = (IcpDescriptor)obj;
			return getName().equals(rhs.getName());
		}
		throw new ClassCastException(obj + " not of type " + IcpDescriptor.class);
	}

	@Override
	public int compareTo(IcpDescriptor rhs)
	{
		return getName().compareTo(rhs.getName());
	}
	
	static 
	{
		IcpService.getInstance();
	}
}

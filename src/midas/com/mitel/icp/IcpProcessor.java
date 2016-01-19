package com.mitel.icp;

import com.mitel.miutil.*;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents instances of ICP processors.
 * @author haiv
 */
public class IcpProcessor
{
	/**
	 * Enumerated ICP processor names
	 */
	public static enum ProcessorName
	{
		RTC("Rtc"),
		E2T("E2t"),
		UNKNOWN("Unknown");
		public final String value;
		private ProcessorName(String name)
		{
			this.value = name;
		}
		/**
		 * Converts a processor name string to the equivalent enumeration value.
		 * @param name The processor name to ve converted.
		 * @return The enumerated processor name associated with the given name, or UNKNOWN if invalid names were given.
		 */
		public static ProcessorName string2Name(String name)
		{
			ProcessorName ret = lookupMap.get(name);
			if(ret == null)
			{
				MiSystem.logWarning(MiLogMsg.Category.DESIGN, "Failed to convert " + name + " at " + MiExceptionUtil.getCompressedTrace());
			}
			return ret != null ? ret : UNKNOWN;
		}
		private static final Map<String, ProcessorName> lookupMap = new HashMap<>(8);
		static
		{
			for(ProcessorName n : ProcessorName.values())
			{
				lookupMap.put(n.value, n);
			}
		}
	}
	
	private final ProcessorName name;
	
	@SuppressWarnings("PublicField")
	public String m_userId = "";
	
	@SuppressWarnings("PublicField")
	public boolean m_validated = false;
	
	@SuppressWarnings("PublicField")
	public String m_address = "";
	
	@SuppressWarnings("PublicField")
	public String m_password = "";
	
	private static final Pattern gHexBytePattern = Pattern.compile("(0[xX][0-9a-f]+)");
	public final int m_telnetPort;
	/**
	 * This is the salt used for encrypting the password.
	 */
	private byte[] m_param1;
	/**
	 * The is the iteration count used for encrypting the password.
	 */
	private int m_param2;
	
	public IcpProcessor(MiParameter param)
	{
		name = ProcessorName.string2Name(extractName(param));
		int tport = 2002;
		MiParameter t = param.getSubParam("TelnetPort");
		if(t != null)
		{
			try
			{
				tport = Integer.parseInt(t.getValue());
			}
			catch(RuntimeException e)
			{
			}
		}
		m_telnetPort = tport;
		setConfigParameter(param);
	}
	
	public IcpProcessor(ProcessorName procName, int telnetPort)
	{
		name = procName;
		m_telnetPort = telnetPort;
		m_param1 = generateParam();
		m_param2 = generateParam2();
	}
	
	public IcpProcessor(IcpProcessor aCopy)
	{
		name = aCopy.name;
		m_userId = aCopy.m_userId;
		m_validated = aCopy.m_validated;
		m_address = aCopy.m_address;
		m_password = aCopy.m_password;
		m_telnetPort = aCopy.m_telnetPort;
		m_param1 = aCopy.m_param1;
		m_param2 = aCopy.m_param2;
	}
	
	/**
	 * Generate random salt values
	 * @return The 8-byte salt array.
	 */
	private static byte[] generateParam()
	{
		byte[] ret = new byte[8];
		for(int i = 0; i < ret.length; ++i)
		{
			ret[i] = (byte)(Math.random() * 255.0);
		}
		return ret;
	}
	
	/**
	 * Generate random iteration count
	 * @return The iteration count
	 */
	private static int generateParam2()
	{
		return (int)(Math.random() * 65000) + 1;
	}
	
	/**
	 * Retrieves the salt value for this object.
	 * @return The 8-byte salt array, not null.
	 */
	@SuppressWarnings("NestedAssignment")
	public byte[] getParam1()
	{
		return m_param1 != null ? m_param1 : (m_param1 = generateParam());
	}
	
	/**
	 * Sets the salt value used for encrypting the password. Should be an 8-byte array.
	 * @param p1 The 8-byte salt array
	 * @return This object
	 */
	@SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
	public IcpProcessor setParam1(byte[] p1)
	{
		if((p1 != null)&&(p1.length == 8))
		{
			m_param1 = p1;
		}
		else
		{
			m_param1 = null;
		}
		return this;
	}
	
	/**
	 * Retrieves the iteration count
	 * @return The iteration count
	 */
	@SuppressWarnings("NestedAssignment")
	public int getParam2()
	{
		return m_param2 != 0 ? m_param2 : (m_param2 = generateParam2());
	}
	
	/**
	 * Sets the interation count used for encrypting the password. Should be a positive integer.
	 * @param p2 The new iteration count
	 * @return This object
	 */
	public IcpProcessor setParam2(int p2)
	{
		m_param2 = p2;
		return this;
	}
	
	/**
	 * Retrieves the configuration parameter for this object.
	 * @return The configuration parameter, non-null.
	 */
	public MiParameter getConfigParameter()
	{
		MiParameter retValue = new MiParameter("IcpProcessor", "");
		retValue.addSubParam(new MiParameter("Name", name.value));
		retValue.addSubParam(new MiParameter("Address", m_address));
		retValue.addSubParam(new MiParameter("UserId", m_userId));
		try
		{
			if((m_address != null)&&(m_password != null)&&(!m_password.isEmpty()))
			{
				retValue.addSubParam(new MiParameter("UserPassword", new MiCrypto(name.value).in(m_password.getBytes("UTF-8"), getParam1(), getParam2())));
			}
		}
		catch(UnsupportedEncodingException ex)
		{
			MiSystem.logWarning(MiLogMsg.Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
		catch(MiException ex)
		{
			MiSystem.logError(MiLogMsg.Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
		retValue.addSubParam(new MiParameter("TelnetPort", String.valueOf(m_telnetPort)));
		StringBuilder s = new StringBuilder(128);
		for(byte b : getParam1())
		{
			s.append(String.format("%#x", b)).append(' ');
		}
		retValue.addSubParam(new MiParameter("Param", s.toString()));
		retValue.addSubParam(new MiParameter("Param2", String.valueOf(getParam2())));
		return retValue;
	}
	
	/**
	 * Extracts the name field from the given configuration parameter.
	 * @param param The configuration parameter from which the name field is to be extracted.
	 * @return The name value extracted from the given parameter, non-null.
	 */
	private static String extractName(MiParameter param)
	{
		String ret = null;
		if(param.getName().equals("IcpProcessor"))
		{
			MiParameter name = param.getSubParam("Name");
			if(name != null)
				ret = name.getValue();
		}
		return (ret != null?ret:"");
	}
	
	private void setConfigParameter(MiParameter param)
	{
		if(param.getName().equals("IcpProcessor"))
		{
			MiParameter addr = param.getSubParam("Address");
			if(null != addr)
			{
				m_address = addr.getValue();
			}
			MiParameter id = param.getSubParam("UserId");
			if(null != id)
			{
				m_userId = id.getValue();
			}
			MiParameter s = param.getSubParam("Param");
			if(null != s)
			{
				List<Integer> bytes = new LinkedList<>();
				Matcher m = gHexBytePattern.matcher(s.getValue());
				while(m.find())
				{
					bytes.add(Integer.decode(m.group(1)));
				}
				m_param1 = new byte[bytes.size()];
				int i = 0;
				for(Integer b : bytes)
				{
					m_param1[i++] = b.byteValue();
				}
			}
			s = param.getSubParam("Param2");
			if(null != s)
			{
				try
				{
					m_param2 = Integer.parseInt(s.getValue());
				}
				catch(RuntimeException e)
				{
				}
			}
			MiParameter p = param.getSubParam("UserPassword");
			if((m_address != null)&&(m_userId != null)&&(name != null)&&(null != p))
			{
				try
				{
					m_password = new String(new MiCrypto(name.value).out(p.getValue(), getParam1(), getParam2()));
				}
				catch(MiException ex)
				{
					MiSystem.logWarning(MiLogMsg.Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
				}
			}
		}
	}
	
	@Override
	public String toString()
	{
		return "<Name>" + name.value + "</Name>" + "<Address>" + m_address + "</Address>";
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj == null)
		{
			return false;
		}
		if(getClass() != obj.getClass())
		{
			return false;
		}
		final IcpProcessor other = (IcpProcessor) obj;
		if(this.name != other.name)
		{
			return false;
		}
		if((this.m_userId == null) ? (other.m_userId != null) : !this.m_userId.equals(other.m_userId))
		{
			return false;
		}
		if((this.m_address == null) ? (other.m_address != null) : !this.m_address.equals(other.m_address))
		{
			return false;
		}
		if((this.m_password == null) ? (other.m_password != null) : !this.m_password.equals(other.m_password))
		{
			return false;
		}
		if(this.m_telnetPort != other.m_telnetPort)
		{
			return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		int hash = 5;
		hash = 31 * hash + (this.name != null ? this.name.hashCode() : 0);
		hash = 31 * hash + (this.m_userId != null ? this.m_userId.hashCode() : 0);
		hash = 31 * hash + (this.m_address != null ? this.m_address.hashCode() : 0);
		hash = 31 * hash + (this.m_password != null ? this.m_password.hashCode() : 0);
		hash = 31 * hash + this.m_telnetPort;
		return hash;
	}

	public ProcessorName getName()
	{
		return name != null ? name : ProcessorName.UNKNOWN;
	}
	
	public String getAddress()
	{
		return m_address != null ? m_address : "";
	}
	
	public IcpProcessor setAddress(String newValue)
	{
		m_address = newValue;
		return this;
	}
	
	public String getUserId()
	{
		return m_userId != null ? m_userId : "";
	}
	
	public IcpProcessor setUserId(String newValue)
	{
		m_userId = newValue;
		return this;
	}
	
	public String getPassword()
	{
		return m_password != null ? m_password : "";
	}
	
	public IcpProcessor setPassword(String newValue)
	{
		m_password = newValue;
		return this;
	}
}


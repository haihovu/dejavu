package com.mitel.icp;

import com.mitel.icp.MiArg.InvalidArgumentException;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class SysCmd
{
	private static final Pattern gSerializedProcessorNames = Pattern.compile("(Rtc|E2t)");
	
	public final String m_cmdName;
	
	public final List<String> m_args = new LinkedList<String>();
	
	public final String m_description;
	
	public final String m_icpType;
	
	private static final String DEFAULT_ICP_TYPE = ".*";
	
	public final String m_alias;
	
	@SuppressWarnings("PublicField")
	public boolean m_noArgEdit = false;
	
	/**
	 * The processor(s) that support this command.
	 */	
	public final IcpProcessor.ProcessorName[] m_supportedProcessor;
	
	@SuppressWarnings("PublicField")
	public IcpProcessor.ProcessorName m_selectedProcessor;
	
	private boolean m_enabled = true;
	
	public final List<SysCmd> m_subCmds = new LinkedList<SysCmd>();
	
	private int m_argType = MiArg.ARG_TYPE_STRING;
	
	/**
	 * Constructor for creating system command with no argument
	 * @param supportedProcessors
	 * @param name
	 * @param icpType
	 * @param desc
	 */
	public SysCmd(IcpProcessor.ProcessorName[] supportedProcessors, java.lang.String name, String icpType, String desc)
	{
		supportedProcessors.getClass(); // Null check
		m_supportedProcessor = supportedProcessors;
		m_selectedProcessor = m_supportedProcessor[0];
		m_cmdName = name;
		m_alias = name;
		m_icpType = icpType;
		m_description = desc;
	}
	
	/**
	 * Constructor for creating system command with a single argument
	 * @param supportedProcessors
	 * @param name
	 * @param arg
	 * @param icpType
	 * @param desc
	 */
	public SysCmd(IcpProcessor.ProcessorName[] supportedProcessors, java.lang.String name, String arg, String icpType, String desc)
	{
		m_supportedProcessor = supportedProcessors;
		m_selectedProcessor = m_supportedProcessor[0];
		m_cmdName = name;
		m_alias = name;
		
		if(null != arg)
			m_args.add(arg);
		
		m_icpType = icpType;
		m_description = desc;
	}
	
	/**
	 * Constructor for creating system command with an array of arguments
	 * @param supportedProcessors
	 * @param name
	 * @param args
	 * @param icpType
	 * @param desc
	 */
	public SysCmd(IcpProcessor.ProcessorName[] supportedProcessors, java.lang.String name, String[] args, String icpType, String desc)
	{
		m_supportedProcessor = supportedProcessors;
		m_selectedProcessor = m_supportedProcessor[0];
		m_cmdName = name;
		m_alias = name;
		
		for(int i = 0; (null != args)&&(i < args.length); ++i)
		{
			m_args.add(args[i]);
		}
		m_icpType = icpType;
		m_description = desc;
	}
	
	public SysCmd(Element param)
	{
		Element top = locateTopNode(param, "Cmd");
		IcpProcessor.ProcessorName[] supportedProcessor = new IcpProcessor.ProcessorName[]
		{
			IcpProcessor.ProcessorName.RTC, IcpProcessor.ProcessorName.E2T
		};

		if(top == null)
		{
			m_cmdName = "INVALID";
			m_description = "";
			m_icpType = DEFAULT_ICP_TYPE;
			m_alias = "";
			m_supportedProcessor = supportedProcessor;
			m_selectedProcessor = m_supportedProcessor[0];
			return;
		}
		
		// Look for cmd name
		Element el = locateChildNode(top, "CmdName");
		if(el != null)
		{
			m_cmdName = el.getTextContent();
		}
		else
		{
			m_cmdName = "INVALID";
		}

		// Look for sub commands
		el = locateChildNode(top, "CmdList");
		if(el != null)
		{
			NodeList children = el.getChildNodes();
			int len = children.getLength();
			for(int i = 0; i < len; ++i)
			{
				Node child = children.item(i);
				if((child instanceof Element)&&(child.getNodeName().equals("Cmd")))
				{
					m_subCmds.add(new SysCmd((Element)child));
				}
			}
		}

		// Now look for arguments
		el = locateChildNode(top, "Args");
		if(el != null)
		{
			NodeList children = el.getElementsByTagName("Arg");
			int len = children.getLength();
			for(int i = 0; i < len; ++i)
			{
				m_args.add(children.item(i).getTextContent());
			}
		}

		// Now look for argument type
		el = locateChildNode(top, "ArgType");
		if(el != null)
		{
			try
			{
				m_argType = MiArg.convertArgType2Int(el.getTextContent());
			}
			catch(InvalidArgumentException e)
			{
				MiSystem.logError(Category.DESIGN, "Unexpected " + e
					+ " while attempting to grok arg type " + el.getTextContent());
			}
		}

		el = locateChildNode(top, "NoArgEdit");
		if(el != null)
		{
			m_noArgEdit = true;
		}
		
		el = locateChildNode(top, "Desc");
		if(el != null)
		{
			m_description = el.getTextContent();
		}
		else
		{
			m_description = "";
		}
		
		el = locateChildNode(top, "Alias");
		if(el != null)
		{
			m_alias = el.getTextContent();
		}
		else
		{
			m_alias = m_cmdName;
		}
		
		el = locateChildNode(top, "SupportedProcessors");
		el = el != null ? el : locateChildNode(top, "Processor" /*DEPRECATED*/);
		if(el != null)
		{
			Matcher m = gSerializedProcessorNames.matcher(el.getTextContent());
			List<IcpProcessor.ProcessorName> procNames = new LinkedList<IcpProcessor.ProcessorName>();
			while(m.find())
			{
				procNames.add(IcpProcessor.ProcessorName.string2Name(el.getTextContent()));
			}
			if(procNames.size() > 0)
			{
				supportedProcessor = new IcpProcessor.ProcessorName[procNames.size()];
				int i = 0;
				for(IcpProcessor.ProcessorName n : procNames)
				{
					supportedProcessor[i++] = n;
				}
			}
		}

		m_supportedProcessor = supportedProcessor;
		m_selectedProcessor = supportedProcessor[0]; // Default

		el = locateChildNode(top, "SelectedProcessor");
		if(el != null)
		{
			m_selectedProcessor = IcpProcessor.ProcessorName.string2Name(el.getTextContent());
		}

		el = locateChildNode(top, "IcpType");
		if(el != null)
		{
			m_icpType = el.getTextContent();
		}
		else
		{
			m_icpType = DEFAULT_ICP_TYPE;
		}
	}
	
	public SysCmd(SysCmd aCopy)
	{
		m_cmdName = aCopy.m_cmdName;
		m_alias = aCopy.m_alias;
		m_icpType = aCopy.m_icpType;
		m_description = aCopy.m_description;
		m_argType = aCopy.m_argType;
		m_subCmds.clear();
		Iterator<SysCmd> iter = aCopy.m_subCmds.iterator();
		while(iter.hasNext())
		{
			addCmd(new SysCmd(iter.next()));
		}

		m_args.clear();
		Iterator<String> iter2 = aCopy.m_args.iterator();
		while(iter2.hasNext())
		{
			m_args.add(iter2.next());
		}

		m_noArgEdit = aCopy.m_noArgEdit;
		m_supportedProcessor = aCopy.m_supportedProcessor;
		m_selectedProcessor = aCopy.m_selectedProcessor;
	}
	
	private static Element locateTopNode(Element param, String name)
	{
		if(!param.getNodeName().equals(name))
		{
			NodeList children = param.getChildNodes();
			int len = children.getLength();
			for(int i = 0; i < len; ++i)
			{
				Node child = children.item(i);
				if(child instanceof Element)
				{
					Element ret = locateTopNode((Element)child, name);
					if(ret != null)
					{
						return ret;
					}
				}
			}
		}
		return param;
	}
	
	private static Element locateChildNode(Element param, String name)
	{
		NodeList children = param.getChildNodes();
		int len = children.getLength();
		for(int i = 0; i < len; ++i)
		{
			Node child = children.item(i);
			if((child instanceof Element)&&(child.getNodeName().equals(name)))
			{
				return (Element)child;
			}
		}
		return null;
	}
	
	@Override
	public String toString()
	{
		StringBuilder retValue = new StringBuilder(256);
		Iterator iter = m_subCmds.iterator();
		while(iter.hasNext())
		{
			retValue.append(((SysCmd)iter.next()));
			retValue.append(';');
		}
		retValue.append(this.m_cmdName);
		retValue.append('(');
		iter = this.m_args.iterator();
		int counter = 0;
		while(iter.hasNext())
		{
			if(counter > 0)
			{
				retValue.append(',');
			}
			++counter;
			retValue.append(' ').append(iter.next().toString());
		}
		retValue.append(')');
		return retValue.toString();
	}
	
	public boolean isEnabled()
	{
		return m_enabled;
	}
	
	public void enable()
	{
		m_enabled = true;
	}
	
	public void disable()
	{
		m_enabled = false;
	}
	
	public final void addCmd(SysCmd subCmd)
	{
		m_subCmds.add(subCmd);
	}
	
	public void removeCmd(SysCmd subCmd)
	{
		m_subCmds.remove(subCmd);
	}
	
	public Element getConfigParameter(Document doc)
	{
		Element myConfig = doc.createElement("Cmd");
		Element tmp = doc.createElement("CmdName");
		tmp.setTextContent(m_cmdName);
		myConfig.appendChild(tmp);
		
		if(!m_subCmds.isEmpty())
		{
			Element subCmds = doc.createElement("CmdList");
			Iterator<SysCmd> iter = this.m_subCmds.iterator();
			while(iter.hasNext())
			{
				subCmds.appendChild(iter.next().getConfigParameter(doc));
			}
			myConfig.appendChild(subCmds);
		}
		
		try
		{
			tmp = doc.createElement("ArgType");
			tmp.setTextContent(MiArg.convertArgType2String(getArgType()));
			myConfig.appendChild(tmp);
		}
		catch(InvalidArgumentException ex)
		{
			MiSystem.logError(Category.DESIGN, "Unexpected " + ex
				+ " when parsing ArgType " + getArgType());
		}
		
		if(!m_args.isEmpty())
		{
			tmp = doc.createElement("Args");
			myConfig.appendChild(tmp);
			Iterator<String> iter = this.m_args.iterator();
			while(iter.hasNext())
			{
				Element arg = doc.createElement("Arg");
				arg.setTextContent(iter.next());
				tmp.appendChild(arg);
			}
		}
		
		if(m_noArgEdit)
		{
			tmp = doc.createElement("NoArgEdit");
			tmp.setTextContent(String.valueOf(m_noArgEdit));
			myConfig.appendChild(tmp);
		}
		
		tmp = doc.createElement("Desc");
		tmp.setTextContent(m_description);
		myConfig.appendChild(tmp);
		
		if((null != m_alias)&&(m_alias.length() > 0))
		{
			tmp = doc.createElement("Alias");
			tmp.setTextContent(m_alias);
			myConfig.appendChild(tmp);
		}
		
		tmp = doc.createElement("SupportedProcessors");
		StringBuilder procs = new StringBuilder(64);
		for(IcpProcessor.ProcessorName n : m_supportedProcessor)
		{
			procs.append(' ').append(n.value);
		}
		tmp.setTextContent(procs.toString());
		myConfig.appendChild(tmp);
		
		if(m_selectedProcessor != null)
		{
			tmp = doc.createElement("SelectedProcessor");
			tmp.setTextContent(m_selectedProcessor.value);
			myConfig.appendChild(tmp);
		}
		
		if((null != m_icpType)&&(m_icpType.length() > 0))
		{
			tmp = doc.createElement("IcpType");
			tmp.setTextContent(m_icpType);
			myConfig.appendChild(tmp);
		}

		return myConfig;
	}
	
	public void setArgs(java.lang.String args)
	{
		Pattern p = Pattern.compile("[,\\s]+");
		String[] argsArray = p.split(args);
		this.m_args.clear();
		for(int index = 0; index < argsArray.length; ++index)
		{
			if(argsArray[index].length()>0)
				m_args.add(argsArray[index]);
		}
	}
	
	public synchronized void setArgType(int argType)
	{
		this.m_argType = argType;
	}

	public synchronized int getArgType()
	{
		return m_argType;
	}

	public String getArgAsString()
	{
		Iterator iter = this.m_args.iterator();
		int counter = 0;
		StringBuilder retValue = new StringBuilder(256);
		while(iter.hasNext())
		{
			if(counter > 0)
			{
				retValue.append(',');
			}
			++counter;
			retValue.append(' ').append(iter.next());
		}
		return retValue.toString();
	}
	
}


package com.mitel.guiutil;

import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import java.awt.Component;
import java.awt.Window;
import java.lang.reflect.Constructor;
import java.util.Map.Entry;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Utility class for managing look and feel.
 */
public class MiLookAndFeel
{
	private static final Map<String, String> s_LafsClassMap = new HashMap<String, String>(32);
	/**
	 * Look and feel map keyed by their names. A tree map is used here so that the names are sorted.
	 */
	private static final Map<String, LookAndFeel> s_LafMap = new TreeMap<String, LookAndFeel>();
	/**
	 * List of components that needs updated when look and feel is changed.
	 */
	private static final List<Component> s_LafableComponents = new LinkedList<Component>();

	private static final Pattern s_LookAndFeelCmdArgPattern = Pattern.compile("--laf=([A-Za-z0-9_]+)");

	/**
	 * Used for synchronization of class members.
	 */
	private static final Object s_StaticLock = new Object();
	private static boolean s_LafInitialized;
	public static final String LAF_SUBSTANCE_MIST_SILVER = "Substance Mist Silver";
	public static final String LAF_SUBSTANCE_RAVEN_GRAPHITE = "Substance Raven Graphite";
	public static final String LAF_SUBSTANCE_RAVEN = "Substance Raven";
	public static final String LAF_SUBSTANCE_BUSINESS = "Substance Business";
	public static final String LAF_SUBSTANCE_NEBULA = "Substance Nebula";
	public static final String LAF_SUBSTANCE_CREME_COFFEE = "Substance Creme Coffee";
	public static final String LAF_NIMROD = "NimROD";
	public static final String LAF_WINDOWS = "Windows";
	public static final String LAF_JGOODIES = "JGoodies Plastic 3D";
	public static final String LAF_NAPKIN = "Napkin";
	public static final String LAF_NIMBUS = "Nimbus";
	public static final String LAF_METAL = "Metal";

	/**
	 * Enumeration of supported look and feels.
	 */
	public static enum SupportedLookAndFeel
	{
		SUBSTANCE_MIST_SILVER("Substance Mist Silver"),
		SUBSTANCE_RAVEN_GRAPHITE("Substance Raven Graphite"),
		SUBSTANCE_RAVEN("Substance Raven"),
		SUBSTANCE_BUSINESS("Substance Business"),
		SUBSTANCE_NEBULA("Substance Nebula"),
		SUBSTANCE_CREME_COFFEE("Substance Creme Coffee"),
		NIMROD("NimROD"),
		WINDOWS("Windows"),
		JGOODIES("JGoodies Plastic 3D"),
		NAPKIN("Napkin"),
		NIMBUS("Nimbus"),
		METAL("Metal");
		private static final long serialVersionUID = 1L;

		/**
		 * Creates a new look&feel.
		 * @param name The name of the look&feel
		 */
		private SupportedLookAndFeel(String name)
		{
			m_Name = name;
		}

		/**
		 * Converts a string representation of an enumerated look and feel to the associated enumeration value.
		 * @param strValue The string representation of an enumerated look and feel,
		 * note that this is not the same as the look and feel name.
		 * @return The enumeration associated with the given value, or null if no match is found.
		 */
		public static SupportedLookAndFeel fromString(String strValue)
		{
			return s_Lookup.get(strValue);
		}

		/**
		 * Retrieves the name of the class that loads this look and feel.
		 * @return The name of the Look & Fell load class or null if none is available.
		 */
		public String getLoadClassName()
		{
			return s_LafsClassMap.get(m_Name);
		}

		/**
		 * The name of the look & feel.
		 */
		public final String m_Name;
		private static final Map<String, SupportedLookAndFeel> s_Lookup = new HashMap<String, SupportedLookAndFeel>(32);
		static
		{
			s_Lookup.put(METAL.toString(), METAL);
			s_Lookup.put(JGOODIES.toString(), JGOODIES);
			s_Lookup.put(NAPKIN.toString(), NAPKIN);
			s_Lookup.put(NIMBUS.toString(), NIMBUS);
			s_Lookup.put(NIMROD.toString(), NIMROD);
			s_Lookup.put(SUBSTANCE_BUSINESS.toString(), SUBSTANCE_BUSINESS);
			s_Lookup.put(SUBSTANCE_CREME_COFFEE.toString(), SUBSTANCE_CREME_COFFEE);
			s_Lookup.put(SUBSTANCE_MIST_SILVER.toString(), SUBSTANCE_MIST_SILVER);
			s_Lookup.put(SUBSTANCE_NEBULA.toString(), SUBSTANCE_NEBULA);
			s_Lookup.put(SUBSTANCE_RAVEN.toString(), SUBSTANCE_RAVEN);
			s_Lookup.put(SUBSTANCE_RAVEN_GRAPHITE.toString(), SUBSTANCE_RAVEN_GRAPHITE);
		}
	}
	
	/**
	 * Attempt to load/activate some look & feels.
	 * @param lookAndFeelNames The array of desired look & feel names.
	 */
	public static void includeLookAndFeel(String[] lookAndFeelNames)
	{
		synchronized(s_StaticLock)
		{
			if(!s_LafInitialized)
			{
				includeNativeLookAndFeel();
				for(String lafName : lookAndFeelNames)
				{
					String lafClass = s_LafsClassMap.get(lafName);
					if(lafClass != null)
					{
						LookAndFeel laf = loadLookAndFeel(lafClass);
						if(laf != null)
						{
							addLookAndFeel(laf);
						}
					}
				}
				s_LafInitialized = true;
			}
		}
	}

	/**
	 * Attempt to load/activate all supported look & feels.
	 */
	public static void includeAllLookAndFeel()
	{
		synchronized(s_StaticLock)
		{
			if(!s_LafInitialized)
			{
				includeNativeLookAndFeel();
				for(Entry<String, String> lafEntry : s_LafsClassMap.entrySet())
				{
					LookAndFeel laf = loadLookAndFeel(lafEntry.getValue());
					if(laf != null)
					{
						addLookAndFeel(laf);
					}
				}
				s_LafInitialized = true;
			}
		}
	}

	/**
	 * Attempt to load/activate the native look & feels.
	 */
	public static void includeNativeLookAndFeel()
	{
		// Native Look and feel
		synchronized(s_StaticLock)
		{
			if(!s_LafInitialized)
			{
				UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
				for(UIManager.LookAndFeelInfo laf : lafs)
				{
					LookAndFeel lafObj = loadLookAndFeel(laf.getClassName());
					if(null != lafObj)
					{
						addLookAndFeel(lafObj);
					}
				}
				s_LafInitialized = true;
			}
		}
	}

	public static void main(String[] args)
	{
		includeAllLookAndFeel();
		UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
		for(UIManager.LookAndFeelInfo laf : lafs)
		{
			System.out.println("Found look and feel " + laf);
		}
	}

	/**
	 * Retrieves the system (native) look & feel.
	 * @return The system look & feel, or null if none can be loaded.
	 */
	public static LookAndFeel getSystemLookAndFeel()
	{
		String sysLafClassName = UIManager.getSystemLookAndFeelClassName();
		try
		{
			Class<?> sysLafClass = Class.forName(sysLafClassName);
			Constructor<?> sysLafConstructor = sysLafClass.getConstructor(new Class[0]);
			Object sysLafObj = sysLafConstructor.newInstance(new Object[0]);
			if(sysLafObj instanceof LookAndFeel)
			{
				LookAndFeel sysLaf = (LookAndFeel)sysLafObj;
				return sysLaf;
			}
		}
		catch(Exception ex)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
		return null;
	}

	/**
	 * Loads a look and feel from class loader.
	 * @param className The canonical class name of the desired look and feel.
	 * @return The requested look and feel object or null if one matching the specified description cannot be located.
	 */
	private static LookAndFeel loadLookAndFeel(String className)
	{
		if(className == null)
			return null;

		Class<?> lafClass = null;
		try
		{
			lafClass = MiLookAndFeel.class.getClassLoader().loadClass(className);
			Constructor defaultConst = lafClass.getConstructor(new Class[0]);
			if (null != defaultConst)
			{
				LookAndFeel lafObj = (LookAndFeel)defaultConst.newInstance(new Object[0]);
				UIManager.LookAndFeelInfo[] installedLafs = UIManager.getInstalledLookAndFeels();
				boolean alreadyInstalled = false;
				for(UIManager.LookAndFeelInfo laf : installedLafs)
				{
					if(laf.getName().equals(lafObj.getName()))
					{
						alreadyInstalled = true;
						break;
					}
				}
				if(!alreadyInstalled)
				{
					UIManager.installLookAndFeel(lafObj.getName(), className);
				}
				return lafObj;
			}
		}
		catch(ClassCastException e)
		{
			MiSystem.logInfo(Category.DESIGN, className + " is not a valid look and feel class");
		}
		catch (NoSuchMethodException e)
		{
			MiSystem.logInfo(Category.DESIGN, "Could not locate default constructor of " + lafClass);
		}
		catch (ClassNotFoundException e)
		{
			MiSystem.logInfo(Category.DESIGN, "Could not locate LAF " + className);
		}
		catch(Exception e)
		{
			MiSystem.logInfo(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
		
		return null;
	}
	
	/**
	 * Registers a component that needs updating when the look and feel changes.
	 * Remember to deregister the component before it is destroyed.
	 * @param comp The target component.
	 */
	public static void registerLafComponent(Component comp)
	{
		synchronized(s_StaticLock)
		{
			s_LafableComponents.add(comp);
		}
	}
	
	/**
	 * Deregisters a component from look and feel change notification.
	 * @param comp The target component.
	 */
	public static void deregisterLafComponent(Component comp)
	{
		synchronized(s_StaticLock)
		{
			s_LafableComponents.remove(comp);
		}
	}
	
	/**
	 * Adds a new look and feel to the repository.
	 * @param laf The look and feel to add.
	 * @NotThreadSafe
	 */
	private static void addLookAndFeel(LookAndFeel laf)
	{
		if(s_LafMap.get(laf.getName()) != null)
		{
			return;
		}
		s_LafMap.put(laf.getName(), laf);
	}
	
	/**
	 * Locates a look and feel by its name.
	 * @param name The name of the desired look and feel.
	 * @return The look and feel associated with the given name,
	 * or null if the desired look and feel cannot be located.
	 */
	public static LookAndFeel getLookAndFeel(String name)
	{
		synchronized(s_StaticLock)
		{
			return s_LafMap.get(name);
		}
	}
	
	/**
	 * Retrieves all look and feels in the repository.
	 * @return The collection of look and feels in the repository.
	 */
	public static Collection<LookAndFeel> getLookAndFeels()
	{
		synchronized(s_StaticLock)
		{
			return s_LafMap.values();
		}
	}
	
	/**
	 * Gets the current look and feel.
	 * @return The current look and feel.
	 */
	public static LookAndFeel getCurrentLookAndFeel()
	{
		return UIManager.getLookAndFeel();
	}

	/**
	 * Parses the look and feel from the list of command line arguments.
	 * @param args The command line arguments. Look and feel arguments are specified as --laf=<i>desired look and feel</i>.
	 * @return The look and feel found in the given command line arguements,
	 * or null if no look and feel is specified in the given command line arguments.
	 */
	public static SupportedLookAndFeel parseLookAndFeel(String[] args)
	{
		for(String arg : args)
		{
			Matcher m = s_LookAndFeelCmdArgPattern.matcher(arg);
			if(m.find())
			{
				return SupportedLookAndFeel.fromString(m.group(1));
			}
		}
		return null;
	}

	/**
	 * Sets the current look and feel.
	 * If there is no look and feel in the repository matching the specified name then nothing is done.
	 * @param name The name of the desired look and feel.
	 * @ThreadSafe May be invoked by any thread.
	 */
	public static void setCurrentLookAndFeel(String name)
	{
		class LafUpdater implements Runnable
		{
			private final String m_LafName;
			LafUpdater(String name)
			{
				m_LafName = name;
			}

			@Override
			public void run()
			{
				String lafName = m_LafName;
				synchronized(s_StaticLock)
				{
					if(!s_LafInitialized)
					{
						if((lafName == null)||(lafName.length() < 1))
						{
							includeNativeLookAndFeel();
							LookAndFeel sysLaf = getSystemLookAndFeel();
							if(sysLaf != null)
							{
								lafName = sysLaf.getName();
							}
						}
						else
						{
							includeLookAndFeel(new String[]{lafName});
						}
					}
					if(lafName == null)
					{
						MiSystem.logError(Category.DESIGN, "Unable to locate the default system look and feel");
						return;
					}
					LookAndFeel lafObj = s_LafMap.get(lafName);
					if(lafObj == null)
					{
						MiSystem.logWarning(Category.DESIGN, "Failed to instantiate look and feel "
							+ lafName + ", attempt to system look and feel");
						lafObj = getSystemLookAndFeel();
					}
					if(null != lafObj)
					{
						try
						{
							UIManager.setLookAndFeel(lafObj);
							for(Component comp : s_LafableComponents)
							{
								SwingUtilities.updateComponentTreeUI(comp);
								if(comp instanceof Window)
								{
									((Window)comp).pack();
								}
							}
						}
						catch (UnsupportedLookAndFeelException ex)
						{
							MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
						}
					}
				}
			}
		}
		
		if(SwingUtilities.isEventDispatchThread())
		{
			new LafUpdater(name).run();
		}
		else
		{
			try
			{
				SwingUtilities.invokeAndWait(new LafUpdater(name));
			}
			catch(Exception ex)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
			}
		}
	}

	/**
	 * Sets the current look and feel.
	 * @param laf The desired look and feel. Use null for default system look and feel.
	 * @ThreadSafe May be invoked by any thread.
	 */
	public static void setCurrentLookAndFeel(SupportedLookAndFeel laf)
	{
		setCurrentLookAndFeel(laf != null?laf.m_Name:null);
	}
	
	static
	{
		s_LafsClassMap.put(LAF_SUBSTANCE_MIST_SILVER, "org.pushingpixels.substance.api.skin.SubstanceMistSilverLookAndFeel");
		s_LafsClassMap.put(LAF_SUBSTANCE_RAVEN_GRAPHITE, "org.pushingpixels.substance.api.skin.SubstanceRavenGraphiteLookAndFeel");
		s_LafsClassMap.put(LAF_SUBSTANCE_RAVEN, "org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel");
		s_LafsClassMap.put(LAF_SUBSTANCE_BUSINESS, "org.pushingpixels.substance.api.skin.SubstanceBusinessLookAndFeel");
		s_LafsClassMap.put(LAF_SUBSTANCE_NEBULA, "org.pushingpixels.substance.api.skin.SubstanceNebulaLookAndFeel");
		s_LafsClassMap.put(LAF_SUBSTANCE_CREME_COFFEE, "org.pushingpixels.substance.api.skin.SubstanceCremeCoffeeLookAndFeel");
		s_LafsClassMap.put(LAF_NIMROD, "com.nilo.plaf.nimrod.NimRODLookAndFeel");
		s_LafsClassMap.put(LAF_JGOODIES, "com.jgoodies.looks.plastic.Plastic3DLookAndFeel");
		s_LafsClassMap.put(LAF_NAPKIN, "net.sourceforge.napkinlaf.NapkinLookAndFeel");
	}
}
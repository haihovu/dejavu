package com.mitel.miutil;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for describing media codecs supported by Mitel.
 * Defined in its scope are bit-map representation of the various codecs supported
 * by the MiNET specs (though only a subset is supported by MiDAS).
 * @author Hai Vu
 */
public class MiCodec implements MiAttributeDescriptor.StringConverter
{
	/**
	 * MiNET specification - No codec
	 */
	public static final int TERM_CAP_NO_CODEC = 0x0;
	/**
	 * MiNET specification - G711 uLaw
	 */
	public static final int TERM_CAP_G711_ULAW64 = 0x1;
	/**
	 * MiNET specification - G711 ALaw
	 */
	public static final int TERM_CAP_G711_ALAW64 = 0x2;
	/**
	 * MiNET specification - G728
	 */
	public static final int TERM_CAP_G728 = 0x4;
	/**
	 * MiNET specification - G729
	 */
	public static final int TERM_CAP_G729 = 0x8;
	/**
	 * MiNET specification - G729B
	 */
	public static final int TERM_CAP_G729_ANNEXB = 0x10;
	/**
	 * MiNET specification - G729AB
	 */
	public static final int TERM_CAP_G729_ANNEXAB = 0x20;
	/**
	 * MiNET specification - G723
	 */
	public static final int TERM_CAP_G723 = 0x40;
	/**
	 * MiNET specification - G7231
	 */
	public static final int TERM_CAP_G7231_ANNEXC = 0x80;
	/**
	 * MiNET specification - L16-256
	 */
	public static final int TERM_CAP_L16_256 = 0x100;
	/**
	 * MiNET specification - G722
	 */
	public static final int TERM_CAP_G722 = 0x200;
	/**
	 * MiNET specification - Data (not voice)
	 */
	public static final int TERM_CAP_DATA = 0x400;
	
	/**
	 * Enumeration of codecs supported by MiDAS.
	 */
	public static enum SupportedCodec
	{
		/**
		 * G711 ALaw
		 */
		G711ALaw(TERM_CAP_G711_ALAW64),
		/**
		 * G711 uLaw
		 */
		G711uLaw(TERM_CAP_G711_ULAW64),
		/**
		 * G729
		 */
		G729(TERM_CAP_G729),
		/**
		 * G729B
		 */
		G729AnexB(TERM_CAP_G729_ANNEXB),
		/**
		 * G729AB
		 */
		G729AnexAB(TERM_CAP_G729_ANNEXAB),
		/**
		 * No codec
		 */
		None(TERM_CAP_NO_CODEC);
		
		private SupportedCodec(int value)
		{
			bitMapValue = value;
		}
		/**
		 * Bitmap value associated with this codec
		 */
		public final int bitMapValue;

		/**
		 * Converts a bit-map integer representation of codec (MiNET) to the equivalent
		 * enumeration value.
		 * @param value The integer bit-map value to be converted.
		 * @return The enumeration value associated with the given integer value, or None
		 * if the given value is invalid or not supported.
		 */
		public static SupportedCodec intToCodec(int value)
		{
			SupportedCodec ret = gCodecMap.get(Integer.valueOf(value));
			return ret != null?ret:None;
		}

		private final static Map<Integer,SupportedCodec> gCodecMap = new HashMap<Integer, SupportedCodec>(10);
		static
		{
			for(SupportedCodec codec : values())
			{
				gCodecMap.put(Integer.valueOf(codec.bitMapValue), codec);
			}
		}
	}
	
	/**
	 * Map for looking up codec types from their string format.
	 */
	private static final AbstractMap<String, Integer> gNameValueMap = new HashMap<String, Integer>(20);
	/**
	 * Regular expression for parsing codecs.
	 */
	private static final Pattern gCodecsPattern = Pattern.compile("([A-Za-z_0-9]+)");
	
	static 
	{
		gNameValueMap.put(codecToString(TERM_CAP_NO_CODEC), new Integer(TERM_CAP_NO_CODEC));
		gNameValueMap.put(codecToString(TERM_CAP_G711_ULAW64), new Integer(TERM_CAP_G711_ULAW64));
		gNameValueMap.put(codecToString(TERM_CAP_G711_ALAW64), new Integer(TERM_CAP_G711_ALAW64));
		gNameValueMap.put(codecToString(TERM_CAP_G728), new Integer(TERM_CAP_G728));
		gNameValueMap.put(codecToString(TERM_CAP_G729), new Integer(TERM_CAP_G729));
		gNameValueMap.put(codecToString(TERM_CAP_G729_ANNEXB), new Integer(TERM_CAP_G729_ANNEXB));
		gNameValueMap.put(codecToString(TERM_CAP_G729_ANNEXAB), new Integer(TERM_CAP_G729_ANNEXAB));
		gNameValueMap.put(codecToString(TERM_CAP_G723), new Integer(TERM_CAP_G723));
		gNameValueMap.put(codecToString(TERM_CAP_G7231_ANNEXC), new Integer(TERM_CAP_G7231_ANNEXC));
		gNameValueMap.put(codecToString(TERM_CAP_L16_256), new Integer(TERM_CAP_L16_256));
		gNameValueMap.put(codecToString(TERM_CAP_G722), new Integer(TERM_CAP_G722));
		gNameValueMap.put(codecToString(TERM_CAP_DATA), new Integer(TERM_CAP_DATA));
	}
	
	/**
	 * Default constructor.
	 */
	public MiCodec()
	{
		super();
	}	

	@Override
	public Object fromString(String attributeValue)
	{
		return Integer.valueOf(stringToCodec(attributeValue));
	}

	@Override
	public String toString(Object attributeValue)
	{
		if(attributeValue instanceof Integer)
		{
			Integer val = (Integer)attributeValue;
			return codecsToString(val.intValue());
		}
		
		// Invalid
		return attributeValue.toString();
	}

	@Override
	public Set getValueSet()
	{
		return gNameValueMap.keySet();
	}

	/**
	 * Converts one or more codecs in a string into the equivalent bit map format.
	 * @param codecs The string containing the codec(s). The codecs are delimited by |.
	 * @return The bit map containing the codecs found in the string.
	 */
	public static int stringToCodec(String codecs)
	{
		int retValue = 0;
		if(null != codecs)
		{
			Matcher m = gCodecsPattern.matcher(codecs);
			while(m.find())
			{
				Integer value = gNameValueMap.get(m.group(1));

				if(null != value)
					retValue |= value.intValue();
			}
		}
		
		return retValue;
	}

	/**
	 * <p>Converts one or more codecs (bit map) into their equivalent human-readable string.</p>
	 * <p>Codecs are separated by |.</p>
	 * @param codecs The bit map containing the codecs.
	 * @return The string equivalent of the given codecs, or "None" if no valid codec was given.
	 */
	public static String codecsToString(int codecs)
	{
		StringBuilder retValue = null;
		Iterator<Map.Entry<String, Integer>> iter = gNameValueMap.entrySet().iterator();
		while(iter.hasNext())
		{
			Map.Entry<String, Integer> entry = iter.next();
			if(0 != (codecs & entry.getValue().intValue()))
			{
				// Add delimiter if necessary
				if(null == retValue)
				{
					retValue = new StringBuilder(512);
				}
				else
				{
					retValue.append("|");
				}

				// Append the string value
				retValue.append(entry.getKey());
			}
		}

		if(null != retValue)
		{
			return retValue.toString();
		}

		return "None";
	}

	/**
	 * Retrieves the set of all supported codecs, in human readable string format.
	 * @return The sets of supported codecs.
	 */
	public static Set<String> getCodecs()
	{
		return gNameValueMap.keySet();
	}
	
	/**
	 * Converts a single codec type into its equivalent human-readable string format.
	 * @param singleCodec The codec to be converted.
	 * @return The string representing the given codec.
	 */
	private static String codecToString(int singleCodec)
	{
		switch(singleCodec)
		{
			case TERM_CAP_NO_CODEC:
				return "TERM_CAP_NO_CODEC";
			case TERM_CAP_G711_ULAW64:
				return "TERM_CAP_G711_ULAW64";
			case TERM_CAP_G711_ALAW64:
				return "TERM_CAP_G711_ALAW64";
			case TERM_CAP_G728:
				return "TERM_CAP_G728";
			case TERM_CAP_G729:
				return "TERM_CAP_G729";
			case TERM_CAP_G729_ANNEXB:
				return "TERM_CAP_G729_ANNEXB";
			case TERM_CAP_G729_ANNEXAB:
				return "TERM_CAP_G729_ANNEXAB";
			case TERM_CAP_G723:
				return "TERM_CAP_G723";
			case TERM_CAP_G7231_ANNEXC:
				return "TERM_CAP_G7231_ANNEXC";
			case TERM_CAP_L16_256:
				return "TERM_CAP_L16_256";
			case TERM_CAP_G722:
				return "TERM_CAP_G722";
			case TERM_CAP_DATA:
				return "TERM_CAP_DATA";
			default:
				return "INVALID_CODEC";
		}
	}
}

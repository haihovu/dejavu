/*
 * MiLanguage.java
 *
 * Created on May 23, 2006, 8:04 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.mitel.miutil;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents MiDAS's supported languages and locales.
 * A couple of standards are used for coding the languages and countries (locales):
 * <ul><li>ISO 639 (language)</li><li>ISO 3166 (country).</li></ul>
 * There is some debate as to how these are used, 
 * RFC3066 defines language tags which combines ISO639 and ISO3166 using a hyphen (-).
 * The Java spec joins ISO639 and ISO3166 with an under-score (_). 
 * HTTP language specification follows RFC3066 but states that the code is case-insensitive.
 * The popular browsers (IE, Fire Fox, ...) use all lower-case RFC3066.
 * @author haiv
 */
public class MiLanguage
{
	/**
	 * Creates a new instance of MiLanguage
	 * @param languageLocale The languageLocale code (either Java spec or RFC3066).
	 */
	public MiLanguage(String languageLocale)
	{
		// Parse the language-locale string
		if((null != languageLocale)&&(languageLocale.length() > 0))
		{
			Matcher m = s_PatternLanguageLocale.matcher(languageLocale);
			String language = null;
			String locale = null;
			if(m.find())
			{
				language = m.group(1);
				if(m.groupCount() > 2)
				{
					locale = m.group(3);
				}

			}

			if((null != language)&&(language.length() > 0))
			{
				m_LanguageCode = language;
				if((null != locale)&&(locale.length() > 0))
				{
					m_LocaleCode = locale;
				}
			}
		}
	}

	/**
	 * Creates a new instance of MiLanguage
	 * @param language The language code (ISO639).
	 * @param locale The locale code (ISO3166)
	 */
	public MiLanguage(String language, String locale)
	{
		m_LanguageCode = language;
		m_LocaleCode = locale;
	}
	
	/**
	 * Retrieves the language-locale combination as per RFC3066 (all lower-case).
	 * E.g. en-us, es-es, fr-ca
	 * @return The language-locale combination or empty string if language-locale is not specified.
	 */
	public String getLanguageLocaleRFC3066()
	{
		if((null != m_LanguageCode)&&(m_LanguageCode.length() > 0))
		{
			if((null != m_LocaleCode)&&(m_LocaleCode.length() > 0))
			{
				return new StringBuffer(m_LanguageCode).append("-")
					.append(m_LocaleCode.toLowerCase()).toString();
			}
			return m_LanguageCode;
		}
		return "";
	}
	
	/**
	 * Retrieves the language-locale combination as per Java specification
	 * E.g. en_US, es_ES, fr_CA
	 * @return The language-locale combination or empty string if language-locale is not specified.
	 */
	public String getLanguageLocaleJava()
	{
		if((null != m_LanguageCode)&&(m_LanguageCode.length() > 0))
		{
			if((null != m_LocaleCode)&&(m_LocaleCode.length() > 0))
			{
				return new StringBuffer(m_LanguageCode).append("_")
					.append(m_LocaleCode).toString();
			}
			return m_LanguageCode;
		}
		return "";
	}
	
	/**
	 * Determines whether a language/locale string matches this language object.
	 * @param languageLocale The language/locale string (can be either Java spec or RFC3066) to examine.
	 * @return True if the supplied string matches this language specification. 
	 * False otherwise.
	 */
	public boolean equals(String languageLocale)
	{
		Matcher m = s_PatternLanguageLocale.matcher(languageLocale);
		String language = null;
		String locale = null;
		if(m.find())
		{
			language = m.group(1);
			if(m.groupCount() > 2)
			{
				locale = m.group(3);
			}
			
		}
		return equals(language, locale);
	}
	
	/**
	 * Determines whether a language/locale pair matches this language object.
	 * @param language The language code (ISO639).
	 * @param locale The locale code (ISO3166).
	 * @return True if the supplied string pair matches this language specification. 
	 * False otherwise.
	 */
	public boolean equals(String language, String locale)
	{
		if((null == language)||(language.length() < 1)) // Language cannot be null/empty
		{
			return false;
		}
		
		if((null != locale)&&(locale.length() > 0))
		{
			if((null == m_LocaleCode)||(m_LocaleCode.length() < 1))
			{
				return false;
			}
			
			return (language.equals(m_LanguageCode))&&(locale.equalsIgnoreCase(m_LocaleCode));
		}
		
		if((null != m_LocaleCode)&&(m_LocaleCode.length() > 0))
		{
			return false;
		}
		
		// Locale not specified, we'll just match the language
		return language.equals(m_LanguageCode);
	}
	
	/**
	 * Determines whether a language specification matches this language object.
	 * @param rhs The right-hand-side of the comparison.
	 * @return True if the supplied language matches this language specification. 
	 * False otherwise.
	 */
	public boolean equals(MiLanguage rhs)
	{
		return equals(rhs.getLanguageCode(), rhs.getLocaleCode());
	}
	
	@Override
	public boolean equals(Object rhs)
	{
		if(rhs instanceof MiLanguage)
		{
			MiLanguage lang = (MiLanguage)rhs;
			return equals(lang);
		}
		
		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 61 * hash + (this.m_LanguageCode != null ? this.m_LanguageCode.hashCode() : 0);
		hash = 61 * hash + (this.m_LocaleCode != null ? this.m_LocaleCode.hashCode() : 0);
		return hash;
	}
	
	@Override
	public String toString()
	{
		if((null != m_LanguageCode)&&(m_LanguageCode.length() > 0))
		{
			if((null != m_LocaleCode)&&(m_LocaleCode.length() > 0))
			{
				return new StringBuffer(m_LanguageCode).append("-").append(m_LocaleCode).toString();
			}
			return m_LanguageCode;
		}
		return "";
	}
	
	/**
	 * Retrieves the language code.
	 * @return The language code.
	 */
	public String getLanguageCode()
	{
		return m_LanguageCode;
	}
	
	/**
	 * Retrieves the locale code.
	 * @return The locale code.
	 */
	public String getLocaleCode()
	{
		return m_LocaleCode;
	}
	
	/**
	 * Parses a string containing language-locale information (Java spec OR RFC3066) into an equivalent MiLanguage object.
	 * @param languageLocale The language-locale string to parse.
	 * @return The desired MiLanguage object, non-null.
	 */
	public static MiLanguage parseLanguageLocale(String languageLocale)
	{
		synchronized(s_StaticLock) {
			for(int i = 0; i < s_SupportedLanguageFreeIndex; ++i)
			{
				MiLanguage lang = s_SupportedLanguages[i];
				if(lang.equals(languageLocale))
				{
					return lang;
				}
			}
		}
		return new MiLanguage(languageLocale);
	}
	
	/**
	 * Retrieves the list of all supported languages from the language repository.
	 * This repository is initially empty until addSupportedLanguage() is invoked,
	 * typically by client apps.
	 * @return The array contains the supported languages.
	 */
	public static MiLanguage[] getSupportedLanguages()
	{
		synchronized(s_StaticLock)
		{
			MiLanguage[] retValue = new MiLanguage[s_SupportedLanguageFreeIndex];
			for(int i = 0; i < s_SupportedLanguageFreeIndex; ++i)
			{
				retValue[i] = s_SupportedLanguages[i];
			}
			return retValue;
		}
	}
	
	/**
	 * Called by clients to add a new language to the language repository.
	 * The repository is initially empty until this method is invoked.
	 * @param newLang The new language to add.
	 */
	public static void addSupportedLanguage(MiLanguage newLang)
	{
		synchronized(s_StaticLock)
		{
			if(MiSystem.diagnosticEnabled())
			{
				MiSystem.logInfo(MiLogMsg.Category.DESIGN, "Adding " + newLang);
			}
			
			boolean alreadyExisted = false;
			for(int i = 0; i < s_SupportedLanguageFreeIndex; ++i)
			{
				if(s_SupportedLanguages[i].getLanguageLocaleRFC3066().equals(newLang.getLanguageLocaleRFC3066()))
				{
					alreadyExisted = true;
					break;
				}
			}
			
			if((!alreadyExisted)&&(s_SupportedLanguageFreeIndex < s_SupportedLanguages.length))
			{
				s_SupportedLanguages[s_SupportedLanguageFreeIndex++] = newLang;
			}
		}
	}
	
	/**
	 * <p>Retrieves the full human-readable string representing the language-local code in following format:</p>
	 * <p><I>LanguageName</I>-<I>LocaleName</I> [<I>LanguageCode</I>-<I>LocaleCode</I>]</p>
	 * <p>The <I>LanguageName</I> and <I>LocaleName</I> are internationalized to the default local host settings.</p>
	 * @param lang The language specification to retrieve the description.
	 * @return The full description of the specified language.
	 */
	public static String getFullDescription(MiLanguage lang)
	{
		if(null == lang)
		{
			return "";
		}
		
		try
		{
			StringBuilder fullString = new StringBuilder(64)
				.append(java.util.ResourceBundle.getBundle("com/mitel/miutil/MiLanguage").getString(lang.getLanguageCode()));

			String locale = lang.getLocaleCode();
			if((null != locale)&&(locale.length() > 0))
			{
				fullString.append("-").append(java.util.ResourceBundle.getBundle("com/mitel/miutil/MiLanguage").getString(lang.getLocaleCode()));
			}

			return fullString.append(" [").append(lang.getLanguageLocaleRFC3066()).append("]").toString();
		}
		catch(RuntimeException e)
		{
			MiSystem.logError(MiLogMsg.Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
		
		return "";
	}
	
	/**
	 * <p>Retrieves the full human-readable string representing the language-local code in following format:</p>
	 * <p><I>LanguageName</I>-<I>LocaleName</I> [<I>LanguageCode</I>-<I>LocaleCode</I>]</p>
	 * <p>The <I>LanguageName</I> and <I>LocaleName</I> are internationalized to the specified language locale.</p>
	 * @param lang The language specification to retrieve the description.
	 * @param targetLocale The language-locale against which to retrieve the description.
	 * @return The full description of the specified language.
	 */
	public static String getFullDescription(MiLanguage lang, String targetLocale)
	{
		if(null == lang)
		{
			return "";
		}
		Matcher m = s_PatternLanguageLocale.matcher(targetLocale);
		if(m.find())
		{
			String language = m.group(1);
			String country = m.group(3);
			Locale localeSelection;
			if((null == country)||(country.length() < 1))
			{
				localeSelection = new Locale(language);
			}
			else
			{
				localeSelection = new Locale(language);
			}
			
			// Retrieve the description ...
			StringBuilder fullString = new StringBuilder(64)
				.append(java.util.ResourceBundle.getBundle("com/mitel/miutil/MiLanguage", localeSelection).getString(lang.getLanguageCode()));

			String locale = lang.getLocaleCode();
			if((null != locale)&&(locale.length() > 0))
			{
				fullString.append("-").append(java.util.ResourceBundle.getBundle("com/mitel/miutil/MiLanguage", localeSelection).getString(lang.getLocaleCode()));
			}

			// ... and then the codes.
			return fullString.append(" [").append(lang.getLanguageLocaleRFC3066()).append("]").toString();
		}
		return getFullDescription(lang);
	}
	
	/**
	 * Retrieves a MiLanguage object based on its full description.
	 * @param desc The full description of the desired language-locale.
	 * @return The equivalent MiLanguage of the specified language description, or null if no match is found.
	 */
	public static MiLanguage parseFullDescription(String desc)
	{
		// Extract the language code from the description
		Matcher m = s_LanguageEnclosePattern.matcher(desc);
		if(m.find())
		{
			return parseLanguageLocale(m.group(1));
		}
		
		return null;
	}
	
	/**
	 * Test program
	 *
	 * @param args Command line arguments
	 */
	public static void main(String[] args)
	{
		MiLanguage.addSupportedLanguage(MiLanguage.en);
		MiLanguage.addSupportedLanguage(MiLanguage.en_CA);
		MiLanguage.addSupportedLanguage(MiLanguage.en_GB);
		MiLanguage.addSupportedLanguage(MiLanguage.en_US);
		MiLanguage.addSupportedLanguage(MiLanguage.es);
		MiLanguage.addSupportedLanguage(MiLanguage.es_ES);
		MiLanguage.addSupportedLanguage(MiLanguage.es_MX);
		MiLanguage.addSupportedLanguage(MiLanguage.fr);
		MiLanguage.addSupportedLanguage(MiLanguage.fr_CA);
		MiLanguage.addSupportedLanguage(MiLanguage.fr_FR);
		MiLanguage.addSupportedLanguage(MiLanguage.it);
		MiLanguage.addSupportedLanguage(MiLanguage.de);
		MiLanguage.addSupportedLanguage(MiLanguage.pt);
		MiLanguage.addSupportedLanguage(MiLanguage.nl);
		
		System.out.println("parseLanguageLocale test");
		System.out.println(MiLanguage.parseLanguageLocale("en"));
		System.out.println(MiLanguage.parseLanguageLocale("en-US"));
		System.out.println(MiLanguage.parseLanguageLocale("es-CA"));
		System.out.println(MiLanguage.parseLanguageLocale("123-CA"));
		System.out.println(MiLanguage.parseLanguageLocale("gh-ddd"));
		System.out.println(MiLanguage.parseLanguageLocale("gh-dddfff"));
		System.out.println(MiLanguage.parseLanguageLocale("ghgggg-gv"));
		System.out.println(MiLanguage.parseLanguageLocale("en-"));
		System.out.println("parseFullDescription test");
		System.out.println(MiLanguage.parseFullDescription("HGASDHG-KJHG KJASHD [en]"));
		System.out.println(MiLanguage.parseFullDescription("More junk [en-US]"));
		System.out.println(MiLanguage.parseFullDescription("a asd asd [es-CA"));
		System.out.println(MiLanguage.parseFullDescription("[] asd [123-CA]"));
		System.out.println(MiLanguage.parseFullDescription("gh-ddd]"));
		System.out.println(MiLanguage.parseFullDescription("[gh-dddfff]"));
		System.out.println(MiLanguage.parseFullDescription("ghg]gg-gv]"));
		System.out.println(MiLanguage.parseFullDescription("[[en-]]"));
		System.out.println("getSupportedLanguages test");
		MiLanguage[] langs = getSupportedLanguages();
		for(int i = 0; i < langs.length; ++i)
		{
			System.out.println(langs[i]);
			System.out.println(MiLanguage.getFullDescription(langs[i]));
			System.out.println(MiLanguage.getFullDescription(langs[i], langs[i].getLanguageLocaleRFC3066()));
		}
	}

	/**
	 * Language code as per ISO639
	 */
	private String m_LanguageCode = "";
	
	/**
	 * Country code as per ISO3166
	 */
	private String m_LocaleCode = null;
	
	/**
	 * Pattern searching for [language-code]
	 */
	private static final Pattern s_LanguageEnclosePattern = Pattern.compile("\\[([^\\]]+)\\]");

	/**
	 * Pattern searching for nnn-mmm or nnn_MMM Language-Locale combo.
	 */
	private static final Pattern s_PatternLanguageLocale = Pattern.compile("([a-z]{2,3})([\\-_]([a-zA-Z]{2,3}))?");
	
	/**
	 * Default english (US)
	 */
	public static final MiLanguage en = new MiLanguage("en");
	/**
	 * Enghish US
	 */
	public static final MiLanguage en_US = new MiLanguage("en", "US");
	/**
	 * Enghish Canada
	 */
	public static final MiLanguage en_CA = new MiLanguage("en", "CA");
	/**
	 * Enghish UK
	 */
	public static final MiLanguage en_GB = new MiLanguage("en", "GB");
	/**
	 * Default french (France)
	 */
	public static final MiLanguage fr = new MiLanguage("fr");
	/**
	 * French France (Europe)
	 */
	public static final MiLanguage fr_FR = new MiLanguage("fr", "FR");
	/**
	 * French Canada
	 */
	public static final MiLanguage fr_CA = new MiLanguage("fr", "CA");
	/**
	 * Default spanish (Spain)
	 */
	public static final MiLanguage es = new MiLanguage("es");
	/**
	 * Spanish Spain (Europe)
	 */
	public static final MiLanguage es_ES = new MiLanguage("es", "ES");
	/**
	 * Spanish Mexico (Latin America)
	 */
	public static final MiLanguage es_MX = new MiLanguage("es", "MX");
	/**
	 * Italian
	 */
	public static final MiLanguage it = new MiLanguage("it");
	/**
	 * Dutch
	 */
	public static final MiLanguage nl = new MiLanguage("nl");
	/**
	 * Portuguese
	 */
	public static final MiLanguage pt = new MiLanguage("pt");
	/**
	 * German
	 */
	public static final MiLanguage de = new MiLanguage("de");
	
	private static int s_SupportedLanguageFreeIndex = 0;
	/**
	 * All supported languages
	 */
	private static MiLanguage[] s_SupportedLanguages = new MiLanguage[512];
	/**
	 * Used for synchronizing access to class members.
	 */
	private static final Object s_StaticLock = new Object();
}

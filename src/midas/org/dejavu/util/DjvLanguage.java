/*
 * DjvLanguage.java
 *
 * Created on May 23, 2006, 8:04 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.dejavu.util;

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
public class DjvLanguage
{
	/**
	 * Creates a new instance of DjvLanguage
	 * @param languageLocale The languageLocale code (either Java spec or RFC3066).
	 */
	public DjvLanguage(String languageLocale)
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
	 * Creates a new instance of DjvLanguage
	 * @param language The language code (ISO639).
	 * @param locale The locale code (ISO3166)
	 */
	public DjvLanguage(String language, String locale)
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
	public boolean equals(DjvLanguage rhs)
	{
		return equals(rhs.getLanguageCode(), rhs.getLocaleCode());
	}
	
	@Override
	public boolean equals(Object rhs)
	{
		if(rhs instanceof DjvLanguage)
		{
			DjvLanguage lang = (DjvLanguage)rhs;
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
	 * Parses a string containing language-locale information (Java spec OR RFC3066) into an equivalent DjvLanguage object.
	 * @param languageLocale The language-locale string to parse.
	 * @return The desired DjvLanguage object, non-null.
	 */
	public static DjvLanguage parseLanguageLocale(String languageLocale)
	{
		synchronized(s_StaticLock) {
			for(int i = 0; i < s_SupportedLanguageFreeIndex; ++i)
			{
				DjvLanguage lang = s_SupportedLanguages[i];
				if(lang.equals(languageLocale))
				{
					return lang;
				}
			}
		}
		return new DjvLanguage(languageLocale);
	}
	
	/**
	 * Retrieves the list of all supported languages from the language repository.
	 * This repository is initially empty until addSupportedLanguage() is invoked,
	 * typically by client apps.
	 * @return The array contains the supported languages.
	 */
	public static DjvLanguage[] getSupportedLanguages()
	{
		synchronized(s_StaticLock)
		{
			DjvLanguage[] retValue = new DjvLanguage[s_SupportedLanguageFreeIndex];
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
	public static void addSupportedLanguage(DjvLanguage newLang)
	{
		synchronized(s_StaticLock)
		{
			if(DjvSystem.diagnosticEnabled())
			{
				DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, "Adding " + newLang);
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
	public static String getFullDescription(DjvLanguage lang)
	{
		if(null == lang)
		{
			return "";
		}
		
		try
		{
			StringBuilder fullString = new StringBuilder(64)
				.append(java.util.ResourceBundle.getBundle("org/dejavu/miutil/DjvLanguage").getString(lang.getLanguageCode()));

			String locale = lang.getLocaleCode();
			if((null != locale)&&(locale.length() > 0))
			{
				fullString.append("-").append(java.util.ResourceBundle.getBundle("org/dejavu/miutil/DjvLanguage").getString(lang.getLocaleCode()));
			}

			return fullString.append(" [").append(lang.getLanguageLocaleRFC3066()).append("]").toString();
		}
		catch(RuntimeException e)
		{
			DjvSystem.logError(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
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
	public static String getFullDescription(DjvLanguage lang, String targetLocale)
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
				.append(java.util.ResourceBundle.getBundle("org/dejavu/miutil/DjvLanguage", localeSelection).getString(lang.getLanguageCode()));

			String locale = lang.getLocaleCode();
			if((null != locale)&&(locale.length() > 0))
			{
				fullString.append("-").append(java.util.ResourceBundle.getBundle("org/dejavu/miutil/DjvLanguage", localeSelection).getString(lang.getLocaleCode()));
			}

			// ... and then the codes.
			return fullString.append(" [").append(lang.getLanguageLocaleRFC3066()).append("]").toString();
		}
		return getFullDescription(lang);
	}
	
	/**
	 * Retrieves a DjvLanguage object based on its full description.
	 * @param desc The full description of the desired language-locale.
	 * @return The equivalent DjvLanguage of the specified language description, or null if no match is found.
	 */
	public static DjvLanguage parseFullDescription(String desc)
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
		DjvLanguage.addSupportedLanguage(DjvLanguage.en);
		DjvLanguage.addSupportedLanguage(DjvLanguage.en_CA);
		DjvLanguage.addSupportedLanguage(DjvLanguage.en_GB);
		DjvLanguage.addSupportedLanguage(DjvLanguage.en_US);
		DjvLanguage.addSupportedLanguage(DjvLanguage.es);
		DjvLanguage.addSupportedLanguage(DjvLanguage.es_ES);
		DjvLanguage.addSupportedLanguage(DjvLanguage.es_MX);
		DjvLanguage.addSupportedLanguage(DjvLanguage.fr);
		DjvLanguage.addSupportedLanguage(DjvLanguage.fr_CA);
		DjvLanguage.addSupportedLanguage(DjvLanguage.fr_FR);
		DjvLanguage.addSupportedLanguage(DjvLanguage.it);
		DjvLanguage.addSupportedLanguage(DjvLanguage.de);
		DjvLanguage.addSupportedLanguage(DjvLanguage.pt);
		DjvLanguage.addSupportedLanguage(DjvLanguage.nl);
		
		System.out.println("parseLanguageLocale test");
		System.out.println(DjvLanguage.parseLanguageLocale("en"));
		System.out.println(DjvLanguage.parseLanguageLocale("en-US"));
		System.out.println(DjvLanguage.parseLanguageLocale("es-CA"));
		System.out.println(DjvLanguage.parseLanguageLocale("123-CA"));
		System.out.println(DjvLanguage.parseLanguageLocale("gh-ddd"));
		System.out.println(DjvLanguage.parseLanguageLocale("gh-dddfff"));
		System.out.println(DjvLanguage.parseLanguageLocale("ghgggg-gv"));
		System.out.println(DjvLanguage.parseLanguageLocale("en-"));
		System.out.println("parseFullDescription test");
		System.out.println(DjvLanguage.parseFullDescription("HGASDHG-KJHG KJASHD [en]"));
		System.out.println(DjvLanguage.parseFullDescription("More junk [en-US]"));
		System.out.println(DjvLanguage.parseFullDescription("a asd asd [es-CA"));
		System.out.println(DjvLanguage.parseFullDescription("[] asd [123-CA]"));
		System.out.println(DjvLanguage.parseFullDescription("gh-ddd]"));
		System.out.println(DjvLanguage.parseFullDescription("[gh-dddfff]"));
		System.out.println(DjvLanguage.parseFullDescription("ghg]gg-gv]"));
		System.out.println(DjvLanguage.parseFullDescription("[[en-]]"));
		System.out.println("getSupportedLanguages test");
		DjvLanguage[] langs = getSupportedLanguages();
		for(int i = 0; i < langs.length; ++i)
		{
			System.out.println(langs[i]);
			System.out.println(DjvLanguage.getFullDescription(langs[i]));
			System.out.println(DjvLanguage.getFullDescription(langs[i], langs[i].getLanguageLocaleRFC3066()));
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
	public static final DjvLanguage en = new DjvLanguage("en");
	/**
	 * Enghish US
	 */
	public static final DjvLanguage en_US = new DjvLanguage("en", "US");
	/**
	 * Enghish Canada
	 */
	public static final DjvLanguage en_CA = new DjvLanguage("en", "CA");
	/**
	 * Enghish UK
	 */
	public static final DjvLanguage en_GB = new DjvLanguage("en", "GB");
	/**
	 * Default french (France)
	 */
	public static final DjvLanguage fr = new DjvLanguage("fr");
	/**
	 * French France (Europe)
	 */
	public static final DjvLanguage fr_FR = new DjvLanguage("fr", "FR");
	/**
	 * French Canada
	 */
	public static final DjvLanguage fr_CA = new DjvLanguage("fr", "CA");
	/**
	 * Default spanish (Spain)
	 */
	public static final DjvLanguage es = new DjvLanguage("es");
	/**
	 * Spanish Spain (Europe)
	 */
	public static final DjvLanguage es_ES = new DjvLanguage("es", "ES");
	/**
	 * Spanish Mexico (Latin America)
	 */
	public static final DjvLanguage es_MX = new DjvLanguage("es", "MX");
	/**
	 * Italian
	 */
	public static final DjvLanguage it = new DjvLanguage("it");
	/**
	 * Dutch
	 */
	public static final DjvLanguage nl = new DjvLanguage("nl");
	/**
	 * Portuguese
	 */
	public static final DjvLanguage pt = new DjvLanguage("pt");
	/**
	 * German
	 */
	public static final DjvLanguage de = new DjvLanguage("de");
	
	private static int s_SupportedLanguageFreeIndex = 0;
	/**
	 * All supported languages
	 */
	private static DjvLanguage[] s_SupportedLanguages = new DjvLanguage[512];
	/**
	 * Used for synchronizing access to class members.
	 */
	private static final Object s_StaticLock = new Object();
}

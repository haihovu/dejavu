/*
 * MiLanguageComboBox.java
 *
 * Created on May 23, 2006, 5:39 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.mitel.guiutil;

import com.mitel.miutil.MiLanguage;
import javax.swing.JComboBox;

/**
 * An extension to the JComboBox that allows clients to select language-locales
 * using their full descriptions, in their local language settings.
 * @author haiv
 */
public class MiLanguageComboBox extends JComboBox
{
	private static final long serialVersionUID = 1L;

	/**
	 * Supported languages.
	 */
	private String[] m_SupportedLanguages;
	
	private void init()
	{
		// Initialize the supported languages array
		MiLanguage[] langs = MiLanguage.getSupportedLanguages();
		addItem(""); // A blank selection
		for(int i = 0; i < langs.length; ++i)
		{
			addItem(MiLanguage.getFullDescription(langs[i]));
		}
	}

	/** Creates a new instance of MiLanguageComboBox */
	public MiLanguageComboBox()
	{
		super();
		init();
		setEditable(false);
	}
	
	/**
	 * Creates a new instance of MiLanguageComboBox
	 * @param defaultLanguage The default language selection.
	 */
	public MiLanguageComboBox(MiLanguage defaultLanguage)
	{
		super();
		init();
		setEditable(false);
		setSelectedLanguage(defaultLanguage);
	}

	/**
	 * Retrieves the selected language.
	 * @return The selected language as a MiLanguage object, or null if none can be found.
	 */
	public MiLanguage getSelectedLanguage()
	{
		return MiLanguage.parseFullDescription((String)getSelectedItem());
	}
	
	/**
	 * Sets the selected language.
	 * @param language The language to be selected.
	 */
	public void setSelectedLanguage(MiLanguage language)
	{
		setSelectedItem(MiLanguage.getFullDescription(language));
	}
}

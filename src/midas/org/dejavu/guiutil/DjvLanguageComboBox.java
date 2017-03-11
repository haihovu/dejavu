/*
 * DjvLanguageComboBox.java
 *
 * Created on May 23, 2006, 5:39 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.dejavu.guiutil;

import org.dejavu.util.DjvLanguage;
import javax.swing.JComboBox;

/**
 * An extension to the JComboBox that allows clients to select language-locales
 * using their full descriptions, in their local language settings.
 * @author haiv
 */
public class DjvLanguageComboBox extends JComboBox
{
	private static final long serialVersionUID = 1L;

	/**
	 * Supported languages.
	 */
	private String[] m_SupportedLanguages;
	
	@SuppressWarnings("unchecked")
	private void init()
	{
		// Initialize the supported languages array
		DjvLanguage[] langs = DjvLanguage.getSupportedLanguages();
		addItem(""); // A blank selection
		for(int i = 0; i < langs.length; ++i)
		{
			addItem(DjvLanguage.getFullDescription(langs[i]));
		}
	}

	/** Creates a new instance of MiLanguageComboBox */
	public DjvLanguageComboBox()
	{
		super();
		init();
		setEditable(false);
	}
	
	/**
	 * Creates a new instance of MiLanguageComboBox
	 * @param defaultLanguage The default language selection.
	 */
	public DjvLanguageComboBox(DjvLanguage defaultLanguage)
	{
		super();
		init();
		setEditable(false);
		setSelectedLanguage(defaultLanguage);
	}

	/**
	 * Retrieves the selected language.
	 * @return The selected language as a DjvLanguage object, or null if none can be found.
	 */
	public DjvLanguage getSelectedLanguage()
	{
		return DjvLanguage.parseFullDescription((String)getSelectedItem());
	}
	
	/**
	 * Sets the selected language.
	 * @param language The language to be selected.
	 */
	public void setSelectedLanguage(DjvLanguage language)
	{
		setSelectedItem(DjvLanguage.getFullDescription(language));
	}
}

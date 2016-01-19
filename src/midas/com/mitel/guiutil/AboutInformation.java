/*
 * AboutDialog.java
 *
 * Created on November 15, 2003, 4:23 PM
 */

package com.mitel.guiutil;

import java.awt.Dimension;
import java.util.List;

public final class AboutInformation
{
	public String m_Title;
	public String m_ImageName;
	public List m_OtherStrings = null;
	public Dimension m_Size;
	
	public AboutInformation(String title, String imageName, Dimension size, List otherStrs)
	{
		m_Title = title;
		m_ImageName = imageName;
		m_Size = size;
		m_OtherStrings = otherStrs;
	}
	
}



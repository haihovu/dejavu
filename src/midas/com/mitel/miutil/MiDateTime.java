/*
 * MiDateTime.java
 *
 * Created on July 12, 2004, 3:21 PM
 */

package com.mitel.miutil;

import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author  haiv
 */
public class MiDateTime
{
	
	public int m_year;
	public int m_month;
	public int m_day;
	public int m_hour;
	public int m_minute;
	public int m_second;
	private Calendar m_calendar;
	
	/** Creates a new instance of MiDateTime */
	public MiDateTime(Date aDate)
	{
		m_calendar = Calendar.getInstance();
		m_calendar.setTime(aDate);
		update();
	}
	
	/** Creates a new instance of MiDateTime */
	public MiDateTime(int year, int mon, int day, int hour, int min, int sec)
	{
		m_calendar = Calendar.getInstance();
		m_calendar.set(year, mon, day, hour, min, sec);
		update();
	}
	
	public MiDateTime(MiDateTime aCopy)
	{
		copy(aCopy);
	}
	
	public MiDateTime()
	{
		m_calendar = Calendar.getInstance();
		update();
	}
	
	public void copy(MiDateTime aCopy)
	{
		m_calendar = (Calendar)aCopy.m_calendar.clone();
		update();
	}
	
	private void update()
	{
		m_year = m_calendar.get(Calendar.YEAR);
		m_month = m_calendar.get(Calendar.MONTH);
		m_day = m_calendar.get(Calendar.DAY_OF_MONTH);
		m_hour = m_calendar.get(Calendar.HOUR_OF_DAY);
		m_minute = m_calendar.get(Calendar.MINUTE);
		m_second = m_calendar.get(Calendar.SECOND);
	}
	
	public String toString()
	{
		return (String.valueOf(m_year)+"/"+String.valueOf(m_month)+"/"+String.valueOf(m_day)+" "+String.valueOf(m_hour)+":"+String.valueOf(m_minute)+":"+String.valueOf(m_second));
	}
	
}

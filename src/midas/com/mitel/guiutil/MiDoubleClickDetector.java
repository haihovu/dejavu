/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mitel.guiutil;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Implementation for a detector of double-click events.
 * @author haiv
 */
public class MiDoubleClickDetector
{
	private final JComponent targetComponent;
	private long clickTs;
	private long doubleClickThresholdMs;
	private final Runnable doubleClickHandler;
	private MouseAdapter mouseAdaptor;
	
	/**
	 * Creates a new double-click detector
	 * @param target The component against which double-click events are to be detected
	 * @param handler A handler that will be executed by the EDT when a double click event is detected.
	 * @param thresholdMs Double click timing threshold, recommended is 200ms.
	 */
	public MiDoubleClickDetector(JComponent target, Runnable handler, long thresholdMs)
	{
		targetComponent = target;
		doubleClickThresholdMs = thresholdMs;
		doubleClickHandler = handler;
		SwingUtilities.invokeLater(new SwingTaskInit());
	}
	
	/**
	 * Disposes the double click detector
	 */
	public void dispose()
	{
		SwingUtilities.invokeLater(new SwingTaskDispose());
	}
	
	private class SwingTaskDispose implements Runnable
	{
		private SwingTaskDispose()
		{
			super();
		}

		@Override
		public void run()
		{
			if(mouseAdaptor != null)
			{
				targetComponent.removeMouseListener(mouseAdaptor);
				mouseAdaptor = null;
			}
		}
	}
	
	private class SwingTaskInit implements Runnable
	{
		private SwingTaskInit()
		{
			super();
		}

		@Override
		public void run()
		{
			mouseAdaptor = new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if(!e.isPopupTrigger())
					{
						if(clickTs == 0)
						{
							clickTs = System.currentTimeMillis();
						}
						else
						{
							long ts = System.currentTimeMillis();
							if((ts - clickTs) < doubleClickThresholdMs)
							{
								clickTs = 0;
								if(doubleClickHandler != null)
								{
									doubleClickHandler.run();
								}
							}
							else
							{
								clickTs = ts;
							}
						}
					}
					else
					{
						clickTs = 0;
					}
				}

				@Override
				public void mousePressed(MouseEvent e)
				{
					if(e.isPopupTrigger())
					{
						clickTs = 0;
					}
					
				}

				@Override
				public void mouseReleased(MouseEvent e)
				{
					if(e.isPopupTrigger())
					{
						clickTs = 0;
					}
				}
			};
			
			targetComponent.addMouseListener(mouseAdaptor);
		}
	}
}

package org.dejavu.guiutil;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import org.dejavu.util.DjvExceptionUtil;
import org.dejavu.util.DjvLogMsg;
import org.dejavu.util.DjvSystem;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * A pane that can be used to display some notification messages on top of a Window or Dialog,
 * as glass panes.
 */
public class DjvMessagePane extends DjvFadingPane {
	private static final long serialVersionUID = 1L;
	/**
	 * All attributes of the message pane must only be accessed from the EDT.
	 * Swing task for setting the messages to be displayed.
	 */
	private class SwingSetMessage implements Runnable {
		private final Object msg;
		private final int type;
		
		/**
		 * Creates a Swing task for setting the messages to be displayed.
		 * @param msg The messages
		 * @param type The type of message
		 */
		private SwingSetMessage(Object msg, int type) {
			super();
			this.msg = msg;
			this.type = type;
		}

		@Override
		public void run() {
			if(msg instanceof Object[]) {
				Object[] msgs = (Object[])msg;
				messages = new String[msgs.length];
				for(int i = 0; i < msgs.length; ++i) {
					messages[i] = msgs[i].toString();
				}
			} else if(msg instanceof Collection<?>) {
				Collection<?> msgs = (Collection<?>)msg;
				messages = new String[msgs.size()];
				int i = 0;
				for(Object pojo : msgs) {
					messages[i++] = pojo.toString();
				}
			} else {
				messages = new String[]{String.valueOf(msg)};
			}
			switch(type) {
				case JOptionPane.INFORMATION_MESSAGE:
					fontColor = Color.GREEN;
					break;
					
				case JOptionPane.WARNING_MESSAGE:
					fontColor = Color.ORANGE;
					break;
					
				case JOptionPane.ERROR_MESSAGE:
					fontColor = Color.RED;
					break;
					
				default:fontColor = Color.WHITE;
					break;
			}
			repaint();
		}
	}
	
	private String[] messages;
	private final Font font = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private final int lineHeight;
	private final int lineSpacing = 4;
	private Color fontColor;
	
	/**
	 * Creates a new message pane. Must be invoked from the EDT.
	 * @param disposalListener Optional listener for disposal events, i.e. when the pane is completely
	 * faded away, or otherwise destroyed. Typically for callers to reset their glass pane. Will be invoked
	 * once from the EDT.
	 */
	public DjvMessagePane(ActionListener disposalListener) {
		super(disposalListener);
		lineHeight = (int)(font.getStringBounds("Tglp", new FontRenderContext(null, false, false)).getHeight());
		
		setOpaque(false);
	}

	/**
	 * Specifies the message(s) to be displayed in this pane. May be invoked from any thread.
	 * @param msg The message(s) to be displayed. May be one of:
	 * <ul>
	 * <li>Object[] - E.g. String[]</li>
	 * <li>Collection - E.g. List</li>
	 * <li>Object - E.g. String</li>
	 * </ul>
	 * @param type One of JOptionPane message types, e.g. JOptionPane.INFORMATION_MESSAGE
	 * @return This pane.
	 */
	public DjvMessagePane setMessage(Object msg, int type) {
		SwingUtilities.invokeLater(new SwingSetMessage(msg, type));
		return this;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		try {
			Rectangle bounds = getBounds();
			Graphics local = g.create();
			try {
				if (local instanceof Graphics2D) {
					Graphics2D g2d = (Graphics2D) local;
					g2d.setPaint(new LinearGradientPaint(0.0f, 0.0f, (float)bounds.getWidth(), (float)bounds.getHeight(), new float[]{0.0f, 1.0f}, new Color[]{Color.DARK_GRAY, Color.BLACK}));
					g2d.setComposite(AlphaComposite.SrcOver.derive(alfa));
					g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
					// If not fading then restore the alpha to full before writing text
					if(!isFading()) {
						g2d.setComposite(AlphaComposite.SrcOver);
					}
					g2d.setColor(fontColor);
					g2d.setFont(font);
					if(messages != null) {
						int yStart = ((bounds.height - (messages.length * (lineHeight + lineSpacing))) / 2) + lineHeight;
						FontRenderContext fontCtx = new FontRenderContext(null, false, false);
						for(int i = 0; i < messages.length; ++i) {
							String msg = messages[i];
							Rectangle2D mtx = font.getStringBounds(msg, fontCtx);
							int xStart = (bounds.width - (int)mtx.getWidth()) / 2;
							g2d.drawString(msg, xStart, yStart);
							yStart += (lineHeight + lineSpacing);
						}
					}
				}
			} finally {
				local.dispose();
			}
		} catch (RuntimeException e) {
			DjvSystem.logError(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
		}
		super.paintComponent(g);
	}
}
 
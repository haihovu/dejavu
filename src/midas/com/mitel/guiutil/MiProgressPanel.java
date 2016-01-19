package com.mitel.guiutil;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicLabelUI;

/**
 * A progress panel that can be used to display some notification messages on top of a Window or Dialog,
 * as glass panes.
 */
public class MiProgressPanel extends MiFadingPane {
	private static final long serialVersionUID = 1L;
	
	private final JProgressBar progressBar;

	/**
	 * Creates a new progress pane. Must be invoked from EDT.
	 * @param msgs The optional messages to be displayed in the progress panel. May be one of:
	 * <ul>
	 * <li>Object, e.g. String, or some other objects that can be turned into a string</li>
	 * <li>Object[], e.g. String[], or arrays of other objects that can be turned into strings</li>
	 * <li>Collection, e.g. List of objects that can be turned into strings</li>
	 * </ul>
	 * @param dismissListener Optional listener for handling dismiss events. Typical dismiss
	 * event handler replaces this progress panel with the original glass pane of the parent window.
	 */
	public MiProgressPanel(Object msgs, ActionListener dismissListener) {
		super(dismissListener);
		setOpaque(false);
		
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		
		// Add any messages
		String[] messages;
		if(msgs instanceof Object[]) {
			Object[] msg = (Object[])msgs;
			messages = new String[msg.length];
			for(int i = 0; i < msg.length; ++i) {
				messages[i] = msg[i].toString();
			}
		} else if(msgs instanceof Collection<?>) {
			Collection<?> msg = (Collection<?>)msgs;
			messages = new String[msg.size()];
			int i = 0;
			for(Object pojo : msg) {
				messages[i++] = pojo.toString();
			}
		} else {
			messages = new String[]{String.valueOf(msgs)};
		}
		GridBagConstraints constraint = new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0);
		if(messages != null) {
			Font font = new Font(Font.SANS_SERIF, Font.BOLD, 12);
			for(int i = 0; i < messages.length; ++i) {
				constraint.gridy = i;
				JLabel msgLabel = new JLabel(messages[i]);
				// Prevent the look and feel from changing the colour/composite of the label
				msgLabel.setUI(new BasicLabelUI());
				msgLabel.setFont(font);
				msgLabel.setForeground(Color.GREEN);
				add(msgLabel, constraint);
			}
		}
		
		// Add the progress bar
		constraint.gridy += 1;
		constraint.fill = GridBagConstraints.HORIZONTAL;
		progressBar = new JProgressBar();
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		progressBar.setStringPainted(true);
		add(progressBar, constraint);
		
		// Add the dismiss button
		constraint.gridy += 1;
		constraint.fill = GridBagConstraints.NONE;
		JButton dismissButton = new JButton("Dismiss");
		dismissButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				dismiss(0);
			}
		});
		add(dismissButton, constraint);
	}

	/**
	 * Specifies the percentage value for the progress bar in this panel. May be invoked from any thread.
	 * @param percent The percentage value to be set to the progress bar. Values
	 * outside of the 0-100 range are considered undeterminate.
	 * @return This panel
	 */
	public MiProgressPanel setProgress(final int percent) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if((percent >= 0)&&(percent <= 100)) {
					progressBar.setIndeterminate(false);
					progressBar.setValue(percent);
				} else {
					progressBar.setIndeterminate(true);
				}
			}
		});
		return this;
	}
	
	/**
	 * Specifies the text value for the progress bar in this panel. May be invoked from any thread.
	 * @param text The text value to be set to the progress bar.
	 * @return This panel
	 */
	public MiProgressPanel setProgress(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				progressBar.setString(text);
			}
		});
		return this;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		Rectangle bounds = getBounds();
		Graphics local = g.create();
		try {
			if (local instanceof Graphics2D) {
				Graphics2D g2d = (Graphics2D) local;
				g2d.setPaint(new LinearGradientPaint(0.0f, 0.0f, (float)bounds.getWidth(), (float)bounds.getHeight(), new float[]{0.0f, 1.0f}, new Color[]{Color.DARK_GRAY, Color.BLACK}));
				g2d.setComposite(AlphaComposite.SrcOver.derive(alfa));
				g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		} finally {
			local.dispose();
		}
		super.paintComponent(g);
	}
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.guiutil;

import org.dejavu.util.DjvBackgroundTask;
import org.dejavu.util.DjvExceptionUtil;
import org.dejavu.util.DjvLogMsg;
import org.dejavu.util.DjvSystem;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Base class for panes (typically glass panes) that can fade away.
 * @author haiv
 */
public abstract class DjvFadingPane extends JPanel{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Background task for dismissing the panel. A gradual fading feature
	 * is supported.
	 */
	private class TaskDismissInititate extends DjvBackgroundTask {
		/**
		 * Duration of actual fade, anything longer will be delayed
		 */
		private final long fadeDurationMs = 1000;
		/**
		 * Period of each fade execution
		 */
		private final long fadePeriodMs = 100;
		/**
		 * Fade intervals, fade duration divide by fade period
		 */
		private final long fadeIntervals = fadeDurationMs / fadePeriodMs;
		private float fadeDec = 0.005f;
		private final float fadeDecInc = (2 * (alfa - (fadeDec * fadeIntervals))) / (fadeIntervals * (fadeIntervals - 1));

		private final long delayMs;
		
		/**
		 * Creates a new background task for dismissing the progress panel. A gradual fading feature
		 * @param delayMs Fading delay before the panel is completely dismissed.
		 */
		private TaskDismissInititate(long delayMs) {
			super("TaskFading");
			this.delayMs = delayMs;
		}
		
		@Override
		@SuppressWarnings("SleepWhileInLoop")
		public void run() {
			try {
				// Perform any fading action
				if(delayMs > 0) {
					try {
						if(delayMs > fadeDurationMs) {
							Thread.sleep(delayMs - fadeDurationMs);
						}
						long ts = System.currentTimeMillis();
						long timeLeft = fadeDurationMs;
						synchronized(DjvFadingPane.this) {
							fading = true;
						}
						while(getRunFlag() && (timeLeft > 0)) {
							if(isCleared()) {
								break;
							}
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									alfa -= fadeDec; // One fade degree
									fadeDec += fadeDecInc;
									if(alfa < 0.0f) {
										// The panel is now cleared
										alfa = 0.0f;
										setCleared(true);
									}
									repaint();
								}
							});
							Thread.sleep(fadePeriodMs);
							timeLeft = delayMs - (System.currentTimeMillis() - ts);
						}
					} catch(InterruptedException e) {
					}
				}
				// Finally actually dismissing the panel, and invoking any callback.
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if(disposalListener != null) {
							disposalListener.actionPerformed(new ActionEvent(this, 0, "dismiss"));
							disposalListener = null; // Only callback once
						}
					}
				});
			} catch(RuntimeException e) {
				DjvSystem.logError(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
			} finally {
				synchronized(DjvFadingPane.this) {
					if(taskFadeProcessor == this) {
						taskFadeProcessor = null;
					}
				}
			}
		}
	}

	/**
	 * The alpha value for rendering the background for the pane in normal condition,
	 * or all content during fading situation. Meant to be used ONLY by the EDT.
	 */
	@SuppressWarnings({"PackageVisibleField", "ProtectedField"})
	protected float alfa = 0.65f;
	/**
	 * Flag determining whether the pane had faded completely.
	 */
	private boolean cleared;
	private boolean fading;
	
	/**
	 * Optional listener to receive the disposal event (when the pane is destroyed),
	 * typically to restore the glass pane to the original state. Will be invoked from
	 * the EDT. Meant to be used ONLY by the EDT.
	 */
	@SuppressWarnings({"PackageVisibleField", "ProtectedField"})
	protected ActionListener disposalListener;
	private DjvBackgroundTask taskFadeProcessor;
	/**
	 * Creates a new fading pane
	 * @param disposalListener Optional listener for disposal events, i.e. when the pane is completely faded,
	 * or otherwise destroyed. This event is invoked only once, from the EDT.
	 */
	protected DjvFadingPane(ActionListener disposalListener) {
		super();
		this.disposalListener = disposalListener;
		
		// Block all mouse/key events from reaching the layers underneath
		addMouseMotionListener(new MouseMotionAdapter() {});
		addKeyListener(new KeyAdapter() {});
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent ce) {
				// Grab the focus ...
				requestFocusInWindow();
				// And not letting it going back to the pane below.
				setFocusTraversalKeysEnabled(false);
			}
		});
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent me) {
				dismiss(0);
			}
		});
		
		// ESCAPE will also dismiss the pane
		String action = "dismiss";
		getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), action);
		getActionMap().put(action, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				dismiss(0);
			}
		});
	}
	
	/**
	 * Starts this pane, making it visible. May be invoked from any thread.
	 * @return This object
	 */
	public DjvFadingPane start() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setVisible(false); // In case we are replacing some existing visible glass pane component
				setVisible(true);
			}
		});
		return this;
	}
	
	/**
	 * Dismisses the pane, and executes any action listener previously registered.
	 * May be invoked from any thread.
	 * @param fadeDuration Delay in fading the pane to invisible. Zero or less means no delay,
	 * clear the pane immediately.
	 */
	public void dismiss(long fadeDuration) {
		synchronized(this) {
			if(taskFadeProcessor != null) {
				taskFadeProcessor.stop();
			}
			taskFadeProcessor = new TaskDismissInititate(fadeDuration).start();
		}
	}

	/**
	 * Is the pane cleared, i.e. completely faded away.
	 * @return Yes: cleared, no: not really.
	 */
	protected boolean isCleared() {
		synchronized(this) {
			return cleared;
		}
	}

	/**
	 * Specifies whether the pane had been cleared, i.e. completely faded away.
	 * @param value The new clear value
	 * @return This object
	 */
	protected DjvFadingPane setCleared(boolean value) {
		synchronized(this) {
			cleared = value;
		}
		return this;
	}
	
	/**
	 * Determines whether fading is in effect.
	 * @return True is fading, false otherwise.
	 */
	protected boolean isFading() {
		synchronized(this) {
			return fading;
		}
	}

	@Override
	protected void paintComponent(Graphics grphcs) {
		if(isFading()) {
			if(grphcs instanceof Graphics2D) {
				((Graphics2D)grphcs).setComposite(AlphaComposite.SrcOver.derive(alfa));
			}
		}
		super.paintComponent(grphcs);
	}
}

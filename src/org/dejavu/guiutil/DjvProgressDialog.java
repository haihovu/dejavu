/*
 * DjvProgressDialog.java
 *
 * Created on May 10, 2004, 2:53 PM
 */

package org.dejavu.guiutil;

import org.dejavu.util.DjvBackgroundTask;
import org.dejavu.util.DjvExceptionUtil;
import org.dejavu.util.DjvLogMsg;
import org.dejavu.util.DjvSystem;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * A progress dialog, which has a progress bar, a cancel button,
 * and an optional label nestled in between the progress bar and the cancel button.
 * All constructors are only safe to be invoked from inside the EDT.
 * Use safeInstantiate() to create instances from other threads.
 * @author  Hai Vu
 */
public class DjvProgressDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * <p>Creates a new instance of MiProgressDialog against a Frame parent.</p>
	 * <b>THIS MUST BE INVOKED FROM THE CONTEXT OF THE AWT EVENT DISPATCH THREAD.
	 * Use safeInstantiate() to be safe.</b>
	 * @param parent The optional parent Frame of this dialog.
	 * @param anchor The optional anchor for this dialog.
	 * @param title The tile of the dialog
	 * @param initialValue The initial progress value (typically zero)
	 * @param indetermninate Flag indicating whether the progress bar is to be indeterminate or not.
	 * True means indeterminate.
	 * @param listener The optional listener for events such as cancel.
	 */
	private DjvProgressDialog(Frame parent, Component anchor, String title, int initialValue, boolean indetermninate, MiProgressListener listener)
	{
		super(parent, true);
		m_AnchorComponent = anchor;
		m_Indeterminate = indetermninate;
		constructorInit(title, initialValue, listener);
	}
	
	/**
	 * <p>Creates a new instance of MiProgressDialog against a Frame parent.</p>
	 * <b>THIS MUST BE INVOKED FROM THE CONTEXT OF THE AWT EVENT DISPATCH THREAD.
	 * Use safeInstantiate() to be safe.</b>
	 * @param parent The optional parent Frame of this dialog.
	 * @param anchor The optional anchor for this dialog.
	 * @param title The tile of the dialog
	 * @param initialValue The initial progress value (typically zero)
	 * @param indetermninate Flag indicating whether the progress bar is to be indeterminate or not.
	 * True means indeterminate.
	 * @param listener The optional listener for events such as cancel.
	 */
	private DjvProgressDialog(Dialog parent, Component anchor, String title, int initialValue, boolean indetermninate, MiProgressListener listener)
	{
		super(parent, true);
		m_AnchorComponent = anchor;
		m_Indeterminate = indetermninate;
		constructorInit(title, initialValue, listener);
	}
	
	/**
	 * <p>Creates a new instance of MiProgressDialog against a Frame parent.</p>
	 * <b>THIS MUST BE INVOKED FROM THE CONTEXT OF THE AWT EVENT DISPATCH THREAD.
	 * Use safeInstantiate() to be safe.</b>
	 * @param parent The optional parent Frame of this dialog.
	 * @param anchor The optional anchor for this dialog.
	 * @param title The tile of the dialog
	 * @param initialValue The initial progress value (typically zero)
	 * @param indetermninate Flag indicating whether the progress bar is to be indeterminate or not.
	 * True means indeterminate.
	 * @param listener The optional listener for events such as cancel.
	 */
	private DjvProgressDialog(Window parent, Component anchor, String title, int initialValue, boolean indetermninate, MiProgressListener listener)
	{
		super(parent, ModalityType.APPLICATION_MODAL);
		m_AnchorComponent = anchor;
		m_Indeterminate = indetermninate;
		constructorInit(title, initialValue, listener);
	}
	
	/**
	 * <p>Creates a new instance of MiProgressDialog against a non-Frame parent.</p>
	 * <b>THIS MUST BE INVOKED FROM THE CONTEXT OF THE AWT EVENT DISPATCH THREAD.
	 * Use safeInstantiate() to be safe.</b>
	 * @param anchor The optional anchor for this dialog.
	 * @param title The tile of the dialog
	 * @param initialValue The initial progress value (typically zero)
	 * @param indetermninate Flag indicating whether the progress bar is to be indeterminate or not.
	 * True means indeterminate.
	 * @param listener The optional listener for events such as cancel.
	 */
	private DjvProgressDialog(Component anchor, String title, int initialValue, boolean indetermninate, MiProgressListener listener)
	{
		super((Frame)null, true);
		m_AnchorComponent = anchor;
		m_Indeterminate = indetermninate;
		constructorInit(title, initialValue, listener);
		if(m_Indeterminate)
		{
			myProgress.setStringPainted(true);
		}
	}
	
	/**
	 * Instantiates a new instance of DjvProgressDialog, outside of AWT event dispatch thread.
	 * @param anchor The anchor for this dialog.
	 * @param title The tile of the dialog
	 * @param initialValue The initial progress value (typically zero)
	 * @param listener The optional listener for events such as cancel.
	 * @return A new instance of DjvProgressDialog, or null if one cannot be instantiated.
	 */
	public static DjvProgressDialog safeInstantiate(Component anchor, String title, int initialValue, MiProgressListener listener)
	{
		return safeInstantiate(anchor, title, initialValue, false, listener);
	}

	/**
	 * Instantiates a new instance of DjvProgressDialog, outside of AWT event dispatch thread.
	 * @param parent The parent Component of this dialog.
	 * @param title The tile of the dialog
	 * @param initialValue The initial progress value (typically zero)
	 * @param indeterminate Flag indicating whether the progress (percentage completion) is indeterminate.
	 * @param listener The optional listener for events such as cancel.
	 * @return A new instance of DjvProgressDialog, or null if one cannot be instantiated.
	 */
	public static DjvProgressDialog safeInstantiate(Component parent, String title, int initialValue, boolean indeterminate, MiProgressListener listener)
	{
		if(SwingUtilities.isEventDispatchThread())
		{
			if(parent instanceof Frame)
			{
				return new DjvProgressDialog((Frame)parent, parent, title, initialValue, indeterminate, listener);
			}
			else if(parent instanceof Dialog)
			{
				return new DjvProgressDialog((Dialog)parent, parent, title, initialValue, indeterminate, listener);
			}
			else if(parent instanceof Window)
			{
				return new DjvProgressDialog((Window)parent, parent, title, initialValue, indeterminate, listener);
			}
			return new DjvProgressDialog(parent, title, initialValue, indeterminate, listener);
		}

		SwingTaskStaticInstantiator swingTask = new SwingTaskStaticInstantiator(parent, title, initialValue, indeterminate, listener);
		SwingUtilities.invokeLater(swingTask);
		try
		{
			return swingTask.waitForReturn(20000);
		}
		catch(InterruptedException ex)
		{
		}

		return null;
	}

	/**
	 * Common code between all constructors.
	 * @param title
	 * @param initialValue
	 * @param listener
	 */
	private void constructorInit(String title, int initialValue, MiProgressListener listener)
	{
		m_Listener = listener;
		
		initComponents();
		this.setTitle(title);
		optionalText.setText(" ");
		if(m_Indeterminate)
		{
			myProgress.setIndeterminate(m_Indeterminate);
		}
		else
		{
			myProgress.setValue(initialValue);
		}
		pack();
		DjvGuiUtil.registerKeyAction(getRootPane(), KeyEvent.VK_ESCAPE, new AbstractAction()
		{
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e)
			{
				fireCancelEvent();
			}
		});
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        myProgress = new javax.swing.JProgressBar();
        optionalText = new javax.swing.JLabel();
        progressDialogOk = new javax.swing.JButton();

        setTitle("m_Title");
        setModal(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        myProgress.setFocusable(false);
        myProgress.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 4;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        getContentPane().add(myProgress, gridBagConstraints);

        optionalText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        optionalText.setText("-");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 4;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(optionalText, gridBagConstraints);

        progressDialogOk.setText(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("org/dejavu/guiutil/GeneralGui").getString("cancelLabel"), new Object[] {})); // NOI18N
        progressDialogOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                progressDialogOkActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        getContentPane().add(progressDialogOk, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void formWindowClosed(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosed
	{//GEN-HEADEREND:event_formWindowClosed

	}//GEN-LAST:event_formWindowClosed

	private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
	{//GEN-HEADEREND:event_formWindowClosing
		fireCancelEvent();
	}//GEN-LAST:event_formWindowClosing

	private void progressDialogOkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_progressDialogOkActionPerformed
	{//GEN-HEADEREND:event_progressDialogOkActionPerformed
		fireCancelEvent();
	}//GEN-LAST:event_progressDialogOkActionPerformed
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{
		DjvLookAndFeel.setCurrentLookAndFeel(DjvLookAndFeel.LAF_SUBSTANCE_CREME_COFFEE);
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				JFrame testFrame = new JFrame();
				DjvProgressDialog dialog = DjvProgressDialog.safeInstantiate(testFrame, "Test", 12, new MiProgressListener()
				{
					@Override
					public void onCancel( JDialog dialog )
					{
						dialog.dispose();
					}
				});
				if(dialog != null) {
					dialog.setOptionalText(ResourceBundle.getBundle("org/dejavu/guiutil/GeneralGui").getString("pleaseWaitLabel"));
					dialog.setProgress(55);
					dialog.setVisible(true);
				}
				testFrame.dispose();
			}
		});
	}
	
	/**
	 * Sets the optional label (below the progress bar and above the dismiss button) text.
	 * May be invoked from any thread.
	 * @param text THe text to be set.
	 * @return This object.
	 */
	public DjvProgressDialog setOptionalText(java.lang.String text)
	{
		SwingUtilities.invokeLater(new SwingTaskOptionalTextUpdater(text));
		return this;
	}
	
	/**
	 * Changes the cancel button's text. May be invoked from any thread.
	 * @param newText The new text to be set.
	 * @return This object.
	 */
	public DjvProgressDialog setCancelButtonText(String newText)
	{
		SwingUtilities.invokeLater(new SwingTaskDismissButtonTextUpdater(newText));
		return this;
	}
	
	/**
	 * Sets the progress, in percentage.
	 * May be invoked from any thread.
	 * @param percent The progress in percent value.
	 * @return This object.
	 */
	public DjvProgressDialog setProgress(int percent)
	{
		SwingUtilities.invokeLater(new SwingTaskProgressUpdater(percent));
		return this;
	}

	/**
	 * Sets the progress with some arbitrary textual info.
	 * May be invoked from any thread.
	 * @param text The text to be displayed on the progress bar.
	 * @return This object.
	 */
	public DjvProgressDialog setProgress(String text)
	{
		SwingUtilities.invokeLater(new SwingTaskProgressUpdater(text));
		return this;
	}

	private void fireCancelEvent()
	{
		if( null != m_Listener )
		{
			m_Listener.onCancel(this);
			m_Listener = null;
		}
		else
		{
			dismiss();
		}
	}
	
	/**
	 * Starts the progress dialog, in effect making it visible.
	 * This is a non-blocking method.
	 * May be invoked from any thread.
	 * @return This dialog.
	 */
	public DjvProgressDialog start()
	{
		DjvGuiUtil.centerAndMakeVisible(this, m_AnchorComponent);
		return this;
	}
	
	/**
	 * A thread-safe dispose().
	 * May be invoked from any thread.
	 */	
	public void dismiss()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				DjvProgressDialog.this.setVisible(false);
				DjvProgressDialog.this.dispose();
			}
		});
	}
	
	/**
	 * A thread-safe, read asynchronous, dispose().
	 * May be invoked from any thread.
	 * @param delayMs Delay before dismissing the dialog.
	 */
	public void dismiss(long delayMs)
	{
		new BgTaskDismiss(delayMs).start();
	}
	
	/**
	 * Background task for dismissing the dialog with an optional delay.
	 */
	class BgTaskDismiss extends DjvBackgroundTask
	{
		private final long localDelayMs;
		/**
		 * Creates a new background task for dismissing this dialog.
		 * @param delayMs Optional delay before actually dismissing the dialog, in milliseconds.
		 * Zero or less means no delay.
		 */
		private BgTaskDismiss(long delayMs)
		{
			super();
			localDelayMs = delayMs;
		}

		@Override
		public void run()
		{
			try
			{
				if(localDelayMs > 0)
				{
					Thread.sleep(localDelayMs);
				}
			}
			catch(InterruptedException ex)
			{
			}

			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					DjvProgressDialog.this.setVisible(false);
					DjvProgressDialog.this.dispose();
				}
			});
		}
	}
	
	/**
	 * Safely sets the dimension of the dialog, using the EDT.
	 * May be invoked from any thread.
	 * @param newDimension The new dimension to set.
	 * @return This object.
	 */
	public DjvProgressDialog setDimension(Dimension newDimension)
	{
		SwingUtilities.invokeLater(new SwingTaskSetDimension(newDimension));
		return this;
	}
	
	private class SwingTaskDismissButtonTextUpdater implements Runnable
	{
		private final String newText;
		private SwingTaskDismissButtonTextUpdater(String text)
		{
			newText = text;
		}
		@Override
		public void run()
		{
			progressDialogOk.setText(newText);
		}
		
	}
	private final class SwingTaskOptionalTextUpdater implements Runnable
	{
		private final String newText;
		
		private SwingTaskOptionalTextUpdater(java.lang.String text)
		{
			newText = text;
		}

		@Override
		public void run()
		{
			optionalText.setText(newText);
			repaint();
		}
		
	}
	
	/**
	 * Swing task for setting the dimension of the dialog.
	 */
	private final class SwingTaskSetDimension implements Runnable
	{
		private final Dimension newDimension;
		private SwingTaskSetDimension(Dimension dim)
		{
			newDimension = dim;
		}

		@Override
		public void run()
		{
			setSize(newDimension);
		}
	}
		
	private final class SwingTaskProgressUpdater implements Runnable
	{
		private final int newPercent;
		private final String newText;
		private SwingTaskProgressUpdater(int percent)
		{
			newPercent = percent;
			newText = null;
		}

		private SwingTaskProgressUpdater(String text)
		{
			newPercent = -1;
			newText = text;
		}

		@Override
		public void run()
		{
			if(newText != null)
			{
				if(myProgress.isStringPainted())
				{
					myProgress.setString(newText);
				}
			}
			else
			{
				if((newPercent == 100)&&(m_Indeterminate))
				{
					myProgress.setIndeterminate(false);
				}
				else if((newPercent > -1)&&(newPercent <= 100))
				{
					myProgress.setIndeterminate(false);
				}
				else if((newPercent < 0)&&(m_Indeterminate))
				{
					if(!myProgress.isIndeterminate())
					{
						myProgress.setIndeterminate(true);
					}
				}
				myProgress.setValue(newPercent);
			}
			repaint();
		}
	}
	
	/**
	 * Listener interface for receiving progress dialog asynchronous events.
	 */
	public static abstract interface MiProgressListener
	{
		
		/**
		 * Handles the cancel event.
		 * @param progressDialog The dialog from which the cancel event originated.
		 */
		public abstract void onCancel(JDialog progressDialog);
		
	}
	
	private static class SwingTaskStaticInstantiator implements Runnable
	{
		/**
		 * Optional parent of the new progress dialog. If this is set then <i>parentFrame</i> should be null.
		 */
		private final Component parentComponent;
		private final String dialogTitle;
		private final int initialPercent;
		private final boolean indeterminateFlag;
		private final MiProgressListener clientListener;
		
		private final Object returnLock = new Object();
		private DjvProgressDialog returnValue;
	
		/**
		 * Creates a new Swing task for instantiating a new progress dialog.
		 * @param parent Optional parent of the new progress dialog.
		 * @param title Title of the new progress dialog.
		 * @param initialValue Initial percent progress
		 * @param indeterminate Flag indicates whether the new progress dialog is indeterminate,
		 * i.e. there is no way of knowing the percent completion.
		 * @param listener Listener interface for receiving asynchronous progress/status updates.
		 */
		private SwingTaskStaticInstantiator(Component parent, String title, int initialValue, boolean indeterminate, MiProgressListener listener)
		{
			parentComponent = parent;
			dialogTitle = title;
			initialPercent = initialValue;
			indeterminateFlag = indeterminate;
			clientListener  = listener;
		}

		private DjvProgressDialog waitForReturn(long timeoutMs) throws InterruptedException
		{
			synchronized(returnLock)
			{
				if((returnValue == null)&&(timeoutMs > 0))
				{
					returnLock.wait(timeoutMs);
				}
				return returnValue;
			}
		}
		
		@Override
		public void run()
		{
			try
			{
				synchronized(returnLock)
				{
					if(parentComponent instanceof Frame)
					{
						returnValue = new DjvProgressDialog((Frame)parentComponent, parentComponent, dialogTitle, initialPercent, indeterminateFlag, clientListener);
					}
					else if(parentComponent instanceof Dialog)
					{
						returnValue = new DjvProgressDialog((Dialog)parentComponent, parentComponent, dialogTitle, initialPercent, indeterminateFlag, clientListener);
					}
					else if(parentComponent instanceof Window)
					{
						returnValue = new DjvProgressDialog((Window)parentComponent, parentComponent, dialogTitle, initialPercent, indeterminateFlag, clientListener);
					}
					else
					{
						returnValue = new DjvProgressDialog(parentComponent, dialogTitle, initialPercent, indeterminateFlag, clientListener);
					}
					
					returnLock.notify();
				}
			}
			catch(RuntimeException e)
			{
				DjvSystem.logError(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
			}
		}
	}
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JProgressBar myProgress;
    private javax.swing.JLabel optionalText;
    private javax.swing.JButton progressDialogOk;
    // End of variables declaration//GEN-END:variables

	private MiProgressListener m_Listener;
	private final Component m_AnchorComponent;
	private final boolean m_Indeterminate;
}

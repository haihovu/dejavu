/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MiSshPasswdPrompt.java
 *
 * Created on Jun 24, 2009, 10:06:51 AM
 */

package org.dejavu.ssh;

import org.dejavu.guiutil.DjvGuiUtil;
import org.dejavu.util.DjvBackgroundTask;
import org.dejavu.util.DjvLogMsg.Category;
import org.dejavu.util.DjvSystem;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

/**
 * A dialog for prompting username/password.
 * @author haiv
 */
public class DjvSshPasswdPrompt extends javax.swing.JDialog
{
	private static final long serialVersionUID = 1L;
	/**
	 * Listener interface for receiving asynchronous events.
	 */
	public interface PasswordListener
	{
		/**
		 * The user commits his/her username/password input.
		 * @param userName The username input.
		 * @param password The password.
		 */
		void commit(String userName, String password);
		/**
		 * The user cancelled the password entry process.
		 */
		void cancelled();
	}

	/**
	 * Task for invoking PasswordListener.commit(), asynchronously.
	 */
	private class BgTaskCommit extends DjvBackgroundTask
	{
		private final String myUserName;
		private final String myPassword;
		/**
		 * Creates a new instance of BgTaskCommit
		 * @param username
		 * @param passwd
		 */
		private BgTaskCommit(String username, String passwd)
		{
			myUserName = username;
			myPassword = passwd;
		}

		@Override
		public void run()
		{
			m_Listener.commit(myUserName, myPassword);
		}
	}
	
	private final PasswordListener m_Listener;
	private boolean m_Done;

	/**
	 * Creates new form RumbaPasswordPrompt.
	 * Must be invoked from EDT.
	 * @param parent The parent of the dialog, if any.
	 * @param existingUserName Value used for initializing the user name field.
	 * @param initialPassword An initial password with which to populate the prompt.
	 * @param listener Mandatory listener interface for receiving user events.
	 */
    public DjvSshPasswdPrompt(java.awt.Frame parent, String existingUserName, String initialPassword, PasswordListener listener)
	{
        super(parent, true);

		listener.getClass(); // Verify non-null
		m_Listener = listener;
		
        initComponents();

		userName.setText(existingUserName);
		if(initialPassword != null)
		{
			password.setText(initialPassword);
		}

		DjvGuiUtil.registerKeyAction(getRootPane(), KeyEvent.VK_ESCAPE, new AbstractAction()
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				dismiss();
			}
		});

		pack();
		if((existingUserName != null)&&(existingUserName.length() > 0))
		{
			password.requestFocusInWindow();
		}
		else
		{
			userName.requestFocusInWindow();
		}
    }

	/**
	 * Swing task for safely instantiating a password prompt dialog.
	 */
	private static class SwingTaskSafeInstance implements Runnable
	{
		private final Frame parentFrame;
		private final String existingUserName;
		private final String initialPassword;
		private final PasswordListener passwordLlistener;
		private DjvSshPasswdPrompt returnValue;
		/**
		 * Creates a new Swing task for safely instantiating a password prompt dialog.
		 * @param parent The optional parent frame
		 * @param userName The initial user name to populate the dialog
		 * @param password The initial password to populate the dialog
		 * @param listener Mandatory listener to be given to the dialog for reporting asynchronous progress/status events.
		 */
		private SwingTaskSafeInstance(Frame parent, String userName, String password, PasswordListener listener)
		{
			parentFrame = parent;
			existingUserName = userName;
			initialPassword = password;
			passwordLlistener = listener;
		}

		@Override
		public void run()
		{
			synchronized(this)
			{
				returnValue = new DjvSshPasswdPrompt(parentFrame, existingUserName, initialPassword, passwordLlistener);
				notify();
			}
		}
		
		/**
		 * Retrieves the password prompt dialog being instantiated.
		 * @param timeoutMs timeout Time out for the wait for the dialog to be created by the EDT.
		 * Zero or less means no wait.
		 * @return The requested prompt dialog, or null if none is currently available.
		 */
		public DjvSshPasswdPrompt getPromptDialog(long timeoutMs)
		{
			synchronized(this)
			{
				if(returnValue != null)
				{
					return returnValue;
				}
				
				try
				{
					if(timeoutMs > 0)
					{
						wait(timeoutMs);
					}
				}
				catch(InterruptedException ex)
				{
				}

				return returnValue;
			}
		}
	}
	
	/**
	 * Safely instantiating a SSH password prompt dialog, using the EDT. Any thread may invoke this method.
	 * This may block for up to 10 seconds to allow the EDT to create the desired dialog.
	 * @param parent An optional parent frame for the requested password prompt dialog
	 * @param existingUserName Initial user name to populate the password prompt dialog
	 * @param initialPassword Initial password to populate the password prompt dialog
	 * @param listener Mandatory listener to handle progress/status events.
	 * @return The requested prompt dialog, or null if none can be instantiated in a timely manner.
	 */
	public static DjvSshPasswdPrompt safeInstantiate(Frame parent, String existingUserName, String initialPassword, PasswordListener listener)
	{
		SwingTaskSafeInstance instantiator = new SwingTaskSafeInstance(parent, existingUserName, initialPassword, listener);
		SwingUtilities.invokeLater(instantiator);
		return instantiator.getPromptDialog(10000);
	}

	/**
	 * Commits this dialog.
	 * Use a separate thread to invoke to callback to avoid blocking the EDT.
	 */
	private void commit()
	{
		if(!m_Done)
		{
			m_Done = true;
			new BgTaskCommit(userName.getText(), String.valueOf(password.getPassword())).start();
		}
		dispose();
	}

	/**
	 * Dismisses/cancels the dialog.
	 * Use a separate thread to invoke to callback to avoid blocking the EDT.
	 */
	private void dismiss()
	{
		if(!m_Done)
		{
			m_Done = true;
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					m_Listener.cancelled();
				}
			}).start();
		}
		dispose();
	}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        userName = new javax.swing.JTextField();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        password = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("SSH Authentication");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        jPanel1.setPreferredSize(new java.awt.Dimension(300, 100));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("User name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel1, gridBagConstraints);

        userName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                userNameActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(userName, gridBagConstraints);

        jLabel2.setText("Password:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel2, gridBagConstraints);

        password.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                passwordActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(password, gridBagConstraints);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void userNameActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_userNameActionPerformed
	{//GEN-HEADEREND:event_userNameActionPerformed
		commit();
}//GEN-LAST:event_userNameActionPerformed

	private void passwordActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_passwordActionPerformed
	{//GEN-HEADEREND:event_passwordActionPerformed
		commit();
	}//GEN-LAST:event_passwordActionPerformed

	private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
	{//GEN-HEADEREND:event_formWindowClosing
		dismiss();
	}//GEN-LAST:event_formWindowClosing

    /**
    * @param args the command line arguments
    */
    public static void main(String args[])
	{
        java.awt.EventQueue.invokeLater(new Runnable()
		{
			@Override
            public void run()
			{
                DjvSshPasswdPrompt dialog = new DjvSshPasswdPrompt(new javax.swing.JFrame(), "1234", null, new PasswordListener()
				{
					@Override
					public void commit(String userName, String password)
					{
						DjvSystem.logInfo(Category.DESIGN, "user name = " + userName + ", password = " + password);
					}

					@Override
					public void cancelled()
					{
						DjvSystem.logInfo(Category.DESIGN, "cancelled");
					}
				});
                dialog.addWindowListener(new java.awt.event.WindowAdapter()
				{
					@Override
                    public void windowClosing(java.awt.event.WindowEvent e)
					{
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPasswordField password;
    private javax.swing.JTextField userName;
    // End of variables declaration//GEN-END:variables

}

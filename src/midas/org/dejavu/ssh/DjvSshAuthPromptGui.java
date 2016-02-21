package org.dejavu.ssh;

import org.dejavu.guiutil.DjvGuiUtil;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolException;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationPrompt;
import java.awt.Frame;
import javax.swing.SwingUtilities;

/**
 * <p>An authentication prompt implementation, using MiSshPasswdPrompt for prompting authentication info.
 * Other implementations may not be GUI based, e.g. terminal based.</P>
 * <p>This is typically for single-use purpose, i.e. discard after use.</p>
 */
public class DjvSshAuthPromptGui implements SshAuthenticationPrompt
{
	private boolean m_PasswordSet;
	private final Frame m_Parent;
	private final PasswordAuthenticationClient m_AuthClient;
	private final DjvSshAuthInfo m_AuthRecord;

	/**
	 * Creates a new instance of MiSshAuthPromptGui.
	 * @param parent The optional parent frame for launching any authentication dialog.
	 * This is used for centering the dialog(s) and may be set to null.
	 * @param authRecord The authentication record with which to authenticate the SSH session.
	 * Any user-entered information in the authentication process will be updated to this record.
	 * @throws AuthenticationProtocolException
	 */
	@SuppressWarnings("LeakingThisInConstructor")
	public DjvSshAuthPromptGui(Frame parent, DjvSshAuthInfo authRecord) throws AuthenticationProtocolException
	{
		super();
		m_Parent = parent;
		m_AuthRecord = authRecord;
		m_AuthClient = new PasswordAuthenticationClient();
		m_AuthClient.setAuthenticationPrompt(this);
		m_AuthClient.setUsername(authRecord.getUserName());
		if(authRecord.getPassword() != null)
		{
			m_AuthClient.setPassword(authRecord.getPassword());
		}
	}

	/**
	 * Retrieves the associated authentication client.
	 * In this case a PasswordAuthenticationClient instance.
	 * @return The authentication client. Non-null.
	 */
	public SshAuthenticationClient getAuthClient()
	{
		return m_AuthClient;
	}

	/**
	 * <p>Implements the SshAuthenticationPrompt method(s).</p>
	 * <b>This is a blocking call, and will not return until the users had entered
	 * their authentication information.</b>
	 * @param authClient This argument is currently ignored.
	 * @return True if the authentication info had been successfully collected
	 * and saved to the given authClient argument.
	 * @throws AuthenticationProtocolException 
	 */
	public boolean showPrompt(SshAuthenticationClient authClient) throws AuthenticationProtocolException
	{
		synchronized(this)
		{
			// Fire off the password prompt dialog ...
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					DjvGuiUtil.centerAndMakeVisible(new DjvSshPasswdPrompt(m_Parent, m_AuthRecord.getUserName(), m_AuthRecord.getPassword(), new DjvSshPasswdPrompt.PasswordListener()
					{
						@Override
						public void commit(String userName, String password)
						{
							synchronized(DjvSshAuthPromptGui.this)
							{
								m_AuthClient.setPassword(password);
								m_AuthClient.setUsername(userName);

								m_AuthRecord.setPassword(password);
								m_AuthRecord.setUserName(userName);

								m_PasswordSet = true;
								DjvSshAuthPromptGui.this.notifyAll();
							}
						}

						@Override
						public void cancelled()
						{
							synchronized(DjvSshAuthPromptGui.this)
							{
								DjvSshAuthPromptGui.this.notifyAll();
							}
						}
					}), m_Parent);
				}
			});
			
			// ... then block here waiting for user input.
			try
			{
				if(!m_PasswordSet)
				{
					wait();
				}
			}
			catch(InterruptedException ex)
			{
			}
			return m_PasswordSet;
		}
	}
}

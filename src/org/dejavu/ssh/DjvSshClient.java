/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dejavu.ssh;

import org.dejavu.util.DjvLogMsg.Category;
import org.dejavu.util.DjvSystem;
import com.sshtools.j2ssh.ScpClient;
import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.SshAuthenticationClient;
import com.sshtools.j2ssh.connection.Channel;
import com.sshtools.j2ssh.connection.ChannelEventAdapter;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;
import java.io.IOException;

/**
 * An SSH client used for managing a single SSH connection from which multiple channels may be opened.
 * @author haiv
 */
public class DjvSshClient
{
	private final SshClient m_SshClient;

	private int m_SftpNumOpen;
	private int m_SftpNumClose;
	private int m_SftpCurrOpen;
	
	/**
	 * Creates a new RumbaClient instance.
	 * This will create a new SshClient instance.
	 * Clients must invoke connectAndAuthenticate() before using.
	 */
	public DjvSshClient()
	{
		m_SshClient = new SshClient();
	}

	/**
	 * Disconnects the client session. Once disconnected this object is no longer usable and should be discarded.
	 */
	public void disconnect()
	{
		m_SshClient.disconnect();
	}

	/**
	 * Connects to a specific SSH server using a given user name.
	 * This will block until the connection is either established successfully or failed for some reasons.
	 * Client shoud always invoke disconnect() on this object once no longer needed, to prevent resource leaks.
	 * @param authClient The authentication client to be used in the authentication process.
	 * @param host The SSH server address/hostname
	 * @throws java.io.IOException
	 */
	public void connectAndAuthenticate(SshAuthenticationClient authClient, String host) throws IOException
	{
		m_SshClient.setSocketTimeout(2000);
		m_SshClient.connect(host, (String arg0, SshPublicKey arg1) -> true);

		DjvSystem.logInfo(Category.MAINTENANCE, "Connecting to " + authClient.getUsername() + "@" + host);

		try
		{
			int res = m_SshClient.authenticate(authClient);
			switch(res)
			{
				case AuthenticationProtocolState.COMPLETE:
					break;

				case AuthenticationProtocolState.CANCELLED:
					throw new IOException("Failed to authenticate due to CANCELLED");

				case AuthenticationProtocolState.FAILED:
					throw new IOException("Failed to authenticate due to FAILED");

				case AuthenticationProtocolState.PARTIAL:
					throw new IOException("Failed to authenticate due to PARTIAL");

				case AuthenticationProtocolState.READY:
					throw new IOException("Failed to authenticate due to READY");
			}
		}
		catch(IOException e)
		{
			m_SshClient.disconnect();
			throw e;
		}
	}

	/**
	 * Opens a new session channel from this SSH session. This is a low level channel from j2ssh,
	 * and can be used to start a shell or to execute a single command.
	 * Note that with single command execution, the channel is automatically closed after the command is invoked,
	 * i.e. it can not be used to execute further commands.
	 * To execute multiple commands, start a shell (openShell() is a much more convenient way of doing this).
	 * @return The new SCP client representing a new channel.
	 * @throws java.io.IOException
	 */
	public SessionChannelClient openSessionChannel() throws IOException
	{
		return m_SshClient.openSessionChannel();
	}

	/**
	 * Opens a new SCP channel from this SSH session.
	 * Each channel may only be used once, i.e. invoke one command and then must be discarded.
	 * @return The new SCP client representing a new channel.
	 * @throws java.io.IOException
	 */
	public ScpClient openScpChannel() throws IOException
	{
		return m_SshClient.openScpClient();
	}

	/**
	 * Dumps the stats for the SSH client.
	 * @return
	 */
	public String dumpStats()
	{
		StringBuilder ret = new StringBuilder("(").append(m_SshClient.toString());
		ret.append("(NumSftpOpen ").append(m_SftpNumOpen).append(")");
		ret.append("(NumSftpClose ").append(m_SftpNumClose).append(")");
		synchronized(this) {
			ret.append("(CurrSftpOpen ").append(m_SftpCurrOpen).append(")");
		}
		return ret.append(")").toString();
	}

	/**
	 * Opens a new SFTP channel from this SSH session.
	 * @return The new SFTP client representing a new channel.
	 * @throws java.io.IOException
	 */
	public SftpClient openSftpChannel() throws IOException
	{
		synchronized(this)
		{
			++m_SftpNumOpen;
			++m_SftpCurrOpen;
		}
		return m_SshClient.openSftpClient(new ChannelEventAdapter()
		{
			@Override
			public void onChannelClose(Channel arg0)
			{
				super.onChannelClose(arg0);
				synchronized(DjvSshClient.this)
				{
					++m_SftpNumClose;
					--m_SftpCurrOpen;
				}
			}

			@Override
			public void onChannelEOF(Channel arg0)
			{
				super.onChannelEOF(arg0);
			}
		});
	}

	/**
	 * Opens a new SSH shell (on top of a new session channel) from this SSH client.
	 * A shell may be used to invoke multiple commands until being disposed().
	 * @param encoding The SSH shell's encoding scheme, e.g. "UTF-8", ...
	 * @return The new SSH shell.
	 * @throws java.io.IOException
	 */
	public DjvSshShell openShell(String encoding) throws IOException
	{
		return new DjvSshShell(m_SshClient, encoding);
	}
}

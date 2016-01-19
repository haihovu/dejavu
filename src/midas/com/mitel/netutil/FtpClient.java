/*
 * FtpClient.java
 *
 * Created on April 16, 2004, 10:13 PM
 */

package com.mitel.netutil;

import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import com.mitel.netutil.EsmFtpClient.EsmFtpEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;


/**
 *
 * @author  haiv
 */
public class FtpClient
{
	private String m_Server;
	private String m_User;
	private String m_Passwd;
	private EsmFtpClient m_Client;
	private final long timeoutMs;
	
	/** Creates a new instance of FtpClient
	 * @param server The address of the FTP server
	 * @param user The FTP user name
	 * @param pass The FTP password for the aforementioned user
	 * @param timeoutMs The default timeout for all interaction with the server, in milliseconds
	 * @throws FtpException
	 */
	public FtpClient(java.lang.String server, java.lang.String user, java.lang.String pass, long timeoutMs) throws FtpException
	{
		if(MiSystem.diagnosticEnabled())
		{
			MiSystem.logInfo(Category.DESIGN, "Invoked with " + user + "/" + pass + " against " + server + " at " + MiExceptionUtil.getCompressedTrace());
		}

		if((null != server)&&(null != user)&&(null != pass))
		{
			m_Server = server;
			m_User =  user;
			m_Passwd =  pass;
			try
			{
				m_Client = new EsmFtpClient(m_Server, m_User, m_Passwd, timeoutMs);
			}
			catch(Exception e)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
				throw new FtpException(e);
			}
		}
		else
		{
			throw new FtpException("NULL arguments");
		}
		this.timeoutMs = timeoutMs;
	}
	
	public void uploadData(String dir, String file, java.io.InputStream inputData, boolean ascii, long estimatedSize, iFtpListener listener) throws IOException, InterruptedException
	{
		EsmFtpClient client;
		synchronized(this)
		{
			client = m_Client;
		}
		if(client != null)
		{
			client.upload(dir, file, inputData, ascii, estimatedSize, new FtpClient.ListenerConnector(listener));
		}
	}
	
	public void close()
	{
		EsmFtpClient client;
		synchronized(this)
		{
			client = m_Client;
			m_Client = null;
		}
		if(client != null)
		{
			client.ftpLogout();
		}
	}
	
	public void open(java.lang.String server, java.lang.String user, java.lang.String pass, long timeoutMs) throws FtpException
	{
		synchronized(this)
		{
			m_Server = server;
			m_User =  user;
			m_Passwd =  pass;
			try
			{
				m_Client = new EsmFtpClient ( m_Server, m_User, m_Passwd, timeoutMs );
			}
			catch(Exception e)
			{
				throw new FtpException(e);
			}
		}
	}
	
	public void downloadData(String remoteDir, String remoteFile, String localDir, String localFile, long estimatedSize, iFtpListener listener) throws IOException, InterruptedException
	{
		EsmFtpClient client;
		synchronized(this)
		{
			client = m_Client;
		}
		if(client != null)
		{
			client.download(remoteDir, remoteFile, localDir, localFile, false, estimatedSize, new FtpClient.ListenerConnector(listener));
		}
	}
	
	public void downloadData(String remoteDir, String remoteFile, java.io.OutputStream os, long estimatedSize, iFtpListener listener) throws IOException, InterruptedException
	{
		EsmFtpClient client;
		synchronized(this)
		{
			client = m_Client;
		}
		if(client != null)
		{
			client.download(remoteDir, remoteFile, os, false, estimatedSize, new FtpClient.ListenerConnector(listener));
		}
	}
	
	public List<FileInfo> list(java.lang.String dirName)
	{
		EsmFtpClient client;
		synchronized(this)
		{
			client = m_Client;
		}
		if(client != null)
		{
			return extractFilesFromDir(client.list(dirName));
		}
		return new ArrayList<FileInfo>(0);
	}
	
	private List<FileInfo> extractFilesFromDir(String dirList)
	{
		List<FileInfo> retValue = new LinkedList<FileInfo>();
		BufferedReader r = new BufferedReader(new java.io.StringReader(dirList));
		while( true )
		{
			String aLine;
			try
			{
				aLine = r.readLine();
			}
			catch( Exception e )
			{
				break;
			}

			if( aLine != null )
			{
				// Parse the directory listing into file names, excluding subdirs
				Pattern p = Pattern.compile("^[\\-rwx]+\\s+[0-9]+\\s+[a-z_0-9A-Z]+\\s+[a-z_0-9A-Z]+\\s+([0-9]+)\\s+[a-z_0-9A-Z]+\\s+[0-9]+\\s+[0-9:]+\\s+([a-zA-Z_0-9\\.\\-]+)");
				java.util.regex.Matcher m = p.matcher(aLine);
				if(m.find())
				{
					retValue.add(new FileInfo(m.group(2), Long.parseLong(m.group(1))));
				}
			}
			else
			{
				break;
			}
		}
		return retValue;
	}
	
	public void deleteFile(java.lang.String pathName, java.lang.String fileName)
	{
		EsmFtpClient client;
		synchronized(this)
		{
			client = m_Client;
		}
		if(client != null)
		{
			client.deleteFile(pathName, fileName);
		}
	}
	
	public void onDownLoad(EsmFtpEvent evt)
	{
	}
	
	public void onUpLoad(EsmFtpEvent evt)
	{
	}
	
	public static class FtpException extends Exception
	{
		private static final long serialVersionUID = 1L;
		public FtpException(String desc)
		{
			super(desc);
		}
		public FtpException(Throwable cause)
		{
			super(cause);
		}
		public FtpException(String msg, Throwable cause)
		{
			super(msg, cause);
		}
	}
	
	public final class FileInfo
	{
		
		public final String m_fileName;
		
		public final long m_fileSize;
		
		public FileInfo(java.lang.String file, long size)
		{
			m_fileName = file;
			m_fileSize = size;
		}
		
	}
	
	public abstract interface iFtpListener
	{
		
		public void onUpload(FtpEvent evt);
		
		public void onDownload(FtpEvent evt);
		
	}
	
	public static class FtpEvent
	{
		
		public final long m_bytesRead;
		
		public final long m_totalBytes;
		
		public final boolean m_completed;
		
		public FtpEvent(long bytesRead, long totalBytes, boolean completed)
		{
			m_bytesRead = bytesRead;
			m_totalBytes = totalBytes;
			m_completed = completed;
		}
		
		public FtpEvent(EsmFtpEvent evt)
		{
			m_bytesRead = evt.m_bytesSent;
			m_totalBytes = evt.m_totalBytes;
			m_completed = evt.m_completed;
		}
		
	}
	
	private class ListenerConnector implements EsmFtpClient.iEsmFtpListener
	{
		
		private final iFtpListener m_listener;
		
		ListenerConnector(iFtpListener listener)
		{
			m_listener = listener;
		}
		
		@Override
		public void onDownLoad(EsmFtpEvent evt)
		{
			if(null != m_listener)
				m_listener.onDownload(new FtpClient.FtpEvent(evt));
		}
		
		@Override
		public void onUpLoad(EsmFtpEvent evt)
		{
			if(null != m_listener)
				m_listener.onUpload(new FtpClient.FtpEvent(evt));
		}
		
	}
	
}

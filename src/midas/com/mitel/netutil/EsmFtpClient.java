/*******************************************************************************
 *
 * MODULE:       ESM_FTP_Client.java
 *
 * DOCUMENTS:    DKxxxxxx - ESM FTP Client Design Specification
 *
 * DESCRIPTION: This java module contains the implementation of the
 *              Embedded Systems Management (ESM) FTP Client component.
 *
 * HISTORY:
 * 1 - Oct 2 00   - J. Schreuders - Creation
 * 9 - Oc 10 02   - M. Luu - See ipvlan dpar 8518.  Transfer the IPServices.tar
 *                  from ICP 3300 to local pc as a separate piece from the large
 *                  backup file.
 * 10  Oct 23 03  - N. Girgis - Add catching Exceptions because transfer status is 
 *                  true by default, and sometimes behaves as though file was 
 *                  transferred when it failed.
 *     Mar 16 04  - K. Liu - Add Exceptions throw and stop progress bar if fail 
 *     Mar 31 04  - K. Liu - Enchance exception handle on failure 
 *
 ******************************************************************************/
package com.mitel.netutil;

import com.mitel.miutil.MiBackgroundTask;
import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Taken from ESM code base. 
 */
public class EsmFtpClient
{
	/**
	 *   This custom constructor is called when the ESM_FTP_Client class is
	 *   created. To instantiate this class, crucial login information is
	 *   needed in order to establish the FTP connection with the remote server.
	 * 
	 * @param server
	 * @param user
	 * @param pass
	 * @param timeoutMs The default timeout for all interaction with the server, in milliseconds.
	 * @throws java.io.IOException
	 * @throws com.mitel.netutil.EsmFtpClient.EsmFtpException
	 * @throws InterruptedException 
	 */
	public EsmFtpClient(String server, String user, String pass, long timeoutMs) throws IOException, EsmFtpException, InterruptedException
	{
		sFTPServer = server;
		defaultTimeoutMs = timeoutMs;
		ftpConnect(server);
		ftpLogin(user, pass);
	} // ESM_FTP_Client

	/**
	 * This public method will download a file from the FTP server to the
	 * local machine. The parameter "remoteDir" and "remoteFile" specify
	 * the location and name of the desired file on the server. The parameter
	 * "localDir" and "localFile" indicate the location and name of the file
	 * to be created on the local machine. Lastly, the "asc" parameter
	 * specifies the mode of transfer.
	 * @param remoteDir
	 * @param remoteFile 
	 * @param localDir
	 * @param localFileName
	 * @param asc
	 * @param estimatedFileSize 
	 * @param listener
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void download(String remoteDir,
		String remoteFile,
		String localDir,
		String localFileName,
		boolean asc,
		long estimatedFileSize,
		iEsmFtpListener listener)
		throws IOException, InterruptedException
	{
		String localFilePath;
		Pattern p = Pattern.compile("^(.*)[\\\\/]&");
		Matcher m = p.matcher(localDir);
		if(m.find())
		{
			localFilePath = m.group(1);
		}
		else
		{
			localFilePath = localDir;
		}
		localFilePath += (File.separator + localFileName);

		File localFile = new File(localFilePath);
		if(!localFile.exists())
		{
			localFile.createNewFile();
		}
		OutputStream os = new FileOutputStream(localFile);
		try
		{
			download(remoteDir, remoteFile, os, asc, estimatedFileSize, listener);
		}
		finally
		{
			os.close();
		}
	} // download

	public void download(String remoteDir,
		String remoteFile,
		OutputStream os,
		boolean asc,
		long estimatedFileSize,
		iEsmFtpListener listener)
		throws IOException, InterruptedException
	{
		Socket dataSock = ftpGetDataSock();
		try
		{
			InputStream is = dataSock.getInputStream();
			try
			{
				// Set the FTP server to the correct source directory
				ftpSetDir(remoteDir);

				// Set the FTP server to the correct transfer type
				ftpSetTransferType(asc);

				// Download the file from the FTP server
				ftpResp = ftpSendCmd("retr " + remoteFile, defaultTimeoutMs);
				if(!ftpResp.matches("^5.*"))
				{
					int bytesRead;
					byte[] binFileContents = new byte[(int)MaxTransSize];
					long byteCount = 0;
					try
					{
						while(true)
						{
							bytesRead = is.read(binFileContents);
							if(bytesRead == -1)
							{
								break;
							}
							else
							{
								byteCount += bytesRead;
								if(bytesRead > 0)
								{
									os.write(binFileContents, 0, bytesRead);
								}
								if(null != listener)
								{
									listener.onDownLoad(new EsmFtpEvent(EsmFtpEvent.Type.PROGRESS, byteCount, estimatedFileSize, false));
								}
							}
						}
						if(null != listener)
						{
							listener.onDownLoad(new EsmFtpEvent(EsmFtpEvent.Type.COMPLETED, byteCount, byteCount, true));
						}
					}
					catch(IOException e)
					{
						if(null != listener)
						{
							listener.onDownLoad(new EsmFtpEvent(EsmFtpEvent.Type.FAILED, byteCount, estimatedFileSize, true));
						}
						throw e;
					}
					// Indicate that the file transfer was successful
					transferStatus = true;
					ftpResp = ("File download successful");
				}
				else
				{
					throw new IOException(ftpResp);
				}
			}
			finally
			{
				is.close();
			}
		}
		finally
		{
			dataSock.close();
		}
	} // download

	/**
	 *   This public method will upload a file from the local machine to the
	 *   FTP server. The parameter "remoteDir" and "remoteFile" specify the
	 *   location and name of the file to be created on the server. The
	 *   "localDir" and "localFile" parameters indicate the location and name
	 *   of the local file that is to be transferred. Lastly, the "asc"
	 *   parameter specifies the mode of transfer.
	 *
	 * @param remoteDir
	 * @param remoteFile 
	 * @param localDir 
	 * @param listener
	 * @param asc 
	 * @param localFile
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void upload(String remoteDir,
		String remoteFile,
		String localDir,
		String localFile,
		boolean asc,
		iEsmFtpListener listener)
		throws IOException, InterruptedException
	{
		String filePath = (localDir + File.separator + localFile);
		File inputFile = new File(filePath);
		int fileSize = 0;
		InputStream is = new FileInputStream(inputFile);
		try
		{
			byte[] tmp = new byte[1024];
			while(true)
			{
				int bytesRead = is.read(tmp);
				if(bytesRead < 1)
					break;
				fileSize += bytesRead;
			}
		}
		finally
		{
			is.close();
		}
		is = new FileInputStream(inputFile);
		try
		{
			upload(remoteDir, remoteFile, is, asc, fileSize, listener);
		}
		finally
		{
			is.close();
		}
	} // upload

	/**
	 *   This public method will upload a specific string of data to the
	 *   FTP server. The "remoteDir" and "remoteFile" parameters specify the
	 *   location and name of the file to be created on the server. The
	 *   "fileContents" parameter contains the data to be stored on the newly
	 *   created file.
	 * @param remoteDir
	 * @param remoteFile
	 * @param fileContents 
	 * @param listener
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void upload(String remoteDir,
		String remoteFile,
		String fileContents,
		iEsmFtpListener listener)
		throws IOException, InterruptedException
	{
		InputStream is = new ByteArrayInputStream(fileContents.getBytes());
		try
		{
			upload(remoteDir, remoteFile, is, true, fileContents.length(), listener);
		}
		finally
		{
			is.close();
		}
	} // upload

	public void upload(String remoteDir, String remoteFile, InputStream istream, boolean asc, long estimatedSize, iEsmFtpListener listener) throws IOException, InterruptedException
	{
		try
		{
			Socket dataSock = ftpGetDataSock();
			try
			{
				DataOutputStream dos = new DataOutputStream(dataSock.getOutputStream());
				try
				{
					// Set the FTP server to the correct destination directory
					ftpSetDir(remoteDir);
					if(transferStatus == false)
					{
						throw new IOException("Failed to set dir to " + remoteDir);
					}

					ftpSetTransferType(asc);
					if(transferStatus == false)
					{
						throw new IOException("Failed to set transfer type to binary mode");
					}

					// Upload string data to the server
					ftpResp = ftpSendCmd("stor " + remoteFile, defaultTimeoutMs);
					if(transferStatus == false)
					{
						throw new IOException("Failed to send command " + "stor " + remoteFile);
					}

					byte[] buffer = new byte[1024];
					long byteCount = 0;
					try
					{
						while(true)
						{
							int bytesRead = istream.read(buffer);
							byteCount += bytesRead;

							if(bytesRead < 1)
							{
								break;
							}
							
							dos.write(buffer, 0, bytesRead);
							if(null != listener)
							{
								listener.onUpLoad(new EsmFtpEvent(EsmFtpEvent.Type.PROGRESS, byteCount, estimatedSize, false));
							}
						}

						if(null != listener)
						{
							listener.onUpLoad(new EsmFtpEvent(EsmFtpEvent.Type.COMPLETED, byteCount, byteCount, true));
						}
					}
					catch(IOException e)
					{
						if(null != listener)
						{
							listener.onUpLoad(new EsmFtpEvent(EsmFtpEvent.Type.FAILED, byteCount, estimatedSize, true));
						}
						throw e;
					}

					// Indicate that the transfer was successful
					transferStatus = true;
					ftpResp = ("Data upload successful.");

					dos.flush();
				}
				finally
				{
					dos.close();
				}
			}
			finally
			{
				dataSock.close();
			}
		}
		// Exception thrown when network timeout occurs
		catch(SocketException sioe)
		{
			transferStatus = false;
			System.out.println("Exception " + sioe.toString() + " thrown in ESM_FTP_Client.Upload.");
			throw new IOException("Unable to upload file. Connection reset.");
		}
		catch(InterruptedIOException iioe)
		{
			transferStatus = false;
			System.out.println("Exception " + iioe.toString() + " thrown in ESM_FTP_Client.Upload.");
			throw new IOException("Unable to upload file. Write to remote host timeout.");
		}
		catch(IOException ioe)
		{
			// Indicate that the data transfer failed
			transferStatus = false;
			System.out.println("Exception " + ioe.toString() + " thrown in ESM_FTP_Client.Upload.");
			throw new IOException("Unable to upload file due to " + ioe.getMessage());
		}
		catch(RuntimeException e)
		{
			// Indicate that the data transfer failed
			transferStatus = false;
			System.out.println("Exception " + e.toString() + " thrown in ESM_FTP_Client.Upload.");
			throw new IOException("Unable to upload file due to " + e.getMessage());
		}
	} // upload

	/**
	 * @return The FTP response, non-null.
	 */
	public String getFTPResults()
	{
		return (ftpResp != null?ftpResp:"");
	} // getFTPResults

	/**
	 * @return The transfer status.
	 */
	public boolean getTransferStatus()
	{
		return transferStatus;
	} // getTransferStatus

	/**
	 *   This API function will create a connection with the remote FTP server.
	 * @param server
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void ftpConnect(String server) throws IOException, InterruptedException
	{
		synchronized(this)
		{
			connSock = new Socket(server, CNTRL_PORT);
			connSock.setSoTimeout(60000);

			// Open control streams
			controlReader = new BufferedReader(new InputStreamReader(connSock.getInputStream()));

			// Set the autoflush to "true"
			controlWriter = new PrintWriter(connSock.getOutputStream(), true);

			bgTaskCmdReader = new BgTaskCmdReader();
			bgTaskCmdReader.start();
		}

		// Check if the FTP server is alive
		String numerals = responseHandler(null, defaultTimeoutMs);

		if((numerals.length() >= 3)&&(numerals.substring(0, 3).equals("220")))
		{
			ftpResp = "Successfully connected to the server.";
			if(MiSystem.diagnosticEnabled())
			{
				MiSystem.logInfo(Category.DESIGN, "Successfully connected to the server.");
			}
		}
		else
		{
			ftpResp = ("Unable to connect to server " + server);
			if(MiSystem.diagnosticEnabled())
			{
				MiSystem.logInfo(Category.DESIGN, "Error connecting to the FTP server!");
			}
		}
	} // ftpConnect

	/**
	 *   This API function will return the current data socket with the FTP
	 *   server.
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private Socket ftpGetDataSock() throws IOException, InterruptedException
	{
		String reply = ftpSendCmd("pasv ", defaultTimeoutMs);
		Socket dataSock = null;
		try
		{
			// New technique: just find numbers before and after ","!
			StringTokenizer st = new StringTokenizer(reply, ",");
			String[] parts = new String[6]; // parts, incl. some garbage
			int i = 0; // put tokens into String array
			while(st.hasMoreElements())
			{
				// stick pieces of host, port in String array
				parts[i++] = st.nextToken();
				if(i >= parts.length)
				{
					break;
				}
			} // end getting parts of host, port

			// Get rid of everything before first "," except digits
			String[] possNum = new String[3];
			for(int j = 0; j < possNum.length; j++)
			{
				// Get 3 characters, inverse order, check if digit/character
				possNum[j] = parts[0].substring(parts[0].length() - (j + 1),
					parts[0].length() - j); // next: digit or character?

				if(!Character.isDigit(possNum[j].charAt(0)))
				{
					possNum[j] = "";
				}
			}
			parts[0] = possNum[2] + possNum[1] + possNum[0];
			// Get only the digits after the last ","
			String[] porties = new String[3];
			for(int k = 0; k < 3; k++)
			{
				// Get 3 characters, in order, check if digit/character
				// May be less than 3 characters
				if((k + 1) <= parts[5].length())
				{
					porties[k] = parts[5].substring(k, k + 1);
				}
				else
				{
					porties[k] = "FOOBAR"; // definitely not a digit!
				}			// next: digit or character?
				if(!Character.isDigit(porties[k].charAt(0)))
				{
					porties[k] = "";
				}
			} // Have to do this one in order, not inverse order
			parts[5] = porties[0] + porties[1] + porties[2];
			// Get dotted quad IP number first
			String ip = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];

			// Determine port
			int port;
			
			// Get first part of port, shift by 8 bits.
			int big = Integer.parseInt(parts[4]) << 8;
			int small = Integer.parseInt(parts[5]);
			port = big + small; // port number
				
			/* Commented out as part of security fixes made in OAM#6657
			if((ip != null) && (port != -1))

			dataSock = new Socket(ip, port);
			else throw new IOException();*/
			// Added as part of OAM#6657
			if((sFTPServer != null) && (port != -1))
			{
				dataSock = new Socket(sFTPServer, port);
			}
			else
			{
				throw new IOException();
			}
		}
		catch(RuntimeException e)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
		return dataSock;
	} // ftpGetDataSock

	/**
	 *   This API function will perform a login into the FTP server.
	 * @param user
	 * @param pass
	 * @throws EsmFtpException
	 */
	private void ftpLogin(String user, String pass) throws EsmFtpException
	{
		try
		{
			ftpSendCmd("user " + user, defaultTimeoutMs);
			String response = ftpSendCmd("pass " + pass, defaultTimeoutMs);

			if(response.matches("5.*"))
			{
				// Indicate that the transfer failed
				transferStatus = false;
				throw new EsmFtpException(response);
			}
		}
		catch(RuntimeException e)
		{
			// Indicate that the transfer failed
			transferStatus = false;
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
			throw new EsmFtpException(e.toString());
		}
		catch(IOException ex) {
			// Indicate that the transfer failed
			transferStatus = false;
			MiSystem.logWarning(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
			throw new EsmFtpException(ex.toString());
		}
		catch(InterruptedException ex) {
			// Indicate that the transfer failed
			transferStatus = false;
			throw new EsmFtpException("Interrupted");
		}
	} // ftpLogin

	/*****************************************************************************
	 * METHOD: ftpLogout
	 *
	 * DESCRIPTION:
	 *   This API function will perform a logout from the FTP server.
	 *
	 * HISTORY:
	 *   1.0 - Oct 00 - John Schreuders - Creation.
	 *   9 - Oc 10 02 - M. Luu - See ipvlan dpar 8518.  Make this function public.
	 *
	 *****************************************************************************/
	public void ftpLogout()
	{
		synchronized(this)
		{
			try
			{
				// Log off the FTP server and shutdown all streams and connections.
				if(controlWriter != null)
				{
					controlWriter.print("bye " + "\r\n");
					controlWriter.flush();
					controlWriter.close();
					controlWriter = null;
				}
				
				if(controlReader != null)
				{
					controlReader.close();
				}
				
				if(connSock != null)
				{
					connSock.close();
					connSock = null;
				}
				
				if(bgTaskCmdReader != null)
				{
					bgTaskCmdReader.stop();
				}
			}
			catch(IOException ioe)
			{
				// Indicate that the transfer failed
				transferStatus = false;
				ftpResp = ("Exception " + ioe.toString() + " thown in ESM_FTP_Client.ftpLogout.");
			}
			catch(Exception e)
			{
				// Indicate that the transfer failed
				transferStatus = false;
				ftpResp = ("Exception " + e.toString() + " thown in ESM_FTP_Client.ftpLogout.");
			}
		}
	} // ftpLogout

	/**
	 *   This API function will send a specific FTP command to the server. The
	 *   command sent to the server corresponds to FTP standard RFC 959.
	 * @param cmd
	 * @param timeoutMs
	 * @return The response, may be empty but not null.
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private String ftpSendCmd(String cmd, long timeoutMs) throws IOException, InterruptedException
	{
		// Check if the FTP server is still processing a previous command.
		BgTaskCmdReader cmdReader;
		PrintWriter cmdWriter;
		boolean busy;
		synchronized(this)
		{
			cmdReader = bgTaskCmdReader;
			busy = serverBusy;
			serverBusy = false;
			cmdWriter = controlWriter;
		}

		if(busy)
		{
			if(cmdReader != null)
			{
				// @todo Potential indefinite block
				String discard = cmdReader.getNextLine(timeoutMs);
				while(discard != null)
				{
					if(discard.length() > 0)
					{
						char numeric = discard.charAt(0);
						switch(numeric)
						{
							case '2':
								busy = false;
								break;
						}
					}
					if(MiSystem.diagnosticEnabled())
					{
						MiSystem.logInfo(Category.DESIGN, "Keeping the handler in sync" +
							" by discarding the next response: " + discard);
					}

					if(!busy)
						break;

					discard = cmdReader.getNextLine(timeoutMs);
				}
			}
		}

		if(cmdWriter != null)
		{
			cmdWriter.print(cmd + "\r\n");
			cmdWriter.flush();
			return responseHandler(cmd, timeoutMs);
		}

		return "";
	} // ftpSendCmd

	/**
	 *   This API function will change the current working directory on the FTP
	 *   server.
	 * @param dir
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private String ftpSetDir(String dir)
		throws IOException, InterruptedException
	{
		ftpResp = ftpSendCmd("cwd " + dir, defaultTimeoutMs);
		return ftpResp;
	} // ftpSetDir

	/**
	 *   This API function will change the mode of data transfer on the FTP
	 *   server.
	 * @param asc
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void ftpSetTransferType(boolean asc) throws IOException, InterruptedException
	{
		ftpResp = ftpSendCmd("type " + (asc ? "a" : "i"), defaultTimeoutMs);
	} // ftpSetTransferType

	/**
	 *   This API function will return the FTP server's response for a
	 *   specific command.
	 * @param cmd
	 * @param timeoutMs
	 * @return The response, may be empty but not null
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private String responseHandler(String cmd, long timeoutMs) throws IOException, InterruptedException
	{
		BgTaskCmdReader cmdReader;
		synchronized(this)
		{
			cmdReader = bgTaskCmdReader;
		}
		if(null != cmdReader)
		{
			String reply = this.responseParser(cmdReader.getNextLine(timeoutMs));
			if((null != reply)&&(reply.length() > 3))
			{
				String numerals = reply.substring(0, 3);
				String hyph_test = reply.substring(3, 4);
				String next = null;

				if(hyph_test.equals("-"))
				{
					String tester = numerals + " ";
					boolean done = false;
					while(!done)
					{
						next = cmdReader.getNextLine(timeoutMs);
						if(next == null)
						{
							MiSystem.logWarning(Category.DESIGN, "End of stream detected, exit");
							break;
						}
						
						// Read over blank lines
						while(next.isEmpty() || next.equals("  "))
						{
							next = cmdReader.getNextLine(timeoutMs);
							if(next == null)
								break;
						}

						if(next == null)
						{
							MiSystem.logWarning(Category.DESIGN, "End of stream detected, exit");
							break;
						}
						
						// If next starts with "tester" then we are done
						if((next.length() > 3)&&next.substring(0, 4).equals(tester))
						{
							done = true;
						}
						else
						{
							MiSystem.logInfo(Category.DESIGN, "Skipping " + next);
						}
					}

					if(MiSystem.diagnosticEnabled())
					{
						if(cmd != null)
						{
							MiSystem.logInfo(Category.DESIGN, "Response to: " + cmd + " was: " + next);
						}
						else
						{
							MiSystem.logInfo(Category.DESIGN, "Response was: " + next);
						}
					}
					return next != null?next:"";
				}
				else // hyph-test does not equal "-"
				{
					if(MiSystem.diagnosticEnabled())
					{
						if(cmd != null)
						{
							MiSystem.logInfo(Category.DESIGN, "Response to: " + cmd + " was: " + reply);
						}
						else
						{
							MiSystem.logInfo(Category.DESIGN, "Response was: " + reply);
						}
					}
					return reply;
				}
			}
			else if((reply != null)&&(reply.length() < 4))
			{
				MiSystem.logWarning(Category.DESIGN, "Invalid reply " + reply);
			}
		}
		else
		{
			MiSystem.logError(Category.DESIGN, "Command reader is not set");
		}
		return "";
	} // responseHandler

	/**
	 *   This API function will parse the response returned from the FTP
	 *   server for a specific command.
	 * @param resp
	 * @return The parsed response or null if errors are encountered.
	 */
	private String responseParser(String resp)
	{
		if((null != resp)&&(resp.length() > 0))
		{
			String firstDigit = resp.substring(0, 1);
			if(firstDigit.equals("1"))
			{
				if(MiSystem.diagnosticEnabled())
				{
					MiSystem.logInfo(Category.DESIGN,
						"FTP Server is busy with previous command at " + MiExceptionUtil.getCompressedTrace());
				}
				// Indicate that the FTP server is currently busy
				serverBusy = true;
				return resp;
			}
			else if(firstDigit.equals("2"))
			{
				if(MiSystem.diagnosticEnabled())
				{
					MiSystem.logInfo(Category.DESIGN,
						"FTP Server is finished with command at " + MiExceptionUtil.getCompressedTrace());
				}
				// Indicate that the FTP server is no longer busy
				serverBusy = false;
				return resp;
			}
			else if(firstDigit.equals("3") || firstDigit.equals("4") || firstDigit.equals("5"))
			{
				if(MiSystem.diagnosticEnabled())
				{
					MiSystem.logInfo(Category.DESIGN, "In 3-4-5 handler.");
				}
				// Flag that this FTP operation has failed
				if(firstDigit.equals("4") || firstDigit.equals("5"))
				{
					transferStatus = false;
				}
				return resp;
			}
			else
			{
				// An unhandled response was returned from the server
				MiSystem.logWarning(Category.DESIGN, "Unhandled response " + resp);
				return null;
			}
		}
		return null;
	}

	public String list(String dirName)
	{
		StringBuilder retValue = new StringBuilder(1024);
		try
		{
			String cmdResponse;
			Socket dataSock = ftpGetDataSock();
			try
			{
				// Set the socket timeout for 60 seconds
				dataSock.setSoTimeout(60000);
				BufferedReader is = new BufferedReader(new InputStreamReader(dataSock.getInputStream()));
				try
				{
					// Set the FTP server to the correct source directory
					cmdResponse = ftpSetDir(dirName);
					if(cmdResponse.matches("501.*")) // Check for failure
					{
						return retValue.toString();
					}

					// Set the FTP server to the correct transfer type
					ftpSetTransferType(true);

					// Download the file from the FTP server
					ftpSendCmd("list", defaultTimeoutMs);
					while(true)
					{
						String line = is.readLine();
						if(null != line)
						{
							retValue.append("\n").append(line);
						}
						else
						{
							break;
						}
					}
				} finally {
					is.close();
				}
			} finally {
				dataSock.close();
			}
		} catch (InterruptedException ex) {
		} catch(IOException ex) {
			MiSystem.logWarning(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		} catch(RuntimeException e)	{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
		return retValue.toString();
	}

	public void deleteFile(String pathName, String fileName)
	{
		try
		{
			// Set the FTP server to the correct destination directory
			ftpSetDir(pathName);
			if(transferStatus == false)
			{
				System.out.println("ERROR: " + getClass().getName() + ".deleteFile() - Failed to set dir to " + pathName);
				return;
			}

			// Upload string data to the server
			ftpResp = ftpSendCmd("dele " + fileName, defaultTimeoutMs);
			if(transferStatus == false)
			{
				System.out.println("ERROR: " + getClass().getName() + ".deleteFile() - Failed to send delete command");
			}
			else if((ftpResp != null) && (!ftpResp.matches("^2.*")))
			{
				System.out.println("WARNING: " + getClass().getName() + ".deleteFile() - Server returns " + ftpResp);
			}
		}
		// Exception thrown when network timeout occurs
		catch(RuntimeException e)
		{
			// Indicate that the data transfer failed
			transferStatus = false;
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
		catch(IOException ex) {
			// Indicate that the data transfer failed
			transferStatus = false;
			MiSystem.logWarning(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
		catch(InterruptedException ex) {
			// Indicate that the data transfer failed
			transferStatus = false;
		}
	}

	public static class EsmFtpException extends Exception
	{
		private static final long serialVersionUID = 1L;
		public EsmFtpException(String desc)
		{
			super(desc);
		}
	}

	public abstract interface iEsmFtpListener
	{
		public void onDownLoad(EsmFtpClient.EsmFtpEvent evt);

		public void onUpLoad(EsmFtpClient.EsmFtpEvent evt);
	}

	public static class EsmFtpEvent
	{
		public static enum Type
		{
			PROGRESS,
			COMPLETED,
			FAILED
		}
		public final Type m_type;
		public final long m_bytesSent;
		public final boolean m_completed;
		public final long m_totalBytes;

		public EsmFtpEvent(Type type, long bytesSent, long totalBytes, boolean completed)
		{
			m_type = type;
			m_bytesSent = bytesSent;
			m_completed = completed;
			m_totalBytes = totalBytes;
		}
	}
	// responseParser

//************************** PRIVATE ATTRIBUTES *******************************
	private static final int CNTRL_PORT = 21;
	private Socket connSock = null;
	private BufferedReader controlReader;
	private PrintWriter controlWriter;
	private String ftpResp = "";
	private boolean serverBusy = false;
	private boolean transferStatus = true;
	private static final long MaxTransSize = 2097152;
	private final String sFTPServer;
	private final long defaultTimeoutMs;

	private BgTaskCmdReader bgTaskCmdReader;
	
	private class BgTaskCmdReader extends MiBackgroundTask
	{
		private final LinkedList<String> lineBuf = new LinkedList<String>();
		BgTaskCmdReader()
		{
			super("FtpCmdReader");
		}

		String getNextLine(long timeoutMs) throws InterruptedException
		{
			synchronized(lineBuf)
			{
				if((lineBuf.isEmpty())&&(timeoutMs > 0))
				{
					lineBuf.wait(timeoutMs);
				}
				if(!lineBuf.isEmpty())
				{
					String ret = lineBuf.removeFirst();
					if(MiSystem.diagnosticEnabled())
					{
						MiSystem.logInfo(Category.DESIGN, "Got " + ret + " at " + MiExceptionUtil.getCompressedTrace());
					}
					return ret;
				}
			}
			if(MiSystem.diagnosticEnabled())
			{
				MiSystem.logInfo(Category.DESIGN, "Got nothing at " + MiExceptionUtil.getCompressedTrace());
			}
			return null;
		}
		
		@Override
		public void run()
		{
			MiSystem.logInfo(Category.DESIGN, Thread.currentThread() + " started");
			try
			{
				BufferedReader reader;
				synchronized(EsmFtpClient.this)
				{
					reader = controlReader;
				}

				if(reader == null)
				{
						MiSystem.logWarning(Category.DESIGN, "Reader not set, exit");
					return;
				}
				
				while(getRunFlag())
				{
					String line = reader.readLine();
					
					if(line == null)
					{
						MiSystem.logInfo(Category.DESIGN, "End of stream detected, extit");
						break;
					}
					
					synchronized(lineBuf)
					{
						lineBuf.add(line);
						lineBuf.notify();
					}
				}
			}
			catch(IOException ex)
			{
			}
			catch(RuntimeException ex)
			{
			}
			finally
			{
				synchronized(EsmFtpClient.this)
				{
					if(bgTaskCmdReader == this)
					{
						bgTaskCmdReader = null;
					}
				}
				MiSystem.logInfo(Category.DESIGN, Thread.currentThread() + " end");
			}
		}
	}
} // ESM_FTP_Client

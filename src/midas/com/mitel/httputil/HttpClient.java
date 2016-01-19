package com.mitel.httputil;

import com.mitel.miutil.MiTimer;
import java.io.IOException;


/*
 * httpclient.java
 *
 * Created on August 3, 2003, 2:07 PM
 */

/**
 *
 * @author  haiv
 */
public class HttpClient extends Thread 
{
    protected HttpServerConnector m_Server;
    protected String m_HttpCommand;
	protected int m_CmdId;
    protected String m_HttpResponse = null;
    protected HttpListener m_Listener = null;
    protected int m_Priority = 1;    
    protected boolean m_Running = true;
    protected int m_ServerPort;
    protected String m_ServerName;
    protected boolean m_Secured = false;
    protected boolean m_bHttp11 = false;
    
    protected boolean m_bSendCommand = true;
    
    /** Creates a new instance of httpclient */
    public HttpClient(java.lang.String ServerName, java.lang.String HttpCommand, int cmdId, boolean secured, boolean http11, boolean sendCmd, HttpListener aListener)  
    {
		if( secured )
		{
	        m_ServerPort = 443;
		}
		else
		{
	        m_ServerPort = 80;
		}
        m_ServerName = ServerName;
        m_HttpCommand = HttpCommand;
        m_Listener = aListener;
		m_CmdId = cmdId;
		m_Priority = Thread. currentThread(). getPriority() - 1;
        m_Secured = secured;
        m_bHttp11 = http11;
        m_bSendCommand = sendCmd; /* if false only make connection without sending anything */
    }
    
    public HttpClient(java.lang.String ServerName, int ServerPort, java.lang.String HttpCommand, int cmdId, boolean secured, boolean http11, boolean sendCmd, HttpListener aListener)  
	{
        m_ServerPort = ServerPort;
        m_ServerName = ServerName;
        m_HttpCommand = HttpCommand;
        m_Listener = aListener;
		m_CmdId = cmdId;
		m_Priority = Thread. currentThread(). getPriority() - 1;
        m_Secured = secured;
        m_bHttp11 = http11;
        m_bSendCommand = sendCmd; /* if false only make connection without sending anything */
    }
	
    public void run()
	{
        setPriority( m_Priority );
        mainLoop();
	}
    
    protected synchronized void mainLoop()
    {
        try
        {
            if( !m_Secured )
            {
                m_Server = new HttpServerConnectorImpl ( m_ServerName, m_ServerPort );
            }
            else
            {
                m_Server = new HttpServerConnectorImplSsl( m_ServerName, m_ServerPort );
            }
            m_Running = true;
            if(( m_bSendCommand )&&( null != m_HttpCommand ))
            {
                try
                {
                    if( sendCmd( m_HttpCommand ))
					{
						m_HttpResponse = receiveData();
						if( null != m_Listener )
						{
							m_Listener. httpResponse( m_HttpResponse, m_CmdId );
						}
					}
					else
					{
						if( null != m_Listener )
						{
							m_Listener. httpError( "Failed to send HTTP command to web server", m_CmdId );
						}
					}
                }
                catch( IOException e )
                {
                    if( null != m_Listener )
                    {
                        m_Listener. httpError( e. toString(), m_CmdId );
                    }
                }
            } //if(( m_bSendCommand )&&( null != m_HttpCommand ))
			else
			{
				if( null != m_Listener )
				{
					m_Listener. httpResponse( "(Done)", m_CmdId );
				}
			}
        }
        catch( java.lang.Exception e )
        {
            if( null != m_Listener )
            {
                m_Listener. httpError( e.toString(), m_CmdId );
            }        
        }

        if( !m_bHttp11 )
        {
            close();
        }
        notifyAll();
        m_Running = false;
    }
    
    protected boolean sendCmd(java.lang.String HttpString) throws IOException
	{
		if( null != m_Server )
		{
            if( null != HttpString )
            {
				MiTimer timer = new MiTimer(15000); // Fifteen seconds timeout
				timer. start();
                m_Server. writeHttpCommand( HttpString );
				timer.stop();
				return true;
            }
		}
		return false;
    }
    
    protected String receiveData() throws IOException
	{
        String returnValue = "";
 		MiTimer timer = new MiTimer(5000); // Fifteen seconds timeout
		timer. start();

		if( null != m_Server )
		{
            returnValue = m_Server. readHttpData();
		}
		else
		{
			returnValue = "No Server";
		}
		timer. stop();
        return returnValue;
    }
    
	/** Close HTTP socket */	
	public void close() 
	{
		if( m_Server != null )
		{
            m_Server. close();
			m_Server = null;
		}
	}
	
    public synchronized void waitForCompletion() throws java.lang.InterruptedException 
	{
        // In case clients invoke this too early
        if( m_Running )
        {
			wait();
        }
    }
}

/*
 * httpserversock.java
 *
 * Created on August 3, 2003, 2:10 PM
 */

package com.mitel.httputil;

import java.io.*;
import java.net.*;

/**
 *
 * @author  haiv
 */
class HttpServerConnectorImpl implements HttpServerConnector {
    
    /** Socket to the HTTP server */    
    public Socket m_Socket;
    
    /** Writer to send requests to the HTTP server */    
    public Writer m_Writer;
    /** Reader to get data from the HTTP server */    
    public BufferedReader m_Reader;
   
    /** Creates a new instance of httpserversock */
    public HttpServerConnectorImpl(java.lang.String ServerName, int ServerHttpPort) throws IOException {
		try
		{
			m_Socket = new Socket();
			m_Socket. connect( new InetSocketAddress( ServerName, ServerHttpPort ), 30000 );
			m_Writer = new PrintWriter(new OutputStreamWriter(m_Socket.getOutputStream()));
			m_Reader = new BufferedReader(new InputStreamReader(m_Socket.getInputStream()));
		}
		catch( UnknownHostException e )
		{
			System.err.println( "ERROR: " + getClass().getName() + " failed to create socket due to " + e );
			throw e;
		}
		catch( IOException e )
		{
			System.err.println( "ERROR: " + getClass().getName() + " failed to create socket due to " + e );
			throw e;
		}
    }
    
	/** Only served as default constructor for sub-classes to invoke. */	
    public HttpServerConnectorImpl() {
    }
    
    public String readHttpData() throws java.io.IOException
	{
        StringBuilder returnValue = new StringBuilder(4096);
        String tmp;
        int waitCount = 10;
        
        while(( tmp =  m_Reader. readLine()) != null )
        {
            returnValue.append(tmp);
        }
        return returnValue.toString();
    }
    
    public void writeHttpCommand(java.lang.String data) throws java.io.IOException 
    {
		if( null != m_Writer )
		{
	        m_Writer. write( data );
	        m_Writer. flush();
		}
    }
    
    public void close() {
        try
        {
			if( m_Reader != null )
			{
	            m_Reader. close();
			}
			if( m_Writer != null )
			{
	            m_Writer. close();
			}
			if( m_Socket != null )
			{
	            m_Socket. close();
			}
			m_Reader = null;
			m_Writer = null;
			m_Socket = null;
        }
        catch( java.io.IOException e )
        {
            System.err.println( "ERROR: " + getClass().getName() + " server close failed due to: " + e );
        }
    }
    
}

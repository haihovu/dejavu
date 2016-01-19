/*
 * httpserversock.java
 *
 * Created on August 3, 2003, 2:10 PM
 */

package com.mitel.httputil;

import com.mitel.netutil.MiSocketSslFactory;
import java.io.*;
import javax.net.ssl.SSLSocket;

/**
 *
 * @author  haiv
 */
class HttpServerConnectorImplSsl extends HttpServerConnectorImpl {
    
    /** Creates a new instance of httpserver_connector_secured
	 * @param ServerName
	 * @param ServerHttpPort
	 * @throws java.io.IOException */
    HttpServerConnectorImplSsl(java.lang.String ServerName, int ServerHttpPort) throws IOException
    {
        super();
		
		SSLSocket sock = (SSLSocket)MiSocketSslFactory.getInstance().getClientSocketFactory().createSocket(ServerName, ServerHttpPort);
		try {
			sock.setKeepAlive(true);
			sock.setSoTimeout(30000);

			// Handshake
			sock.startHandshake();

			m_Socket = sock;

			m_Writer = new BufferedWriter(new OutputStreamWriter(m_Socket.getOutputStream()));
			m_Reader = new BufferedReader(new InputStreamReader(m_Socket.getInputStream()));
		} catch(IOException e) {
			sock.close();
			throw e;
		}
    }

	/** @link dependency */
    /*# httpdiagtrust lnkhttpdiagtrust; */    
}

/*
 * iHttpServerConnector.java
 *
 * Created on October 23, 2003, 2:37 PM
 */

package com.mitel.httputil;

/**
 *
 * @author  Hai Vu
 */
public interface HttpServerConnector 
{
    
    public String readHttpData() throws java.io.IOException;
    
    public void writeHttpCommand(java.lang.String data) throws java.io.IOException;
    
    public void close();
    
}

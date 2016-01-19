/*
 * iHttpListener.java
 *
 * Created on October 16, 2003, 11:07 PM
 */

package com.mitel.httputil;

/**
 *
 * @author  Administrator
 */
public interface HttpListener
{
    public void httpResponse( String httpData, int cmdId );
    
    public void httpError( java.lang.String errorDescription, int cmdId );
    
}

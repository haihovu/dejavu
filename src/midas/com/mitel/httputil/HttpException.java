/*
 * httpdiagexception.java
 *
 * Created on November 1, 2003, 9:47 PM
 */

package com.mitel.httputil;

import java.lang.Exception;

/**
 *
 * @author  Hai Vu
 */
public class HttpException extends Exception {
    
    /** Creates a new instance of httpdiagexception */
    public HttpException(String exceptionDesc) {
        super(exceptionDesc);
    }
    
}

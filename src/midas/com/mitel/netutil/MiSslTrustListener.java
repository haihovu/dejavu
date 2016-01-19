/*
 * MiSslTrustListener.java
 *
 * Created on December 4, 2006, 8:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.mitel.netutil;

import java.security.cert.X509Certificate;

/**
 * Asynchronous listener interface for handling trust negotiation events.
 * @author haiv
 */
public interface MiSslTrustListener
{
	/**
	 * Handles a certificate chain.
	 * @param par1 The certificate chain to be examined.
	 * @return True if the given certificate chain is trusted, or false otherwise.
	 */
	boolean challengeTrust(X509Certificate[] par1);
}

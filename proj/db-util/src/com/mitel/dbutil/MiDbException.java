/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mitel.dbutil;

/**
 * Customize exception for MiDAS dbutil logic.
 */
public class MiDbException extends Exception
{
	private static final long serialVersionUID = 1L;
	/**
	 * Creates an empty exception.
	 */
	public MiDbException()
	{
		super();
	}
	/**
	 * Creates an exception with a single message.
	 * @param msg The message describing the exception
	 */
	public MiDbException(String msg)
	{
		super(msg);
	}
	/**
	 * Creates an exception with a root cause.
	 * @param cause The root cause of this exception 
	 */
	public MiDbException(Throwable cause)
	{
		super(cause);
	}
	/**
	 * Creates an exception with a message, and a root cause.
	 * @param msg The message describing the exception
	 * @param cause The root cause of this exception 
	 */
	public MiDbException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}

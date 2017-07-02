/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.dbutil;

/**
 *
 * @author hai
 */
public class DbException extends Exception {
	public DbException() {
		super();
	}
	public DbException(String msg) {
		super(msg);
	}
	public DbException(Throwable cause) {
		super(cause);
	}
	public DbException(String msg, Throwable cause) {
		super(msg, cause);
	}
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.dbutil;

import java.net.URL;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

/**
 *
 * @author hai
 */
public class DbSession {
	private static SessionFactory sessionFactory;
	private final Session session;
	private final Transaction transaction;
	private final Transaction ownTransaction;
	private boolean done;
	public DbSession() throws DbException {
		super();
		SessionFactory fact = getSessionFactory();
		if(fact != null) {
			session = fact.openSession();
			transaction = session.beginTransaction();
			ownTransaction = transaction;
		} else {
			throw new DbException("DB not yet initialised.");
		}
	}
	public DbSession(DbSession copy) throws DbException {
		super();
		SessionFactory sfact = getSessionFactory();
		if(sfact == null) {
			throw new DbException("DB not yet initialised");
		}
		session = copy.session != null ? copy.session : sfact.openSession();
		transaction = copy.session != null ? copy.session.getTransaction() : session.beginTransaction();
		ownTransaction= copy.session != null ? null : transaction;
	}
	
	public Session getSession() {
		return session;
	}
	
	public DbSession commit() {
		if(ownTransaction != null) {
			synchronized(this) {
				if(!done) {
					done = true;
					ownTransaction.commit();
				}
			}
		}
		return this;
	}
	
	public DbSession rollback() {
		if(ownTransaction != null) {
			synchronized(this) {
				if(!done) {
					done = true;
					ownTransaction.rollback();
				}
			}
		}
		return this;
	}
	
	public DbSession dispose() {
		if(ownTransaction != null) {
			synchronized(this) {
				if(!done) {
					done = true;
					ownTransaction.rollback();
				}
			}
		}
		return this;
	}
	private SessionFactory getSessionFactory() {
		synchronized(DbSession.class) {
			return sessionFactory;
		}
	}
	public static void init(URL configFile) {
		synchronized(DbSession.class) {
			sessionFactory = new Configuration().configure(configFile).buildSessionFactory(new StandardServiceRegistryBuilder().build());
		}
	}
}

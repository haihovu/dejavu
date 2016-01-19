package com.mitel.dbutil;

import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;

/**
 * Wrapper to help manage locally created database session/transaction (1-1
 * relationship). If there is an existing Session/Transaction then no new
 * transaction will be created, this wrapper is basically a no-op dummy. But if
 * there is no existing Session/Transaction then the a new session/transaction
 * will be created. Typical usage:
 * <pre>
 * try
 * {
 *     MiDbSession newSession = new MiDbSession(session);
 *     try
 *     {
 *         // Do some work here
 *         ...
 *         newSession.commit();
 *     }
 *     finally
 *     {
 *         newSession.dispose();
 *     }
 * }
 * catch(RuntimeException e)
 * {
 *     // Handle this
 * }
 *
 * </pre>
 *
 * @author haiv
 */
public class MiDbSession
{

	/**
	 * Represents the session/transaction that this wrapper starts, if required.
	 * If an existing session/transaction can be located then this shall be
	 * null.
	 */
	private final Transaction localTransaction;
	private final Session externSession;
	private final Session localSession;
	private boolean commited;
	private boolean closed;

	/**
	 * Creates a new session wrapper. Starts a new session if an existing can
	 * not be located.
	 *
	 * @param session The optional session to be wrapped. Null means no session
	 * exists, try to use the <i>current session</i>.
	 * @throws MiDbException If some failures are encountered, typically
	 * Hibernate was not configured properly.
	 */
	MiDbSession(Session session) throws MiDbException
	{
		this(session, null);
	}

	/**
	 * Creates a new session wrapper. Starts a new session if an existing can
	 * not be located.
	 *
	 * @param session The optional session to be wrapped. Null means no session
	 * exists, try to use the <i>current session</i>.
	 * @param interceptor An optional interceptor for handling database events
	 * @throws MiDbException If some failures are encountered, typically
	 * Hibernate was not configured properly.
	 */
	MiDbSession(Session session, Interceptor interceptor) throws MiDbException
	{
		try
		{
			externSession = session;
			if(interceptor != null) {
				localSession = session != null ? null : getSessionFactory().openSession(interceptor);
			} else {
				localSession = session != null ? null : getSessionFactory().openSession();
			}
			localTransaction = localSession != null ? localSession.beginTransaction() : null;
			if(localSession != null)
			{
				synchronized(gLock)
				{
					gOutstandingSession.add(localSession);
				}
			}
		}
		catch(HibernateException e)
		{
			throw new MiDbException("Failed to create a new Ubiquity database session", e);
		}
	}

	/**
	 * Creates a new session wrapper based on an optional existing session.
	 * Starts a new session if an existing is not provided.
	 *
	 * @param session The optional session to be wrapped. Null means no session
	 * exists, create a new one.
	 * @throws MiDbException If some failures are encountered, typically
	 * Hibernate was not configured properly.
	 */
	public MiDbSession(MiDbSession session) throws MiDbException
	{
		this(session, null);
	}

	/**
	 * Creates a new session wrapper based on an optional existing session.
	 * Starts a new session if an existing is not provided.
	 *
	 * @param session The optional session to be wrapped. Null means no session
	 * exists, create a new one.
	 * @param interceptor An optional interceptor for handling database events
	 * @throws MiDbException If some failures are encountered, typically
	 * Hibernate was not configured properly.
	 */
	public MiDbSession(MiDbSession session, Interceptor interceptor) throws MiDbException
	{
		try
		{
			externSession = session != null ? session.getSession() : null;
			if(interceptor != null) {
				localSession = session != null ? null : getSessionFactory().openSession(interceptor);
			} else {
				localSession = session != null ? null : getSessionFactory().openSession();
			}
			localTransaction = localSession != null ? localSession.beginTransaction() : null;
			if(localSession != null)
			{
				synchronized(gLock)
				{
					gOutstandingSession.add(localSession);
				}
			}
		}
		catch(HibernateException e)
		{
			throw new MiDbException("Failed to create a new Ubiquity database session", e);
		}
	}

	/**
	 * Creates a session wrapper with a brand spanking new session.
	 * @throws MiDbException If some failures are encountered, typically
	 * Hibernate was not configured properly.
	 */
	MiDbSession() throws MiDbException
	{
		this((Interceptor)null);
	}

	/**
	 * Creates a session wrapper with a brand spanking new session.
	 * @param interceptor An optional interceptor for handling database events
	 * @throws MiDbException If some failures are encountered, typically
	 * Hibernate was not configured properly.
	 */
	MiDbSession(Interceptor interceptor) throws MiDbException
	{
		try
		{
			externSession = null;
			if(interceptor != null) {
				localSession = getSessionFactory().openSession(interceptor);
			} else {
				localSession = getSessionFactory().openSession();
			}
			localTransaction = localSession.beginTransaction();

			synchronized(gLock)
			{
				gOutstandingSession.add(localSession);
			}
		}
		catch(HibernateException e)
		{
			throw new MiDbException("Failed to create a new Ubiquity database session", e);
		}
	}

	/**
	 * Retrieves the session associated with this wrapper.
	 * @return The session, not null.
	 */
	public Session getSession()
	{
		return externSession != null ? externSession : localSession;
	}

	/**
	 * Commits any locally started transaction.
	 * @throws MiDbException If the commit operation fails for some reason.
	 */
	public void commit() throws MiDbException
	{
		try
		{
			// Only commit the transaction that this wrapper started
			synchronized(this)
			{
				if((localTransaction != null) && (!commited) && (!closed))
				{
					localTransaction.commit();
					commited = true;
				}
			}
		}
		catch(HibernateException e)
		{
			throw new MiDbException("Failed to commit the Ubiquity database session", e);
		}
	}

	/**
	 * Rolls back any locally started transaction.
	 * @throws MiDbException If the rollback operation fails for some
	 * reason.
	 */
	public void rollback() throws MiDbException
	{
		synchronized(this) {
			try
			{
				// Only commit the transaction that this wrapper started
				if((localTransaction != null) && (!closed) && (!commited))
				{
					localTransaction.rollback();
				}
			}
			catch(HibernateException e)
			{
				throw new MiDbException("Failed to rollback the Ubiquity database session", e);
			}
			finally
			{
				closed = true;
			}
		}
	}

	/**
	 * Disposes the wrapper, rollback any uncommitted local transaction.
	 */
	public void dispose()
	{
		try
		{
			synchronized(this)
			{
				if((!commited) && (!closed))
				{
					// Only rollback the transaction that this wrapper started
					if(localTransaction != null)
					{
						localTransaction.rollback();
					}
				}
			}
		}
		catch(RuntimeException e)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
		finally
		{
			Session tmp;
			synchronized(this) { // Thread safety man, avoiding dead-lock
				tmp = localSession;
			}
			if(tmp != null)
			{
				try
				{
					synchronized(gLock)
					{
						gOutstandingSession.remove(tmp);
					}
					if(tmp.isOpen())
					{
						tmp.close();
					}
				}
				catch(RuntimeException e)
				{
					MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
				}
				finally
				{
					synchronized(this)
					{
						closed = true;
					}
				}
			}
		}
	}

	/**
	 * Retrieves the session factory singleton.
	 *
	 * @return The session factory singleton, not null.
	 * @throws MiDbException If a singleton factory could not be created.
	 */
	static SessionFactory getSessionFactory() throws MiDbException
	{
		synchronized(gLock)
		{
			if(sessionFactory != null)
			{
				return sessionFactory;
			}
		}
		throw new MiDbException("Session factory no available");
	}

	/**
	 * Destroys the Hibernate session factory, cleans up all outstanding
	 * sessions. This renders this module useless, so only call this on exit of
	 * the application.
	 */
	static void destroySessionFactory()
	{
		SessionFactory factory;
		try
		{
			synchronized(gLock)
			{
				for(Session s : gOutstandingSession)
				{
					MiSystem.logInfo(Category.DESIGN, "Forced close session " + s);
					s.close();
				}
				gOutstandingSession.clear();

				factory = sessionFactory;
				sessionFactory = null;
			}
			if(factory != null)
			{
				factory.close();
			}
		}
		catch(RuntimeException e)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
		}
	}
	/**
	 * The session factory singleton.
	 */
	private static SessionFactory sessionFactory;
	private static final Object gLock = new Object();
	/**
	 *
	 */
	private static Set<Session> gOutstandingSession = new HashSet<Session>(1024);

	static void init(URL url)
	{
		synchronized(gLock)
		{
			try
			{
				sessionFactory = new AnnotationConfiguration().configure(url).buildSessionFactory();
			}
			catch(HibernateException e)
			{
				MiSystem.logError(Category.DESIGN, "Failed to grok " + url + ' ' + MiExceptionUtil.simpleTrace(e));
			}
		}
	}

	static
	{
		Logger.getLogger("org.hibernate").setLevel(Level.WARNING);
	}
}

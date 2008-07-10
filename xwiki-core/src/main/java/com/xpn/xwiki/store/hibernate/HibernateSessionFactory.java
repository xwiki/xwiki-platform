package com.xpn.xwiki.store.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Class used by hibernate stores for obtain sessions.
 * Simple holder for real sessionFactory and configuration for now.
 */
public class HibernateSessionFactory
{
    /**
     * This component's role, used when code needs to look it up.
     */
    public static final String ROLE = HibernateSessionFactory.class.getName();

    /**
     * Hibernate configuration object.
     */
    private Configuration configuration;

    /**
     * Real hibernate session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * @return Hibernate Configuration object
     */
    public Configuration getConfiguration()
    {
        return configuration;
    }

    /**
     * @return Real hibernate session factory
     */
    public SessionFactory getSessionFactory()
    {
        return sessionFactory;
    }

    // NOTE: this methods will be removed in 3rd step of XWIKI-2332
    public void setConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }

    public void setSessionFactory(SessionFactory sessionFactory)
    {
        this.sessionFactory = sessionFactory;
    }
}

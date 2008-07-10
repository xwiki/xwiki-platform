package com.xpn.xwiki.store.hibernate;

import java.io.InputStream;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.util.Util;

/**
 * Class used by hibernate stores for obtain sessions.
 * Simple holder for real sessionFactory and configuration for now.
 */
public class HibernateSessionFactory implements Initializable
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
     * Path to hibernate.cfg.xml
     */
    private String configPath = "/WEB-INF/hibernate.cfg.xml";

    public void initialize() throws InitializationException
    {
        // there is no #configure(InputStream) so we use #configure(String) and override #getConfigurationInputStream
        Configuration cfg = new Configuration() {
            private static final long serialVersionUID = 1L;
            @Override
            protected InputStream getConfigurationInputStream(String resource)
                throws HibernateException
            {
                return Util.getResourceAsStream(resource);
            }
        };
        cfg.configure(configPath);

        XWiki wiki = context.getWiki();
        if (wiki != null && wiki.Param("xwiki.db") != null && !wiki.isVirtualMode()) {
            // substitute default db name to configured.
            // note, that we can't call getSchemaFromWikiName() here,
            // because it ask getDatabaseProduct() which use connection
            // which must be opened. But here (before connection init)
            // we have no opened connections yet.
            String schemaName = getSchemaFromWikiName(context.getDatabase(), null, context);

            System.out.println(schemaName);
            
            String dialect = cfg.getProperty(Environment.DIALECT);
            if ("org.hibernate.dialect.MySQLDialect".equals(dialect)) {
                cfg.setProperty(Environment.DEFAULT_CATALOG, schemaName);
            } else {
                cfg.setProperty(Environment.DEFAULT_SCHEMA, schemaName);
            }
        }
        setConfiguration(cfg);

        setSessionFactory(cfg.buildSessionFactory());
    }

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

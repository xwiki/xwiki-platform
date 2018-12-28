/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.internal.store.hibernate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Stoppable;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.migration.DataMigrationManager;

/**
 * Instance shared by all hibernate based stores.
 * 
 * @version $Id$
 * @since 9.10RC1
 */
@Component(roles = HibernateStore.class)
@Singleton
public class HibernateStore
{
    /**
     * @see #isInSchemaMode()
     */
    private static final String VIRTUAL_MODE_SCHEMA = "schema";

    private static final String CONTEXT_SESSION = "hibsession";

    private static final String CONTEXT_TRANSACTION = "hibtransaction";

    @Inject
    private Logger logger;

    @Inject
    private HibernateSessionFactory sessionFactory;

    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    private DataMigrationManager dataMigrationManager;

    @Inject
    private Execution execution;

    @Inject
    private WikiDescriptorManager wikis;

    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    private ConfigurationSource xwikiConfiguration;

    private Dialect dialect;

    private DatabaseProduct databaseProductCache = DatabaseProduct.UNKNOWN;

    /**
     * @return the Hibernate configuration
     */
    public Configuration getConfiguration()
    {
        return this.sessionFactory.getConfiguration();
    }

    /**
     * @return true if the user has configured Hibernate to use XWiki in schema mode (vs database mode)
     */
    public boolean isInSchemaMode()
    {
        String virtualModePropertyValue = getConfiguration().getProperty("xwiki.virtual_mode");
        if (virtualModePropertyValue == null) {
            virtualModePropertyValue = VIRTUAL_MODE_SCHEMA;
        }
        return StringUtils.equals(virtualModePropertyValue, VIRTUAL_MODE_SCHEMA);
    }

    /**
     * Convert wiki name in database/schema name.
     *
     * @param wikiId the wiki name to convert.
     * @param product the database engine type.
     * @return the database/schema name.
     */
    public String getSchemaFromWikiName(String wikiId, DatabaseProduct product)
    {
        if (wikiId == null) {
            return null;
        }

        String mainWikiId = this.wikis.getMainWikiId();

        String schema;
        if (StringUtils.equalsIgnoreCase(wikiId, mainWikiId)) {
            schema = this.xwikiConfiguration.getProperty("xwiki.db");
            if (schema == null) {
                if (product == DatabaseProduct.DERBY) {
                    schema = "APP";
                } else if (product == DatabaseProduct.HSQLDB || product == DatabaseProduct.H2) {
                    schema = "PUBLIC";
                } else if (product == DatabaseProduct.POSTGRESQL && isInSchemaMode()) {
                    schema = "public";
                } else {
                    schema = wikiId.replace('-', '_');
                }
            }
        } else {
            // virtual
            schema = wikiId.replace('-', '_');

            // For HSQLDB/H2 we only support uppercase schema names. This is because Hibernate doesn't properly generate
            // quotes around schema names when it qualifies the table name when it generates the update script.
            if (DatabaseProduct.HSQLDB == product || DatabaseProduct.H2 == product) {
                schema = StringUtils.upperCase(schema);
            }
        }

        // Apply prefix
        String prefix = this.xwikiConfiguration.getProperty("xwiki.db.prefix", "");
        schema = prefix + schema;

        return schema;
    }

    /**
     * Convert wiki name in database/schema name.
     * <p>
     * Need hibernate to be initialized.
     *
     * @param wikiId the wiki name to convert.
     * @return the database/schema name.
     */
    public String getSchemaFromWikiName(String wikiId)
    {
        return getSchemaFromWikiName(wikiId, getDatabaseProductName());
    }

    /**
     * Convert wiki name in database/schema name.
     * <p>
     * Need hibernate to be initialized.
     *
     * @return the database/schema name.
     */
    public String getSchemaFromWikiName()
    {
        return getSchemaFromWikiName(this.wikis.getCurrentWikiId());
    }

    /**
     * Retrieve the current database product name.
     * <p>
     * Note that the database product name is cached for improved performances.
     * </p>
     *
     * @return the database product name, see {@link DatabaseProduct}
     * @since 4.0M1
     */
    public DatabaseProduct getDatabaseProductName()
    {
        DatabaseProduct product = this.databaseProductCache;

        if (product == DatabaseProduct.UNKNOWN) {
            DatabaseMetaData metaData = getDatabaseMetaData();
            if (metaData != null) {
                try {
                    product = DatabaseProduct.toProduct(metaData.getDatabaseProductName());
                } catch (SQLException ignored) {
                    // do not care, return UNKNOWN
                }
            } else {
                // do not care, return UNKNOWN
            }
        }

        return product;
    }

    /**
     * Retrieve metadata about the database used (name, version, etc).
     * <p>
     * Note that the database metadata is not cached and it's retrieved at each call. If all you need is the database
     * product name you should use {@link #getDatabaseProductName()} instead, which is cached.
     * </p>
     *
     * @return the database meta data or null if an error occurred
     * @since 6.1M1
     */
    public DatabaseMetaData getDatabaseMetaData()
    {
        DatabaseMetaData result;
        Connection connection = null;
        // Note that we need to do the cast because this is how Hibernate suggests to get the Connection Provider.
        // See http://bit.ly/QAJXlr
        ConnectionProvider connectionProvider =
            ((SessionFactoryImplementor) getSessionFactory()).getConnectionProvider();
        try {
            connection = connectionProvider.getConnection();
            result = connection.getMetaData();
        } catch (SQLException ignored) {
            result = null;
        } finally {
            if (connection != null) {
                try {
                    connectionProvider.closeConnection(connection);
                } catch (SQLException ignored) {
                    // Ignore
                }
            }
        }

        return result;
    }

    /**
     * Escape schema name depending of the database engine.
     *
     * @param schema the schema name to escape
     * @return the escaped version
     */
    public String escapeSchema(String schema)
    {
        String escapedSchema;

        // - Oracle converts user names in uppercase when no quotes is used.
        // For example: "create user xwiki identified by xwiki;" creates a user named XWIKI (uppercase)
        // - In Hibernate.cfg.xml we just specify: <property name="connection.username">xwiki</property> and Hibernate
        // seems to be passing this username as is to Oracle which converts it to uppercase.
        //
        // Thus for Oracle we don't escape the schema.
        if (DatabaseProduct.ORACLE == getDatabaseProductName()) {
            escapedSchema = schema;
        } else {
            String closeQuote = String.valueOf(getDialect().closeQuote());
            escapedSchema = getDialect().openQuote() + schema.replace(closeQuote, closeQuote + closeQuote) + closeQuote;
        }

        return escapedSchema;
    }

    /**
     * @return a singleton instance of the configured {@link Dialect}
     */
    public Dialect getDialect()
    {
        if (this.dialect == null) {
            this.dialect = Dialect.getDialect(getConfiguration().getProperties());
        }

        return this.dialect;
    }

    /**
     * Set the current wiki in the passed session.
     * 
     * @param session the hibernate session
     * @throws XWikiException when failing to switch wiki
     */
    public void setWiki(Session session) throws XWikiException
    {
        setWiki(session, this.wikis.getCurrentWikiId());
    }

    /**
     * Set the passed wiki in the passed session
     *
     * @param session the hibernate session
     * @param wikiId the id of the wiki to switch to
     * @throws XWikiException when failing to switch wiki
     */
    public void setWiki(Session session, String wikiId) throws XWikiException
    {
        try {
            this.logger.debug("Set the right catalog/schema in teh session [{}]", wikiId);

            // Switch the database only if we did not switched on it last time
            if (wikiId != null) {
                String schemaName = getSchemaFromWikiName(wikiId);
                String escapedSchemaName = escapeSchema(schemaName);

                DatabaseProduct product = getDatabaseProductName();
                if (DatabaseProduct.ORACLE == product) {
                    executeSQL("alter session set current_schema = " + escapedSchemaName, session);
                } else if (DatabaseProduct.DERBY == product || DatabaseProduct.HSQLDB == product
                    || DatabaseProduct.DB2 == product || DatabaseProduct.H2 == product) {
                    executeSQL("SET SCHEMA " + escapedSchemaName, session);
                } else if (DatabaseProduct.POSTGRESQL == product && isInSchemaMode()) {
                    executeSQL("SET search_path TO " + escapedSchemaName, session);
                } else {
                    session.doWork(connection -> {
                        String catalog = connection.getCatalog();
                        catalog = (catalog == null) ? null : catalog.replace('_', '-');
                        if (!schemaName.equals(catalog)) {
                            connection.setCatalog(schemaName);
                        }
                    });
                }
            }

            this.dataMigrationManager.checkDatabase();
        } catch (Exception e) {
            // close session with rollback to avoid further usage
            endTransaction(false);

            Object[] args = { wikiId };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SWITCH_DATABASE, "Exception while switching to database {0}",
                e, args);
        }
    }

    /**
     * @return the current {@link Session} or null
     */
    public Session getCurrentSession()
    {
        ExecutionContext context = this.execution.getContext();

        if (context != null) {
            Session session = (Session) context.getProperty(CONTEXT_SESSION);

            // Make sure we are in this mode
            try {
                if (session != null) {
                    session.setFlushMode(FlushMode.COMMIT);
                }
            } catch (org.hibernate.SessionException ex) {
                session = null;
            }

            return session;
        }

        return null;
    }

    /**
     * @param session the new current {@link Session}
     */
    public void setCurrentSession(Session session)
    {
        ExecutionContext context = this.execution.getContext();

        if (session == null) {
            context.removeProperty(CONTEXT_SESSION);
        } else {
            context.setProperty(CONTEXT_SESSION, session);
        }
    }

    /**
     * @return the current {@link Transaction} or null
     */
    public Transaction getCurrentTransaction()
    {
        ExecutionContext context = this.execution.getContext();

        return (Transaction) context.getProperty(CONTEXT_TRANSACTION);
    }

    /**
     * @param transaction the new current {@link Transaction}
     */
    public void setCurrentTransaction(Transaction transaction)
    {
        ExecutionContext context = this.execution.getContext();

        if (transaction == null) {
            context.removeProperty(CONTEXT_TRANSACTION);
        } else {
            context.setProperty(CONTEXT_TRANSACTION, transaction);
        }
    }

    /**
     * Begins a transaction with a specific SessionFactory.
     *
     * @return true if a new transaction has been created, false otherwise.
     * @throws XWikiException if an error occurs while retrieving or creating a new session and transaction.
     */
    public boolean beginTransaction() throws XWikiException
    {
        return beginTransaction(null);
    }

    /**
     * Begins a transaction with a specific SessionFactory.
     *
     * @param sfactory the session factory used to begin a new session if none are available
     * @return true if a new transaction has been created, false otherwise.
     * @throws XWikiException if an error occurs while retrieving or creating a new session and transaction.
     */
    public boolean beginTransaction(SessionFactory sfactory) throws XWikiException
    {
        Transaction transaction = getCurrentTransaction();
        Session session = getCurrentSession();

        // XWiki uses a new Session for a new Transaction so we need to keep both in sync and thus we check if that's
        // the case. If it isn't it means some code is faulty somewhere.
        if (((session == null) && (transaction != null)) || ((transaction == null) && (session != null))) {
            this.logger.warn("Incompatible session ({}) and transaction ({}) status", session, transaction);

            // TODO: Fix this problem, don't ignore it!
            return false;
        }

        if (session != null) {
            this.logger.debug("Taking session from context [{}]", session);
            this.logger.debug("Taking transaction from context [{}]", transaction);

            return false;
        }

        // session is obviously null here
        this.logger.debug("Trying to get session from pool");
        if (sfactory == null) {
            session = getSessionFactory().openSession();
        } else {
            session = sfactory.openSession();
        }

        this.logger.debug("Taken session from pool [{}]", session);

        setCurrentSession(session);

        this.logger.debug("Trying to open transaction");
        transaction = session.beginTransaction();
        this.logger.debug("Opened transaction [{}]", transaction);
        setCurrentTransaction(transaction);

        // during #setDatabase, the transaction and the session will be closed if the database could not be
        // safely accessed due to version mismatch
        setWiki(session);

        return true;
    }

    private SessionFactory getSessionFactory()
    {
        return this.sessionFactory.getSessionFactory();
    }

    /**
     * Ends a transaction and close the session.
     *
     * @param commit should we commit or not
     */
    public void endTransaction(boolean commit)
    {
        Session session = null;
        try {
            session = getCurrentSession();
            Transaction transaction = getCurrentTransaction();
            setCurrentSession(null);
            setCurrentTransaction(null);

            if (transaction != null) {
                this.logger.debug("Releasing hibernate transaction [{}]", transaction);

                if (commit) {
                    transaction.commit();
                } else {
                    transaction.rollback();
                }
            }
        } catch (HibernateException e) {
            // Ensure the original cause will get printed.
            throw new HibernateException(
                "Failed to commit or rollback transaction. Root cause [" + getExceptionMessage(e) + "]", e);
        } finally {
            closeSession(session);
        }
    }

    /**
     * Hibernate and JDBC will wrap the exception thrown by the trigger in another exception (the
     * java.sql.BatchUpdateException) and this exception is sometimes wrapped again. Also the
     * java.sql.BatchUpdateException stores the underlying trigger exception in the nextException and not in the cause
     * property. The following method helps you to get to the underlying trigger message.
     */
    private String getExceptionMessage(Throwable t)
    {
        StringBuilder sb = new StringBuilder();
        Throwable next = null;
        for (Throwable current = t; current != null; current = next) {
            next = current.getCause();
            if (next == current) {
                next = null;
            }
            if (current instanceof SQLException) {
                SQLException sx = (SQLException) current;
                while (sx.getNextException() != null) {
                    sx = sx.getNextException();
                    sb.append("\nSQL next exception = [" + sx + "]");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Allows to shut down the hibernate configuration Closing all pools and connections.
     */
    public void shutdownHibernate()
    {
        Session session = getCurrentSession();
        closeSession(session);

        // Close all connections
        if (getSessionFactory() != null) {
            // Note that we need to do the cast because this is how Hibernate suggests to get the Connection Provider.
            // See http://bit.ly/QAJXlr
            ConnectionProvider connectionProvider =
                ((SessionFactoryImplementor) getSessionFactory()).getConnectionProvider();
            if (connectionProvider instanceof Stoppable) {
                ((Stoppable) connectionProvider).stop();
            }
        }
    }

    /**
     * Closes the hibernate session.
     *
     * @param session the sessions to close
     * @throws HibernateException
     */
    private void closeSession(Session session)
    {
        if (session != null) {
            session.close();
        }
    }

    /**
     * Execute an SQL statement using Hibernate.
     *
     * @param sql the SQL statement to execute
     * @param session the Hibernate Session in which to execute the statement
     */
    private void executeSQL(final String sql, Session session)
    {
        session.doWork(new Work()
        {
            @Override
            public void execute(Connection connection) throws SQLException
            {
                Statement stmt = null;
                try {
                    stmt = connection.createStatement();
                    stmt.execute(sql);
                } finally {
                    try {
                        if (stmt != null) {
                            stmt.close();
                        }
                    } catch (Exception e) {
                    }
                }
            }
        });
    }
}

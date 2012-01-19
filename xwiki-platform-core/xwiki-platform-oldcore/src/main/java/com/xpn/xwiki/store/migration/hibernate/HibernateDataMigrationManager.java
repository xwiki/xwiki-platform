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

package com.xpn.xwiki.store.migration.hibernate;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.logging.LoggerManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.AbstractDataMigrationManager;
import com.xpn.xwiki.store.migration.DataMigration;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration manager for hibernate store.
 * 
 * @version $Id$
 * @since 3.4M1
 */
@Component
@Named("hibernate")
@Singleton
public class HibernateDataMigrationManager extends AbstractDataMigrationManager
{
    /** LoggerManager to suspend logging during normal faulty SQL operation. */
    @Inject
    private LoggerManager loggerManager;

    /**
     * @return store system for execute store-specific actions.
     * @throws DataMigrationException if the store could not be reached
     */
    public XWikiHibernateBaseStore getStore() throws DataMigrationException
    {
        try {
            return (XWikiHibernateBaseStore) componentManager.lookup(XWikiStoreInterface.class, "hibernate");
        } catch (ComponentLookupException e) {
            throw new DataMigrationException(String.format("Unable to reach the store for database %s",
                getXWikiContext().getDatabase()), e);
        }
    }

    @Override
    public XWikiDBVersion getDBVersionFromDatabase() throws DataMigrationException
    {
        XWikiDBVersion ver = getDBVersionFromConfig();
        if (ver != null) {
            return ver;
        }

        final XWikiContext context = getXWikiContext();
        final XWikiHibernateBaseStore store = getStore();

        final Session originalSession = store.getSession(context);
        final Transaction originalTransaction = store.getTransaction(context);
        store.setSession(null, context);
        store.setTransaction(null, context);

        // Prevent JDBCExceptionReporter from logging an error for below accepted exception.
        // We want to return a DB version what ever it happens and not log any error.
        loggerManager.pushLogListener(null);
        try {
            // Try retrieving a version from the database
            try {
                ver = store.executeRead(context, true,
                    new HibernateCallback<XWikiDBVersion>()
                    {
                        @Override
                        public XWikiDBVersion doInHibernate(Session session) throws HibernateException
                        {
                            // Retrieve the version from the database
                            return (XWikiDBVersion) session.createCriteria(XWikiDBVersion.class).uniqueResult();
                        }
                    });
            }
            catch (Exception ignored) {
                // ignore exception since missing schema will cause them
            }
            // if it fails, return version 0 if there is some documents in the database, else null (empty db?)
            if (ver == null) {
                try {
                    ver = store.executeRead(getXWikiContext(), true,
                        new HibernateCallback<XWikiDBVersion>()
                        {
                            @Override
                            public XWikiDBVersion doInHibernate(Session session) throws HibernateException
                            {
                                if (((Number) session.createCriteria(XWikiDocument.class)
                                    .setProjection(Projections.rowCount())
                                    .uniqueResult()).longValue() > 0)
                                {
                                    return new XWikiDBVersion(0);
                                }
                                return null;
                            }
                        });
                }
                catch (Exception ignored) {
                    // ignore exception since missing schema will cause them
                }
            }
        } finally {
            loggerManager.popLogListener();
            store.setSession(originalSession, context);
            store.setTransaction(originalTransaction, context);
        }

        return ver;
    }

    @Override
    protected void initializeEmptyDB() throws DataMigrationException {
        final XWikiContext context = getXWikiContext();
        final XWikiHibernateBaseStore store = getStore();

        final Session originalSession = store.getSession(context);
        final Transaction originalTransaction = store.getTransaction(context);
        store.setSession(null, context);
        store.setTransaction(null, context);

        try {
            updateSchema();
            setDBVersion(getLatestVersion());
        } finally {
            store.setSession(originalSession, context);
            store.setTransaction(originalTransaction, context);
        }
    }

    @Override
    protected void setDBVersionToDatabase(final XWikiDBVersion version) throws DataMigrationException
    {
        final XWikiContext context = getXWikiContext();
        final XWikiHibernateBaseStore store = getStore();
        final boolean bTransaction = store.getTransaction(context) == null;

        try {
            getStore().executeWrite(context, bTransaction, new HibernateCallback<Object>()
            {
                @Override
                public Object doInHibernate(Session session) throws HibernateException
                {
                    session.createQuery("delete from " + XWikiDBVersion.class.getName()).executeUpdate();
                    session.save(version);
                    return null;
                }
            });
        } catch (Exception e) {
            throw new DataMigrationException(String.format("Unable to store data version into database %s",
                context.getDatabase()), e);
        }
    }


    @Override
    protected void updateSchema() throws DataMigrationException {
        try {
            getStore().updateSchema(getXWikiContext(), true);
        } catch (Exception e) {
            throw new DataMigrationException(String.format("Unable to update schema of database %s",
                getXWikiContext().getDatabase()), e);
        }
    }

    @Override
    protected synchronized void startMigrations() throws DataMigrationException {
        if (this.migrations == null) {
            return;
        }

        XWikiContext context = getXWikiContext();
        XWikiHibernateBaseStore store = getStore();

        Session originalSession = store.getSession(context);
        Transaction originalTransaction = store.getTransaction(context);
        store.setSession(null, context);
        store.setTransaction(null, context);

        try {
            super.startMigrations();
        } finally {
            store.setSession(originalSession, context);
            store.setTransaction(originalTransaction, context);
        }
    }

    @Override
    protected List<? extends DataMigration> getAllMigrations() throws DataMigrationException
    {
        try {
            return componentManager.lookupList(HibernateDataMigration.class);
        } catch (ComponentLookupException e) {
            throw new DataMigrationException("Unable to retrieve the list of hibernate data migrations", e);
        }
    }
}

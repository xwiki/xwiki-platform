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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.AbstractXWikiMigrationManager;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.XWikiMigratorInterface;

/**
 * Migration manager for hibernate store.
 * 
 * @version $Id$
 */
public class XWikiHibernateMigrationManager extends AbstractXWikiMigrationManager
{
    /** logger */
    protected static final Log LOG = LogFactory.getLog(XWikiHibernateMigrationManager.class);

    /** {@inheritDoc} */
    public XWikiHibernateMigrationManager(XWikiContext context) throws XWikiException
    {
        super(context);
    }

    /**
     * @return store system for execute store-specific actions.
     * @param context - used everywhere
     */
    public XWikiHibernateBaseStore getStore(XWikiContext context)
    {
        return context.getWiki().getHibernateStore();
    }

    /** {@inheritDoc} */
    @Override
    public XWikiDBVersion getDBVersion(XWikiContext context) throws XWikiException
    {
        XWikiDBVersion ver = getDBVersionFromConfig(context);
        return ver != null ? ver : getStore(context).executeRead(context, true, new HibernateCallback<XWikiDBVersion>()
        {
            public XWikiDBVersion doInHibernate(Session session) throws HibernateException
            {
                XWikiDBVersion result = (XWikiDBVersion) session.createCriteria(XWikiDBVersion.class).uniqueResult();
                return result == null ? new XWikiDBVersion(0) : result;
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    protected void setDBVersion(final XWikiDBVersion version, XWikiContext context) throws XWikiException
    {
        getStore(context).executeWrite(context, true, new HibernateCallback<Object>()
        {
            public Object doInHibernate(Session session) throws HibernateException
            {
                session.createQuery("delete from " + XWikiDBVersion.class.getName()).executeUpdate();
                session.save(version);
                return null;
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    protected List<XWikiMigratorInterface> getAllMigrations(XWikiContext context) throws XWikiException
    {
        List<XWikiMigratorInterface> result = new ArrayList<XWikiMigratorInterface>();
        // TODO: how to register migrations?
        // 1st way:
        result.add(new R4340XWIKI883Migrator());
        result.add(new R4359XWIKI1459Migrator());
        result.add(new R6079XWIKI1878Migrator());
        result.add(new R6405XWIKI1933Migrator());
        result.add(new R7350XWIKI2079Migrator());
        result.add(new R15428XWIKI2977Migrator());
        // 2nd way - via component manager

        return result;
    }
}

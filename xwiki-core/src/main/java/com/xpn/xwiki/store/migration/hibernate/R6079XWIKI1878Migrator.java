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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.Query;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiPatch;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;

import java.util.Iterator;

/**
 * Migration for XWIKI1878: Fix xwikircs table isdiff data not matching RCS state of some revisions (when the state
 * says "full" the isdiff column in the database should be false).
 *
 * Note: This migrator should only be executed if the R4359XWIKI1459 one has already been executed (i.e. if the
 * database is in version < 4360). This is because this current migrator is because of a bug in R4359XWIKI1459 which
 * has now been fixed.
 *
 * @version $Id$
 */
public class R6079XWIKI1878Migrator extends AbstractXWikiHibernateMigrator
{
    /** logger. */
    private static final Log LOG = LogFactory.getLog(R6079XWIKI1878Migrator.class);

    /**
     * {@inheritDoc}
     * @see com.xpn.xwiki.store.migration.hibernate.AbstractXWikiHibernateMigrator#getName()
     */
    public String getName()
    {
        return "R6079XWIKI1878";
    }

    /**
     * {@inheritDoc}
     * @see AbstractXWikiHibernateMigrator#getDescription()
     */
    public String getDescription()
    {
        return "See http://jira.xwiki.org/jira/browse/XWIKI-1878";
    }

    /** {@inheritDoc} */
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(6079);
    }

    /**
     * {@inheritDoc}
     * @see AbstractXWikiHibernateMigrator#shouldExecute(com.xpn.xwiki.store.migration.XWikiDBVersion)
     */
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        return (startupVersion.getVersion() >= 4360);
    }

    /** {@inheritDoc} */
    public void migrate(XWikiHibernateMigrationManager manager, final XWikiContext context)
        throws XWikiException
    {
        // migrate data
        manager.getStore(context).executeWrite(context, true, new XWikiHibernateBaseStore.HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                try {
                    Query query = session.createQuery("select rcs.id, rcs.patch, doc.fullName "
                        + "from XWikiDocument as doc, XWikiRCSNodeContent as rcs where "
                        + "doc.id = rcs.id.docId and rcs.patch.diff = true and rcs.patch.content like '<?xml%'");
                    Iterator it = query.list().iterator();

                    Transaction originalTransaction = ((XWikiHibernateVersioningStore)context.getWiki().getVersioningStore()).getTransaction(context);
                    ((XWikiHibernateVersioningStore)context.getWiki().getVersioningStore()).setSession(null, context);
                    ((XWikiHibernateVersioningStore)context.getWiki().getVersioningStore()).setTransaction(null, context);

                    while (it.hasNext()) {
                        Object[] result = (Object[]) it.next();
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Fixing document [" + result[2] + "]...");
                        }

                        // Reconstruct a XWikiRCSNodeContent object with isDiff set to false and update it.
                        XWikiRCSNodeId nodeId = (XWikiRCSNodeId) result[0];
                        XWikiRCSNodeContent fixedNodeContent = new XWikiRCSNodeContent(nodeId);
                        XWikiPatch patch = (XWikiPatch) result[1];
                        patch.setDiff(false);
                        fixedNodeContent.setPatch(patch);

                        session.update(fixedNodeContent);
                    }

                    ((XWikiHibernateVersioningStore)context.getWiki().getVersioningStore()).setSession(session, context);
                    ((XWikiHibernateVersioningStore)context.getWiki().getVersioningStore()).setTransaction(originalTransaction, context);
                } catch (Exception e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_STORE_MIGRATION, getName() + " migration failed", e);
                }
                return Boolean.TRUE;
            }
        });
    }
}

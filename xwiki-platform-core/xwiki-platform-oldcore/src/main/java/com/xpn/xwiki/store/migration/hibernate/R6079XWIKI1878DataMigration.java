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

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.rcs.XWikiPatch;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI1878: Fix xwikircs table isdiff data not matching RCS state of some revisions (when the state says
 * "full" the isdiff column in the database should be false). Note: This data migration should only be executed if the
 * R4359XWIKI1459 one has already been executed (i.e. if the database is in version &gt;= 4360). This is because it fixes a
 * bug in R4359XWIKI1459 which has now been fixed.
 *
 * @version $Id$
 */
@Component
@Named("R6079XWIKI1878")
@Singleton
public class R6079XWIKI1878DataMigration extends AbstractHibernateDataMigration
{
    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "See http://jira.xwiki.org/jira/browse/XWIKI-1878";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(6079);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        return (startupVersion.getVersion() >= 4360);
    }

    /**
     * @return version store system for execute store-specific actions.
     * @throws XWikiException if the store could not be reached
     */
    private XWikiHibernateVersioningStore getVersioningStore() throws XWikiException
    {
        try {
            return (XWikiHibernateVersioningStore) this.componentManager
                .getInstance(XWikiVersioningStoreInterface.class, "hibernate");
        } catch (ComponentLookupException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_MIGRATION,
                String.format("Unable to reach the versioning store for database %s", getXWikiContext().getWikiId()),
                e);
        }
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // migrate data
        getStore().executeWrite(getXWikiContext(), true,
            new XWikiHibernateBaseStore.HibernateCallback<Object>()
            {
                @Override
                public Object doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    try {
                        Query query = session.createQuery("select rcs.id, rcs.patch, doc.fullName "
                            + "from XWikiDocument as doc, XWikiRCSNodeContent as rcs where "
                            + "doc.id = rcs.id.docId and rcs.patch.diff = true and rcs.patch.content like '<?xml%'");
                        Iterator it = query.list().iterator();

                        XWikiContext context = getXWikiContext();
                        XWikiHibernateVersioningStore versioningStore = getVersioningStore();
                        Transaction originalTransaction = versioningStore.getTransaction(context);
                        versioningStore.setSession(null, context);
                        versioningStore.setTransaction(null, context);

                        while (it.hasNext()) {
                            Object[] result = (Object[]) it.next();
                            if (R6079XWIKI1878DataMigration.this.logger.isInfoEnabled()) {
                                R6079XWIKI1878DataMigration.this.logger.info("Fixing document [{}]...", result[2]);
                            }

                            // Reconstruct a XWikiRCSNodeContent object with isDiff set to false and update it.
                            XWikiRCSNodeId nodeId = (XWikiRCSNodeId) result[0];
                            XWikiRCSNodeContent fixedNodeContent = new XWikiRCSNodeContent(nodeId);
                            XWikiPatch patch = (XWikiPatch) result[1];
                            patch.setDiff(false);
                            fixedNodeContent.setPatch(patch);

                            session.update(fixedNodeContent);
                        }

                        versioningStore.setSession(session, context);
                        versioningStore.setTransaction(originalTransaction, context);
                    } catch (Exception e) {
                        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                            XWikiException.ERROR_XWIKI_STORE_MIGRATION, getName() + " migration failed", e);
                    }
                    return Boolean.TRUE;
                }
            });
    }
}

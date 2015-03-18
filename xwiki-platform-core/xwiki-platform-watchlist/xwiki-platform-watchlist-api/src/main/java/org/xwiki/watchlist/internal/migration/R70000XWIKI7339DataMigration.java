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

package org.xwiki.watchlist.internal.migration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for XWIKI7339: Update the WatchListClass document and its objects to use DBStringList instead of TextArea
 * fields for the watched elements.
 *
 * @version $Id$
 */
@Component
@Named("R70000XWIKI7339")
@Singleton
public class R70000XWIKI7339DataMigration extends AbstractHibernateDataMigration
{

    /** WatchList class local reference. */
    private static final LocalDocumentReference WATCHLIST_CLASS_REFERENCE = new LocalDocumentReference("XWiki",
        "WatchListClass");

    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "See http://jira.xwiki.org/browse/XWIKI-7339";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // XWiki 7.0, first migration.
        return new XWikiDBVersion(70000);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        final XWikiContext context = getXWikiContext();

        // migrate data
        getStore().executeWrite(context, new DoWorkHibernateCallback(context));
    }

    private final class DoWorkHibernateCallback implements HibernateCallback<Object>
    {
        private final XWikiContext context;

        private DoWorkHibernateCallback(XWikiContext context)
        {
            this.context = context;
        }

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            // Migrate the class.
            // Use XWiki API to edit the BaseClass but save the document directly in the Hibernate session to avoid
            // useless work that does not apply for our migration.

            XWikiDocument watchListClassDocument = context.getWiki().getDocument(WATCHLIST_CLASS_REFERENCE, context);
            if (watchListClassDocument.isNew()) {
                // The class document is not yet initialized, it will be initialized by the watchlist module.
                // Nothing for us to migrate.
                return null;
            }

            BaseClass watchListClass = watchListClassDocument.getXClass();
            fixClassProperty(watchListClass, "wikis", "Wiki list");
            fixClassProperty(watchListClass, "spaces", "Space list");
            fixClassProperty(watchListClass, "documents", "Document list");
            fixClassProperty(watchListClass, "users", "User list");

            // Serialize the modified XClass into the document and save it.
            watchListClassDocument.setXClassXML(watchListClass.toXMLString());
            session.save(watchListClassDocument);

            logger.info("Migrated class document [{}]", watchListClassDocument.getDocumentReference());

            // Migrate existing objects.

            Query q =
                session.createQuery("SELECT ls FROM BaseObject o, LargeStringProperty ls"
                    + " WHERE o.className='XWiki.WatchListClass' AND o.id=ls.id");

            @SuppressWarnings("unchecked")
            List<LargeStringProperty> oldProperties = q.list();
            if (oldProperties.size() == 0) {
                // No watched elements exist that need migrating.
                return null;
            }

            // Create a migrated equivalent for each old property.
            List<DBStringListProperty> newProperties = new ArrayList<DBStringListProperty>(oldProperties.size());
            for (LargeStringProperty oldProperty : oldProperties) {
                DBStringListProperty newProperty = new DBStringListProperty();
                newProperty.setId(oldProperty.getId());
                newProperty.setName(oldProperty.getName());
                // Migrate the data to the new format (1 entry per item).
                newProperty.setList(ListClass.getListFromString(oldProperty.getValue(), ",", false));

                newProperties.add(newProperty);
            }

            // First delete the old properties in a separate step, to avoid ID collisions.
            for (LargeStringProperty oldProperty : oldProperties) {
                session.delete(oldProperty);
            }

            // Finally, add the new migrated properties (with the old IDs).
            for (DBStringListProperty newProperty : newProperties) {
                session.save(newProperty);
            }

            logger.info("Migrated [{}] object properties", oldProperties.size());

            return null;
        }

        private void fixClassProperty(BaseClass bclass, String name, String prettyName)
        {
            PropertyInterface property = bclass.get(name);
            if (!(property instanceof DBListClass)) {
                bclass.removeField(name);

                bclass.addDBListField(name, prettyName, 80, true, null);
                DBListClass fixedProperty = (DBListClass) bclass.get(name);
                fixedProperty.setDisplayType(ListClass.DISPLAYTYPE_INPUT);
            }
        }
    }
}

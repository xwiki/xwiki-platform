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
package org.xwiki.eventstream.store.internal;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;

/**
 * Fix entries stored in the activity stream events table that have an absolute serialized reference in the "page"
 * field. It concerns some RecordableEvent generated between 9.2RC1 and 9.3RC1.
 *
 * @since 11.1RC1
 * @version $Id$
 */
@Component
@Named("R93000RecordableEventMigrator")
@Singleton
public class RecordableEventMigrator extends AbstractHibernateDataMigration
{
    @Inject
    private QueryManager queryManager;
    
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "https://jira.xwiki.org/browse/XWIKI-14172";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(93000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        String hql = "select event from LegacyEvent event where event.page LIKE concat(event.wiki, ':%')";
        try {
            List<LegacyEvent> events;
            do {
                Query query = queryManager.createQuery(hql, Query.HQL);
                query.setLimit(50);
                events = query.execute();
                for (LegacyEvent event : events) {
                    fixEvent(event);
                }
            } while (!events.isEmpty());
        } catch (QueryException e) {
            throw new DataMigrationException("Failed to fix RecordableEvent problems.", e);
        }
    }

    private void fixEvent(LegacyEvent event)
    {
        String fullName = event.getPage();

        // Remove the "wiki:" part of the fullname
        event.setPage(fullName.substring(String.format("%s:", event.getWiki()).length()));

        // Update the event in the database
        saveEvent(event);
    }

    private void saveEvent(LegacyEvent event)
    {
        XWikiContext context = contextProvider.get();
        XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();
        try {
            hibernateStore.beginTransaction(context);
            Session session = hibernateStore.getSession(context);
            session.update(event);
            hibernateStore.endTransaction(context, true);
        } catch (XWikiException e) {
            hibernateStore.endTransaction(context, false);
            logger.warn("Failed to update the event [{}].", event.getEventId());
        }
    }
}

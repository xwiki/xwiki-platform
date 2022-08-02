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
package org.xwiki.notifications.notifiers.internal.email;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

/**
 * Remove some mandatory documents created by the Watchlist Application that the notification email notifier replaces.
 *
 * @since 11.3RC1
 * @version $Id$
 */
@Component
// Should be 1103000 but too late to change it...
@Named("R1130000WatchlistLeftoversCleaner")
@Singleton
public class WatchlistLeftoversCleaner extends AbstractHibernateDataMigration
{
    private static final String SCHEDULER_SPACE_NAME = "Scheduler";

    private static final String XWIKI_SPACE = "XWiki";

    private static final List<LocalDocumentReference> DOCUMENTS_TO_REMOVE = Arrays.asList(
        new LocalDocumentReference(SCHEDULER_SPACE_NAME, "WatchListDailyNotifier"),
        new LocalDocumentReference(SCHEDULER_SPACE_NAME, "WatchListHourlyNotifier"),
        new LocalDocumentReference(SCHEDULER_SPACE_NAME, "WatchListWeeklyNotifier"),
        new LocalDocumentReference(XWIKI_SPACE, "WatchListClass"),
        new LocalDocumentReference(XWIKI_SPACE, "WatchListJobClass")
    );

    @Inject
    private Logger logger;

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        XWikiContext context = this.getXWikiContext();
        logger.info("Starting WatchlistLeftoversCleaner on wiki [{}].", context.getWikiId());

        try {
            XWiki xwiki = context.getWiki();
            for (LocalDocumentReference documentToRemove : DOCUMENTS_TO_REMOVE) {
                XWikiDocument document = xwiki.getDocument(documentToRemove, context);
                if (document.isNew()) {
                    continue;
                }
                xwiki.deleteDocument(document, context);
            }
        } catch (Exception e) {
            throw new DataMigrationException("Failed to remove watchlist leftovers.", e);
        }

        logger.info("End of WatchlistLeftoversCleaner on wiki [{}].", context.getWikiId());
    }

    @Override
    public String getDescription()
    {
        return "Remove some mandatory documents created by the Watchlist Application that the notification email "
                + "notifier replaces.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // Should be 1103000 but too late to change it...
        return new XWikiDBVersion(1130000);
    }
}

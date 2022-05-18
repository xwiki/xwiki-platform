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
package org.xwiki.notifications.filters.migration;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Cleanup the notification filters preferences remaining on the main wiki from previously removed sub-wikis. Note: this
 * class is named {@code R131006000XWIKI1546} in branch {@code 13.10.6+}.
 *
 * @version $Id$
 * @see <a href="https://jira.xwiki.org/browse/XWIKI-15460">XWIKI-15460: Notification filter preferences are not cleaned
 *     when a wiki is deleted</a>
 * @since 14.4
 * @since 13.10.6
 */
@Component
@Singleton
@Named("R140400000XWIKI1546")
@Unstable
public class R140400000XWIKI15460DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private NotificationFilterPreferenceStore store;

    @Inject
    private Logger logger;

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(140400000);
    }

    @Override
    public String getDescription()
    {
        return "Remove the notification filters preferences remaining from removed sub-wikis.";
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException
    {
        XWikiContext context = getXWikiContext();
        if (!Objects.equals(context.getWikiId(), context.getMainXWiki())) {
            this.logger.info("Skipping, this migration only applies to the main wiki.");
            return;
        }

        int version = this.manager.get().getDBVersion().getVersion();
        if (version >= 131006000 && version < 140000000) {
            this.logger.info("Skipping, this migration has already been performed in 13.10.6+.");
            return;
        }

        try {
            Collection<String> wikiIds = this.wikiDescriptorManager.getAllIds();
            // TODO: setting the limit to 3 for the tests, but must be moved back to 1000 afterwards
            int limit = 3;
            int offset = 0;
            Set<NotificationFilterPreference> allNotificationFilterPreferences = this.store
                .getPaginatedFilterPreferences(limit, offset);
            while (!allNotificationFilterPreferences.isEmpty()) {
                // We count the deleted filter preferences and adapt the next offset accordingly.
                int removed = 0;
                for (NotificationFilterPreference filterPreference : allNotificationFilterPreferences) {
                    boolean isFromExistingWiki = wikiIds.stream().anyMatch(filterPreference::isFromWiki);
                    if (!isFromExistingWiki) {
                        removed++;
                        this.store.deleteFilterPreference(filterPreference);
                    }
                }
                offset += limit - removed;
                allNotificationFilterPreferences = this.store.getPaginatedFilterPreferences(limit, offset);
            }
        } catch (NotificationException e) {
            throw new DataMigrationException("Failed to retrieve the notification filters preferences.", e);
        } catch (WikiManagerException e) {
            throw new DataMigrationException("Failed to retrieve the ids of wikis of the farm.", e);
        }
    }
}

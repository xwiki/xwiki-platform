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
package org.xwiki.notifications.filters.internal.migration;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.internal.ModelBridge;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Cleanup the notification filters preferences remaining on the main wiki from previously removed sub-wikis.
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
public class R140400000XWIKI15460DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private ModelBridge modelBridge;

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
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
// TODO: check if main wiki, skip otherwise
        XWikiContext context = getXWikiContext();
        if (!Objects.equals(context.getWikiId(), context.getMainXWiki())) {
            // TODO: log skipped because not the main wiki
            return;
        }
        
        // TODO: skip if already migrated in 13.10.6
        
        
        try {
            Collection<String> wikiIds = this.wikiDescriptorManager.getAllIds();

            // TODO: replace by a call to get all the notification filter preferences!
            // TODO: do we want to execute this for each kind of existing model bridge?
            Set<NotificationFilterPreference> allNotificationFilterPreferences = this.modelBridge
                .getAllFilterPreferences();
            for (NotificationFilterPreference filterPreference : allNotificationFilterPreferences) {
                boolean isFromExistingWiki = wikiIds.stream().anyMatch(filterPreference::isFromWiki);
                if (!isFromExistingWiki) {
                    // TODO: remove filter preference
                }
            }
        } catch (NotificationException e) {
            // TODO: handle exceptions
            throw new RuntimeException(e);
        } catch (WikiManagerException e) {
            // TODO: handle exceptions
            throw new RuntimeException(e);
        }
    }
}

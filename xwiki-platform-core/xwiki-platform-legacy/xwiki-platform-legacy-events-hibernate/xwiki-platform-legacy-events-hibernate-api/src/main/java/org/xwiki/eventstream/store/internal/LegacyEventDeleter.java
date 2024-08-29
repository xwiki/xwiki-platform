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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatusManager;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiHibernateStore;

/**
 * Delete an event from the legacy event store.
 *
 * @version $Id$
 * @since 11.1RC1
 */
@Component(roles = LegacyEventDeleter.class)
@Singleton
public class LegacyEventDeleter
{
    @Inject
    private LegacyEventConverter eventConverter;

    @Inject
    private EventStatusManager statusManager;

    @Inject
    private LegacyEventStreamStoreConfiguration configuration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Inject
    private NamespaceContextExecutor namespaceContextExecutor;

    /**
     * Delete the given event.
     * 
     * @param event the event to delete
     */
    public void deleteEvent(Event event)
    {
        LegacyEvent legacyEvent = eventConverter.convertEventToLegacyActivity(event);

        boolean hasBeenDeletedOnMainStore = false;
        try {
            if (configuration.useLocalStore() && wikiDescriptorManager.exists(legacyEvent.getWiki())) {
                // delete event from the database where it is stored
                deleteLegacyEvent(legacyEvent, legacyEvent.getWiki());
                hasBeenDeletedOnMainStore = wikiDescriptorManager.isMainWiki(legacyEvent.getWiki());
            }
        } catch (Exception e) {
            this.logger.error("Failed to delete the event [{}] in the local store.", event.getId(), e);
        }

        if (configuration.useMainStore() && !hasBeenDeletedOnMainStore) {
            // delete event from the main database
            try {
                deleteLegacyEvent(legacyEvent, wikiDescriptorManager.getMainWikiId());
            } catch (Exception e) {
                this.logger.error("Failed to delete the event [{}] in the main store.", event.getId(), e);
            }
        }
    }

    private void deleteLegacyEvent(LegacyEvent event, String wikiId) throws Exception
    {
        namespaceContextExecutor.execute(new WikiNamespace(wikiId), () -> {
            XWikiContext context = contextProvider.get();
            XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();

            hibernateStore.executeWrite(context, session -> {
                // Make sure to delete all associated statuses first
                if (this.statusManager instanceof LegacyEventStatusManager) {
                    ((LegacyEventStatusManager) this.statusManager).deleteAllForEventInStore(session,
                        event.getEventId());
                }

                session.delete(event);

                return null;
            });

            return null;
        });
    }
}

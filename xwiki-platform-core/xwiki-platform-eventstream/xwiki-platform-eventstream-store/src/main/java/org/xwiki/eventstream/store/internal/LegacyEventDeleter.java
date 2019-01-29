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
import com.xpn.xwiki.store.XWikiHibernateStore;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.events.EventStreamDeletedEvent;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.observation.ObservationManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

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
    private LegacyEventStreamStoreConfiguration configuration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private Logger logger;

    @Inject
    private NamespaceContextExecutor namespaceContextExecutor;

    /**
     * Delete the given event.
     * @param event the event to delete
     */
    public void deleteEvent(Event event)
    {
        LegacyEvent legacyEvent = eventConverter.convertEventToLegacyActivity(event);

        try {
            if (configuration.useLocalStore()) {
                // delete event from the database where it is stored
                deleteLegacyEvent(legacyEvent, legacyEvent.getWiki());
            }

            if (configuration.useMainStore()) {
                // delete event from the main database
                deleteLegacyEvent(legacyEvent, wikiDescriptorManager.getMainWikiId());
            }

            observationManager.notify(new EventStreamDeletedEvent(), event);
        } catch (Exception e) {
            logger.error("Failed to delete the event [%s].", event.getId(), e);
        }
    }

    private void deleteLegacyEvent(LegacyEvent event, String wikiId) throws Exception
    {
        namespaceContextExecutor.execute(new WikiNamespace(wikiId),
            () -> {
                XWikiContext context = contextProvider.get();
                XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();
                boolean bTransaction = true;

                try {
                    hibernateStore.checkHibernate(context);
                    bTransaction = hibernateStore.beginTransaction(context);
                    Session session = hibernateStore.getSession(context);

                    session.delete(event);

                    if (bTransaction) {
                        hibernateStore.endTransaction(context, true);
                    }

                } catch (Exception e) {
                    if (bTransaction) {
                        hibernateStore.endTransaction(context, false);
                    }
                    throw e;
                }

                return null;
            }
        );
    }
}

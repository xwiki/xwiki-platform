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
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiHibernateStore;

/**
 * Save an event into the legacy event store.
 *
 * @since 11.1RC1
 * @version $Id$
 */
@Component(roles = LegacyEventSaver.class)
@Singleton
public class LegacyEventSaver
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
    private Logger logger;

    @Inject
    private NamespaceContextExecutor namespaceContextExecutor;

    /**
     * Add a new event to the storage.
     *
     * @param event the event to store
     */
    public void saveEvent(Event event)
    {
        LegacyEvent legacyEvent = eventConverter.convertEventToLegacyActivity(event);

        try {
            boolean isSavedOnMainStore = false;

            if (configuration.useLocalStore()) {
                // save event into the database where it should be located
                saveLegacyEvent(legacyEvent, legacyEvent.getWiki());
                isSavedOnMainStore = wikiDescriptorManager.isMainWiki(legacyEvent.getWiki());
            }

            if (configuration.useMainStore() && !isSavedOnMainStore) {
                // save event into the main database (if the event was not already be recorded on the main store,
                // otherwise we would duplicate the event)
                saveLegacyEvent(legacyEvent, wikiDescriptorManager.getMainWikiId());
            }
        } catch (Exception e) {
            logger.error("Failed to save an event in the event stream.", e);
        }
    }

    private void saveLegacyEvent(LegacyEvent event, String wikiId) throws Exception
    {
        namespaceContextExecutor.execute(new WikiNamespace(wikiId), () -> {
            XWikiContext context = contextProvider.get();
            XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();

            hibernateStore.executeWrite(context, session -> session.save(event));

            return null;
        });
    }
}

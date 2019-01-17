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
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.events.EventStreamAddedEvent;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Date;

/**
 * Save an event into the legacy event store.
 *
 * @since 11.0RC1
 * @version $Id$
 */
@Component(roles = LegacyEventSaver.class)
@Singleton
public class LegacyEventSaver
{
    /**
     * Key used to store the request ID in the context.
     */
    private static final String GROUP_ID_CONTEXT_KEY = "activitystream_requestid";

    @Inject
    private LegacyEventConverter eventConverter;

    @Inject
    private LegacyEventStreamStoreConfiguration configuration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ObservationManager observationManager;

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
        XWikiContext context = contextProvider.get();

        prepareEvent(event, context);

        LegacyEvent legacyEvent = eventConverter.convertEventToLegacyActivity(event);

        try {

            if (configuration.useLocalStore()) {
                // save event into the database where it should be located
                saveLegacyEvent(legacyEvent, legacyEvent.getWiki());
            }

            if (configuration.useMainStore()) {
                // save event into the main database
                saveLegacyEvent(legacyEvent, wikiDescriptorManager.getMainWikiId());
            }

            observationManager.notify(new EventStreamAddedEvent(), event);
        } catch (Exception e) {
            logger.error("Failed to save an event in the event stream.", e);
        }
    }

    /**
     * Set fields in the given event object.
     *
     * @param event the event to prepare
     */
    private void prepareEvent(Event event, XWikiContext context)
    {
        if (event.getUser() == null) {
            event.setUser(documentAccessBridge.getCurrentUserReference());
        }

        if (event.getWiki() == null) {
            event.setWiki(new WikiReference(wikiDescriptorManager.getCurrentWikiId()));
        }

        if (event.getApplication() == null) {
            event.setApplication("xwiki");
        }

        if (event.getDate() == null) {
            event.setDate(new Date());
        }

        if (event.getId() == null) {
            event.setId(generateEventId(event, context));
        }

        if (event.getGroupId() == null) {
            event.setGroupId((String) context.get(GROUP_ID_CONTEXT_KEY));
        }
    }

    /**
     * Generate event ID for the given ID. Note that this method does not perform the set of the ID in the event object.
     *
     * @param event event to generate the ID for
     * @param context the XWiki context
     * @return the generated ID
     */
    private String generateEventId(Event event, XWikiContext context)
    {
        final String key = String.format("%s-%s-%s-%s", event.getStream(), event.getApplication(),
                serializer.serialize(event.getDocument()), event.getType());
        long hash = key.hashCode();
        if (hash < 0) {
            hash = -hash;
        }

        final String id = String.format("%d-%d-%s", hash, event.getDate().getTime(),
                RandomStringUtils.randomAlphanumeric(8));
        if (context.get(GROUP_ID_CONTEXT_KEY) == null) {
            context.put(GROUP_ID_CONTEXT_KEY, id);
        }

        return id;
    }

    private void saveLegacyEvent(LegacyEvent event, String wikiId) throws Exception
    {
        namespaceContextExecutor.execute(new WikiNamespace(wikiId),
            () -> {
                XWikiContext context = contextProvider.get();
                XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();
                try {
                    hibernateStore.beginTransaction(context);
                    Session session = hibernateStore.getSession(context);
                    session.save(event);
                    hibernateStore.endTransaction(context, true);
                } catch (XWikiException e) {
                    hibernateStore.endTransaction(context, false);
                }

                return null;
            }
        );
    }
}

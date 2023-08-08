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
package org.xwiki.mail.internal.configuration;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.mail.GeneralMailConfigurationUpdatedEvent;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Event generator for {@link GeneralMailConfigurationUpdatedEvent}.
 *
 * @since 14.10.15
 * @since 15.5.2
 * @since 15.7RC1
 * @version $Id$
 */
@Component
@Named(GeneralMailConfigurationUpdatedEventGenerator.NAME)
@Singleton
public class GeneralMailConfigurationUpdatedEventGenerator implements EventListener
{
    static final String NAME = "generalmailconfigurationchangedeventgenerator";

    @Inject
    protected EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private Provider<ObservationManager> observationManagerProvider;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("mailgeneral")
    private ConfigurationSource currentWikiMailConfigSource;

    @Inject
    @Named("mailgeneralmainwiki")
    private ConfigurationSource mainWikiMailConfigSource;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<Event> getEvents()
    {
        String serializedClassReference = this.referenceSerializer.serialize(
            AbstractGeneralMailConfigClassDocumentConfigurationSource.GENERAL_MAILCONFIGCLASS_REFERENCE);
        RegexEntityReference filter = BaseObjectReference.any(serializedClassReference);

        return List.of(new XObjectAddedEvent(filter), new XObjectDeletedEvent(filter), new XObjectUpdatedEvent(filter));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ObservationManager observationManager = this.observationManagerProvider.get();

        if (source instanceof XWikiDocument) {
            XWikiDocument document = (XWikiDocument) source;

            // Test that the document is really a mail configuration document.
            LocalDocumentReference localDocumentReference = new LocalDocumentReference(document.getDocumentReference());
            if (!AbstractMailConfigClassDocumentConfigurationSource.MAILCONFIG_REFERENCE
                .equals(localDocumentReference))
            {
                return;
            }

            // Clear the cache of the current wiki mail config source to ensure that any listener of the new events will
            // see the new configuration value regardless of which listener is called first.
            clearCache(this.currentWikiMailConfigSource);

            // Get the wiki id from the document reference. If it is the main wiki, notify without wiki, otherwise
            // notify with the wiki id.
            String wikiId = document.getDocumentReference().getWikiReference().getName();
            if (this.contextProvider.get().isMainWiki(wikiId)) {
                // Clear the cache of the main wiki mail config source to ensure that any listener of the events will
                // see the new configuration value regardless of which listener is called first.
                clearCache(this.mainWikiMailConfigSource);
                observationManager.notify(new GeneralMailConfigurationUpdatedEvent(), null);
            } else {
                observationManager.notify(new GeneralMailConfigurationUpdatedEvent(wikiId), wikiId);
            }
        }
    }

    private void clearCache(ConfigurationSource mainWikiMailConfigSource)
    {
        if (mainWikiMailConfigSource instanceof AbstractGeneralMailConfigClassDocumentConfigurationSource) {
            ((AbstractGeneralMailConfigClassDocumentConfigurationSource) mainWikiMailConfigSource).clearCache();
        }
    }
}

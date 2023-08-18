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
package org.xwiki.annotation.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.RegexEventFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * When the wiki is initialized or the configuration document is updated, make sure that the configured annotation class
 * contains the minimum required properties for the Annotation Application to function properly.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named(CheckAnnotationClassEventListener.NAME)
@Singleton
public class CheckAnnotationClassEventListener implements EventListener
{
    static final String NAME = "CheckAnnotationClassEventListener";

    /**
     * The reference to match document the Annotations Application's configuration document on whatever wiki.
     */
    private static final RegexEventFilter CONFIGURATION_DOCUMENT_REFERENCE = new RegexEventFilter(String.format(
        ".*:%s.%s", AnnotationConfiguration.CONFIGURATION_PAGE_SPACE_NAME,
        AnnotationConfiguration.CONFIGURATION_PAGE_NAME));

    /**
     * The matched events.
     */
    private static final List<Event> EVENTS = Arrays.asList(new DocumentUpdatedEvent(
        CONFIGURATION_DOCUMENT_REFERENCE), new DocumentCreatedEvent(CONFIGURATION_DOCUMENT_REFERENCE));

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    /**
     * The Annotation Application's configuration.
     */
    @Inject
    protected AnnotationConfiguration configuration;

    @Inject
    @Named(AnnotationClassDocumentInitializer.HINT)
    protected MandatoryDocumentInitializer initializer;

    @Inject
    protected Logger logger;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentReference annotationClassReference = this.configuration.getAnnotationClassReference();

        try {
            if (!this.configuration.isInstalled()) {
                // If the Annotations Application is not installed on the current wiki, do nothing.
                return;
            }

            XWikiContext deprecatedContext = this.xcontextProvider.get();
            XWikiDocument annotationClassDocument =
                deprecatedContext.getWiki().getDocument(annotationClassReference, deprecatedContext);

            if (this.initializer.updateDocument(annotationClassDocument)) {
                deprecatedContext.getWiki().saveDocument(annotationClassDocument,
                    "Automatically added missing annotation class fields required by the Annotation Application.",
                    deprecatedContext);
            }
        } catch (Exception e) {
            this.logger.error("Failed to update the configured annotation class [{}]", annotationClassReference, e);
        }
    }
}

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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.annotation.AnnotationServiceException;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Role;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.RegexEventFilter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * When the wiki is initialized or the configuration document is updated, make sure that the configured annotation class
 * contains the minimum required properties for the Annotation Application to function properly.
 * 
 * @version $Id$
 * @since 4.0M2
 */
@Role
@Named("CheckAnnotationClassEventListener")
@Singleton
public class CheckAnnotationClassEventListener implements EventListener
{
    /**
     * The reference to match document the Annotations Application's configuration document on whatever wiki.
     */
    private static final RegexEventFilter CONFIGURATION_DOCUMENT_REFERENCE = new RegexEventFilter(String.format(
        ".*:%s.%s", AnnotationConfiguration.CONFIGURATION_PAGE_SPACE_NAME,
        AnnotationConfiguration.CONFIGURATION_PAGE_NAME));

    /**
     * The matched events.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new ApplicationReadyEvent(),
        new DocumentUpdatedEvent(CONFIGURATION_DOCUMENT_REFERENCE), new DocumentCreatedEvent(
            CONFIGURATION_DOCUMENT_REFERENCE));

    /**
     * Logging framework.
     */
    @Inject
    protected Logger logger;

    /**
     * The execution context needed to perform operations on wiki pages.
     */
    @Inject
    protected Execution execution;

    /**
     * The Annotation Application's configuration.
     */
    @Inject
    protected AnnotationConfiguration configuration;

    /**
     * Reference serializer used in logging.
     */
    @Inject
    protected EntityReferenceSerializer<String> serializer;

    @Override
    public String getName()
    {
        return this.getClass().getAnnotation(Named.class).value();
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        try {
            XWikiContext deprecatedContext = getXWikiContext();
            XWiki xwiki = deprecatedContext.getWiki();

            if (xwiki.isVirtualMode() && event instanceof ApplicationReadyEvent) {
                // Application started. Need to go trough all subwikis and ensure validity.
                String currentDatabase = deprecatedContext.getDatabase();

                for (String wikiName : xwiki.getVirtualWikisDatabaseNames(deprecatedContext)) {
                    // Change the context wiki.
                    deprecatedContext.setDatabase(wikiName);
                    // Do the work for the wiki.
                    ensureAnnotationClassIsValid();
                }

                // Restore the original wiki.
                deprecatedContext.setDatabase(currentDatabase);
            } else {
                // Config document of the current wiki got modified.
                ensureAnnotationClassIsValid();
            }
        } catch (Exception e) {
            logger.error("Failed to update the configured annotation class for wiki [{}] on event [{}]", new Object[] {
                getXWikiContext().getDatabase(), event.getClass().getSimpleName(), e});
        }
    }

    /**
     * Ensures that the configured annotation class contains the minimum properties required by the Annotation
     * Application to work properly. This method also checks if the Annotations application is installed.
     * 
     * @throws AnnotationServiceException if anything goes wrong while updating the configured annotation class.
     * @see Annotation
     */
    private void ensureAnnotationClassIsValid() throws AnnotationServiceException
    {
        DocumentReference annotationClassReference = null;

        try {
            if (!configuration.isInstalled()) {
                // If the Annotations Application is not installed on the current wiki, do nothing.
                return;
            }

            annotationClassReference = configuration.getAnnotationClassReference();

            XWikiContext deprecatedContext = getXWikiContext();
            XWikiDocument annotationClassDocument =
                deprecatedContext.getWiki().getDocument(annotationClassReference, deprecatedContext);
            BaseClass annotationClass = annotationClassDocument.getXClass();

            boolean needsUpdate = false;

            needsUpdate |= annotationClass.addTextField(Annotation.AUTHOR_FIELD, "Author", 30);
            needsUpdate |= annotationClass.addDateField(Annotation.DATE_FIELD, "Date");

            needsUpdate |= annotationClass.addTextAreaField(Annotation.SELECTION_FIELD, "Selection", 40, 5);
            needsUpdate |=
                annotationClass.addTextAreaField(Annotation.SELECTION_LEFT_CONTEXT_FIELD, "Selection Left Context", 40,
                    5);
            needsUpdate |=
                annotationClass.addTextAreaField(Annotation.SELECTION_RIGHT_CONTEXT_FIELD, "Selection Right Context",
                    40, 5);
            needsUpdate |=
                annotationClass.addTextAreaField(Annotation.ORIGINAL_SELECTION_FIELD, "Original Selection", 40, 5);
            needsUpdate |= annotationClass.addTextField(Annotation.TARGET_FIELD, "Target", 30);
            needsUpdate |= annotationClass.addTextField(Annotation.STATE_FIELD, "State", 30);

            if (needsUpdate) {
                deprecatedContext.getWiki().saveDocument(annotationClassDocument,
                    "Automatically added missing annotation class fields required by the Annotation Application.",
                    deprecatedContext);
            }
        } catch (Exception e) {
            throw new AnnotationServiceException(String.format(
                "Failed to update the configured annotation class [%s:%s.%s]",
                serializer.serialize(annotationClassReference, (Object[]) null)), e);
        }
    }

    /**
     * @return the deprecated xwiki context
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }
}

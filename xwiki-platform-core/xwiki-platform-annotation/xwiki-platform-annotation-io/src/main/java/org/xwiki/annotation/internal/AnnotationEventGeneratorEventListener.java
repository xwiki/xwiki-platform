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
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.annotation.event.AnnotationAddedEvent;
import org.xwiki.annotation.event.AnnotationDeletedEvent;
import org.xwiki.annotation.event.AnnotationEvent;
import org.xwiki.annotation.event.AnnotationUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Listens to object events of the same class as the currently configured annotation class and launches corresponding
 * {@link AnnotationEvent}s.
 *
 * @version $Id$
 * @since 4.0RC1
 */
@Component
@Named("AnnotationEventGeneratorEventListener")
@Singleton
public class AnnotationEventGeneratorEventListener implements EventListener
{
    /**
     * The matched events.
     */
    private static final List<Event> EVENTS = Arrays.asList(new XObjectAddedEvent(), new XObjectUpdatedEvent(),
        new XObjectDeletedEvent());

    /**
     * Used to check if the Annotations Application is installed on the current wiki.
     */
    @Inject
    protected AnnotationConfiguration annotationConfiguration;

    /**
     * Used to launch annotation events. Lazy loaded to avoid cycles.
     */
    @Inject
    protected Provider<ObservationManager> observationManager;

    /**
     * Execution context used to get the current wiki.
     */
    @Inject
    protected Execution execution;

    /**
     * Used to serializer document.
     */
    @Inject
    protected EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * Logging framework.
     */
    @Inject
    protected Logger logger;

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
        // Don`t rely on the context from the data parameter.
        XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        String currentWiki = context.getWikiId();

        try {
            XWikiDocument document = (XWikiDocument) source;
            String wikiOfAffectedDocument = document.getDocumentReference().getWikiReference().getName();

            // Always work on the wiki of the source document. The Annotation Application's configuration looks at the
            // context to provide values for the current wiki. Objects could be modified cross-wiki and the context
            // database might not be right.
            context.setWikiId(wikiOfAffectedDocument);

            // Only work if the Annotations Application is installed on the wiki.
            if (!this.annotationConfiguration.isInstalled()) {
                return;
            }

            // Extract the BaseObjectReference to be able to inspect the XClassReference.
            BaseObjectReference objectReference = getBaseObjectReference((XObjectEvent) event);
            DocumentReference objectClassReference = objectReference.getXClassReference();

            // Only interested in objects that are of the same class as the currently configured annotation class.
            if (!objectClassReference.equals(this.annotationConfiguration.getAnnotationClassReference())) {
                return;
            }

            // The object is needed for the final check. See below.
            BaseObject object = document.getXObject(objectReference);

            // Build the new event to launch using the current document reference and object number.
            Event newEvent = null;
            String documentReference = this.defaultEntityReferenceSerializer.serialize(document.getDocumentReference());
            String number = String.valueOf(objectReference.getObjectNumber());
            if (event instanceof XObjectAddedEvent) {
                newEvent = new AnnotationAddedEvent(documentReference, number);
            } else if (event instanceof XObjectUpdatedEvent) {
                newEvent = new AnnotationUpdatedEvent(documentReference, number);
            } else if (event instanceof XObjectDeletedEvent) {
                // Current document might be deleted. Always use the original document for *Deleted events.
                object = document.getOriginalDocument().getXObject(objectReference);
                newEvent = new AnnotationDeletedEvent(documentReference, number);
            }

            // Handle specially the default annotations class which coincides with the default comments class. We need
            // to avoid mistaking comments for annotations.
            DocumentReference defaultCommentsClassReference =
                context.getWiki().getCommentsClass(context).getDocumentReference();
            if (defaultCommentsClassReference.equals(object.getXClassReference())) {
                // A comment is considered an annotation when it has a text selection.
                String selection = object.getStringValue(Annotation.SELECTION_FIELD);
                if (selection == null || selection.trim().length() == 0) {
                    // This is a simple comment. Skip it.
                    return;
                }
            }

            // Launch the new event.
            this.observationManager.get().notify(newEvent, source, context);
        } catch (Exception e) {
            this.logger.error("Failed to handle event of type [{}]", event.getClass().getName(), e);
        } finally {
            // Restore the context database.
            context.setWikiId(currentWiki);
        }
    }

    /**
     * @param objectEvent the event involving an object
     * @return the {@link BaseObjectReference} of the object corresponding to the object event
     */
    private BaseObjectReference getBaseObjectReference(XObjectEvent objectEvent)
    {
        EntityReference objectReference = objectEvent.getReference();
        BaseObjectReference baseObjectReference = null;
        if (objectReference instanceof BaseObjectReference) {
            baseObjectReference = (BaseObjectReference) objectEvent.getReference();
        } else {
            baseObjectReference = new BaseObjectReference(objectEvent.getReference());
        }
        return baseObjectReference;
    }
}

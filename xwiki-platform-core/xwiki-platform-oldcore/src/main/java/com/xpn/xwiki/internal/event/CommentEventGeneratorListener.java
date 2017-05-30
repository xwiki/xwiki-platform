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
package com.xpn.xwiki.internal.event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiCommentsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Produce comments related events based on {@link XObjectEvent object events}.
 *
 * @version $Id$
 * @since 3.2M1
 */
@Component
@Singleton
@Named("CommentEventGeneratorListener")
public class CommentEventGeneratorListener extends AbstractEventListener
{
    /**
     * The reference to match class XWiki.Comment on whatever wiki.
     */
    private static final RegexEntityReference COMMENTCLASS_REFERENCE =
        XWikiCommentsDocumentInitializer.OBJECT_REFERENCE;

    /**
     * Used to serializer document.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    private ObservationManager observation;

    /**
     * Default constructor.
     */
    public CommentEventGeneratorListener()
    {
        super("CommentEventGeneratorListener", new XObjectAddedEvent(COMMENTCLASS_REFERENCE),
            new XObjectDeletedEvent(COMMENTCLASS_REFERENCE), new XObjectUpdatedEvent(COMMENTCLASS_REFERENCE));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        XWikiDocument originalDoc = doc.getOriginalDocument();
        XWikiContext context = (XWikiContext) data;

        XObjectEvent objectEvent = (XObjectEvent) event;

        String reference = this.defaultEntityReferenceSerializer.serialize(doc.getDocumentReference());

        if (event instanceof XObjectDeletedEvent) {
            BaseObject obj = originalDoc.getXObject((ObjectReference) objectEvent.getReference());
            String number = String.valueOf(obj.getNumber());
            this.observation.notify(new CommentDeletedEvent(reference, number), source, context);
        } else {
            BaseObject obj = doc.getXObject((ObjectReference) objectEvent.getReference());
            String number = String.valueOf(obj.getNumber());
            if (event instanceof XObjectAddedEvent) {
                this.observation.notify(new CommentAddedEvent(reference, number), source, context);
            } else {
                this.observation.notify(new CommentUpdatedEvent(reference, number), source, context);
            }
        }
    }
}

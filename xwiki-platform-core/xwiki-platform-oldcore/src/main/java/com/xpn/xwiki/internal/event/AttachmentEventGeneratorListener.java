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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.AttachmentDiff;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

/**
 * Produce attachment events based on document events.
 * 
 * @version $Id$
 * @since 3.2M1
 */
@Component
@Singleton
@Named("AttachmentEventGeneratorListener")
public class AttachmentEventGeneratorListener implements EventListener
{
    /**
     * The events to match.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new DocumentDeletedEvent(),
        new DocumentCreatedEvent(), new DocumentUpdatedEvent());

    /**
     * Used to serializer document.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Override
    public String getName()
    {
        return "AttachmentEventGeneratorListener";
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        XWikiDocument originalDoc = doc.getOriginalDocument();
        XWikiContext context = (XWikiContext) data;

        if (event instanceof DocumentUpdatedEvent) {
            onDocumentUpdatedEvent(originalDoc, doc, context);
        } else if (event instanceof DocumentDeletedEvent) {
            onDocumentDeletedEvent(originalDoc, doc, context);
        } else if (event instanceof DocumentCreatedEvent) {
            onDocumentCreatedEvent(originalDoc, doc, context);
        }
    }

    /**
     * @param originalDoc the previous version of the document
     * @param doc the new version of the document
     * @param context the XWiki context
     */
    private void onDocumentCreatedEvent(XWikiDocument originalDoc, XWikiDocument doc, XWikiContext context)
    {
        ObservationManager observation = Utils.getComponent(ObservationManager.class);

        for (XWikiAttachment attachment : doc.getAttachmentList()) {
            String reference =
                this.defaultEntityReferenceSerializer.serialize(attachment.getDoc().getDocumentReference());
            observation.notify(new AttachmentAddedEvent(reference, attachment.getFilename()), doc, context);
        }
    }

    /**
     * @param originalDoc the previous version of the document
     * @param doc the new version of the document
     * @param context the XWiki context
     */
    private void onDocumentDeletedEvent(XWikiDocument originalDoc, XWikiDocument doc, XWikiContext context)
    {
        ObservationManager observation = Utils.getComponent(ObservationManager.class);

        for (XWikiAttachment attachment : originalDoc.getAttachmentList()) {
            String reference =
                this.defaultEntityReferenceSerializer.serialize(attachment.getDoc().getDocumentReference());
            observation.notify(new AttachmentDeletedEvent(reference, attachment.getFilename()), doc, context);
        }
    }

    /**
     * @param originalDoc the previous version of the document
     * @param doc the new version of the document
     * @param context the XWiki context
     */
    private void onDocumentUpdatedEvent(XWikiDocument originalDoc, XWikiDocument doc, XWikiContext context)
    {
        ObservationManager observation = Utils.getComponent(ObservationManager.class);

        String reference = this.defaultEntityReferenceSerializer.serialize(doc.getDocumentReference());

        for (AttachmentDiff diff : doc.getAttachmentDiff(originalDoc, doc, context)) {
            if (StringUtils.isEmpty(diff.getOrigVersion())) {
                observation.notify(new AttachmentAddedEvent(reference, diff.getFileName()), doc, context);
            } else if (StringUtils.isEmpty(diff.getNewVersion())) {
                observation.notify(new AttachmentDeletedEvent(reference, diff.getFileName()), doc, context);
            } else {
                observation.notify(new AttachmentUpdatedEvent(reference, diff.getFileName()), doc, context);
            }
        }
    }
}

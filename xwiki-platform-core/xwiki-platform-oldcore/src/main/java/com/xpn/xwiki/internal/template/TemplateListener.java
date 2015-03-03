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
package com.xpn.xwiki.internal.template;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.template.event.TemplateDeletedEvent;
import org.xwiki.template.event.TemplateUpdatedEvent;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.AbstractAttachmentEvent;
import com.xpn.xwiki.internal.event.AttachmentDeletedEvent;
import com.xpn.xwiki.internal.event.AttachmentUpdatedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.internal.skin.WikiSkinUtils;

/**
 * Listener to modification to wiki based template and generate related {@link org.xwiki.template.event.TemplateEvent}s.
 *
 * @version $Id$
 */
@Component
@Named("templates")
@Singleton
// TODO: add support for TemplateAddedEvent
public class TemplateListener extends AbstractEventListener
{
    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private ObservationManager observation;

    /**
     * Default constructor.
     */
    public TemplateListener()
    {
        super("templates", new XObjectPropertyUpdatedEvent(), new XObjectPropertyDeletedEvent(),
            new AttachmentDeletedEvent(), new AttachmentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        // Is this a skin document
        if (document.getXObject(WikiSkinUtils.SKINCLASS_REFERENCE) != null) {
            if (event instanceof AbstractAttachmentEvent) {
                XWikiAttachment attachment = document.getAttachment(((AbstractAttachmentEvent) event).getName());
                String id = this.referenceSerializer.serialize(attachment.getReference());
                if (event instanceof AttachmentDeletedEvent) {
                    this.observation.notify(new TemplateDeletedEvent(id), this);
                } else if (event instanceof AttachmentUpdatedEvent) {
                    this.observation.notify(new TemplateUpdatedEvent(id), this);
                }
            } else if (event instanceof XObjectPropertyEvent) {
                String id = this.referenceSerializer.serialize(((XObjectPropertyEvent) event).getReference());
                if (event instanceof XObjectPropertyDeletedEvent) {
                    this.observation.notify(new TemplateDeletedEvent(id), this);
                } else if (event instanceof XObjectPropertyUpdatedEvent) {
                    this.observation.notify(new TemplateUpdatedEvent(id), this);
                }
            }
        }
    }
}

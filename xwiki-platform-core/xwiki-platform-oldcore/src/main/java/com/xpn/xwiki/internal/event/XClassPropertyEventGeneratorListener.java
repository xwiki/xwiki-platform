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

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.web.Utils;

/**
 * Produce {@link XClassPropertyEvent} based on document events.
 * 
 * @version $Id$
 * @since 3.2M1
 */
@Component
@Named("XObjectModificationsListener")
public class XClassPropertyEventGeneratorListener implements EventListener
{
    private static final List<Event> EVENTS = Arrays.<Event> asList(new DocumentDeletedEvent(),
        new DocumentCreatedEvent(), new DocumentUpdatedEvent());

    @Inject
    private Logger logger;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return "XObjectModificationsListener";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        XWikiDocument originalDoc = doc.getOriginalDocument();
        XWikiContext context = (XWikiContext) data;

        ObservationManager observation = Utils.getComponent(ObservationManager.class);

        try {
            for (List<ObjectDiff> objectChanges : doc.getObjectDiff(originalDoc, doc, context)) {
                boolean modified = false;
                for (ObjectDiff diff : objectChanges) {
                    BaseObject xobject = doc.getXObject(diff.getXClassReference(), diff.getNumber());
                    BaseObject xobjectOriginal = originalDoc.getXObject(diff.getXClassReference(), diff.getNumber());
                    if (ObjectDiff.ACTION_OBJECTREMOVED.equals(diff.getAction())) {
                        observation.notify(
                            new XObjectDeletedEvent(xobjectOriginal.getReference()),
                            new XObjectEventData(doc, originalDoc.getXObject(diff.getXClassReference(),
                                diff.getNumber())), context);
                    } else {
                        if (ObjectDiff.ACTION_OBJECTADDED.equals(diff.getAction())) {
                            observation.notify(new XObjectAddedEvent(xobject.getReference()), new XObjectEventData(doc,
                                xobject), context);
                        } else {
                            if (!modified) {
                                observation.notify(new XObjectUpdatedEvent(xobject.getReference()),
                                    new XObjectEventData(doc, xobject), context);
                                modified = true;
                            }

                            onObjectPropertyModified(observation, originalDoc, diff, context);
                        }
                    }
                }
            }
        } catch (XWikiException e) {
            this.logger.error("Failed to diff documents [" + originalDoc + "] and [" + doc + "]");
        }
    }

    private void onObjectPropertyModified(ObservationManager observation, XWikiDocument doc, ObjectDiff diff,
        XWikiContext context)
    {
        if (ObjectDiff.ACTION_PROPERTYREMOVED.equals(diff.getAction())) {
            BaseObject object = doc.getOriginalDocument().getXObject(diff.getXClassReference(), diff.getNumber());
            BaseProperty<ObjectPropertyReference> property =
                (BaseProperty<ObjectPropertyReference>) object.getField(diff.getPropName());
            observation.notify(new XObjectPropertyDeletedEvent(property.getReference()), new XObjectPropertyEventData(
                doc, object, property), context);
        } else {
            BaseObject object = doc.getXObject(diff.getXClassReference(), diff.getNumber());
            BaseProperty<ObjectPropertyReference> property =
                (BaseProperty<ObjectPropertyReference>) object.getField(diff.getPropName());
            if (ObjectDiff.ACTION_PROPERTYADDED.equals(diff.getAction())) {
                observation.notify(new XObjectPropertyAddedEvent(property.getReference()),
                    new XObjectPropertyEventData(doc, object, property), context);
            } else if (ObjectDiff.ACTION_PROPERTYCHANGED.equals(diff.getAction())) {
                observation.notify(new XObjectPropertyUpdatedEvent(property.getReference()),
                    new XObjectPropertyEventData(doc, object, property), context);
            }
        }
    }
}

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
import java.util.Collection;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

/**
 * Produce {@link XClassPropertyEvent property events} based on document events.
 * 
 * @version $Id$
 * @since 3.2M1
 */
@Component
@Singleton
@Named("XClassPropertyEventGeneratorListener")
public class XClassPropertyEventGeneratorListener implements EventListener
{
    /**
     * The events to match.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new DocumentDeletedEvent(),
        new DocumentCreatedEvent(), new DocumentUpdatedEvent());

    @Override
    public String getName()
    {
        return "XClassPropertyEventGeneratorListener";
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

        for (PropertyInterface property : (Collection<PropertyInterface>) doc.getXClass().getFieldList()) {
            observation.notify(new XClassPropertyAddedEvent(property.getReference()), doc, context);
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

        for (PropertyInterface property : (Collection<PropertyInterface>) originalDoc.getXClass().getFieldList()) {
            observation.notify(new XClassPropertyDeletedEvent(property.getReference()), doc, context);
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

        BaseClass baseClass = doc.getXClass();
        BaseClass baseClassOriginal = originalDoc.getXClass();

        for (List<ObjectDiff> objectChanges : doc.getClassDiff(originalDoc, doc, context)) {
            for (ObjectDiff diff : objectChanges) {
                PropertyInterface property = baseClass.getField(diff.getPropName());
                PropertyInterface propertyOriginal = baseClassOriginal.getField(diff.getPropName());

                if (ObjectDiff.ACTION_PROPERTYREMOVED.equals(diff.getAction())) {
                    observation.notify(new XClassPropertyDeletedEvent(propertyOriginal.getReference()), doc, context);
                } else if (ObjectDiff.ACTION_PROPERTYADDED.equals(diff.getAction())) {
                    observation.notify(new XClassPropertyAddedEvent(property.getReference()), doc, context);
                } else if (ObjectDiff.ACTION_PROPERTYCHANGED.equals(diff.getAction())) {
                    observation.notify(new XClassPropertyUpdatedEvent(property.getReference()), doc, context);
                }
            }
        }
    }
}

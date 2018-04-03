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
package com.xpn.xwiki.internal.observation.remote.converter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;

/**
 * Convert all document event to remote events and back to local events.
 * <p>
 * It also make sure the context contains the proper information like the user or the wiki.
 *
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Singleton
@Named("document")
public class DocumentEventConverter extends AbstractXWikiEventConverter
{
    /**
     * The events supported by this converter.
     */
    private static final Set<Class<? extends Event>> EVENTS = new HashSet<Class<? extends Event>>()
    {
        {
            add(DocumentDeletedEvent.class);
            add(DocumentCreatedEvent.class);
            add(DocumentUpdatedEvent.class);
        }
    };

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (EVENTS.contains(localEvent.getEvent().getClass())) {
            // fill the remote event
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            remoteEvent.setSource(serializeXWikiDocument((XWikiDocument) localEvent.getSource()));
            remoteEvent.setData(serializeXWikiContext((XWikiContext) localEvent.getData()));

            return true;
        }

        return false;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (EVENTS.contains(remoteEvent.getEvent().getClass())) {
            // fill the local event
            XWikiContext xcontext = unserializeXWikiContext(remoteEvent.getData());

            try {
                if (xcontext != null) {
                    localEvent.setData(xcontext);
                    localEvent.setEvent((Event) remoteEvent.getEvent());

                    if (remoteEvent.getEvent() instanceof DocumentDeletedEvent) {
                        localEvent.setSource(unserializeDeletdDocument(remoteEvent.getSource(), xcontext));
                    } else {
                        localEvent.setSource(unserializeDocument(remoteEvent.getSource()));
                    }
                }
            } catch (XWikiException e) {
                this.logger.error("Failed to convert remote event [{}]", remoteEvent, e);
            }

            return true;
        }

        return false;
    }

    private XWikiDocument unserializeDeletdDocument(Serializable remoteData, XWikiContext xcontext)
        throws XWikiException
    {
        Map<String, Serializable> remoteDataMap = (Map<String, Serializable>) remoteData;

        DocumentReference docReference = (DocumentReference) remoteDataMap.get(DOC_NAME);
        Locale locale = LocaleUtils.toLocale((String) remoteDataMap.get(DOC_LANGUAGE));

        XWikiDocument doc = new XWikiDocument(docReference, locale);

        XWikiDocument origDoc = new XWikiDocument(docReference, locale);

        // We have to get deleted document from the trash (hoping it is in the trash...)
        XWiki xwiki = xcontext.getWiki();
        XWikiRecycleBinStoreInterface store = xwiki.getRecycleBinStore();
        XWikiDeletedDocument[] deletedDocuments = store.getAllDeletedDocuments(origDoc, xcontext, true);
        if (deletedDocuments != null && deletedDocuments.length > 0) {
            long index = deletedDocuments[0].getId();
            try {
                origDoc = store.restoreFromRecycleBin(index, xcontext, true);
            } catch (Exception e) {
                // The deleted document can be found in the database but there is an issue with the content
                // Better a partial notification than no notification at all (what most listener care about if the
                // reference of the deleted document)
                this.logger.error("Failed to restore deleted document [{}]", docReference);
            }
        }

        doc.setOriginalDocument(origDoc);

        return doc;
    }
}

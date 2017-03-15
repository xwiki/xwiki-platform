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
package com.xpn.xwiki.plugin.activitystream.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.AllRecordableEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.eventstream.TargetableEvent;
import org.xwiki.eventstream.internal.DefaultEvent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default converter for any type of RecordableEvent.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
public class DefaultRecordableEventConverter implements RecordableEventConverter
{
    private static final List<RecordableEvent> EVENTS = Arrays.asList(AllRecordableEvent.ALL_RECORDABLE_EVENT);

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        XWikiContext context = contextProvider.get();

        org.xwiki.eventstream.Event convertedEvent = new DefaultEvent();
        convertedEvent.setType(recordableEvent.getClass().getCanonicalName());
        convertedEvent.setApplication(source);
        convertedEvent.setDate(new Date());
        convertedEvent.setUser(context.getUserReference());
        convertedEvent.setWiki(context.getWikiReference());
        if (recordableEvent instanceof TargetableEvent) {
            convertedEvent.setTarget(((TargetableEvent)recordableEvent).getTarget());
        }

        if (data instanceof String) {
            convertedEvent.setBody((String) data);
        } else if (data instanceof XWikiDocument) {
            XWikiDocument document = (XWikiDocument) data;
            convertedEvent.setDocument(document.getDocumentReference());
            convertedEvent.setDocumentVersion(document.getVersion());
            convertedEvent.setDocumentTitle(document.getRenderedTitle(context));
        }
        return convertedEvent;
    }

    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return EVENTS;
    }
}

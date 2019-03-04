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
package org.xwiki.eventstream.internal;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.eventstream.TargetableEvent;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Default converter for any type of RecordableEvent.
 *
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Singleton
public class DefaultRecordableEventConverter implements RecordableEventConverter
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        XWikiContext context = contextProvider.get();

        Event convertedEvent = new DefaultEvent();
        convertedEvent.setType(recordableEvent.getClass().getCanonicalName());
        convertedEvent.setApplication(source);
        convertedEvent.setDate(new Date());
        convertedEvent.setUser(context.getUserReference());
        convertedEvent.setWiki(context.getWikiReference());
        if (recordableEvent instanceof TargetableEvent) {
            convertedEvent.setTarget(((TargetableEvent) recordableEvent).getTarget());
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
        // The default recordable event converter is called manually as a fallback when no converter matches the event,
        // se we don't need to specify a list.
        //
        // We could have defined a AllRecordableEvent class just like org.xwiki.observation.event.AllEvent, but it would
        // not have worked as expected because it would require some special code in
        // org.xwiki.observation.ObservationManager to handle it and xwiki-commons does not know anything about the
        // RecordableEvent interface defined in xwiki-platform.
        //
        return Collections.emptyList();
    }
}

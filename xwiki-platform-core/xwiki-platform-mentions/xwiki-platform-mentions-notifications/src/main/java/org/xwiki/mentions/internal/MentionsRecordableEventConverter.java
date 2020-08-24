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
package org.xwiki.mentions.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.events.MentionEvent;
import org.xwiki.mentions.events.MentionEventParams;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.mentions.events.MentionEvent.EVENT_TYPE;

/**
 * Define the conversion from an {@link MentionEvent} to a {@link org.xwiki.eventstream.internal.DefaultEvent}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
@Named("mentions")
public class MentionsRecordableEventConverter implements RecordableEventConverter
{
    /**
     * Key of the parameter where the mentions specific values are put.
     */
    public static final String MENTIONS_PARAMETER_KEY = "mentions";

    @Inject
    private RecordableEventConverter defaultConverter;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private MentionsNotificationsObjectMapper objectMapper;

    @Inject
    private Logger logger;

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data)
    {
        // This code is called once when creating the notification in db 
        MentionEvent mentionEvent = (MentionEvent) recordableEvent;
        MentionEventParams params = mentionEvent.getParams();

        DocumentReference userDocument = this.documentReferenceResolver.resolve(params.getUserReference());
        DocumentReference document = this.documentReferenceResolver.resolve(params.getDocumentReference());

        // additional information needed later for rendering mentions notification are stored
        // in a MentionEvent object and serialized to json.
        // This object is unserialized when needed for the rendering.
        Optional<String> serialize = this.objectMapper.serialize(mentionEvent.getParams());
        return serialize.map(json -> {
            try {
                Event convertedEvent = this.defaultConverter.convert(recordableEvent, source, data);

                convertedEvent.setUser(userDocument);
                convertedEvent.setDocument(document);
                convertedEvent.setType(EVENT_TYPE);
                convertedEvent.setWiki(document.getWikiReference());
                Map<String, String> parameters = initializeParameters(json);
                convertedEvent.setParameters(parameters);
                return convertedEvent;
            } catch (Exception e) {
                this.logger.warn("Failed to convert the recordable event [{}]. Cause [{}].", recordableEvent,
                    getRootCauseMessage(e));
                return null;
            }
        }).orElse(null);
    }

    private Map<String, String> initializeParameters(String value)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(MENTIONS_PARAMETER_KEY, value);
        return parameters;
    }


    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return singletonList(new MentionEvent(null, null));
    }
}

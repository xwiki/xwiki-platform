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
package org.xwiki.platform.mentions.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.platform.mentions.events.MentionEvent;
import org.xwiki.platform.mentions.events.MentionEventParams;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link MentionsRecordableEventConverter}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@ComponentTest
public class MentionsRecordableEventConverterTest
{
    @InjectMockComponents
    private MentionsRecordableEventConverter converter;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private RecordableEventConverter defaultConverter;

    @MockComponent
    private MentionsNotificationsObjectMapper objectMapper;

    @Test
    void convert() throws Exception
    {
        MentionEventParams params = new MentionEventParams()
                                        .setDocumentReference("xwiki:XWiki.Doc")
                                        .setUserReference("xwiki:XWiki.U1");
        MentionEvent recordableEvent = new MentionEvent(new HashSet<>(), params);

        DocumentReference document = new DocumentReference("xwiki", "XWiki", "Doc");
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Doc"))
            .thenReturn(document);
        DocumentReference userDocument = new DocumentReference("xwiki", "XWiki", "U1");
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.U1"))
            .thenReturn(userDocument);
        Event event = mock(Event.class);
        when(this.defaultConverter.convert(recordableEvent, "aa", null)).thenReturn(event);

        when(this.objectMapper.serialize(params)).thenReturn(
            Optional.of("{\"userReference\":\"xwiki:XWiki.U1\",\"documentReference\":\"xwiki:XWiki.Doc\"}"));

        Event actual = this.converter.convert(recordableEvent, "aa", null);
        assertNotNull(actual);
        verify(event).setUser(userDocument);
        verify(event).setDocument(document);
        verify(event).setType(MentionEvent.EVENT_TYPE);
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(MentionsRecordableEventConverter.MENTIONS_PARAMETER_KEY,
            "{\"userReference\":\"xwiki:XWiki.U1\",\"documentReference\":\"xwiki:XWiki.Doc\"}");
        verify(event).setParameters(parameters);
    }

    @Test
    void getSupportedEvents()
    {
        assertEquals(singletonList(new MentionEvent(null, null)), this.converter.getSupportedEvents());
    }
}
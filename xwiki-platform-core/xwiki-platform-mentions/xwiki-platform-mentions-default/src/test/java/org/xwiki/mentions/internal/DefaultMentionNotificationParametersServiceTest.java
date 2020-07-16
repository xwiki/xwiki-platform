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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.MentionsConfiguration;
import org.xwiki.mentions.events.MentionEvent;
import org.xwiki.mentions.events.MentionEventParams;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultMentionNotificationService}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@ComponentTest
public class DefaultMentionNotificationParametersServiceTest
{
    @InjectMockComponents
    private DefaultMentionNotificationService notificationService;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private QuoteService quote;

    @MockComponent
    private MentionsConfiguration configuration;

    @Test
    void sendNotification()
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Author");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        DocumentReference mentionedIdentity = new DocumentReference("xwiki", "XWiki", "U2");
        XDOM xdom = new XDOM(emptyList());

        Set<String> eventTarget = Collections.singleton("xwiki:XWiki.U2");
        when(this.serializer.serialize(mentionedIdentity)).thenReturn("xwiki:XWiki.U2");
        when(this.configuration.isQuoteActivated()).thenReturn(true);
        when(this.quote.extract(xdom, "anchor")).thenReturn(Optional.of("quote some content"));

        this.notificationService.sendNotification(
            new MentionNotificationParameters(authorReference, documentReference, mentionedIdentity,
                MentionLocation.COMMENT,
                "anchor", xdom));

        MentionEvent event = new MentionEvent(eventTarget,
            new MentionEventParams()
                .setUserReference(authorReference.toString())
                .setDocumentReference(documentReference.toString())
                .setLocation(MentionLocation.COMMENT)
                .setAnchor("anchor")
                .setQuote("quote some content")

        );
        verify(this.observationManager)
            .notify(event, "org.xwiki.contrib:mentions-notifications", MentionEvent.EVENT_TYPE);
    }

    @Test
    void sendNotificationQuoteDeactivated()
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Author");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        DocumentReference mentionedIdentity = new DocumentReference("xwiki", "XWiki", "U2");
        XDOM xdom = new XDOM(emptyList());

        Set<String> eventTarget = Collections.singleton("xwiki:XWiki.U2");
        when(this.serializer.serialize(mentionedIdentity)).thenReturn("xwiki:XWiki.U2");
        when(this.configuration.isQuoteActivated()).thenReturn(false);

        this.notificationService.sendNotification(
            new MentionNotificationParameters(authorReference, documentReference, mentionedIdentity,
                MentionLocation.COMMENT,
                "anchor", xdom));

        MentionEvent event = new MentionEvent(eventTarget,
            new MentionEventParams()
                .setUserReference(authorReference.toString())
                .setDocumentReference(documentReference.toString())
                .setLocation(MentionLocation.COMMENT)
                .setAnchor("anchor")
        );
        verify(this.observationManager)
            .notify(event, "org.xwiki.contrib:mentions-notifications", MentionEvent.EVENT_TYPE);
        verify(this.quote, never()).extract(any(), any());
    }
}
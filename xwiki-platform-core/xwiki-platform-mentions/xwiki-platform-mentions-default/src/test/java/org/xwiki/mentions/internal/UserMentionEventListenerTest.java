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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.MentionsConfiguration;
import org.xwiki.mentions.events.MentionEvent;
import org.xwiki.mentions.events.MentionEventParams;
import org.xwiki.mentions.events.NewMentionsEvent;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.mentions.events.MentionEvent.EVENT_TYPE;

/**
 * Test of {@link UserMentionEventListener}.
 *
 * @version $Id$
 * @since 12.10RC1
 */
@ComponentTest
class UserMentionEventListenerTest
{
    private static final String AUTHOR_REFERENCE = "xwiki:XWiki.Author";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    private static final String MENTIONED_IDENTITY_USER = "xwiki:XWiki.U2";

    @InjectMockComponents
    private UserMentionEventListener notificationService;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @MockComponent
    private QuoteService quote;

    @MockComponent
    private MentionsConfiguration configuration;

    @MockComponent
    private MentionXDOMService xdomService;

    @BeforeEach
    void setUp()
    {
        UserReference userReferenceUser = mock(UserReference.class);
        UserReference userReferenceU2 = mock(UserReference.class);
        WikiReference wikiReference = DOCUMENT_REFERENCE.getWikiReference();
        when(this.userReferenceResolver.resolve("User", wikiReference)).thenReturn(userReferenceUser);
        when(this.userReferenceResolver.resolve(MENTIONED_IDENTITY_USER, wikiReference)).thenReturn(userReferenceU2);
        when(this.userReferenceSerializer.serialize(userReferenceUser)).thenReturn("xwiki:XWiki.User");
        when(this.userReferenceSerializer.serialize(userReferenceU2)).thenReturn(MENTIONED_IDENTITY_USER);
        when(this.entityReferenceSerializer.serialize(DOCUMENT_REFERENCE)).thenReturn("xwiki:XWiki.Doc");
    }

    @Test
    void sendNotifications() throws Exception
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        ObjectReference commentObjectReference = new ObjectReference("XWiki.XWikiComments", DOCUMENT_REFERENCE);
        BaseObject baseObject = new BaseObject();
        baseObject.addField("comment", new LargeStringProperty());
        when(xWikiDocument.getXObject((EntityReference) commentObjectReference)).thenReturn(baseObject);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "4.6")).thenReturn(xWikiDocument);

        Set<String> eventTarget = singleton(MENTIONED_IDENTITY_USER);

        MentionNotificationParameter mentionNotificationParameter =
            new MentionNotificationParameter(MENTIONED_IDENTITY_USER, "anchor");

        this.notificationService.onEvent(null, null,
            new MentionNotificationParameters(AUTHOR_REFERENCE,
                new ObjectPropertyReference("comment", commentObjectReference), MentionLocation.COMMENT, "4.6")
                .addNewMention(null, mentionNotificationParameter));

        MentionEvent event = new MentionEvent(eventTarget,
            new MentionEventParams()
                .setUserReference(AUTHOR_REFERENCE)
                .setDocumentReference("xwiki:XWiki.Doc")
                .setLocation(MentionLocation.COMMENT)
                .setAnchor("anchor")
        );
        verify(this.observationManager).notify(event, "org.xwiki.contrib:mentions-notifications", EVENT_TYPE);
    }

    @Test
    void sentNotificationsMultipleRecipients() throws Exception
    {
        MentionNotificationParameter mention1 = new MentionNotificationParameter(MENTIONED_IDENTITY_USER, "anchor");
        MentionNotificationParameter mention2 = new MentionNotificationParameter("User", "anchor2");
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        ObjectReference commentObjectReference = new ObjectReference("XWiki.Comments", DOCUMENT_REFERENCE);
        BaseObject baseObject = new BaseObject();
        baseObject.addField("comment", new LargeStringProperty());
        when(xWikiDocument.getXObject((EntityReference) commentObjectReference)).thenReturn(baseObject);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "2.3")).thenReturn(xWikiDocument);

        this.notificationService.onEvent(new NewMentionsEvent(), null,
            new MentionNotificationParameters(AUTHOR_REFERENCE,
                new ObjectPropertyReference("comment", commentObjectReference),
                MentionLocation.COMMENT, "2.3")
                .addNewMention("UserMentionEventListener", mention1)
                .addNewMention(null, mention2)
        );

        MentionEvent event = new MentionEvent(singleton(MENTIONED_IDENTITY_USER),
            new MentionEventParams()
                .setUserReference(AUTHOR_REFERENCE)
                .setDocumentReference("xwiki:XWiki.Doc")
                .setLocation(MentionLocation.COMMENT)
                .setAnchor("anchor")
        );
        MentionEvent event2 = new MentionEvent(singleton("xwiki:XWiki.User"),
            new MentionEventParams()
                .setUserReference(AUTHOR_REFERENCE)
                .setDocumentReference("xwiki:XWiki.Doc")
                .setLocation(MentionLocation.COMMENT)
                .setAnchor("anchor2")
        );
        verify(this.observationManager)
            .notify(event2, "org.xwiki.contrib:mentions-notifications", EVENT_TYPE);
        verify(this.observationManager)
            .notify(event, "org.xwiki.contrib:mentions-notifications", EVENT_TYPE);
    }

    @Test
    void sendNotification() throws Exception
    {
        String mentionedIdentity = "xwiki:XWiki.U2";
        XDOM xdom = new XDOM(emptyList());

        Set<String> eventTarget = Collections.singleton("xwiki:XWiki.U2");
        when(this.configuration.isQuoteActivated()).thenReturn(true);
        when(this.quote.extract(xdom, "anchor")).thenReturn(Optional.of("quote some content"));
        when(this.entityReferenceSerializer.serialize(DOCUMENT_REFERENCE)).thenReturn("xwiki:XWiki.Doc");
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "4.9")).thenReturn(xWikiDocument);
        when(xWikiDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        ObjectReference objectReference = new ObjectReference("XWiki.XWikiComments", DOCUMENT_REFERENCE);
        BaseObject baseObject = new BaseObject();
        LargeStringProperty largeStringProperty = new LargeStringProperty();
        largeStringProperty.setValue("some content");
        baseObject.addField("comment", largeStringProperty);
        when(xWikiDocument.getXObject((EntityReference) objectReference)).thenReturn(baseObject);
        when(this.xdomService.parse("some content", Syntax.XWIKI_2_1)).thenReturn(Optional.of(xdom));

        MentionNotificationParameter mentionNotificationParameter =
            new MentionNotificationParameter(mentionedIdentity, "anchor");
        this.notificationService
            .onEvent(null, null,
                new MentionNotificationParameters(AUTHOR_REFERENCE,
                    new ObjectPropertyReference("comment",
                        objectReference), MentionLocation.COMMENT, "4.9")
                    .addNewMention(null, mentionNotificationParameter));

        MentionEvent event = new MentionEvent(eventTarget,
            new MentionEventParams()
                .setUserReference(AUTHOR_REFERENCE)
                .setDocumentReference("xwiki:XWiki.Doc")
                .setLocation(MentionLocation.COMMENT)
                .setAnchor("anchor")
                .setQuote("quote some content")

        );
        verify(this.observationManager)
            .notify(event, "org.xwiki.contrib:mentions-notifications", MentionEvent.EVENT_TYPE);
    }

    @Test
    void sendNotificationQuoteDeactivated() throws Exception
    {
        String mentionedIdentity = "xwiki:XWiki.U2";
        XDOM xdom = new XDOM(emptyList());

        Set<String> eventTarget = Collections.singleton("xwiki:XWiki.U2");
        when(this.configuration.isQuoteActivated()).thenReturn(false);
        when(this.entityReferenceSerializer.serialize(DOCUMENT_REFERENCE)).thenReturn("xwiki:XWiki.Doc");
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "7.5")).thenReturn(xWikiDocument);
        ObjectReference objectReference = new ObjectReference("XWiki.XWikiComments", DOCUMENT_REFERENCE);
        BaseObject baseObject = new BaseObject();
        LargeStringProperty largeStringProperty = new LargeStringProperty();
        largeStringProperty.setValue("some content");
        baseObject.addField("comment", largeStringProperty);
        when(xWikiDocument.getXObject((EntityReference) objectReference)).thenReturn(baseObject);
        when(xWikiDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        MentionNotificationParameter mentionNotificationParameter =
            new MentionNotificationParameter(mentionedIdentity, "anchor");
        this.notificationService.onEvent(null, null,
            new MentionNotificationParameters(AUTHOR_REFERENCE,
                new ObjectPropertyReference("comment", objectReference),
                MentionLocation.COMMENT, "7.5")
                .addNewMention(null, mentionNotificationParameter));

        MentionEvent event = new MentionEvent(eventTarget,
            new MentionEventParams()
                .setUserReference(AUTHOR_REFERENCE)
                .setDocumentReference("xwiki:XWiki.Doc")
                .setLocation(MentionLocation.COMMENT)
                .setAnchor("anchor")
        );
        verify(this.observationManager)
            .notify(event, "org.xwiki.contrib:mentions-notifications", MentionEvent.EVENT_TYPE);
        verify(this.quote, never()).extract(any(), any());
    }
}

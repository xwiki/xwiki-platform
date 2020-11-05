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
package org.xwiki.notifications.notifiers.internal.email;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.notifiers.email.NotificationEmailRenderer;
import org.xwiki.notifications.sources.NotificationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.api.Attachment;

import static org.jgroups.util.Util.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultPeriodicMimeMessageIterator}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultPeriodicMimeMessageIteratorTest
{
    private static final DocumentReference TEMPLATE_REFERENCE = new DocumentReference("xwiki", "XWiki", "Template");

    @InjectMockComponents
    private DefaultPeriodicMimeMessageIterator iterator;

    @MockComponent
    private NotificationManager notificationManager;

    @MockComponent
    @Named("template")
    private MimeMessageFactory<MimeMessage> factory;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private NotificationEmailRenderer defaultNotificationEmailRenderer;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private MailSenderConfiguration mailSenderConfiguration;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;

    @MockComponent
    private UserAvatarAttachmentExtractor userAvatarAttachmentExtractor;

    @BeforeEach
    void beforeEach()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("xwiki");
        when(this.mailSenderConfiguration.getFromAddress()).thenReturn("xwiki@xwiki.org");
        when(this.documentReferenceResolver.resolve(eq(TEMPLATE_REFERENCE), any())).thenReturn(TEMPLATE_REFERENCE);
    }

    @Test
    void test() throws Exception
    {
        Map<String, Object> factoryParameters = new HashMap<>();

        // Mocks
        NotificationUserIterator userIterator = mock(NotificationUserIterator.class);
        DocumentReference userA = new DocumentReference("xwiki", "XWiki", "UserA");
        DocumentReference userB = new DocumentReference("xwiki", "XWiki", "UserB");
        DocumentReference userC = new DocumentReference("xwiki", "XWiki", "UserC");
        when(userIterator.hasNext()).thenReturn(true, true, true, false);
        when(userIterator.next()).thenReturn(userA, userB, userC);
        DocumentReference userClass = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when(this.documentAccessBridge.getProperty(userA, userClass, 0, "email")).thenReturn("userA@xwiki.org");
        when(this.documentAccessBridge.getProperty(userB, userClass, 0, "email")).thenReturn("bad email");
        when(this.documentAccessBridge.getProperty(userC, userClass, 0, "email")).thenReturn("userC@xwiki.org");
        when(this.serializer.serialize(userA)).thenReturn("xwiki:XWiki.UserA");
        when(this.serializer.serialize(userB)).thenReturn("xwiki:XWiki.UserA");
        when(this.serializer.serialize(userC)).thenReturn("xwiki:XWiki.UserC");

        CompositeEvent event1 = mock(CompositeEvent.class);
        CompositeEvent event2 = mock(CompositeEvent.class);

        when(this.notificationManager.getEvents("xwiki:XWiki.UserA", NotificationFormat.EMAIL, Integer.MAX_VALUE / 4,
            null, new Date(0L), Collections.emptyList())).thenReturn(Arrays.asList(event1));
        when(this.notificationManager.getEvents("xwiki:XWiki.UserC", NotificationFormat.EMAIL, Integer.MAX_VALUE / 4,
            null, new Date(0L), Collections.emptyList())).thenReturn(Arrays.asList(event2));

        when(event1.getUsers()).thenReturn(Sets.newSet(userB));
        when(event2.getUsers()).thenReturn(Sets.newSet(userB));

        MimeMessage message = mock(MimeMessage.class);
        when(this.factory.createMessage(TEMPLATE_REFERENCE, factoryParameters)).thenReturn(message, message);

        when(this.defaultNotificationEmailRenderer.renderHTML(eq(event1), anyString())).thenReturn("eventHTML1");
        when(this.defaultNotificationEmailRenderer.renderPlainText(eq(event1), anyString())).thenReturn("event1");
        when(this.defaultNotificationEmailRenderer.renderHTML(eq(event2), anyString())).thenReturn("eventHTML2");
        when(this.defaultNotificationEmailRenderer.renderPlainText(eq(event2), anyString())).thenReturn("event2");

        Attachment userBAvatar = mock(Attachment.class);
        when(this.userAvatarAttachmentExtractor.getUserAvatar(eq(userB), anyInt())).thenReturn(userBAvatar);

        // Test
        this.iterator.initialize(userIterator, factoryParameters, new Date(0L), TEMPLATE_REFERENCE);

        // First iteration
        assertTrue(this.iterator.hasNext());
        assertEquals(message, this.iterator.next());
        assertEquals(new InternetAddress("xwiki@xwiki.org"), factoryParameters.get("from"));
        assertEquals(new InternetAddress("userA@xwiki.org"), factoryParameters.get("to"));
        Map<String, Object> velocityVariables = (Map<String, Object>) factoryParameters.get("velocityVariables");
        assertNotNull(velocityVariables);
        assertEquals(Arrays.asList(event1), velocityVariables.get("events"));
        assertEquals(Arrays.asList("eventHTML1"), velocityVariables.get("htmlEvents"));
        assertEquals(Arrays.asList("event1"), velocityVariables.get("plainTextEvents"));
        assertEquals("xwiki:XWiki.UserA", velocityVariables.get("emailUser"));

        // Count the number of attachments
        assertEquals(2, ((List) factoryParameters.get("attachments")).size());
        assertTrue(((List) factoryParameters.get("attachments")).contains(userBAvatar));

        // Second iteration
        assertTrue(this.iterator.hasNext());
        assertEquals(message, this.iterator.next());
        assertEquals(new InternetAddress("xwiki@xwiki.org"), factoryParameters.get("from"));
        assertEquals(new InternetAddress("userC@xwiki.org"), factoryParameters.get("to"));
        velocityVariables = (Map<String, Object>) factoryParameters.get("velocityVariables");
        assertNotNull(velocityVariables);
        assertEquals(Arrays.asList(event2), velocityVariables.get("events"));
        assertEquals(Arrays.asList("eventHTML2"), velocityVariables.get("htmlEvents"));
        assertEquals(Arrays.asList("event2"), velocityVariables.get("plainTextEvents"));
        assertEquals("xwiki:XWiki.UserC", velocityVariables.get("emailUser"));

        // Make sure there is no duplicated attachments
        assertEquals(2, ((List) factoryParameters.get("attachments")).size());
        assertTrue(((List) factoryParameters.get("attachments")).contains(userBAvatar));

        // End
        assertFalse(this.iterator.hasNext());

        // Verify
        verify(this.serializer, never()).serialize(userB);

        assertEquals(this.iterator, this.iterator.iterator());
    }

}

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

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.collections.map.HashedMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.notifiers.email.NotificationEmailRenderer;
import org.xwiki.notifications.sources.NotificationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.jgroups.util.Util.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class DefaultPeriodicMimeMessageIteratorTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultPeriodicMimeMessageIterator> mocker =
            new MockitoComponentMockingRule<>(DefaultPeriodicMimeMessageIterator.class);

    private NotificationManager notificationManager;
    private MimeMessageFactory<MimeMessage> factory;
    private DocumentAccessBridge documentAccessBridge;
    private NotificationEmailRenderer defaultNotificationEmailRenderer;
    private WikiDescriptorManager wikiDescriptorManager;
    private MailSenderConfiguration mailSenderConfiguration;
    private EntityReferenceSerializer<String> serializer;

    @Before
    public void setUp() throws Exception
    {
        notificationManager = mocker.getInstance(NotificationManager.class);
        factory = mocker.getInstance(
                new DefaultParameterizedType(null, MimeMessageFactory.class, new Type[]{MimeMessage.class}),
                "template"
        );
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        defaultNotificationEmailRenderer = mocker.getInstance(NotificationEmailRenderer.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        mailSenderConfiguration = mocker.getInstance(MailSenderConfiguration.class);
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);

        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("xwiki");
        when(mailSenderConfiguration.getFromAddress()).thenReturn("xwiki@xwiki.org");
    }

    @Test
    public void test() throws Exception
    {
        DocumentReference templateReference = new DocumentReference("xwiki", "XWiki", "Template");
        Map<String, Object> factoryParameters = new HashedMap();

        // Mocks
        NotificationUserIterator userIterator = mock(NotificationUserIterator.class);
        DocumentReference userA = new DocumentReference("xwiki", "XWiki", "UserA");
        DocumentReference userB = new DocumentReference("xwiki", "XWiki", "UserB");
        DocumentReference userC = new DocumentReference("xwiki", "XWiki", "UserC");
        when(userIterator.hasNext()).thenReturn(true, true, true, false);
        when(userIterator.next()).thenReturn(userA, userB, userC);
        DocumentReference userClass = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when(documentAccessBridge.getProperty(userA, userClass, 0, "email")).thenReturn("userA@xwiki.org");
        when(documentAccessBridge.getProperty(userB, userClass, 0, "email")).thenReturn("bad email");
        when(documentAccessBridge.getProperty(userC, userClass, 0, "email")).thenReturn("userC@xwiki.org");
        when(serializer.serialize(userA)).thenReturn("xwiki:XWiki.UserA");
        when(serializer.serialize(userB)).thenReturn("xwiki:XWiki.UserA");
        when(serializer.serialize(userC)).thenReturn("xwiki:XWiki.UserC");

        CompositeEvent event1 = mock(CompositeEvent.class);
        CompositeEvent event2 = mock(CompositeEvent.class);

        when(notificationManager.getEvents("xwiki:XWiki.UserA", NotificationFormat.EMAIL,
                Integer.MAX_VALUE / 4, null, new Date(0L), Collections.emptyList()))
                .thenReturn(Arrays.asList(event1));
        when(notificationManager.getEvents("xwiki:XWiki.UserC", NotificationFormat.EMAIL,
                Integer.MAX_VALUE / 4, null, new Date(0L), Collections.emptyList()))
                .thenReturn(Arrays.asList(event2));

        MimeMessage message = mock(MimeMessage.class);
        when(factory.createMessage(templateReference, factoryParameters)).thenReturn(message, message);

        when(defaultNotificationEmailRenderer.renderHTML(eq(event1), anyString())).thenReturn("eventHTML1");
        when(defaultNotificationEmailRenderer.renderPlainText(eq(event1), anyString())).thenReturn("event1");
        when(defaultNotificationEmailRenderer.renderHTML(eq(event2), anyString())).thenReturn("eventHTML2");
        when(defaultNotificationEmailRenderer.renderPlainText(eq(event2), anyString())).thenReturn("event2");

        // Test
        PeriodicMimeMessageIterator iterator = mocker.getComponentUnderTest();

        iterator.initialize(userIterator, factoryParameters, new Date(0L), templateReference);

        // First iteration
        assertTrue(iterator.hasNext());
        assertEquals(message, iterator.next());
        assertEquals(new InternetAddress("xwiki@xwiki.org"), factoryParameters.get("from"));
        assertEquals(new InternetAddress("userA@xwiki.org"), factoryParameters.get("to"));
        Map<String, Object> velocityVariables = (Map<String, Object>) factoryParameters.get("velocityVariables");
        assertNotNull(velocityVariables);
        assertEquals(Arrays.asList(event1), velocityVariables.get("events"));
        assertEquals(Arrays.asList("eventHTML1"), velocityVariables.get("htmlEvents"));
        assertEquals(Arrays.asList("event1"), velocityVariables.get("plainTextEvents"));

        // Count the number of attachments
        assertEquals(1, ((List)factoryParameters.get("attachments")).size());

        // Second iteration
        assertTrue(iterator.hasNext());
        assertEquals(message, iterator.next());
        assertEquals(new InternetAddress("xwiki@xwiki.org"), factoryParameters.get("from"));
        assertEquals(new InternetAddress("userC@xwiki.org"), factoryParameters.get("to"));
        velocityVariables = (Map<String, Object>) factoryParameters.get("velocityVariables");
        assertNotNull(velocityVariables);
        assertEquals(Arrays.asList(event2), velocityVariables.get("events"));
        assertEquals(Arrays.asList("eventHTML2"), velocityVariables.get("htmlEvents"));
        assertEquals(Arrays.asList("event2"), velocityVariables.get("plainTextEvents"));

        // Make sure there is no duplicated attachments
        assertEquals(1, ((List)factoryParameters.get("attachments")).size());

        // End
        assertFalse(iterator.hasNext());

        // Verify
        verify(serializer, never()).serialize(userB);

        assertEquals(iterator, iterator.iterator());
    }

}

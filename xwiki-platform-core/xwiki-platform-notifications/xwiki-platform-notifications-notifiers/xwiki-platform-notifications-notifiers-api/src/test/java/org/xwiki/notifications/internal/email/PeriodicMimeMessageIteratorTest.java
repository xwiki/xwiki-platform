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
package org.xwiki.notifications.internal.email;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.collections.map.HashedMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.sources.NotificationManager;
import org.xwiki.notifications.notifiers.email.NotificationEmailRenderer;
import org.xwiki.notifications.notifiers.internal.email.NotificationUserIterator;
import org.xwiki.notifications.notifiers.internal.email.PeriodicMimeMessageIterator;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class PeriodicMimeMessageIteratorTest
{
    @Rule
    public final MockitoComponentMockingRule<PeriodicMimeMessageIterator> mocker =
            new MockitoComponentMockingRule<>(PeriodicMimeMessageIterator.class);

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

        Mockito.when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("xwiki");
        Mockito.when(mailSenderConfiguration.getFromAddress()).thenReturn("xwiki@xwiki.org");
    }

    @Test
    public void test() throws Exception
    {
        DocumentReference templateReference = new DocumentReference("xwiki", "XWiki", "Template");
        Map<String, Object> factoryParameters = new HashedMap();

        // Mocks
        NotificationUserIterator userIterator = Mockito.mock(NotificationUserIterator.class);
        DocumentReference userA = new DocumentReference("xwiki", "XWiki", "UserA");
        DocumentReference userB = new DocumentReference("xwiki", "XWiki", "UserB");
        DocumentReference userC = new DocumentReference("xwiki", "XWiki", "UserC");
        Mockito.when(userIterator.hasNext()).thenReturn(true, true, true, false);
        Mockito.when(userIterator.next()).thenReturn(userA, userB, userC);
        DocumentReference userClass = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        Mockito.when(documentAccessBridge.getProperty(userA, userClass, 0, "email")).thenReturn("userA@xwiki.org");
        Mockito.when(documentAccessBridge.getProperty(userB, userClass, 0, "email")).thenReturn("bad email");
        Mockito.when(documentAccessBridge.getProperty(userC, userClass, 0, "email")).thenReturn("userC@xwiki.org");
        Mockito.when(serializer.serialize(userA)).thenReturn("xwiki:XWiki.UserA");
        Mockito.when(serializer.serialize(userB)).thenReturn("xwiki:XWiki.UserA");
        Mockito.when(serializer.serialize(userC)).thenReturn("xwiki:XWiki.UserC");

        CompositeEvent event1 = Mockito.mock(CompositeEvent.class);
        CompositeEvent event2 = Mockito.mock(CompositeEvent.class);

        Mockito.when(notificationManager.getEvents("xwiki:XWiki.UserA", NotificationFormat.EMAIL, false,
                Integer.MAX_VALUE / 4, null, new Date(0L), Collections.emptyList()))
                .thenReturn(Arrays.asList(event1));
        Mockito.when(notificationManager.getEvents("xwiki:XWiki.UserC", NotificationFormat.EMAIL, false,
                Integer.MAX_VALUE / 4, null, new Date(0L), Collections.emptyList()))
                .thenReturn(Arrays.asList(event2));

        MimeMessage message = Mockito.mock(MimeMessage.class);
        Mockito.when(factory.createMessage(templateReference, factoryParameters)).thenReturn(message, message);

        Mockito.when(defaultNotificationEmailRenderer.renderHTML(event1)).thenReturn("eventHTML1");
        Mockito.when(defaultNotificationEmailRenderer.renderPlainText(event1)).thenReturn("event1");
        Mockito.when(defaultNotificationEmailRenderer.renderHTML(event2)).thenReturn("eventHTML2");
        Mockito.when(defaultNotificationEmailRenderer.renderPlainText(event2)).thenReturn("event2");

        // Test
        PeriodicMimeMessageIterator iterator = mocker.getComponentUnderTest();

        iterator.initialize(userIterator, factoryParameters, new Date(0L), templateReference);

        // First iteration
        Assert.assertTrue(iterator.hasNext());
        assertEquals(message, iterator.next());
        Assert.assertEquals(new InternetAddress("xwiki@xwiki.org"), factoryParameters.get("from"));
        Assert.assertEquals(new InternetAddress("userA@xwiki.org"), factoryParameters.get("to"));
        Map<String, Object> velocityVariables = (Map<String, Object>) factoryParameters.get("velocityVariables");
        Assert.assertNotNull(velocityVariables);
        Assert.assertEquals(Arrays.asList(event1), velocityVariables.get("events"));
        Assert.assertEquals(Arrays.asList("eventHTML1"), velocityVariables.get("htmlEvents"));
        Assert.assertEquals(Arrays.asList("event1"), velocityVariables.get("plainTextEvents"));

        // Second iteration
        Assert.assertTrue(iterator.hasNext());
        assertEquals(message, iterator.next());
        Assert.assertEquals(new InternetAddress("xwiki@xwiki.org"), factoryParameters.get("from"));
        Assert.assertEquals(new InternetAddress("userC@xwiki.org"), factoryParameters.get("to"));
        velocityVariables = (Map<String, Object>) factoryParameters.get("velocityVariables");
        Assert.assertNotNull(velocityVariables);
        Assert.assertEquals(Arrays.asList(event2), velocityVariables.get("events"));
        Assert.assertEquals(Arrays.asList("eventHTML2"), velocityVariables.get("htmlEvents"));
        Assert.assertEquals(Arrays.asList("event2"), velocityVariables.get("plainTextEvents"));

        // End
        Assert.assertFalse(iterator.hasNext());

        // Verify
        Mockito.verify(serializer, Mockito.never()).serialize(userB);

        assertEquals(iterator, iterator.iterator());
    }

}

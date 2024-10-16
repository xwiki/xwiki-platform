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
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Provider;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.Event;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.GroupingEventManager;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.notifiers.email.NotificationEmailGroupingStrategy;
import org.xwiki.notifications.notifiers.email.NotificationEmailRenderer;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private ParametrizedNotificationManager notificationManager;

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
    private GroupingEventManager groupingEventManager;

    @MockComponent
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;

    @MockComponent
    private UserAvatarAttachmentExtractor userAvatarAttachmentExtractor;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private NotificationEmailGroupingStrategy notificationEmailGroupingStrategy;

    @MockComponent
    private NotificationConfiguration notificationConfiguration;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        String groupingStrategyHint = "testStrategy";
        when(this.notificationConfiguration.getEmailGroupingStrategyHint()).thenReturn(groupingStrategyHint);
        componentManager.registerComponent(NotificationEmailGroupingStrategy.class, groupingStrategyHint,
            this.notificationEmailGroupingStrategy);
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @BeforeEach
    void beforeEach()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("xwiki");
        when(this.mailSenderConfiguration.getFromAddress()).thenReturn("xwiki@xwiki.org");
        when(this.documentReferenceResolver.resolve(eq(TEMPLATE_REFERENCE), any())).thenReturn(TEMPLATE_REFERENCE);
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
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
        when(this.serializer.serialize(userB)).thenReturn("xwiki:XWiki.UserB");
        when(this.serializer.serialize(userC)).thenReturn("xwiki:XWiki.UserC");

        CompositeEvent compositeEvent1UserA = mock(CompositeEvent.class);
        CompositeEvent compositeEvent2UserA = mock(CompositeEvent.class);
        CompositeEvent compositeEvent1UserC = mock(CompositeEvent.class);

        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);

        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.user = userA;
        notificationParameters.format = NotificationFormat.EMAIL;
        notificationParameters.expectedCount = Integer.MAX_VALUE / 4;
        notificationParameters.fromDate = new Date(0L);
        notificationParameters.endDateIncluded = false;

        when(this.notificationManager.getRawEvents(notificationParameters))
            .thenReturn(Collections.singletonList(event1));

        NotificationParameters notificationParameters2 = new NotificationParameters();
        notificationParameters2.user = userC;
        notificationParameters2.format = NotificationFormat.EMAIL;
        notificationParameters2.expectedCount = Integer.MAX_VALUE / 4;
        notificationParameters2.fromDate = new Date(0L);
        notificationParameters2.endDateIncluded = false;

        when(this.notificationManager.getRawEvents(notificationParameters2))
            .thenReturn(Collections.singletonList(event2));

        UserReference userRefA = mock(UserReference.class, "userA");
        UserReference userRefC = mock(UserReference.class, "userC");

        when(this.userReferenceResolver.resolve(userA)).thenReturn(userRefA);
        when(this.userReferenceResolver.resolve(userC)).thenReturn(userRefC);

        when(this.groupingEventManager.getCompositeEvents(Collections.singletonList(event1), userRefA,
            "EMAIL")).thenReturn(List.of(compositeEvent1UserA, compositeEvent2UserA));
        when(this.groupingEventManager.getCompositeEvents(Collections.singletonList(event2), userRefC,
            "EMAIL")).thenReturn(Collections.singletonList(compositeEvent1UserC));

        when(this.notificationEmailGroupingStrategy.groupEventsPerMail(any())).then(invocationOnMock -> {
            List<CompositeEvent> compositeEvents = invocationOnMock.getArgument(0);
            return compositeEvents.stream().map(List::of).collect(Collectors.toList());
        });

        when(compositeEvent1UserA.getUsers()).thenReturn(Sets.newSet(userB));
        when(compositeEvent1UserC.getUsers()).thenReturn(Sets.newSet(userB));

        ExtendedMimeMessage message = mock(ExtendedMimeMessage.class);
        when(this.factory.createMessage(TEMPLATE_REFERENCE, factoryParameters)).thenReturn(message, message);

        when(this.defaultNotificationEmailRenderer.renderHTML(eq(compositeEvent1UserA), anyString()))
            .thenReturn("eventHTML1UserA");
        when(this.defaultNotificationEmailRenderer.renderPlainText(eq(compositeEvent1UserA), anyString()))
            .thenReturn("compositeEvent1UserA");
        when(this.defaultNotificationEmailRenderer.renderHTML(eq(compositeEvent2UserA), anyString()))
            .thenReturn("eventHTML2UserA");
        when(this.defaultNotificationEmailRenderer.renderPlainText(eq(compositeEvent2UserA), anyString()))
            .thenReturn("compositeEvent2UserA");
        when(this.defaultNotificationEmailRenderer.renderHTML(eq(compositeEvent1UserC), anyString()))
            .thenReturn("eventHTML1UserC");
        when(this.defaultNotificationEmailRenderer.renderPlainText(eq(compositeEvent1UserC), anyString()))
            .thenReturn("compositeEvent1UserC");

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
        assertEquals(Arrays.asList(compositeEvent1UserA), velocityVariables.get("events"));
        assertEquals(Arrays.asList("eventHTML1UserA"), velocityVariables.get("htmlEvents"));
        assertEquals(Arrays.asList("compositeEvent1UserA"), velocityVariables.get("plainTextEvents"));
        assertEquals("xwiki:XWiki.UserA", velocityVariables.get("emailUser"));

        // Count the number of attachments
        assertEquals(2, ((List) factoryParameters.get("attachments")).size());
        assertTrue(((List) factoryParameters.get("attachments")).contains(userBAvatar));

        // Second iteration
        assertTrue(this.iterator.hasNext());
        assertEquals(message, this.iterator.next());
        assertEquals(new InternetAddress("xwiki@xwiki.org"), factoryParameters.get("from"));
        assertEquals(new InternetAddress("userA@xwiki.org"), factoryParameters.get("to"));
        velocityVariables = (Map<String, Object>) factoryParameters.get("velocityVariables");
        assertNotNull(velocityVariables);
        assertEquals(Arrays.asList(compositeEvent2UserA), velocityVariables.get("events"));
        assertEquals(Arrays.asList("eventHTML2UserA"), velocityVariables.get("htmlEvents"));
        assertEquals(Arrays.asList("compositeEvent2UserA"), velocityVariables.get("plainTextEvents"));
        assertEquals("xwiki:XWiki.UserA", velocityVariables.get("emailUser"));

        // Count the number of attachments
        assertEquals(1, ((List) factoryParameters.get("attachments")).size());
        assertFalse(((List) factoryParameters.get("attachments")).contains(userBAvatar));

        // Third iteration
        assertTrue(this.iterator.hasNext());
        assertEquals(message, this.iterator.next());
        assertEquals(new InternetAddress("xwiki@xwiki.org"), factoryParameters.get("from"));
        assertEquals(new InternetAddress("userC@xwiki.org"), factoryParameters.get("to"));
        velocityVariables = (Map<String, Object>) factoryParameters.get("velocityVariables");
        assertNotNull(velocityVariables);
        assertEquals(Arrays.asList(compositeEvent1UserC), velocityVariables.get("events"));
        assertEquals(Arrays.asList("eventHTML1UserC"), velocityVariables.get("htmlEvents"));
        assertEquals(Arrays.asList("compositeEvent1UserC"), velocityVariables.get("plainTextEvents"));
        assertEquals("xwiki:XWiki.UserC", velocityVariables.get("emailUser"));

        // Make sure there is no duplicated attachments
        assertEquals(2, ((List) factoryParameters.get("attachments")).size());
        assertTrue(((List) factoryParameters.get("attachments")).contains(userBAvatar));

        // End
        assertFalse(this.iterator.hasNext());

        // Verify
        verify(this.serializer, never()).serialize(userB);
        verify(this.context, times(2)).setUserReference(userA);
        verify(this.context, never()).setUserReference(userB);
        verify(this.context).setUserReference(userC);
        verify(this.context, times(3)).setUserReference(null);

        assertEquals(this.iterator, this.iterator.iterator());
    }

}

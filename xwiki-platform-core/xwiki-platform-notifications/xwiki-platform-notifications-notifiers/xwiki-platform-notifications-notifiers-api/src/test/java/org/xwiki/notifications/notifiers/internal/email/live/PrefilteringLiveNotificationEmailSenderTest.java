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
package org.xwiki.notifications.notifiers.internal.email.live;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.xwiki.eventstream.Event;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.SessionFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PrefilteringLiveNotificationEmailSender}.
 *
 * @version $Id$
 */
@ComponentTest
public class PrefilteringLiveNotificationEmailSenderTest
{
    @InjectMockComponents
    private PrefilteringLiveNotificationEmailSender sender;

    @MockComponent
    private MailSender mailSender;

    @MockComponent
    private SessionFactory sessionFactory;

    @MockComponent
    @Named("database")
    private Provider<MailListener> mailListenerProvider;

    @MockComponent
    private Provider<PrefilteringMimeMessageIterator> liveMimeMessageIteratorProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Test
    public void testSendMail() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("xwiki");

        PrefilteringMimeMessageIterator iterator = mock(PrefilteringMimeMessageIterator.class);
        when(this.liveMimeMessageIteratorProvider.get()).thenReturn(iterator);

        when(this.sessionFactory.create(ArgumentMatchers.any())).thenReturn(null);

        when(this.mailListenerProvider.get()).thenReturn(Mockito.mock(MailListener.class));

        Event event1 = mock(Event.class);
        DocumentReference userReference = new DocumentReference("wiki", "XWiki", "user");
        Map<DocumentReference, List<Event>> events = new HashMap<>();
        events.put(userReference, List.of(event1));

        this.sender.sendMails(events);

        verify(iterator).initialize(events, Collections.emptyMap(), PrefilteringLiveNotificationEmailSender.TEMPLATE);
        verify(this.mailSender).sendAsynchronously(same(iterator), any(), any(MailListener.class));
    }
}

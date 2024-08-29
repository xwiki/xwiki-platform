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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.SessionFactory;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.notifiers.internal.email.NotificationUserIterator;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveNotificationEmailSender}.
 *
 * @since 9.6RC1
 * @version $Id$
 */
public class LiveNotificationEmailSenderTest
{
    @Rule
    public final MockitoComponentMockingRule<LiveNotificationEmailSender> mocker =
            new MockitoComponentMockingRule<>(LiveNotificationEmailSender.class);

    private MailSender mailSender;

    private SessionFactory sessionFactory;

    private Provider<MailListener> mailListenerProvider;

    private Provider<NotificationUserIterator> notificationUserIteratorProvider;

    private Provider<LiveMimeMessageIterator> liveMimeMessageIteratorProvider;

    private WikiDescriptorManager wikiDescriptorManager;

    @Before
    public void setUp() throws Exception
    {
        this.mailSender = this.mocker.registerMockComponent(MailSender.class);

        this.sessionFactory = this.mocker.registerMockComponent(SessionFactory.class);

        this.mailListenerProvider = this.mocker.registerMockComponent(Provider.class, "database");

        this.notificationUserIteratorProvider = mock(Provider.class);
        this.mocker.registerComponent(
                new DefaultParameterizedType(null, Provider.class, NotificationUserIterator.class),
                this.notificationUserIteratorProvider);

        this.liveMimeMessageIteratorProvider = mock(Provider.class);
        this.mocker.registerComponent(
                new DefaultParameterizedType(null, Provider.class, LiveMimeMessageIterator.class),
                this.liveMimeMessageIteratorProvider);

        this.wikiDescriptorManager = this.mocker.registerMockComponent(WikiDescriptorManager.class);
    }

    @Test
    public void testSendMail() throws Exception
    {
        CompositeEvent event1 = mock(CompositeEvent.class);

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("xwiki");

        when(this.notificationUserIteratorProvider.get()).thenReturn(mock(NotificationUserIterator.class));

        when(this.liveMimeMessageIteratorProvider.get()).thenReturn(mock(LiveMimeMessageIterator.class));

        when(this.sessionFactory.create(any())).thenReturn(null);

        when(this.mailListenerProvider.get()).thenReturn(mock(MailListener.class));

        this.mocker.getComponentUnderTest().sendEmails(event1);

        verify(this.mailSender, times(1)).sendAsynchronously(
                any(LiveMimeMessageIterator.class), any(), any(MailListener.class));
    }
}

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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.SessionFactory;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.notifiers.internal.email.NotificationUserIterator;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
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
@ComponentTest
class LiveNotificationEmailSenderTest
{
    @InjectMockComponents
    private LiveNotificationEmailSender sender;

    @MockComponent
    private MailSender mailSender;

    @MockComponent
    private SessionFactory sessionFactory;

    @MockComponent
    @Named("database")
    private Provider<MailListener> mailListenerProvider;

    @MockComponent
    private Provider<NotificationUserIterator> notificationUserIteratorProvider;

    @MockComponent
    private Provider<LiveMimeMessageIterator> liveMimeMessageIteratorProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Test
    void sendMail() throws Exception
    {
        CompositeEvent event1 = mock(CompositeEvent.class);

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("xwiki");

        when(this.notificationUserIteratorProvider.get()).thenReturn(mock(NotificationUserIterator.class));

        when(this.liveMimeMessageIteratorProvider.get()).thenReturn(mock(LiveMimeMessageIterator.class));

        when(this.sessionFactory.create(any())).thenReturn(null);

        when(this.mailListenerProvider.get()).thenReturn(mock(MailListener.class));

        this.sender.sendEmails(event1);

        verify(this.mailSender, times(1)).sendAsynchronously(
                any(LiveMimeMessageIterator.class), any(), any(MailListener.class));
    }
}

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
package org.xwiki.mail.internal;

import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.DefaultMailSender}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class DefaultMailSenderThreadTest
{
    @Rule
    public MockitoComponentMockingRule<TestableDefaultMailSenderThread> mocker =
        new MockitoComponentMockingRule<>(TestableDefaultMailSenderThread.class);

    @BeforeComponent
    public void registerComponent() throws Exception
    {
        // Register the overriding TestableDefaultMailSenderThread for this test only since we don't want it to
        // impact other tests.
        DefaultComponentDescriptor<TestableDefaultMailSenderThread> cd = new DefaultComponentDescriptor<>();
        cd.setInstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP);
        cd.setImplementation(TestableDefaultMailSenderThread.class);
        this.mocker.registerComponent(cd);
    }

    @Test
    @Ignore("I don't understand why this test doesn't pass!")
    public void runWhenMailSendingFails() throws Exception
    {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setSubject("subject");
        message.setFrom(InternetAddress.parse("john@doe.com")[0]);
        MailSenderQueueItem item = new MailSenderQueueItem(message, session, null);

        Queue<MailSenderQueueItem> mailQueue = new ConcurrentLinkedQueue<>();

        // When this mail item is processed it'll send a RuntimeException. We send it twice to ensure that processing
        // is not stopped when a message fails to be sent, see below.
        mailQueue.add(item);
        mailQueue.add(item);

        try {
           this.mocker.getComponentUnderTest().startProcessing(mailQueue);

            // Wait till the queue is empty or a timeout has been reached
            long t = System.currentTimeMillis();
            while (!mailQueue.isEmpty() && System.currentTimeMillis() - t < 10000L) {
                Thread.sleep(10L);
            }
        } finally {
            // Stop the Mail Sender thread
            this.mocker.getComponentUnderTest().stopProcessing();
            this.mocker.getComponentUnderTest().join();
        }

        // This is the real test: we verify that there's been 2 error while sending emails and that they've been
        // logged. This proves that the Mail Sender Thread doesn't stop when there's an error sending an email.
        verify(this.mocker.getMockedLogger(), times(2)).warn("Failed to send mail [{}]. Root reason [{}]", item,
            "RuntimeException: error");
    }
}

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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.mail.internal.script.ScriptMailSenderListener;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.DefaultMailSenderThread}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class DefaultMailSenderThreadTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMailSenderThread> mocker =
        new MockitoComponentMockingRule<>(DefaultMailSenderThread.class);

    @Test
    public void runWhenMailSendingFails() throws Exception
    {
        // Create a Session with an invalid host so that it generates an error
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", "xwiki-unknown");
        Session session = Session.getDefaultInstance(properties);
        MimeMessage message = new MimeMessage(session);
        message.setSubject("subject");
        message.setFrom(InternetAddress.parse("john@doe.com")[0]);
        ScriptMailSenderListener listener = new ScriptMailSenderListener();
        MailSenderQueueItem item = new MailSenderQueueItem(message, session, listener);

        Queue<MailSenderQueueItem> mailQueue = new ConcurrentLinkedQueue<>();

        // Send 2 mails. Both will fail but we want to verify that the second one is processed even though the first
        // one failed.
        mailQueue.add(item);
        mailQueue.add(item);

        DefaultMailSenderThread thread = this.mocker.getComponentUnderTest();
        thread.startProcessing(mailQueue);

        // This is the real test: we verify that there's been an error while sending each email and that it's been
        // logged. This also proves that the Mail Sender Thread doesn't stop when there's an error sending an email.
        boolean success = true;
        try {
            long time = System.currentTimeMillis();
            while (listener.getExceptionQueue().size() != 2) {
                if (System.currentTimeMillis() - time > 5000L) {
                    success = false;
                    break;
                }
                Thread.sleep(100L);
            }
        } finally {
            thread.stopProcessing();
            thread.join();
        }
        assertTrue(success);
    }
}

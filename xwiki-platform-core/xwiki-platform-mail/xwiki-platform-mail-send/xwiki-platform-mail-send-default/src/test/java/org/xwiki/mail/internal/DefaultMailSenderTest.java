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

import java.util.Arrays;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.DefaultMailSender}.
 *
 * @version $Id$
 * @since 6.4RC1
 */
public class DefaultMailSenderTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMailSender> mocker =
        new MockitoComponentMockingRule<DefaultMailSender>(DefaultMailSender.class);

    private MailListener mailListener;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeComponent
    public void setUpComponents() throws Exception
    {
        this.mailListener = this.mocker.registerMockComponent(MailListener.class, "memory");

        ModelContext modelContext = this.mocker.registerMockComponent(ModelContext.class);
        when(modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("wiki"));

        this.mocker.registerMockComponent(MailQueueManager.class);
    }

    @Test
    public void sendSynchronousWithErrors() throws Exception
    {
        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);

        MailStatusResult statusResult = mock(MailStatusResult.class);
        when(this.mailListener.getMailStatusResult()).thenReturn(statusResult);

        // Return failures for the test
        MailStatus status1 = new MailStatus();
        status1.setErrorSummary("errorsummary1");
        status1.setErrorDescription("errordescription1");
        MailStatus status2 = new MailStatus();
        status2.setErrorSummary("errorsummary2");
        status2.setErrorDescription("errordescription2");
        when(statusResult.getByState(MailState.FAILED)).thenReturn(Arrays.asList(status1, status2).iterator());

        this.thrown.expect(MessagingException.class);
        this.thrown.expectMessage("Some messages have failed to be sent for the following reasons: "
            + "[[[errorsummary1],[errordescription1]][[errorsummary2],[errordescription2]]]");

        this.mocker.getComponentUnderTest().send(Arrays.asList(message), session);
    }
}

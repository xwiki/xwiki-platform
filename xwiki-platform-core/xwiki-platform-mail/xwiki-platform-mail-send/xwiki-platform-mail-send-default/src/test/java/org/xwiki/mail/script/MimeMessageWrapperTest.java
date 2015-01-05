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
package org.xwiki.mail.script;

import java.util.Properties;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.internal.ExtendedMimeMessage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link MimeMessageWrapper}.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class MimeMessageWrapperTest
{
    private MimeMessageWrapper messageWrapper;

    private ExtendedMimeMessage extendedMimeMessage;

    private Session session;

    @Before
    public void setUp() throws Exception
    {
        Execution execution = mock(Execution.class);
        MailSenderConfiguration configuration = mock(MailSenderConfiguration.class);
        ComponentManager componentManager = mock(ComponentManager.class);

        this.session = Session.getInstance(new Properties());
        this.extendedMimeMessage = new ExtendedMimeMessage(session);

        this.messageWrapper =
            new MimeMessageWrapper(extendedMimeMessage, session, execution, componentManager);
    }

    @Test
    public void getMessage() throws Exception
    {
        ExtendedMimeMessage message = this.messageWrapper.getMessage();
        assertEquals(message, this.extendedMimeMessage);
    }

    @Test
    public void getSession() throws Exception
    {
        Session session = this.messageWrapper.getSession();
        assertEquals(session, this.session);
    }

    @Test
    public void setSubject() throws Exception
    {
        this.messageWrapper.setSubject("lorem ipsum");
        ExtendedMimeMessage message = this.messageWrapper.getMessage();
        assertEquals(message.getSubject(), "lorem ipsum");
    }

    @Test
    public void setFrom() throws Exception
    {
        this.messageWrapper.setFrom(InternetAddress.parse("john@doe.com")[0]);
        ExtendedMimeMessage message = this.messageWrapper.getMessage();
        assertArrayEquals(message.getFrom(), InternetAddress.parse("john@doe.com"));
    }

    @Test
    public void addRecipients() throws Exception
    {
        Address[] address = InternetAddress.parse("john@doe.com,jane@doe.com,jannie@doe.com");
        this.messageWrapper
            .addRecipients(Message.RecipientType.TO, address);
        ExtendedMimeMessage message = this.messageWrapper.getMessage();
        assertArrayEquals(message.getRecipients(Message.RecipientType.TO), address);
    }

    @Test
    public void addRecipient() throws Exception
    {
        this.messageWrapper
            .addRecipient(Message.RecipientType.TO, InternetAddress.parse("john@doe.com")[0]);
        ExtendedMimeMessage message = this.messageWrapper.getMessage();
        assertArrayEquals(message.getRecipients(Message.RecipientType.TO), InternetAddress.parse("john@doe.com"));
    }

    @Test
    public void addHeader() throws Exception
    {
        String batchId = UUID.randomUUID().toString();
        String mailId = UUID.randomUUID().toString();

        this.messageWrapper.addHeader("X-BatchID", batchId);
        this.messageWrapper.addHeader("X-MailID", mailId);
        ExtendedMimeMessage message = this.messageWrapper.getMessage();

        assertEquals(message.getHeader("X-BatchID", null), batchId);
        assertEquals(message.getHeader("X-MailID", null), mailId);
    }
}
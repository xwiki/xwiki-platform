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
package org.xwiki.mail;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MailStatus}.
 *
 * @version $Id$
 * @since 6.4RC1
 */
public class MailStatusTest
{
    @Test
    public void setError()
    {
        MailStatus status = new MailStatus();
        status.setError(new Exception("outer", new Exception("inner")));

        assertEquals("Exception: inner", status.getErrorSummary());
        assertTrue(status.getErrorDescription().contains("outer"));
        assertTrue(status.getErrorDescription().contains("inner"));
    }

    @Test
    public void verifyToStringWhenStatusHasNoError() throws Exception
    {
        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setHeader("X-MailID", "mailid");
        message.setHeader("X-BatchID", "batchid");
        message.setHeader("X-MailType", "type");
        message.setRecipients(Message.RecipientType.TO, "john@doe.com");

        MailStatus status = new MailStatus(message, MailState.READY);
        Date date = new Date();
        status.setDate(date);

        assertEquals("messageId = [mailid], batchId = [batchid], state = [ready], date = [" + date.toString()
            + "], recipients = [john@doe.com], type = [type]", status.toString());
    }

    @Test
    public void verifyToStringWhenStatusHasError() throws Exception
    {
        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setHeader("X-MailID", "mailid");
        message.setHeader("X-BatchID", "batchid");
        message.setHeader("X-MailType", "type");
        message.setRecipients(Message.RecipientType.TO, "john@doe.com");

        MailStatus status = new MailStatus(message, MailState.READY);
        Date date = new Date();
        status.setDate(date);
        status.setError(new Exception("outer", new Exception("inner")));

        assertTrue(status.toString().startsWith("messageId = [mailid], batchId = [batchid], state = [ready], "
            + "date = [" + date.toString() + "], recipients = [john@doe.com], type = [type], "
            + "errorSummary = [Exception: inner], errorDescription = ["));
    }
}

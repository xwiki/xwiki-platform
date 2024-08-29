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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link org.xwiki.mail.ExtendedMimeMessage}.
 *
 * @version $Id$
 * @since 7.4.1
 */
class ExtendedMimeMessageTest
{
    private static final String XMAIL_TYPE_HEADER = "X-MailType";

    private static final String TEST_XMAIL_TYPE = "MyType";
    private static final String TEST_MESSAGE_ID = "messageId";
    private static final String TEST_CONTENT = "Content";

    public class ThrowingeExtendedMimeMessage extends ExtendedMimeMessage
    {
        @Override
        public void addHeader(String name, String value) throws MessagingException
        {
            throw new MessagingException("error");
        }

        @Override public String getHeader(String name, String delimiter) throws MessagingException
        {
            throw new MessagingException("error");
        }

        @Override public void setHeader(String name, String value) throws MessagingException
        {
            throw new MessagingException("error");
        }
    }

    @Test
    void wrap() throws Exception
    {
        ExtendedMimeMessage extendedMimeMessage = new ExtendedMimeMessage();
        assertThat(ExtendedMimeMessage.wrap(extendedMimeMessage), sameInstance(extendedMimeMessage));

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        mimeMessage.setText(TEST_CONTENT);
        assertThat(ExtendedMimeMessage.wrap(mimeMessage).getContent(), equalTo(TEST_CONTENT));
    }

    @Test
    void isEmpty() throws Exception
    {
        ExtendedMimeMessage message = new ExtendedMimeMessage();
        assertThat(message.isEmpty(), is(true));

        message.setText(TEST_CONTENT);
        assertThat(message.isEmpty(), is(false));
    }

    @Test
    void setType() throws Exception
    {
        ExtendedMimeMessage message = new ExtendedMimeMessage();
        message.setType(TEST_XMAIL_TYPE);

        assertThat(message.getHeader(XMAIL_TYPE_HEADER, null), equalTo(TEST_XMAIL_TYPE));
    }

    @Test
    void setTypeWhenException()
    {
        Throwable exception = assertThrows(RuntimeException.class, () -> {
            ExtendedMimeMessage message = new ThrowingeExtendedMimeMessage();
            message.setType(TEST_XMAIL_TYPE);
        });
        assertEquals("Failed to set Type header to [MyType]", exception.getMessage());
        assertEquals("MessagingException: error", ExceptionUtils.getRootCauseMessage(exception));
    }

    @Test
    void getType() throws Exception
    {
        ExtendedMimeMessage message = new ExtendedMimeMessage();
        message.setHeader(XMAIL_TYPE_HEADER, TEST_XMAIL_TYPE);

        assertThat(message.getType(), equalTo(TEST_XMAIL_TYPE));
    }

    @Test
    void getTypeWhenException()
    {
        Throwable exception = assertThrows(RuntimeException.class, () -> {
            ExtendedMimeMessage message = new ThrowingeExtendedMimeMessage();
            message.getType();
        });
        assertEquals("Failed to get Type header", exception.getMessage());
        assertEquals("MessagingException: error", ExceptionUtils.getRootCauseMessage(exception));
    }

    @Test
    void getMessageIdAndEnsureSaved() throws Exception
    {
        ExtendedMimeMessage message = new ExtendedMimeMessage();
        message.setText(TEST_CONTENT);
        message.setMessageId(TEST_MESSAGE_ID);
        message.writeTo(new OutputStream() { @Override public void write(int b) { } });

        assertThat(message.getMessageID(), equalTo(TEST_MESSAGE_ID));
    }

    @Test
    void getUniqueMessageId() throws Exception
    {
        ExtendedMimeMessage message = new ExtendedMimeMessage();
        message.setText(TEST_CONTENT);
        message.setMessageId(TEST_MESSAGE_ID);
        assertThat(message.getUniqueMessageId(), equalTo("wmK5jlxm4kPv2caEGeVtsDOT3zk="));

        // Verify that the unique id is modified.
        message.setRecipients(Message.RecipientType.TO, "john.doe@example.com");
        assertThat(message.getUniqueMessageId(), equalTo("g9tEjV2+qAGNIFaQ44+P+iZtZZw="));

        // Verify that the unique id is the same since the Cc address is the same as the To one.
        message.removeHeader("To");
        message.setRecipients(Message.RecipientType.CC, "john.doe@example.com");
        assertThat(message.getUniqueMessageId(), equalTo("g9tEjV2+qAGNIFaQ44+P+iZtZZw="));

        // Verify that the unique id is the same since the Bcc address is the same as the Cc and To ones.
        message.removeHeader("Cc");
        message.setRecipients(Message.RecipientType.BCC, "john.doe@example.com");
        assertThat(message.getUniqueMessageId(), equalTo("g9tEjV2+qAGNIFaQ44+P+iZtZZw="));

        message.setMessageId("AnotherID");
        assertThat(message.getUniqueMessageId(), equalTo("hdr6yyK2Tq9fKpv5hr5eMOL8XYA="));

        // Verify that the unique id is back to what it was above
        message.setMessageId(TEST_MESSAGE_ID);
        message.removeHeader("Bcc");
        message.setRecipients(Message.RecipientType.TO, "john.doe@example.com");
        assertThat(message.getUniqueMessageId(), equalTo("g9tEjV2+qAGNIFaQ44+P+iZtZZw="));
    }

    @Test
    void setMessageIdWhenException()
    {
        Throwable exception = assertThrows(RuntimeException.class, () -> {
            ExtendedMimeMessage message = new ThrowingeExtendedMimeMessage();
            message.setMessageId("whatever");
        });
        assertEquals("Failed to set Message ID header to [whatever]", exception.getMessage());
        assertEquals("MessagingException: error", ExceptionUtils.getRootCauseMessage(exception));
    }

    @Test
    void getAddExtraData()
    {
        ExtendedMimeMessage message = new ExtendedMimeMessage();
        List<String> extraData = new ArrayList<>();
        message.addExtraData("test", extraData);

        assertSame(extraData, message.getExtraData("test"));
    }
}

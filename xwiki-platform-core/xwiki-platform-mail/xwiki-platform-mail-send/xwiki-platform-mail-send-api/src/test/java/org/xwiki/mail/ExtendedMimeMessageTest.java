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

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link org.xwiki.mail.ExtendedMimeMessage}.
 *
 * @version $Id$
 * @since 7.4.1
 */
public class ExtendedMimeMessageTest
{
    private static final String XMAIL_TYPE_HEADER = "X-MailType";

    private static final String TEST_XMAIL_TYPE = "MyType";
    private static final String TEST_MESSAGE_ID = "messageId";
    private static final String TEST_CONTENT = "Content";

    @Test
    public void wrap() throws Exception
    {
        ExtendedMimeMessage extendedMimeMessage = new ExtendedMimeMessage();
        assertThat(ExtendedMimeMessage.wrap(extendedMimeMessage), sameInstance(extendedMimeMessage));

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        mimeMessage.setText(TEST_CONTENT);
        assertThat(ExtendedMimeMessage.wrap(mimeMessage).getContent(), equalTo(TEST_CONTENT));
    }

    @Test
    public void isEmpty() throws Exception
    {
        ExtendedMimeMessage message = new ExtendedMimeMessage();
        assertThat(message.isEmpty(), is(true));

        message.setText(TEST_CONTENT);
        assertThat(message.isEmpty(), is(false));
    }

    @Test
    public void setType() throws Exception
    {
        ExtendedMimeMessage message = new ExtendedMimeMessage();
        message.setType(TEST_XMAIL_TYPE);

        assertThat(message.getHeader(XMAIL_TYPE_HEADER, null), equalTo(TEST_XMAIL_TYPE));
    }

    @Test
    public void getType() throws Exception
    {
        ExtendedMimeMessage message = new ExtendedMimeMessage();
        message.setHeader(XMAIL_TYPE_HEADER, TEST_XMAIL_TYPE);

        assertThat(message.getType(), equalTo(TEST_XMAIL_TYPE));
    }

    @Test
    public void getMessageIdAndEnsureSaved() throws Exception
    {
        ExtendedMimeMessage message = new ExtendedMimeMessage();
        message.setText(TEST_CONTENT);
        message.setMessageId(TEST_MESSAGE_ID);
        message.writeTo(new OutputStream() { @Override public void write(int b) { } });

        assertThat(message.getMessageID(), equalTo(TEST_MESSAGE_ID));
    }

    @Test
    public void getUniqueMessageId() throws Exception
    {
        ExtendedMimeMessage message = new ExtendedMimeMessage();
        message.setText(TEST_CONTENT);
        message.setMessageId(TEST_MESSAGE_ID);
        assertThat(message.getUniqueMessageId(), equalTo("wmK5jlxm4kPv2caEGeVtsDOT3zk="));

        message.setRecipients(Message.RecipientType.TO, "john.doe@example.com");
        assertThat(message.getUniqueMessageId(), equalTo("g9tEjV2+qAGNIFaQ44+P+iZtZZw="));

        message.setMessageId("AnotherID");
        assertThat(message.getUniqueMessageId(), equalTo("hdr6yyK2Tq9fKpv5hr5eMOL8XYA="));

        message.setMessageId(TEST_MESSAGE_ID);
        assertThat(message.getUniqueMessageId(), equalTo("g9tEjV2+qAGNIFaQ44+P+iZtZZw="));
    }
}

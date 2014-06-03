/*
 *
 *  * See the NOTICE file distributed with this work for additional
 *  * information regarding copyright ownership.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.xwiki.mail.internal;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class DefaultMimeMessageFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMailSenderConfiguration> mocker =
            new MockitoComponentMockingRule<>(DefaultMailSenderConfiguration.class);

    @Test
    public void testCreate() throws Exception
    {

        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");

        when(documentsSource.getProperty("javamail_extra_props")).thenReturn("mail.smtp.starttls.enable=true");
        when(documentsSource.getProperty("smtp_server_username", (String) null)).thenReturn(null);
        when(documentsSource.getProperty("smtp_server_password", (String) null)).thenReturn(null);
        when(documentsSource.getProperty("smtp_server", (String) null)).thenReturn("server");
        when(documentsSource.getProperty("smtp_port", (Integer) null)).thenReturn(25);
        when(documentsSource.getProperty("smtp_from", (String) null)).thenReturn("devs@xwiki.org");

        Session session = Session.getInstance(this.mocker.getComponentUnderTest().getAllProperties());

        DefaultMimeMessageFactory mimeMessageFactory = new DefaultMimeMessageFactory();
        MimeMessage mimeMessage = mimeMessageFactory.create("john@doe.com", "Lorem ipsum", session);

        assertEquals(1, mimeMessage.getRecipients(MimeMessage.RecipientType.TO).length);
        assertEquals("john@doe.com", mimeMessage.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        assertEquals("Lorem ipsum", mimeMessage.getSubject());
    }
}
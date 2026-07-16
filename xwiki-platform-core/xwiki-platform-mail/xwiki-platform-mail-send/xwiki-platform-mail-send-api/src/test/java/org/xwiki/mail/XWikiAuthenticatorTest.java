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

import javax.mail.PasswordAuthentication;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.XWikiAuthenticator}.
 *
 * @version $Id$
 */
class XWikiAuthenticatorTest
{
    private static final String MAIL_USER = "mailuser";

    private static final String MAIL_PASSWORD = "mailpassword";

    @Test
    void javaMailPasswordAuthentication()
    {
        MailSenderConfiguration mailSenderConfiguration = mock(MailSenderConfiguration.class);
        when(mailSenderConfiguration.getUsername()).thenReturn(MAIL_USER);
        when(mailSenderConfiguration.getPassword()).thenReturn(MAIL_PASSWORD);

        XWikiAuthenticator xWikiAuthenticator = new XWikiAuthenticator(mailSenderConfiguration);
        PasswordAuthentication passwordAuthentication = xWikiAuthenticator.getPasswordAuthentication();

        assertEquals(MAIL_USER, passwordAuthentication.getUserName());
        assertEquals(MAIL_PASSWORD, passwordAuthentication.getPassword());
    }
}

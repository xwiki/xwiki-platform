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

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * XWiki Java Mail Authenticator taking the user name and password from a
 * {@link org.xwiki.mail.MailSenderConfiguration} instance.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class XWikiAuthenticator extends Authenticator
{
    private String username;

    private String password;

    /**
     * @param configuration the configuration from which to extract the SMTP server's user name and password to use
     */
    public XWikiAuthenticator(MailSenderConfiguration configuration)
    {
        // Note: We resolve the user name and password early since if we were doing it in #getPasswordAuthentication
        // then it would mean having an XWikiContext set up in the Mail Sender Thread (as the Configuration requires
        // an XWikiContext and #getPasswordAuthentication() is called indirectly in the sender thread.
        this.username = configuration.getUsername();
        this.password = configuration.getPassword();
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
        return new PasswordAuthentication(this.username, this.password);
    }
}

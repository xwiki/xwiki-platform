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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.Session;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.XWikiAuthenticator;

/**
 * Provide the JavaMail Session needed to create an instance of {@link javax.mail.internet.MimeMessage}.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Singleton
public class SessionProvider implements Provider<Session>
{
    @Inject
    private MailSenderConfiguration configuration;

    @Override
    public Session get()
    {
        Session session;
        if (this.configuration.usesAuthentication()) {
            session = Session.getInstance(this.configuration.getAllProperties(),
                new XWikiAuthenticator(this.configuration));
        } else {
            session = Session.getInstance(this.configuration.getAllProperties());
        }
        return session;
    }
}

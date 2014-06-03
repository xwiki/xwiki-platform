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

import java.util.Properties;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Represents all XWiki configuration options for the Mail Sending feature.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Role
@Unstable
public interface MailSenderConfiguration
{
    /**
     * @return the SMTP server
     */
    String getHost();

    /**
     * @return the SMTP server port
     */
    int getPort();

    /**
     * @return the SMTP user name to authenticate to the SMTP server, if any
     */
    String getUsername();

    /**
     * @return the SMTP password to authenticate to the SMTP server, if any
     */
    String getPassword();

    /**
     * @return the email address sending the email
     */
    String getFromAddress();

    /**
     * @return the list of properties to use when sending the mail
     *         (eg {@code mail.smtp.starttls.enable=true} if TLS should be used)
     */
    Properties getProperties();

    /**
     * @return if true then the SMTP server requires authentication
     */
    boolean usesAuthentication();
}

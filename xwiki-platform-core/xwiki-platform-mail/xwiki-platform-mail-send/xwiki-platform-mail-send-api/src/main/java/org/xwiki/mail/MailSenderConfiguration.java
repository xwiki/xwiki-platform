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

import java.util.List;
import java.util.Properties;

import org.xwiki.component.annotation.Role;

/**
 * Represents all XWiki configuration options for the Mail Sending feature.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Role
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
     * @return the default email address to use when sending the email. This is optional and if the user of the API sets
     *         it, then it overrides this default value
     */
    String getFromAddress();

    /**
     * @return the list of default email addresses to add to the BCC mail header when sending email.This is optional and
     *         if the user of the API sets it, then it overrides this default value
     * @since 6.4M2
     */
    List<String> getBCCAddresses();

    /**
     * @return the list of additional Java Mail properties (in addition to the host, port, username and from
     *         properties) to use when sending the mail (eg {@code mail.smtp.starttls.enable=true} if TLS should be
     *         used). See <a href="https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html">Java
     *         Mail Properties</a> for the full list of available properties.
     */
    Properties getAdditionalProperties();

    /**
     * @return the full list of Java Mail properties to use when sending the email (this includes the Java Mail
     *         properties for host ({@code mail.smtp.host}), port ({@code mail.smtp.port}),
     *         username ({@code mail.smtp.user}), from {@code mail.smtp.from}) + the all the additional properties
     *         from {@link #getAdditionalProperties()}
     */
    Properties getAllProperties();

    /**
     * @return if true then the SMTP server requires authentication
     */
    boolean usesAuthentication();

    /**
     * @return the hint of the {@link org.xwiki.mail.script.ScriptServicePermissionChecker} component to use to check if a
     *         mail is allowed to be sent or not when using the Mail Sender Script Service API. For example:
     *         "alwaysallow", "programmingrights".
     * @since 6.4M2
     */
    String getScriptServicePermissionCheckerHint();

    /**
     * @return the delay to wait between each mail being sent, in milliseconds. This is done to support mail throttling
     *         and not considered a spammer by mail servers.
     * @since 6.4RC1
     */
    long getSendWaitTime();

    /**
     * @return the max size of the prepare queue. When this size is reached calls to put new elements on the queue will
     *         block
     * @since 11.6RC1
     */
    default int getPrepareQueueCapacity()
    {
        return 1000;
    }

    /**
     * @return the max size of the send queue. When this size is reached calls to put new elements on the queue will
     *         block
     * @since 11.6RC1
     */
    default int getSendQueueCapacity()
    {
        return 1000;
    }
}

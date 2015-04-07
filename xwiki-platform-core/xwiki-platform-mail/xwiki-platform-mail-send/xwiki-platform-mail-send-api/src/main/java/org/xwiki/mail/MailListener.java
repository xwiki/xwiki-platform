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

import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Allows listening to Mail sending results.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Role
@Unstable
public interface MailListener
{
    /**
     * Called when the mail is ready to be sent but before it is actually sent.
     *
     * @param message the message to be sent
     * @param parameters some parameters specifying addition context data (for example the current wiki is stored under
     *        the {@code wiki} key)
     */
    void onPrepare(MimeMessage message, Map<String, Object> parameters);

    /**
     * Called when the mail has been sent successfully.
     *
     * @param message the message sent
     * @param parameters some parameters specifying addition context data (for example the current wiki is stored under
     *        the {@code wiki} key)
     */
    void onSuccess(MimeMessage message, Map<String, Object> parameters);

    /**
     * Called when the mail has failed to be sent.
     *
     * @param message the message that was tried to be sent
     * @param e the exception explaining why the message couldn't be sent
     * @param parameters some parameters specifying addition context data (for example the current wiki is stored under
     *        the {@code wiki} key)
     */
    void onError(MimeMessage message, Exception e, Map<String, Object> parameters);

    /**
     * @return the status of all the mails from the batch (whether they were sent successfully, failed to be sent,
     *         ready to be sent but not sent yet, etc). Note that since mails can be sent asynchronously it's possible
     *         that when calling this method, not all mails have been processed yet for sending and thus users or this
     *         method should call {@link MailStatusResult#getSize()} to ensure that they get the expected number and if
     *         not wait some more
     * @since 6.4M3
     */
    MailStatusResult getMailStatusResult();
}

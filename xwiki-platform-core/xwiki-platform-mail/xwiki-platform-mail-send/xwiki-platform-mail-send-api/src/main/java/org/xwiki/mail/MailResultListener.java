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

import javax.mail.internet.MimeMessage;

import org.xwiki.stability.Unstable;

/**
 * Allows listening to Mail sending results.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Unstable
public interface MailResultListener
{
    /**
     * Called when the mail is sent successfully.
     *
     * @param message the message sent
     */
    void onSuccess(MimeMessage message);

    /**
     * Called when the mail has failed to be sent.
     *
     * @param message the message that was tried to be sent
     * @param e the exception explaining why the message couldn't be sent
     */
    void onError(MimeMessage message, Exception e);
}

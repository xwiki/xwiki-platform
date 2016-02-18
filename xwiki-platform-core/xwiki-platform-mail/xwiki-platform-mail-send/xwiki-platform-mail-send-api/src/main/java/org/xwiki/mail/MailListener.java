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

import org.xwiki.component.annotation.Role;

/**
 * Allows listening to Mail sending results.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Role
public interface MailListener
{
    /**
     * Called when the preparation phase begins.
     *
     * @param batchId identifier of the batch being prepared
     * @param parameters some parameters specifying addition context data
     * @since 7.1RC1
     */
    void onPrepareBegin(String batchId, Map<String, Object> parameters);

    /**
     * Called when a mail has been prepared with success.
     *
     * @param message the message to be sent
     * @param parameters some parameters specifying addition context data
     * @since 7.4.1
     */
    void onPrepareMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters);

    /**
     * Called when a mail has failed to be prepared.
     *
     * @param message the message that was tried to be prepared
     * @param e the exception explaining why the message couldn't be sent
     * @param parameters some parameters specifying addition context data
     * @since 7.4.1
     */
    void onPrepareMessageError(ExtendedMimeMessage message, Exception e, Map<String, Object> parameters);

    /**
     * Called when the preparation phase has encounter a fatal error.
     * The batch in progress has been incompletely processed.
     *
     * @param exception the exception explaining why messages couldn't be prepared
     * @param parameters some parameters specifying addition context data
     * @since 7.1RC1
     */
    void onPrepareFatalError(Exception exception, Map<String, Object> parameters);

    /**
     * Called when the preparation phase is finished.
     *
     * @param parameters some parameters specifying addition context data
     * @since 7.1RC1
     */
    void onPrepareEnd(Map<String, Object> parameters);

    /**
     * Called when a mail has been sent successfully.
     *
     * @param message the message sent
     * @param parameters some parameters specifying addition context data
     * @since 7.4.1
     */
    void onSendMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters);

    /**
     * Called when a mail has failed to be sent.
     *
     * @param message the message that was tried to be sent
     * @param exception the exception explaining why the message couldn't be sent
     * @param parameters some parameters specifying addition context data
     * @since 7.4.1
     */
    void onSendMessageError(ExtendedMimeMessage message, Exception exception, Map<String, Object> parameters);

    /**
     * Called when a message could not be retrieve for sending.
     *
     * @param uniqueMessageId the unique message identifier that was tried to be load for sending
     * @param exception the exception explaining why the message couldn't be sent
     * @param parameters some parameters specifying addition context data
     * @since 7.4.1
     */
    void onSendMessageFatalError(String uniqueMessageId, Exception exception, Map<String, Object> parameters);

    /**
     * @return the status of all the mails from the batch (whether they were sent successfully, failed to be sent,
     *         ready to be sent but not sent yet, etc). Note that since mails can be sent asynchronously it's possible
     *         that when calling this method, not all mails have been processed yet for sending and thus users or this
     *         method should call {@link MailStatusResult#waitTillProcessed(long)}
     *         or {@link MailStatusResult#isProcessed()} to ensure that all mails have been processed
     * @since 6.4M3
     */
    MailStatusResult getMailStatusResult();
}

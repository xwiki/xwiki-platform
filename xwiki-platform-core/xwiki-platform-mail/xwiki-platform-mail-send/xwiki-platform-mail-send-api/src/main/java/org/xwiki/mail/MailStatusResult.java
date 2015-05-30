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

import java.util.Iterator;

/**
 * Provides status for each mail in the batch of mails that have been sent.
 *
 * @version $Id$
 * @since 6.4M3
 */
public interface MailStatusResult
{
    /**
     * @return the total number of mails to be sent in this batch. Note that this value is known only once all mails
     *         have been prepared on the Prepare Queue. Until then, the value returned is -1 signifying that the total
     *         number is currently unknown. WARNING: If the preparation fails, this number will not reflect the total
     *         number of mails that should have been processed, but the number of mail that have been prepared with
     *         or without success.
     * @since 7.1M2
     */
    long getTotalMailCount();

    /**
     * @return the current number of mails that have been processed (sent with success or failed at any stage).
     * @since 7.1M2
     */
    long getProcessedMailCount();

    /**
     * Wait till all messages on the sending queue have been sent (for this batch) before returning.
     *
     * @param timeout the maximum amount of time to wait in milliseconds
     * @since 7.1M2
     */
    void waitTillProcessed(long timeout);

    /**
     * @return true if all the mails from this batch have been processed (sent successfully or not) or false otherwise
     * @since 7.1M2
     */
    boolean isProcessed();

    /**
     * @return the status for all mails
     */
    Iterator<MailStatus> getAll();

    /**
     * @return the status for all messages in error.
     * @since 7.1RC1
     */
    Iterator<MailStatus> getAllErrors();

    /**
     * @param state the state to match (prepare_success, prepare_error, send_success, send_error or send_fatal_error)
     * @return the status for all mails matching the passed state
     */
    Iterator<MailStatus> getByState(MailState state);
}

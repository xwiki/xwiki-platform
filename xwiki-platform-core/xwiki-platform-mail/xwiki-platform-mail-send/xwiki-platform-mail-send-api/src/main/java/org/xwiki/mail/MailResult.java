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

/**
 * The result of the batch of emails being sent.
 *
 * @version $Id$
 * @since 6.4M3
 */
public interface MailResult
{
    /**
     * Wait till all messages on the sending queue have been sent (for this batch) before returning.
     *
     * @param timeout the maximum amount of time to wait in milliseconds
     * @since 6.4
     */
    void waitTillProcessed(long timeout);

    /**
     * @return true if all the mails from this batch have been processed (sent successfully or not) or false otherwise
     * @since 6.4RC1
     */
    boolean isProcessed();

    /**
     * @return the batch id for this session of mail sending
     */
    String getBatchId();
}

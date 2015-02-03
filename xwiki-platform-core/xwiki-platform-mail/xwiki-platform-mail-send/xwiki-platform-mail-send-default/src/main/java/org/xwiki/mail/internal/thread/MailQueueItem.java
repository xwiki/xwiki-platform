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
package org.xwiki.mail.internal.thread;

import javax.mail.Session;

import org.xwiki.mail.MailListener;

/**
 * Represents a Mail messages placed on the queue for processing.
 *
 * @version $Id$
 * @since 6.4
 */
public interface MailQueueItem
{
    /**
     * @return the JavaMail Session to be used when sending
     */
    Session getSession();

    /**
     * @return an optional listener to call when the mail is sent successfully or when there's an error
     */
    MailListener getListener();

    /**
     * @return the unique id of the batch
     */
    String getBatchId();

    /**
     * @return the id of the wiki that will be used to set the context when preparing and sending the Mime Message
     */
    String getWikiId();
}

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
     * @return the number of emails that have started the sending process (i.e. that have been through the "ready to
     *         send" state at least.
     */
    long getSize();

    /**
     * @return the status for all mails
     */
    Iterator<MailStatus> getAll();

    /**
     * @param state the stats to match (ready to send, sent successfully or failed to send)
     * @return the status for all mails matching the passed state
     */
    Iterator<MailStatus> getByState(MailState state);
}

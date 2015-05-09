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

import org.xwiki.mail.MailStatusResult;

/**
 * Extension of {@link MailStatusResult} to make it updateable; more specifically to update the number of mails to
 * send (which can be known only on the Prepare Thread when all mails have been iterated over and prepared) and also
 * to increment the number of mails sent (successfully or not).
 *
 * @version $Id$
 * @since 7.1M2
 */
public interface UpdateableMailStatusResult extends MailStatusResult
{
    /**
     * @param totalSize the number of mails to send
     */
    void setTotalSize(long totalSize);

    /**
     * Increment by one the number of mails sent (successfully or not).
     */
    void incrementCurrentSize();
}

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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Save and load mail to/from disk.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Role
@Unstable
public interface MailStore
{
    /**
     * Same MimeMessage to disk.
     *
     * @param message the save.
     */
    void save(MimeMessage message);

    /**
     * Load message saved on disk.
     *
     * @param batchID correspond to directory name on permanent directory
     * @param messageID correspond to file name of serialized MimeMessage
     * @return MimeMessage serialised on disk
     */
    MimeMessage load(String batchID, String messageID);
}

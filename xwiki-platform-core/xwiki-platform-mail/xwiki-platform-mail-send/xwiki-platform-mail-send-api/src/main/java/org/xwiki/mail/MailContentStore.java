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

import javax.mail.Session;

import org.xwiki.component.annotation.Role;

/**
 * Save, load and delete mail content.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Role
public interface MailContentStore
{
    /**
     * Save MimeMessage to the store.
     *
     * @param batchId the batch id of the message to be saved
     * @param message the message to serialize in the store
     * @throws MailStoreException when an error occurs when saving the message to the store
     * @since 7.4.1
     */
    void save(String batchId, ExtendedMimeMessage message) throws MailStoreException;

    /**
     * Load message from the store.
     *
     * @param session the JavaMail session used to send the mail
     * @param batchId the batch id of the message that was originally saved
     * @param uniqueMessageId the unique id of the message that was originally saved
     * @return the MimeMessage instance deserialized from the store
     * @throws MailStoreException when an error occurs when loading the message from the store
     * @since 7.4.1
     */
    ExtendedMimeMessage load(Session session, String batchId, String uniqueMessageId) throws MailStoreException;

    /**
     * Remove a message from the store.
     *
     * @param batchId the batch id of the message that was originally saved
     * @param uniqueMessageId the unique id of the message that was originally saved
     * @throws MailStoreException when an error occurs when deleting the message from the store
     */
    void delete(String batchId, String uniqueMessageId) throws MailStoreException;
}

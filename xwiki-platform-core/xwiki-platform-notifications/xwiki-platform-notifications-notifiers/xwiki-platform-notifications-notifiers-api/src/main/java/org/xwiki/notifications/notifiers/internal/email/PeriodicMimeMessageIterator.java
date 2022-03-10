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
package org.xwiki.notifications.notifiers.internal.email;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Role;
import org.xwiki.mail.MailListener;
import org.xwiki.model.reference.DocumentReference;

/**
 * Iterator used to generate emails for notifications. Generate MimeMessages.
 *
 * @version $Id$
 * @since 9.10RC1
 */
@Role
public interface PeriodicMimeMessageIterator extends Iterator<MimeMessage>, Iterable<MimeMessage>
{
    /**
     * Initialize the iterator.
     *
     * @param userIterator iterator that returns all users
     * @param factoryParameters parameters for the email factory
     * @param lastTrigger time of the last email sent
     * @param templateReference reference to the mail template
     */
    void initialize(NotificationUserIterator userIterator, Map<String, Object> factoryParameters,
            Date lastTrigger, DocumentReference templateReference);

    /**
     * @return the listener to notify about prepared mail
     */
    MailListener getMailListener();
}

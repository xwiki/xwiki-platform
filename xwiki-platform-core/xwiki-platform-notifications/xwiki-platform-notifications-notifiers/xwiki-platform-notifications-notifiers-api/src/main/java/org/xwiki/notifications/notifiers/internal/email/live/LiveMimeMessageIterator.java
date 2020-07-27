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
package org.xwiki.notifications.notifiers.internal.email.live;

import java.util.Iterator;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.notifiers.internal.email.NotificationUserIterator;

/**
 * MimeMessageIterator for sending live mail notifications.
 *
 * @version $Id$
 * @since 9.10RC1
 */
@Role
public interface LiveMimeMessageIterator extends Iterator<MimeMessage>, Iterable<MimeMessage>
{
    /**
     * Initialize the iterator.
     *
     * @param userIterator iterator that returns all users
     * @param factoryParameters parameters for the email factory
     * @param event the event that has to be sent to every user
     * @param templateReference reference to the mail template
     */
    void initialize(NotificationUserIterator userIterator, Map<String, Object> factoryParameters, CompositeEvent event,
        DocumentReference templateReference);
}

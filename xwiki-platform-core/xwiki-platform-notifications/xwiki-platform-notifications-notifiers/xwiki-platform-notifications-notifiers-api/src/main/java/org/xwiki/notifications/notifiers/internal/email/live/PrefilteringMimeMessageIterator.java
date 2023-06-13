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
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Role;
import org.xwiki.eventstream.Event;
import org.xwiki.mail.MailListener;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * MimeMessageIterator for sending pre filtered live mail notifications.
 *
 * @version $Id$
 * @since 12.6
 */
@Role
public interface PrefilteringMimeMessageIterator extends Iterator<MimeMessage>, Iterable<MimeMessage>
{
    /**
     * Initialize the iterator.
     *
     * @param events the events to send
     * @param factoryParameters parameters for the email factory
     * @param templateReference reference to the mail template
     */
    void initialize(Map<DocumentReference, List<Event>> events, Map<String, Object> factoryParameters,
        EntityReference templateReference);

    /**
     * @return the listener to notify about prepared mail
     */
    MailListener getMailListener();
}

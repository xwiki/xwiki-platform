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
package org.xwiki.notifications.notifiers.email;

import org.xwiki.component.annotation.Role;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;

/**
 * Render a notification for email sendings.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Role
public interface NotificationEmailRenderer
{
    /**
     * @param compositeEvent the event to render
     * @param userId id of the user who will receive the email
     * @return the HTML rendered version of the event
     * @throws NotificationException of error occurs
     * @since 9.10RC1
     */
    String renderHTML(CompositeEvent compositeEvent, String userId) throws NotificationException;

    /**
     * @param compositeEvent the event to render
     * @param userId id of the user who will receive the email
     * @return the plain text rendered version of the event
     * @throws NotificationException of error occurs
     * @since 9.10RC1
     */
    String renderPlainText(CompositeEvent compositeEvent, String userId) throws NotificationException;

    /**
     * @param compositeEvent the event to render
     * @param userId id of the user who will receive the email
     * @return the plain text subject for the email
     * @throws NotificationException of error occurs
     * @since 9.10RC1
     */
    String generateEmailSubject(CompositeEvent compositeEvent, String userId) throws NotificationException;
}

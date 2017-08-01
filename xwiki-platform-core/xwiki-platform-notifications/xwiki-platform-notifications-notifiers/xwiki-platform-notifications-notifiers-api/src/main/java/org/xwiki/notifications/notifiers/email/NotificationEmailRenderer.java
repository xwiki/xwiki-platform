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
import org.xwiki.stability.Unstable;

/**
 * Render a notification for email sendings.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Role
@Unstable
public interface NotificationEmailRenderer
{
    /**
     * @param compositeEvent the event to render
     * @return the HTML rendered version of the event
     * @throws NotificationException of error occurs
     */
    String renderHTML(CompositeEvent compositeEvent) throws NotificationException;

    /**
     * @param compositeEvent the event to render
     * @return the plain text rendered version of the event
     * @throws NotificationException of error occurs
     */
    String renderPlainText(CompositeEvent compositeEvent) throws NotificationException;

    /**
     * @param compositeEvent the event to render
     * @return the plain text subject for the email
     * @throws NotificationException of error occurs
     */
    String generateEmailSubject(CompositeEvent compositeEvent) throws NotificationException;
}

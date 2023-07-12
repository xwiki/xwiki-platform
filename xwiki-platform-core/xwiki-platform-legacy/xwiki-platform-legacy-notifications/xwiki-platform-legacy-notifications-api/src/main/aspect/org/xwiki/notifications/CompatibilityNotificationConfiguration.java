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
package org.xwiki.notifications;

/**
 * Deprecated methods of {@link NotificationConfiguration}.
 *
 * @version $Id$
 * @since 15.5RC1
 */
public interface CompatibilityNotificationConfiguration
{
    /**
     * @return true if pre-filtering should be used for notifications
     * @since 12.6
     * @deprecated The pre-filtering is now the standard way of using notifications and post-filtering is not
     * supported anymore (i.e. new code might not work with post-filtering).
     */
    @Deprecated(since = "15.5RC1")
    default boolean isEventPrefilteringEnabled()
    {
        return true;
    }
}

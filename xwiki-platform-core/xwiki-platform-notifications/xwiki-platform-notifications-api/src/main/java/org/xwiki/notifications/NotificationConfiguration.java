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

import org.xwiki.component.annotation.Role;

/**
 * Get the configuration options concerning the Notification module.
 * 
 * @version $Id$
 * @since 9.4RC1
 */
@Role
public interface NotificationConfiguration
{
    /**
     * @return true if the notification module should be enabled
     */
    boolean isEnabled();

    /**
     * @return true if the notification module can send emails
     * @since 9.5RC1
     */
    boolean areEmailsEnabled();

    /**
     * Get the number of minutes for the notification emails grace time. If the value is 0, then the email will be
     * instantly sent.
     *
     * @return the number of minutes that should last before sending a live notification mail
     * @since 9.6RC1
     */
    int liveNotificationsGraceTime();

    /**
     * @return true if the REST/async cache is enabled.
     * @since 12.2
     */
    default boolean isRestCacheEnabled()
    {
        return true;
    }

    /**
     * @return the number of threads to use for computing notifications in REST.
     * @since 12.5RC1
     */
    default int getRESTPoolSize()
    {
        return 2;
    }

    /**
     * @return the number of threads to use for computing notifications in Async renderer.
     * @since 12.5RC1
     */
    default int getAsyncPoolSize()
    {
        return 2;
    }

    /**
     * @return the hint of the component to be used for the email grouping strategy.
     * @since 15.5RC1
     */
    default String getEmailGroupingStrategyHint()
    {
        return "default";
    }
}

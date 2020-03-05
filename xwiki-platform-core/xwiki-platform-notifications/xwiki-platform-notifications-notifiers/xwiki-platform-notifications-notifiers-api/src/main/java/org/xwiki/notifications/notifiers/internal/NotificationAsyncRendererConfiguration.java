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
package org.xwiki.notifications.notifiers.internal;

import java.util.Arrays;
import java.util.HashSet;

import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.rendering.async.internal.AsyncRendererConfiguration;

/**
 * A configuration for the {@link DefaultAsyncNotificationRenderer}.
 *
 * @since 12.2RC1
 * @version $Id$
 */
public class NotificationAsyncRendererConfiguration extends AsyncRendererConfiguration
{
    private boolean isCount;
    private NotificationParameters notificationParameters;

    /**
     * Default constructor.
     * @param parameters the parameters to perform the request for notifications.
     * @param isCount {@code true} if the request is performed to retrieve only the number of notifications.
     */
    public NotificationAsyncRendererConfiguration(NotificationParameters parameters, boolean isCount)
    {
        this.isCount = isCount;
        this.notificationParameters = parameters;
        this.setContextEntries(new HashSet<>(Arrays.asList("user", "wiki")));
    }

    /**
     * @return {@code true} if the request is performed to retrieve only the number of notifications.
     */
    public boolean isCount()
    {
        return this.isCount;
    }

    /**
     * @return the parameters to perform the request for notifications.
     */
    public NotificationParameters getNotificationParameters()
    {
        return this.notificationParameters;
    }
}

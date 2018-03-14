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
package org.xwiki.notifications.filters.internal.minor;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.NotificationFormat;

/**
 * Filter that hide minor events in the alert menu.
 *
 * @version $Id$
 * @since 10.2RC1
 */
@Component
@Singleton
@Named(MinorEventAlertNotificationFilter.FILTER_NAME)
public class MinorEventAlertNotificationFilter extends AbstractMinorEventNotificationFilter
{
    /**
     * The name of the filter.
     */
    public static final String FILTER_NAME = "minorEventAlertNotificationFilter";

    /**
     * Construct an MinorEventAlertNotificationFilter.
     */
    public MinorEventAlertNotificationFilter()
    {
        super(FILTER_NAME, NotificationFormat.ALERT);
    }

    @Override
    public List<NotificationFormat> getFormats()
    {
        return Collections.singletonList(NotificationFormat.ALERT);
    }
}

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
package org.xwiki.notifications.sources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.preferences.NotificationPreference;

/**
 * @version $Id$
 * @since
 */
public class NotificationParameters
{
    public DocumentReference user;
    public NotificationFormat format;
    public int expectedCount;
    public Date endDate;
    public Date fromDate;
    public Collection<String> blackList = new ArrayList<>();
    public Collection<NotificationPreference> preferences = Collections.emptyList();
    public Collection<NotificationFilterPreference> filterPreferences = Collections.emptyList();
    public Collection<NotificationFilter> filters= Collections.emptyList();
    public Map<String, Boolean> filterActivations;
}

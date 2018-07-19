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
package org.xwiki.notifications.filters.internal;

import org.xwiki.notifications.filters.NotificationFilterProperty;

/**
 * @version $Id$
 * @since
 */
public class ExplodedFilterPreferenceReference
{
    private String objectName;

    private String propertyName;

    private String location;

    public ExplodedFilterPreferenceReference(String explodedPreferenceName)
    {
        String[] properties = explodedPreferenceName.split("_");
        objectName = properties[1];
        propertyName = properties[2];
        if (propertyName.equals(NotificationFilterProperty.PAGE.name())) {
            propertyName = DefaultModelBridge.FIELD_PAGES;
        } else if (propertyName.equals(NotificationFilterProperty.SPACE.name())) {
            propertyName = DefaultModelBridge.FIELD_SPACES;
        } else if (propertyName.equals(NotificationFilterProperty.WIKI.name())) {
            propertyName = DefaultModelBridge.FIELD_WIKIS;
        }
        location = properties[3];
        for (int i = 4; i < properties.length; ++i) {
            location += properties[i];
        }
    }

    public String getObjectName()
    {
        return objectName;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public String getLocation()
    {
        return location;
    }
}

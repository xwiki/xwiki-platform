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

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.model.reference.DocumentReference;

/**
 * Provide information about the activation of {@link ToggleableNotificationFilter}.
 *
 * @version $Id$
 * @since 16.2.0RC1
 */
public class ToggleableNotificationFilterActivation implements Serializable
{
    private final String name;
    private final boolean isEnabled;
    private final DocumentReference objectLocation;
    private final int objectNumber;

    /**
     * Default constructor.
     *
     * @param name the name of the filter
     * @param isEnabled {@code true} if the filter is enabled
     * @param objectLocation the location of the object holding activation information
     * @param objectNumber the number of the actual object holding activation information or {@code -1} if there's no
     *                     object
     */
    public ToggleableNotificationFilterActivation(String name, boolean isEnabled, DocumentReference objectLocation,
        int objectNumber)
    {
        this.name = name;
        this.isEnabled = isEnabled;
        this.objectLocation = objectLocation;
        this.objectNumber = objectNumber;
    }

    /**
     * @return the name of the filter
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return {@code true} if the filter is enabled
     */
    public boolean isEnabled()
    {
        return isEnabled;
    }

    /**
     * @return the location of the object holding activation information
     */
    public DocumentReference getObjectLocation()
    {
        return objectLocation;
    }

    /**
     * @return the number of the actual object holding activation information or {@code -1} if there's no object
     */
    public int getObjectNumber()
    {
        return objectNumber;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ToggleableNotificationFilterActivation that = (ToggleableNotificationFilterActivation) o;

        return new EqualsBuilder()
            .append(isEnabled, that.isEnabled)
            .append(objectNumber, that.objectNumber)
            .append(name, that.name)
            .append(objectLocation, that.objectLocation)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(15, 33)
            .append(name)
            .append(isEnabled)
            .append(objectLocation)
            .append(objectNumber)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("name", name)
            .append("isEnabled", isEnabled)
            .append("objectLocation", objectLocation)
            .append("objectNumber", objectNumber)
            .toString();
    }
}

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
package org.xwiki.resource.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents the action to be executed on an Entity Resource (eg "view", "delete", "get", etc).
 *
 * @version $Id$
 * @since 6.1M2
 */
public class EntityResourceAction
{
    /**
     * The View Action.
     */
    public static final EntityResourceAction VIEW = new EntityResourceAction("view");

    /**
     * The action name (e.g. "view", "download", etc).
     */
    private String actionName;

    /**
     * @param actionName see {@link #getActionName()}
     */
    public EntityResourceAction(String actionName)
    {
        this.actionName = actionName;
    }

    /**
     * Converts a string to an {@link EntityResourceAction} instance.
     *
     * @param actionName see {@link #getActionName()}
     * @return the object representing the action passed as a string
     */
    public static EntityResourceAction fromString(String actionName)
    {
        return new EntityResourceAction(actionName);
    }

    /**
     * @return the action name (eg "view")
     */
    public String getActionName()
    {
        return this.actionName;
    }

    @Override
    public String toString()
    {
        return getActionName();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(13, 7)
            .append(getActionName())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        EntityResourceAction rhs = (EntityResourceAction) object;
        return new EqualsBuilder()
            .append(getActionName(), rhs.getActionName())
            .isEquals();
    }
}

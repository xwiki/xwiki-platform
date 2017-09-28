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
package org.xwiki.notifications.filters.internal.scope;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;

/**
 * @version $Id$
 * @since 9.9RC1
 */
class LocationFilter
{
    private AbstractOperatorNode node;

    private EntityReference location;

    private NotificationFilterType type;

    private boolean hasParent = false;

    private List<LocationFilter> children = new ArrayList<>();

    public LocationFilter(AbstractOperatorNode node, EntityReference location,
            NotificationFilterType type)
    {
        this.node = node;
        this.location = location;
        this.type = type;
    }

    public AbstractOperatorNode getNode()
    {
        return node;
    }

    public EntityReference getLocation()
    {
        return location;
    }

    public NotificationFilterType getType()
    {
        return type;
    }

    public List<LocationFilter> getChildren()
    {
        return children;
    }

    public boolean hasParent()
    {
        return hasParent;
    }

    public void setHasParent(boolean hasParent)
    {
        this.hasParent = hasParent;
    }

    public boolean isParentOf(LocationFilter otherFilter)
    {
        return otherFilter.getLocation().hasParent(this.location);
    }
}

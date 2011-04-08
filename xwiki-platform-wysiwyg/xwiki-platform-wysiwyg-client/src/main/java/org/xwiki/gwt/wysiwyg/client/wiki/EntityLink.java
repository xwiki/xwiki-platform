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
package org.xwiki.gwt.wysiwyg.client.wiki;

/**
 * Defines a link from an entity to a resource.
 * 
 * @version $Id$
 * @param <T> the type of data associated with the link
 */
public class EntityLink<T>
{
    /**
     * The entity that initiates the link.
     */
    private EntityReference origin;

    /**
     * The resource that is targeted by the link.
     */
    private ResourceReference destination;

    /**
     * Link data.
     */
    private T data;

    /**
     * Default constructor.
     */
    public EntityLink()
    {
    }

    /**
     * Explicit constructor.
     * 
     * @param origin the link origin
     * @param destination the link destination
     * @param data the link data
     */
    public EntityLink(EntityReference origin, ResourceReference destination, T data)
    {
        this.origin = origin;
        this.destination = destination;
        this.data = data;
    }

    /**
     * @return the link origin
     */
    public EntityReference getOrigin()
    {
        return origin;
    }

    /**
     * Sets the link origin.
     * 
     * @param origin the new link origin
     */
    public void setOrigin(EntityReference origin)
    {
        this.origin = origin;
    }

    /**
     * @return the link destination
     */
    public ResourceReference getDestination()
    {
        return destination;
    }

    /**
     * Sets the link destination.
     * 
     * @param destination the new link destination
     */
    public void setDestination(ResourceReference destination)
    {
        this.destination = destination;
    }

    /**
     * @return the link data
     */
    public T getData()
    {
        return data;
    }

    /**
     * Sets the link data.
     * 
     * @param data the new link data
     */
    public void setData(T data)
    {
        this.data = data;
    }
}

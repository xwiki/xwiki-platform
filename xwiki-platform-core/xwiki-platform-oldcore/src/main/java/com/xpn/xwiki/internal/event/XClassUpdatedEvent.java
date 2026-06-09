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
package com.xpn.xwiki.internal.event;

import com.xpn.xwiki.objects.PropertyInterface;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;

/**
 * Class updated entity event. The entity is the class reference.
 * @since 18.5.0
 * @version $Id$
 */
public class XClassUpdatedEvent extends AbstractEntityEvent
{
    private final Collection<PropertyInterface[]> updatedProperties;

    /**
     * Default constructor. Matches any {@link XClassUpdatedEvent}.
     */
    public XClassUpdatedEvent()
    {
        updatedProperties = null;
    }

    /**
     * @param classReference the class reference
     * @param updatedProperties the pairs of [old, new] property updates.
     *                          old is null for new properties and new is null for removed properties
     */
    public XClassUpdatedEvent(DocumentReference classReference, Collection<PropertyInterface[]> updatedProperties)
    {
        super(classReference);
        this.updatedProperties = updatedProperties;
    }

    @Override
    public DocumentReference getReference()
    {
        return (DocumentReference) super.getReference();
    }

    /**
     * @return the pairs of [old, new] property updates.
     *         old is null for new properties and new is null for removed properties
     */
    public Collection<PropertyInterface[]> getUpdatedProperties()
    {
        return this.updatedProperties;
    }
}

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
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.model.reference.DocumentReference;

import java.util.Collection;
import java.util.Objects;

/**
 * Class updated entity event. The entity is the class reference.
 * <p>
 * The following parameters get sent:
 * </p>
 * <ul>
 * <li>source: the current {com.xpn.xwiki.doc.XWikiDocument} instance from which you can get the "original" document
 * (the version before all the modifications)</li>
 * <li>data: the current {com.xpn.xwiki.XWikiContext} instance</li>
 * </ul>
 * @since 18.5.0
 * @version $Id$
 */
public class XClassUpdatedEvent extends AbstractEntityEvent
{
    private final Collection<Pair<PropertyInterface, PropertyInterface>> updatedProperties;

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
    public XClassUpdatedEvent(DocumentReference classReference,
            Collection<Pair<PropertyInterface, PropertyInterface>> updatedProperties)
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
    public Collection<Pair<PropertyInterface, PropertyInterface>> getUpdatedProperties()
    {
        return this.updatedProperties;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj)) {
            return true;
        }

        return getClass() == obj.getClass()
                && Objects.equals(this.updatedProperties, ((XClassUpdatedEvent) obj).getUpdatedProperties());
    }

    @Override
    public int hashCode()
    {
        return 3 * super.hashCode() + (updatedProperties == null ? 0 : 5 * updatedProperties.hashCode());
    }
}

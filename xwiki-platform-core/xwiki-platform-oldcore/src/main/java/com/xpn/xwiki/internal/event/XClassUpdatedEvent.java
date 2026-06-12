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


/**
 * Class updated entity event. The entity is the class reference.
 * <p>
 * The following parameters get sent:
 * </p>
 * <ul>
 * <li>source: the current {com.xpn.xwiki.doc.XWikiDocument} instance from which you can get the "original" document
 * (the version before all the modifications)</li>
 * <li>data: the collection of property updates ({@code Collection<PropertyUpdate>})</li>
 * </ul>
 * @since 18.5.0
 * @version $Id$
 */
public class XClassUpdatedEvent extends AbstractEntityEvent
{
    /**
     * @param oldProperty the property before the document update, null if the document is new
     * @param newProperty the property after the document update, null if it was removed
     */
    public record PropertyUpdate(PropertyInterface oldProperty, PropertyInterface newProperty) { }

    /**
     * Default constructor. Matches any {@link XClassUpdatedEvent}.
     */
    public XClassUpdatedEvent()
    {
        // Nothing to do
    }

    /**
     * @param classReference the class reference
     */
    public XClassUpdatedEvent(DocumentReference classReference)
    {
        super(classReference);
    }

    @Override
    public DocumentReference getReference()
    {
        return (DocumentReference) super.getReference();
    }
}

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
package org.xwiki.annotation.reference.internal;

import org.xwiki.annotation.reference.TypedStringEntityReferenceSerializer;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * @version $Id$
 * @since 2.3M1
 */
public abstract class AbstractTypedStringEntityReferenceSerializer implements TypedStringEntityReferenceSerializer
{
    /**
     * {@inheritDoc} <br />
     * Override to add the protocol in front of the serialization.
     * 
     * @see TypedStringEntityReferenceSerializer#serialize(org.xwiki.model.reference.EntityReference)
     */
    @Override
    public String serialize(EntityReference reference)
    {
        // serialize
        String serialization = getSerializer().serialize(reference);
        // and add the protocol in front
        return reference.getType() + "://" + serialization;
    }

    /**
     * @return the serializer used to serialize the reference before adding the type in front. Override to implement a
     *         specific strategy for serialization.
     */
    protected abstract EntityReferenceSerializer<String> getSerializer();
}

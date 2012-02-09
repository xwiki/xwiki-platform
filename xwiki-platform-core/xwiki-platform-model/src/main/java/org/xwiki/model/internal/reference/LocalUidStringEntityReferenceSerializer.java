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

package org.xwiki.model.internal.reference;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * Serialize a reference into a unique identifier string within a wiki. Its similar to the
 * {@link UidStringEntityReferenceSerializer}, but is made appropriate for a wiki independent storage.
 *
 * ie: The string created looks like 5:space3:doc for the wiki:space.doc document.
 * and 5:space3:doc15:xspace.class[0] for the wiki:space.doc^wiki:xspace.class[0] object.
 *
 * @version $Id$
 * @since 4.0M1
 * @see UidStringEntityReferenceSerializer
 */
@Component
@Named("local/uid")
@Singleton
public class LocalUidStringEntityReferenceSerializer extends UidStringEntityReferenceSerializer
{
    @Override
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        boolean isLastReference, Object... parameters)
    {
        if (currentReference.getType() != EntityType.WIKI) {
            super.serializeEntityReference(currentReference, representation, isLastReference, parameters);
        }
    }
}

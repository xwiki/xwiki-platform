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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Generate a entity reference but without the wiki reference part. This is an implementation use for backward
 * compatibility only and it should be dropped in the future since there's no reason to remove the wiki name
 * systematically
 * (usually we don't want to print it but only if it's the same as the current wiki).
 *
 * @version $Id$
 * @since 2.2.3
 * @deprecated you may use {@link EntityReference#removeParent(org.xwiki.model.reference.EntityReference)} since 4.0M2
 */
@Deprecated
@Component("local/reference")
public class DeprecatedLocalReferenceEntityReferenceSerializer implements EntityReferenceSerializer
{
    @Override
    public EntityReference serialize(EntityReference reference, Object... parameters)
    {
        EntityReference newReference = null;
        EntityReference parent;
        for (EntityReference currentReference = reference; currentReference != null; currentReference =
            currentReference.getParent())
        {
            if (currentReference.getType() == EntityType.WIKI) {
                return newReference;
            }

            parent = new EntityReference(currentReference.getName(), currentReference.getType());
            if (newReference != null) {
                newReference = newReference.appendParent(parent);
            } else {
                newReference = parent;
            }
        }

        return newReference;
    }
}

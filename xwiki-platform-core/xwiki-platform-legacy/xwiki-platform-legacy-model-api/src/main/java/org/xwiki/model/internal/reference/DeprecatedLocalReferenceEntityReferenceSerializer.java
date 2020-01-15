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

import javax.inject.Singleton;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Same as LocalReferenceEntityReferenceSerializer but with the extended type in the role hint instead of the role type.
 * 
 * @version $Id$
 * @since 2.2.3
 * @deprecated you may use {@link EntityReference#removeParent(org.xwiki.model.reference.EntityReference)} since 4.0M2
 */
@Deprecated
@Component
@Named("local/reference")
@Singleton
public class DeprecatedLocalReferenceEntityReferenceSerializer implements EntityReferenceSerializer
{
    @Override
    public EntityReference serialize(EntityReference reference, Object... parameters)
    {
        EntityReference newReference = null;
        EntityReference parent;
        for (EntityReference currentReference = reference; currentReference != null; currentReference =
            currentReference.getParent()) {
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

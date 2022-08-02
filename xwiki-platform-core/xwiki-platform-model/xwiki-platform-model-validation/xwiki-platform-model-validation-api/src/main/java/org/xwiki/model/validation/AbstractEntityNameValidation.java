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
package org.xwiki.model.validation;

import org.xwiki.model.reference.EntityReference;

/**
 * Define generic methods for transforming or validating entities based on the implementation of the
 * {@link #transform(String)} and {@link #isValid(String)}.
 *
 * @version $Id$
 * @since 12.0RC1
 */
public abstract class AbstractEntityNameValidation implements EntityNameValidation
{
    @Override
    public EntityReference transform(EntityReference entityReference)
    {
        EntityReference parentReference = null;

        if (entityReference.getParent() != null) {
            parentReference = transform(entityReference.getParent());
        }

        String newName = transform(entityReference.getName());
        return new EntityReference(newName, entityReference.getType(), parentReference,
            entityReference.getParameters());
    }

    @Override
    public boolean isValid(EntityReference entityReference)
    {
        if (entityReference == null) {
            return true;
        }
        return isValid(entityReference.getParent()) && isValid(entityReference.getName());
    }
}

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
package com.xpn.xwiki.internal.model.reference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.PageObjectReference;
import org.xwiki.model.reference.PageObjectReferenceResolver;

/**
 * Specialized version of {@link org.xwiki.model.reference.EntityReferenceResolver} which can be considered a helper
 * component to resolve {@link PageObjectReference} objects from Entity Reference (when they miss some parent references
 * or have NULL values). The behavior is the one defined in
 * {@link com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider}.
 *
 * @version $Id$
 * @since 10.6RC1
 */
@Component
@Named("current")
@Singleton
public class CurrentReferencePageObjectReferenceResolver implements PageObjectReferenceResolver<EntityReference>
{
    /**
     * Used to actually resolve the provided entity references.
     */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> entityReferenceResolver;

    @Override
    public PageObjectReference resolve(EntityReference objectReferenceRepresentation, Object... parameters)
    {
        return new PageObjectReference(
            this.entityReferenceResolver.resolve(objectReferenceRepresentation, EntityType.PAGE_OBJECT, parameters));
    }
}

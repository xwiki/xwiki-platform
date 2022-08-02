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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;

/**
 * Specialized version of {@link org.xwiki.model.reference.EntityReferenceResolver} which can be considered a helper
 * component to resolve {@link PageReference} objects from their partial representation passed as
 * {@link org.xwiki.model.reference.EntityReference}. The behavior is the one defined in
 * {@link org.xwiki.model.internal.reference.ExplicitReferenceEntityReferenceResolver}.
 * 
 * @version $Id$
 * @since 10.6RC1
 */
@Component
@Named("explicit")
@Singleton
public class ExplicitReferencePageReferenceResolver implements PageReferenceResolver<EntityReference>
{
    /**
     * Default entity reference resolver used for resolution.
     */
    @Inject
    @Named("explicit")
    private EntityReferenceResolver<EntityReference> entityReferenceResolver;

    @Override
    public PageReference resolve(EntityReference documentReferenceRepresentation, Object... parameters)
    {
        return new PageReference(
            this.entityReferenceResolver.resolve(documentReferenceRepresentation, EntityType.PAGE, parameters));
    }
}

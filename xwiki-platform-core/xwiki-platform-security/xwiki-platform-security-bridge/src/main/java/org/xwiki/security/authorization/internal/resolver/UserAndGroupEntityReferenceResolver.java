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
package org.xwiki.security.authorization.internal.resolver;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.AbstractStringEntityReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.security.internal.XWikiConstants;

/**
 * Specialized version of {@link org.xwiki.model.reference.EntityReferenceResolver<String>} which ensure the proper
 * space is used to find user documents and allow overwriting the wiki only.
 * 
 * @version $Id$
 * @since 6.2M1
 */
@Component(hints = {"user", "group"})
@Singleton
public class UserAndGroupEntityReferenceResolver extends AbstractStringEntityReferenceResolver
{
    /**
     * Entity reference value provider used to provide default value.
     */
    @Inject
    private EntityReferenceProvider provider;

    @Override
    public EntityReference resolve(String entityReferenceRepresentation, EntityType type, Object... parameters)
    {
        // Special case: if null is passed then consider it's the guest user and return null.
        if (entityReferenceRepresentation == null) {
            return null;
        } else {
            return super.resolve(entityReferenceRepresentation, type, parameters);
        }
    }

    @Override
    protected EntityReference getDefaultReference(EntityType type, Object... parameters)
    {
        if (type == EntityType.SPACE) {
            return XWikiConstants.XWIKI_SPACE_REFERENCE;
        } else {
            return this.provider.getDefaultReference(type);
        }
    }
}

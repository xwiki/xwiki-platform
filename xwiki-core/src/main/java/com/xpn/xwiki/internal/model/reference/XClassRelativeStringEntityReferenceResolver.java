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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.AbstractStringEntityReferenceResolver;
import org.xwiki.model.reference.EntityReference;

/**
 * Resolve a String representing an Entity Reference into an {@link org.xwiki.model.reference.EntityReference} object.
 * The behavior is the one defined in
 * {@link org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver} except that it uses a space with
 * the "XWiki" value if no space is specified.
 *
 * @version $Id$
 * @since 2.2.3
 * @deprecated this is only a backward compatibility resolver since the old behavior for class reference was to use
 *             the "XWiki" space when no space was specified. Since we now pass absolute references there's no need
 *             to resolve anything...
 */
@Deprecated
@Component("xclass")
public class XClassRelativeStringEntityReferenceResolver extends AbstractStringEntityReferenceResolver
{
    /**
     * {@inheritDoc}
     * @see AbstractStringEntityReferenceResolver#getDefaultValue(org.xwiki.model.EntityType, Object...)
     */
    @Override
    protected String getDefaultValue(EntityType type, Object... parameters)
    {
        // Return null to signify to the generic algorithm that we don't want to generate references with default
        // values, in order to obtain a relative reference.
        return null;
    }

    /**
     * {@inheritDoc}
     * @see AbstractStringEntityReferenceResolver#resolve(String, org.xwiki.model.EntityType, Object...)
     */
    @Override
    public EntityReference resolve(String entityReferenceRepresentation, EntityType type,
        Object... parameters)
    {
        // Note: We voluntarily ignore any passed parameter.
        return super.resolve(entityReferenceRepresentation, type, new EntityReference("XWiki", EntityType.SPACE));
    }
}

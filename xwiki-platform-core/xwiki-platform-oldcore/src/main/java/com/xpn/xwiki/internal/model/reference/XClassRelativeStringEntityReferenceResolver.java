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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.AbstractStringEntityReferenceResolver;
import org.xwiki.model.reference.EntityReference;

/**
 * Resolve a String representing an Entity Reference into an {@link org.xwiki.model.reference.EntityReference} object.
 * The behavior is the one defined in {@link org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver}
 * except that it uses a space with the "XWiki" value if no space is specified and that an optional parameter can be
 * passed to specify what page name to use if no page is specified in the passed string representation.
 * 
 * @version $Id$
 * @since 2.2.3
 * @deprecated this is only a backward compatibility resolver since the old behavior for class reference was to use the
 *             "XWiki" space when no space was specified. Since we now pass absolute references there's no need to
 *             resolve anything...
 */
@Deprecated
@Component
@Named("xclass")
@Singleton
public class XClassRelativeStringEntityReferenceResolver extends AbstractStringEntityReferenceResolver
{
    @Override
    protected String getDefaultValue(EntityType type, Object... parameters)
    {
        if (type == EntityType.DOCUMENT) {
            // This means that the user has not passed an optional page reference and we don't have a fallback, we
            // raise an error.
            throw new IllegalArgumentException("A Reference to a page must be passed as a parameter when the string "
                + "to resolve doesn't specify a page");
        }

        return null;
    }

    @Override
    public EntityReference resolve(String entityReferenceRepresentation, EntityType type, Object... parameters)
    {
        // We allow to pass a page reference in parameter. If the passed representation doesn't contain a page then the
        // page from the parameter will be used.
        EntityReference explicitReference = new EntityReference("XWiki", EntityType.SPACE);
        if (parameters.length > 0 && (parameters[0] instanceof EntityReference)) {
            EntityReference extractedPageReference =
                ((EntityReference) parameters[0]).extractReference(EntityType.DOCUMENT);
            if (extractedPageReference != null) {
                explicitReference = new EntityReference(extractedPageReference.getName(), EntityType.DOCUMENT,
                    explicitReference);
            }
        }

        return super.resolve(entityReferenceRepresentation, type, explicitReference);
    }
}

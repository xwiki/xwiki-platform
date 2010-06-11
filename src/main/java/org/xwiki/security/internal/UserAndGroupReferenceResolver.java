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
package org.xwiki.security.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Specialized version of {@link org.xwiki.model.reference.EntityReferenceResolver} which can be considered a helper
 * component to resolve {@link DocumentReference} objects from their string representation.  This resolver
 * is specialized for generating document references of user and group documents.  The resolve
 * 
 * @version $Id: $
 */
@Component(hints = { "user", "group" })
public class UserAndGroupReferenceResolver implements DocumentReferenceResolver<String>
{
    /** Default user space. */
    private static final String DEFAULT_USER_SPACE = "XWiki";

    /** Internally used resolver. */
    @Requirement private EntityReferenceResolver<String> resolver;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.model.reference.DocumentReferenceResolver#resolve
     */
    public DocumentReference resolve(String documentReferenceRepresentation, Object... parameters)
    {
        if (parameters.length == 1 && !(parameters[0] instanceof String)) {
            throw new IllegalArgumentException("The resolver parameter is not a String.");
        }
        EntityReference defaultSpace;
        if (parameters.length == 1) {
            EntityReference defaultWiki = new EntityReference((String) parameters[0], EntityType.WIKI, null);
            defaultSpace = new EntityReference(DEFAULT_USER_SPACE, EntityType.SPACE, defaultWiki);
        } else {
            defaultSpace = resolver.resolve(DEFAULT_USER_SPACE, EntityType.SPACE);
        }
        return new DocumentReference(resolver.resolve(documentReferenceRepresentation,
                                                      EntityType.DOCUMENT,
                                                      defaultSpace));
    }
}

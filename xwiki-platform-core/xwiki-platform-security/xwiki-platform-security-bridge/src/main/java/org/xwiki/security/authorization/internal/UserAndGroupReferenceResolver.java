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
package org.xwiki.security.authorization.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Specialized version of {@link org.xwiki.model.reference.DocumentReferenceResolver<String>} which ensure the
 * proper space is used to find user documents and allow overwriting the wiki only.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component(hints = { "user", "group" })
@Singleton
public class UserAndGroupReferenceResolver implements DocumentReferenceResolver<String>
{
    /** Internally used resolver. */
    @Inject
    private EntityReferenceResolver<String> resolver;

    @Override
    public DocumentReference resolve(String documentReferenceRepresentation, Object... parameters)
    {
        if (parameters.length > 0 && !(parameters[0] instanceof EntityReference)) {
            throw new IllegalArgumentException("The settler parameter is not a WikiReference.");
        }
        EntityReference defaultSpace;
        if (parameters.length > 0) {
            EntityReference defaultWiki = ((EntityReference) parameters[0]).extractReference(EntityType.WIKI);
            defaultSpace = new EntityReference(XWikiConstants.WIKI_SPACE, EntityType.SPACE, defaultWiki);
        } else {
            defaultSpace = resolver.resolve(XWikiConstants.WIKI_SPACE, EntityType.SPACE);
        }
        return new DocumentReference(resolver.resolve(documentReferenceRepresentation,
                                                      EntityType.DOCUMENT,
                                                      defaultSpace));
    }
}

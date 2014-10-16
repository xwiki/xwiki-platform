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
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Specialized version of {@link org.xwiki.model.reference.DocumentReferenceResolver<String>} which ensure the proper
 * space is used to find user documents and allow overwriting the wiki only. The default wiki is the standard default
 * wiki.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component(hints = {"user", "group" })
@Singleton
public class UserAndGroupDocumentReferenceResolver implements DocumentReferenceResolver<String>
{
    @Inject
    @Named("user")
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Override
    public DocumentReference resolve(String documentReferenceRepresentation, Object... parameters)
    {
        EntityReference entityReference =
            this.entityReferenceResolver.resolve(documentReferenceRepresentation, EntityType.DOCUMENT, parameters);
        return entityReference == null ? null : new DocumentReference(entityReference);
    }
}

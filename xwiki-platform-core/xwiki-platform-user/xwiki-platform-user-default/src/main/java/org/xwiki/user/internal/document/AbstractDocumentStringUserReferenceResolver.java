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
package org.xwiki.user.internal.document;

import javax.inject.Inject;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.user.UserReference;

/**
 * Common code for all the converters from a {@link String} representing a user id into a {@link UserReference}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public abstract class AbstractDocumentStringUserReferenceResolver extends AbstractUserReferenceResolver<String>
{
    private static final EntityReference USER_SPACE_REFERENCE = new EntityReference("XWiki", EntityType.SPACE);

    @Inject
    private EntityReferenceProvider entityReferenceProvider;

    protected UserReference resolve(String userName, DocumentReferenceResolver<String> resolver, Object[] parameters)
    {
        UserReference reference = resolveName(userName);
        if (reference == null) {
            EntityReference baseEntityReference;
            if (parameters.length == 1 && parameters[0] instanceof WikiReference) {
                baseEntityReference = new EntityReference(USER_SPACE_REFERENCE, (WikiReference) parameters[0]);
            } else {
                baseEntityReference = USER_SPACE_REFERENCE;
            }
            reference = new DocumentUserReference(resolver.resolve(userName, baseEntityReference), this.entityReferenceProvider);
        }
        return reference;
    }
}

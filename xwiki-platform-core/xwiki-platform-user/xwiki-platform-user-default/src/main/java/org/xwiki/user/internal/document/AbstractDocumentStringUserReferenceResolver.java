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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.user.CurrentUserReference;
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

    @Override
    public UserReference resolve(String userName, Object... parameters)
    {
        UserReference reference;
        if (StringUtils.isEmpty(userName)) {
            reference = CurrentUserReference.INSTANCE;
        } else {
            EntityReference baseEntityReference;
            if (parameters.length == 1 && parameters[0] instanceof WikiReference) {
                baseEntityReference = new EntityReference(USER_SPACE_REFERENCE, (WikiReference) parameters[0]);
            } else {
                baseEntityReference = USER_SPACE_REFERENCE;
            }
            DocumentReference documentReference = getDocumentReferenceResolver().resolve(userName, baseEntityReference);
            UserReference resolvedReference = resolveName(documentReference.getName());
            if (resolvedReference == null) {
                boolean isGlobal = this.entityReferenceProvider.getDefaultReference(EntityType.WIKI)
                    .equals(documentReference.getWikiReference());
                reference = new DocumentUserReference(documentReference, isGlobal);
            } else {
                reference = resolvedReference;
            }
        }
        return reference;
    }

    /**
     * @return the document reference resolver to use to resolve a username string into a
     *         {@link org.xwiki.model.reference.DocumentReference}
     */
    protected abstract DocumentReferenceResolver<String> getDocumentReferenceResolver();
}

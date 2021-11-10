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
package org.xwiki.user.internal.bridge;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.internal.document.DocumentUserReference;

/**
 * This bridge component is deprecated since its creation: its only purpose is to allow supporting old APIs to return
 * a {@link DocumentReference} when the new APIs are relying on a {@link UserReference}.
 * Note that contrarily to other resolver, this one is named so that people using it are aware of what they are doing.
 *
 * @version $Id$
 * @since 13.10RC1
 * @deprecated You should only use this resolver for backward compatibility of old APIs.
 */
@Deprecated
@Component
@Named("bridge")
@Singleton
public class DocumentReferenceUserReferenceResolver implements DocumentReferenceResolver<UserReference>
{
    @Override
    public DocumentReference resolve(UserReference userReference, Object... parameters)
    {
        if (userReference instanceof DocumentUserReference) {
            return ((DocumentUserReference) userReference).getReference();
        } else {
            throw new IllegalArgumentException(
                String.format("The given user reference [%s] cannot be resolved to document reference.", userReference)
            );
        }
    }
}

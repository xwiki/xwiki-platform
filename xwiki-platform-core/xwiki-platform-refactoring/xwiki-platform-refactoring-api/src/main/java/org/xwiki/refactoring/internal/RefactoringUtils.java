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
package org.xwiki.refactoring.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Helper methods for refactoring operations.
 * 
 * @version $Id$
 * @since 14.10.2
 * @since 15.0RC1
 */
public final class RefactoringUtils
{
    private RefactoringUtils()
    {
    }

    /**
     * Resolve a document reference from its string representation.
     * 
     * @param stringDocumentReference the string representation of the document reference
     * @param parameters parameters to pass to the document reference resolver
     * @return the resolved document reference
     * @deprecated this should be used only by legacy code for preserving backwards compatibility
     */
    @Deprecated
    public static DocumentReference resolveDocumentReference(String stringDocumentReference, Object... parameters)
    {
        try {
            // Try to preserve backwards compatibility in contexts where old core is present.
            Class<?> utils = Class.forName("com.xpn.xwiki.web.Utils");
            Method getComponent = utils.getMethod("getComponent", Type.class);
            @SuppressWarnings("unchecked")
            DocumentReferenceResolver<String> documentReferenceResolver =
                (DocumentReferenceResolver<String>) getComponent.invoke(null, DocumentReferenceResolver.TYPE_STRING);
            return documentReferenceResolver.resolve(stringDocumentReference, parameters);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve the document reference.", e);
        }
    }
}

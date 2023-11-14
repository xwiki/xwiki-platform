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
package org.xwiki.rendering.internal.macro.context;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.context.ContextMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Helper in charge of manipulating the context macro document.
 * 
 * @version $Id$
 * @since 15.9RC1
 */
@Component(roles = ContextMacroDocument.class)
@Singleton
public class ContextMacroDocument
{
    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    @Named("macro")
    private DocumentReferenceResolver<String> macroReferenceResolver;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * @param parameters the macro parameters
     * @param context the macro execution context
     * @return the reference of the document
     * @throws MacroExecutionException when accessing the document is not allowed
     */
    public DocumentReference getDocumentReference(ContextMacroParameters parameters, MacroTransformationContext context)
        throws MacroExecutionException
    {
        DocumentReference referencedDocReference;
        if (parameters.getDocument() != null) {
            referencedDocReference =
                this.macroReferenceResolver.resolve(parameters.getDocument(), context.getCurrentMacroBlock());
            DocumentReference currentAuthor = this.documentAccessBridge.getCurrentAuthorReference();

            // Make sure the author is allowed to use the target document
            checkAccess(currentAuthor, referencedDocReference);
        } else {
            referencedDocReference = null;
        }

        return referencedDocReference;
    }

    private void checkAccess(DocumentReference currentAuthor, DocumentReference referencedDocReference)
        throws MacroExecutionException
    {
        // Current author must have view right on the target document to use it as context document
        try {
            this.authorizationManager.checkAccess(Right.VIEW, currentAuthor, referencedDocReference);
        } catch (AccessDeniedException e) {
            throw new MacroExecutionException("Author [" + currentAuthor
                + "] is not allowed to access target document [" + referencedDocReference + "]", e);
        }
    }
}

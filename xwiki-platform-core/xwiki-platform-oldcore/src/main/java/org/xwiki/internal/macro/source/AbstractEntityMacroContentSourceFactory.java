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
package org.xwiki.internal.macro.source;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provide content coming from XWiki model entities.
 * 
 * @param <T> the type of the source
 * @version $Id$
 * @since 15.1RC1
 * @since 14.10.5
 */
public abstract class AbstractEntityMacroContentSourceFactory<T>
{
    @Inject
    @Named("macro")
    private EntityReferenceResolver<String> resolver;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private AuthorizationManager authorization;

    /**
     * @param reference the reference of the content
     * @param context the context of the code macro execution
     * @return the content to highlight
     * @throws MacroExecutionException when failing to get the content
     */
    public T getContent(MacroContentSourceReference reference, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Resolve the reference
        EntityReference entityReference =
            this.resolver.resolve(reference.getReference(), getEntityType(), context.getCurrentMacroBlock());

        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext == null) {
            throw new MacroExecutionException("No XWiki context could be found in the current context");
        }

        // Resolve the document reference
        DocumentReference documentReference = xcontext.getWiki().getDocumentReference(entityReference, xcontext);

        // Current author must have view right on the document
        try {
            this.authorization.checkAccess(Right.VIEW, xcontext.getAuthorReference(), documentReference);
        } catch (AccessDeniedException e) {
            throw new MacroExecutionException(
                "Current author is not allowed to access document [" + documentReference + "]", e);
        }

        // Current user must have view right on the document
        if (!this.authorization.hasAccess(Right.VIEW, xcontext.getUserReference(), documentReference)) {
            throw new MacroExecutionException(
                "Current user is not allowed to access document [" + documentReference + "]");
        }

        // Get the content
        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(documentReference, xcontext);
        } catch (XWikiException e) {
            throw new MacroExecutionException("Failed to load document [" + documentReference + "]", e);
        }

        // Make sure the document exist
        if (document.isNew()) {
            throw new MacroExecutionException("Entity [" + reference + "] does not exist");
        }

        // Get the current translation
        if (documentReference.getLocale() == null) {
            try {
                document = document.getTranslatedDocument(xcontext);
            } catch (XWikiException e) {
                throw new MacroExecutionException(
                    "Failed to load document translation for reference [" + documentReference + "]", e);
            }
        }

        return getContent(document, entityReference, reference, xcontext);
    }

    /**
     * @return the type of the entity
     */
    protected abstract EntityType getEntityType();

    protected abstract T getContent(XWikiDocument document, EntityReference entityReference,
        MacroContentSourceReference reference, XWikiContext xcontext) throws MacroExecutionException;
}

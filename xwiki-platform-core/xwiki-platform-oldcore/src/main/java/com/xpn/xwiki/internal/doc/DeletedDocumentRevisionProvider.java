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
package com.xpn.xwiki.internal.doc;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Get deleted document revisions from the database.
 * 
 * @version $Id$
 * @since 9.4RC1
 */
@Component
@Named("deleted")
@Singleton
public class DeletedDocumentRevisionProvider implements DocumentRevisionProvider
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private PageReferenceResolver<EntityReference> pageReferenceResolver;

    @Override
    public XWikiDocument getRevision(DocumentReference reference, String revision) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDeletedDocument deletedDocument = xcontext.getWiki().getDeletedDocument(Long.valueOf(revision), xcontext);

        // Only local the document if it matches the asked document reference
        if (deletedDocument != null && areReferencesCompatible(reference, deletedDocument.getDocumentReference())) {
            return deletedDocument.restoreDocument(xcontext);
        }

        return null;
    }

    private boolean areReferencesCompatible(DocumentReference requestedDocRef, DocumentReference deletedDocRef)
    {
        boolean result;
        if (requestedDocRef == null) {
            result = true;
        } else {
            // We might be trying to access a terminal doc and the requested reference is a non-terminal doc because
            // if the page doesn't exist we always assume it's non-terminal: so we need to compare the PageReference
            // here.
            PageReference requestedPageRef = this.pageReferenceResolver.resolve(requestedDocRef);
            PageReference deletedPageRef = this.pageReferenceResolver.resolve(deletedDocRef);
            result = requestedPageRef.equals(deletedPageRef);
        }
        return result;
    }

    @Override
    public XWikiDocument getRevision(XWikiDocument document, String revision) throws XWikiException
    {
        return getRevision(document != null ? document.getDocumentReferenceWithLocale() : null, revision);
    }

    @Override
    public void checkAccess(Right right, UserReference userReference, DocumentReference documentReference,
        String revision) throws AuthorizationException, XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDeletedDocument deletedDocument = xcontext.getWiki().getDeletedDocument(Long.valueOf(revision), xcontext);
        if (deletedDocument != null) {
            xcontext.getWiki().getRecycleBinStore().checkAccess(right, userReference, deletedDocument);
        }
    }
}

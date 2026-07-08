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
package org.xwiki.rest.internal.resources.pages;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
public class ModifiablePageResource extends XWikiResource
{
    @Inject
    protected ModelFactory factory;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * Creates or updates the specified page.
     * 
     * @param documentInfo identifies the page to be updated or created
     * @param page the submitted page data
     * @param minorRevision whether to create a minor revision or not
     * @return page data if the operation was successful
     * @throws XWikiException if creating or updating the page fails
     */
    public Response putPage(DocumentInfo documentInfo, Page page, Boolean minorRevision) throws XWikiException
    {
        Document doc = documentInfo.getDocument();

        // Save the document only if there is actually something to do if the document does not exist
        if (this.factory.toDocument(doc, page) || doc.isNew()) {
            doc.save(page.getComment(), Boolean.TRUE.equals(minorRevision));

            Page returnedPage = this.factory.toRestPage(uriInfo.getBaseUri(), uriInfo.getAbsolutePath(), doc, false,
                false, false, false, false);

            if (documentInfo.isCreated()) {
                return Response.created(uriInfo.getAbsolutePath()).entity(returnedPage).build();
            } else {
                return Response.status(Status.ACCEPTED).entity(returnedPage).build();
            }
        } else {
            return Response.status(Status.NOT_MODIFIED).build();
        }
    }

    /**
     * Deletes the specified page.
     *
     * @param documentInfo identifies the page to be deleted
     * @param skipRecycleBin when {@code true}, the page is deleted permanently instead of being sent to the recycle
     *  bin; the caller is responsible for having checked that skipping the recycle bin is allowed (wiki configuration)
     * @throws XWikiException if deleting the page fails
     * @throws WebApplicationException with an {@link Status#UNAUTHORIZED} status if the current user doesn't have the
     *  right to delete the page
     */
    void deletePage(DocumentInfo documentInfo, boolean skipRecycleBin) throws XWikiException
    {
        Document doc = documentInfo.getDocument();
        DocumentReference documentReference = doc.getDocumentReference();

        // Deleting a page requires the DELETE right, whether or not the recycle bin is skipped. Check it up-front so
        // that a proper HTTP 401 is returned instead of a generic server error.
        if (!this.contextualAuthorizationManager.hasAccess(Right.DELETE, documentReference)) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        if (skipRecycleBin) {
            // Permanently delete the page, bypassing the recycle bin.
            XWikiContext xcontext = getXWikiContext();
            XWiki xwiki = xcontext.getWiki();
            xwiki.deleteDocument(xwiki.getDocument(documentReference, xcontext), false, xcontext);
        } else {
            doc.delete();
        }
    }
}

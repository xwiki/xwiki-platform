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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.Page;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
public class ModifiablePageResource extends XWikiResource
{
    @Inject
    protected ModelFactory factory;

    public Response putPage(DocumentInfo documentInfo, Page page, Boolean minorRevision) throws XWikiException
    {
        Document doc = documentInfo.getDocument();

        // Save the document only if there is actually something to do if if the document does not exist
        if (this.factory.toDocument(doc, page) || doc.isNew()) {
            doc.save(page.getComment(), Boolean.TRUE.equals(minorRevision));

            page =
                this.factory.toRestPage(uriInfo.getBaseUri(), uriInfo.getAbsolutePath(), doc, false, false, false, false,
                    false);

            if (documentInfo.isCreated()) {
                return Response.created(uriInfo.getAbsolutePath()).entity(page).build();
            } else {
                return Response.status(Status.ACCEPTED).entity(page).build();
            }
        } else {
            return Response.status(Status.NOT_MODIFIED).build();
        }
    }

    void deletePage(DocumentInfo documentInfo) throws XWikiException
    {
        Document doc = documentInfo.getDocument();

        doc.delete();
    }
}

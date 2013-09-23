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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Page;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
public class ModifiablePageResource extends XWikiResource
{
    public Response putPage(DocumentInfo documentInfo, Page page) throws XWikiException
    {
        Document doc = documentInfo.getDocument();

        boolean save = false;

        if (page.getContent() != null) {
            doc.setContent(page.getContent());
            save = true;
        }

        if (page.getTitle() != null) {
            doc.setTitle(page.getTitle());
            save = true;
        }

        if (page.getParent() != null) {
            doc.setParent(page.getParent());
            save = true;
        }

        if (page.getSyntax() != null) {
            if (Utils.getXWiki(componentManager).getConfiguredSyntaxes().contains(page.getSyntax())) {
                doc.setSyntaxId(page.getSyntax());
                save = true;
            }
        }

        if (save) {
            doc.save(page.getComment());

            page =
                DomainObjectFactory.createPage(objectFactory, uriInfo.getBaseUri(), uriInfo.getAbsolutePath(), doc,
                    false, Utils.getXWikiApi(componentManager), false);

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

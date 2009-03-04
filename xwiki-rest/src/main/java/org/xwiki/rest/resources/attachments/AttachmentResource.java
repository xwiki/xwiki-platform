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
package org.xwiki.rest.resources.attachments;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Attachment;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/attachments/{attachmentName}")
public class AttachmentResource extends XWikiResource
{
    @GET
    public Response getAttachment(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("attachmentName") String attachmentName)
        throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);
        Document doc = documentInfo.getDocument();

        final com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(attachmentName);
        if (xwikiAttachment == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return Response.ok().type(xwikiAttachment.getMimeType()).entity(xwikiAttachment.getContent()).build();
    }

    @PUT
    public Response putAttachment(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("attachmentName") String attachmentName, byte[] content)
        throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, true);

        Document doc = documentInfo.getDocument();

        if (!doc.hasAccessLevel("edit", xwikiUser)) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        boolean existed = false;

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
        XWikiAttachment xwikiAttachment = xwikiDocument.getAttachment(attachmentName);
        if (xwikiAttachment == null) {
            xwikiAttachment = new XWikiAttachment();
            xwikiDocument.getAttachmentList().add(xwikiAttachment);
        } else {
            existed = true;
        }

        xwikiAttachment.setContent(content);
        xwikiAttachment.setAuthor(xwikiUser);
        xwikiAttachment.setFilename(attachmentName);
        xwikiAttachment.setDoc(xwikiDocument);

        xwikiDocument.saveAttachmentContent(xwikiAttachment, xwikiContext);

        doc.save();

        /*
         * We need to retrieve the base XWiki documents because Document doesn't have a method for retrieving the
         * external URL for an attachment
         */
        xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
        String attachmentXWikiUrl =
            xwikiDocument.getExternalAttachmentURL(attachmentName, "download", xwikiContext).toString();

        Attachment attachment =
            DomainObjectFactory.createAttachment(objectFactory, uriInfo.getBaseUri(), new com.xpn.xwiki.api.Attachment(
                doc, xwikiAttachment, xwikiContext), attachmentXWikiUrl);

        if (existed) {
            return Response.status(Status.ACCEPTED).entity(attachment).build();
        } else {
            return Response.created(uriInfo.getAbsolutePath()).entity(attachment).build();
        }

    }

    @DELETE
    public void deleteAttachment(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("attachmentName") String attachmentName)
        throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, true);

        Document doc = documentInfo.getDocument();

        if (!doc.hasAccessLevel("edit", xwikiUser)) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(attachmentName);
        if (xwikiAttachment == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
        XWikiAttachment baseXWikiAttachment = xwikiDocument.getAttachment(attachmentName);

        xwikiDocument.deleteAttachment(baseXWikiAttachment, xwikiContext);

        doc.save();
    }

}

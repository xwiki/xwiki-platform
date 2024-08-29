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
package org.xwiki.rest.internal.resources.attachments;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.BaseAttachmentsResource;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.attachments.AttachmentResourceImpl")
public class AttachmentResourceImpl extends BaseAttachmentsResource implements AttachmentResource
{
    @Override
    public Response getAttachment(String wikiName, String spaceName, String pageName, String attachmentName)
        throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);
            Document doc = documentInfo.getDocument();

            final com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(attachmentName);
            if (xwikiAttachment == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            return Response.ok().type(xwikiAttachment.getMimeType()).entity(xwikiAttachment.getContent()).build();
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public Response putAttachment(String wikiName, String spaceName, String pageName, String attachmentName,
        byte[] content) throws XWikiRestException, AttachmentValidationException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, true);

            Document doc = documentInfo.getDocument();

            if (!this.authorization.hasAccess(Right.EDIT, doc.getDocumentReference())) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            // Attach the file.
            InputStream inputStream = new ByteArrayInputStream(content != null ? content : new byte[0]);
            AttachmentInfo attachmentInfo = storeAndRetrieveAttachment(doc, attachmentName, inputStream, false);

            if (attachmentInfo.isAlreadyExisting()) {
                return Response.status(Status.ACCEPTED).entity(attachmentInfo.getAttachment()).build();
            } else {
                return Response.created(uriInfo.getAbsolutePath()).entity(attachmentInfo.getAttachment()).build();
            }
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public void deleteAttachment(String wikiName, String spaceName, String pageName, String attachmentName)
        throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, true);

            Document doc = documentInfo.getDocument();

            if (!this.authorization.hasAccess(Right.EDIT, doc.getDocumentReference())) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(attachmentName);
            if (xwikiAttachment == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            XWikiDocument xwikiDocument = Utils.getXWiki(componentManager).getDocument(doc.getDocumentReference(),
                Utils.getXWikiContext(componentManager));
            XWikiAttachment baseXWikiAttachment = xwikiDocument.getAttachment(attachmentName);

            xwikiDocument.removeAttachment(baseXWikiAttachment);

            Utils.getXWiki(componentManager).saveDocument(xwikiDocument,
                "Deleted attachment [" + baseXWikiAttachment.getFilename() + "]",
                Utils.getXWikiContext(componentManager));
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}

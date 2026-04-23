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
import org.xwiki.internal.attachment.XWikiAttachmentSecurityManager;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.resources.BaseAttachmentsResource;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.util.Util;

import jakarta.inject.Inject;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.attachments.AttachmentResourceImpl")
public class AttachmentResourceImpl extends BaseAttachmentsResource implements AttachmentResource
{
    @Inject
    private XWikiAttachmentSecurityManager attachmentSecurityManager;

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

            String ofilename = Util.encodeURI(xwikiAttachment.getFilename(), getXWikiContext())
                .replaceAll("\\+", "%20");
            // The inline attribute of Content-Disposition tells the browser that they should display
            // the downloaded file in the page (see http://www.ietf.org/rfc/rfc1806.txt for more
            // details). We do this so that JPG, GIF, PNG, etc are displayed without prompting a Save
            // dialog box. However, all mime types that cannot be displayed by the browser do prompt a
            // Save dialog box (exe, zip, xar, etc).
            String dispType = "inline";
            // If the mimetype is not authorized to be displayed inline,
            // let's force its content disposition to download.
            if (attachmentSecurityManager.shouldBeDownloaded(xwikiAttachment.getAttachment())) {
                dispType = "attachment";
            }
            return Response
                .ok()
                .type(xwikiAttachment.getMimeType())
                .entity(xwikiAttachment.getContent())
                .header("Content-Disposition", dispType + "; filename*=utf-8''" + ofilename)
                .build();
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

            Attachment xwikiAttachment = doc.removeAttachment(attachmentName);
            if (xwikiAttachment == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            doc.save("Deleted attachment [" + attachmentName + "]");
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}

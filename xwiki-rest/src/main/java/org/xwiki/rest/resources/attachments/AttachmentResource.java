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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.restlet.data.Status;
import org.restlet.resource.StringRepresentation;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Utils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class AttachmentResource extends BaseAttachmentResource
{
    @Override
    public boolean allowPut()
    {
        return true;
    }

    @Override
    public boolean allowDelete()
    {
        return true;
    }

    @Override
    public void handlePut()
    {
        try {
            DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), false, true);
            if (documentInfo == null) {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                return;

            }

            Document doc = documentInfo.getDocument();

            String attachmentName = (String) getRequest().getAttributes().get(Constants.ATTACHMENT_NAME_PARAMETER);
            boolean existed = false;

            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
            XWikiAttachment xwikiAttachment = xwikiDocument.getAttachment(attachmentName);
            if (xwikiAttachment == null) {
                xwikiAttachment = new XWikiAttachment();
                xwikiDocument.getAttachmentList().add(xwikiAttachment);
            } else {
                existed = true;
            }

            byte[] buffer = new byte[8192];
            InputStream is = getRequest().getEntity().getStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (true) {
                int read = is.read(buffer);
                bos.write(buffer, 0, read);

                if (read < 8192) {
                    break;
                }
            }

            xwikiAttachment.setContent(bos.toByteArray());
            xwikiAttachment.setAuthor(xwikiUser);
            xwikiAttachment.setFilename(attachmentName);
            xwikiAttachment.setDoc(xwikiDocument);

            if (doc.hasAccessLevel("edit", xwikiUser)) {
                xwikiDocument.saveAttachmentContent(xwikiAttachment, xwikiContext);
            } else {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                return;
            }

            if (existed) {
                getResponse().setStatus(Status.SUCCESS_ACCEPTED);
            } else {
                getResponse().setStatus(Status.SUCCESS_CREATED);
            }

            /*
             * We need to retrieve the base XWiki documents because Document doesn't have a method for retrieving the
             * external URL for an attachment
             */
            /* XWikiDocument */xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
            String attachmentXWikiUrl =
                xwikiDocument.getExternalAttachmentURL(attachmentName, "download", xwikiContext).toString();

            getResponse().setEntity(
                new StringRepresentation(Utils.toXml(DomainObjectFactory.createAttachment(getRequest(),
                    resourceClassRegistry, doc.getAttachment(attachmentName), attachmentXWikiUrl, false))));

        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            } else {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            }
        } catch (IOException e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }
    }

    @Override
    public void handleDelete()
    {
        try {
            DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, true);
            if (documentInfo == null) {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                return;

            }

            Document doc = documentInfo.getDocument();

            String attachmentName = (String) getRequest().getAttributes().get(Constants.ATTACHMENT_NAME_PARAMETER);

            com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(attachmentName);
            if (xwikiAttachment == null) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return;
            }

            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
            XWikiAttachment baseXWikiAttachment = xwikiDocument.getAttachment(attachmentName);
            if (doc.hasAccessLevel("edit", xwikiUser)) {
                xwikiDocument.deleteAttachment(baseXWikiAttachment, xwikiContext);
                getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
            } else {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            }

        } catch (Exception e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }
    }

}

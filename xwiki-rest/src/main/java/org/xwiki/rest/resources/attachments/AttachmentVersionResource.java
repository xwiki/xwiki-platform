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

import java.io.IOException;
import java.io.OutputStream;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.XWikiResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

public class AttachmentVersionResource extends XWikiResource
{
    @Override
    public void init(Context context, Request request, Response response)
    {
        super.init(context, request, response);
        getVariants().add(new Variant(MediaType.ALL));
    }

    @Override
    public Representation represent(Variant variant)
    {
        try {
            DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
            if (documentInfo == null) {
                return null;
            }

            Document doc = documentInfo.getDocument();

            String attachmentName = (String) getRequest().getAttributes().get(Constants.ATTACHMENT_NAME_PARAMETER);
            String attachmentVersion =
                (String) getRequest().getAttributes().get(Constants.ATTACHMENT_VERSION_PARAMETER);

            com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(attachmentName);
            if (xwikiAttachment == null) {
                /* If the attachment doesn't exist send a not found header */
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return null;
            }

            /* Get the requested version */
            final com.xpn.xwiki.api.Attachment xwikiAttachmentVersion =
                xwikiAttachment.getAttachmentRevision(attachmentVersion);
            if (xwikiAttachmentVersion == null) {
                /* If the attachment doesn't exist send a not found header */
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return null;
            }

            return new OutputRepresentation(MediaType.valueOf(xwikiAttachment.getMimeType()))
            {
                @Override
                public void write(OutputStream outputStream) throws IOException
                {
                    /* TODO: Maybe we should write the content N bytes at a time */
                    try {
                        outputStream.write(xwikiAttachmentVersion.getContent());
                    } catch (XWikiException e) {
                        getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                    } finally {
                        outputStream.close();
                    }
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return null;
    }
}

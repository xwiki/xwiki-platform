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

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.resources.attachments.AttachmentVersionResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.attachments.AttachmentVersionResourceImpl")
public class AttachmentVersionResourceImpl extends XWikiResource implements AttachmentVersionResource
{
    @Override
    public Response getAttachment(String wikiName, String spaceName, String pageName, String attachmentName,
            String attachmentVersion) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);
            Document doc = documentInfo.getDocument();

            com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(attachmentName);
            if (xwikiAttachment == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            /* Get the requested version */
            final com.xpn.xwiki.api.Attachment xwikiAttachmentVersion =
                    xwikiAttachment.getAttachmentRevision(attachmentVersion);
            if (xwikiAttachmentVersion == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            return Response.ok().type(xwikiAttachment.getMimeType()).entity(xwikiAttachmentVersion.getContent())
                    .build();
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}

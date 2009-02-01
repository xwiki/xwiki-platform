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

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Attachments;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

public class AttachmentHistoryResource extends XWikiResource
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

            final com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(attachmentName);
            if (xwikiAttachment == null) {
                /* If the attachment doesn't exist send a not found header */
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return null;
            }
            
            /*
             * We need to retrieve the base XWiki documents because Document doesn't have a method for retrieving the
             * external URL for an attachment
             */
            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);

            Attachments attachments = new Attachments();

            Version[] versionList = xwikiAttachment.getVersions();
            for (Version version : versionList) {
                com.xpn.xwiki.api.Attachment xwikiAttachmentAtVersion =
                    xwikiAttachment.getAttachmentRevision(version.toString());

                String attachmentXWikiUrl =
                    xwikiDocument.getExternalAttachmentURL(xwikiAttachment.getFilename(), "download", xwikiContext)
                        .toString();

                attachments.addAttachment(DomainObjectFactory.createAttachmentAtVersion(getRequest(),
                    resourceClassRegistry, xwikiAttachmentAtVersion, attachmentXWikiUrl));
            }

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), attachments);
        } catch (Exception e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return null;
    }
}

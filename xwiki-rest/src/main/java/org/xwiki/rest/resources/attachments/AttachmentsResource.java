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

import java.util.List;

import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Attachments;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

public class AttachmentsResource extends XWikiResource
{
    @Override
    public Representation represent(Variant variant)
    {
        try {
            DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
            if (documentInfo == null) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return null;
            }

            Document doc = documentInfo.getDocument();

            List<com.xpn.xwiki.api.Attachment> xwikiAttachments = doc.getAttachmentList();

            Form queryForm = getRequest().getResourceRef().getQueryAsForm();
            RangeIterable<com.xpn.xwiki.api.Attachment> ri =
                new RangeIterable<com.xpn.xwiki.api.Attachment>(xwikiAttachments, Utils.parseInt(queryForm
                    .getFirstValue(Constants.START_PARAMETER), 0), Utils.parseInt(queryForm
                    .getFirstValue(Constants.NUMBER_PARAMETER), -1));

            /*
             * We need to retrieve the base XWiki documents because Document doesn't have a method for retrieving the
             * external URL for an attachment
             */
            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);

            Attachments attachments = new Attachments();

            for (com.xpn.xwiki.api.Attachment xwikiAttachment : ri) {
                String attachmentXWikiUrl =
                    xwikiDocument.getExternalAttachmentURL(xwikiAttachment.getFilename(), "download", xwikiContext)
                        .toString();

                attachments.addAttachment(DomainObjectFactory.createAttachment(getRequest(), resourceClassRegistry,
                    xwikiAttachment, attachmentXWikiUrl, false));
            }

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), attachments);
        } catch (Exception e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return null;
    }
}

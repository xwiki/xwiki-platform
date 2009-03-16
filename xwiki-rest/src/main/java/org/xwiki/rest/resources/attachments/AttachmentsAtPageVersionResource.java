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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Attachments;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/history/{version}/attachments")
public class AttachmentsAtPageVersionResource extends XWikiResource
{
    @GET
    public Attachments getAttachmentsAtPageVersion(@PathParam("wikiName") String wikiName,
        @PathParam("spaceName") String spaceName, @PathParam("pageName") String pageName,
        @PathParam("version") String version, @QueryParam("start") @DefaultValue("0") Integer start,
        @QueryParam("number") @DefaultValue("-1") Integer number) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, version, true, false);

        Document doc = documentInfo.getDocument();

        Attachments attachments = objectFactory.createAttachments();

        List<com.xpn.xwiki.api.Attachment> xwikiAttachments = doc.getAttachmentList();

        RangeIterable<com.xpn.xwiki.api.Attachment> ri =
            new RangeIterable<com.xpn.xwiki.api.Attachment>(xwikiAttachments, start, number);

        /*
         * We need to retrieve the base XWiki documents because Document doesn't have a method for retrieving the
         * external URL for an attachment
         */
        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);

        for (com.xpn.xwiki.api.Attachment xwikiAttachment : ri) {
            String attachmentXWikiAbsoluteUrl =
                xwikiDocument.getExternalAttachmentURL(xwikiAttachment.getFilename(), "download", xwikiContext)
                    .toString();

            String attachmentXWikiRelativeUrl =
                xwikiDocument.getAttachmentURL(xwikiAttachment.getFilename(), "download", xwikiContext).toString();

            attachments.getAttachments().add(
                DomainObjectFactory.createAttachment(objectFactory, uriInfo.getBaseUri(), xwikiAttachment,
                    attachmentXWikiRelativeUrl, attachmentXWikiAbsoluteUrl));
        }

        return attachments;
    }

}

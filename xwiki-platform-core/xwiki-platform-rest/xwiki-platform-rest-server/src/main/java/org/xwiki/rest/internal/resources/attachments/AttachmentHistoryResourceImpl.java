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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.RangeIterable;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.resources.attachments.AttachmentHistoryResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.attachments.AttachmentHistoryResourceImpl")
public class AttachmentHistoryResourceImpl extends XWikiResource implements AttachmentHistoryResource
{
    @Override
    public Attachments getAttachmentHistory(String wikiName, String spaceName, String pageName, String attachmentName,
            Integer start, Integer number) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);
            Document doc = documentInfo.getDocument();

            final com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(attachmentName);
            if (xwikiAttachment == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            Attachments attachments = new Attachments();

            Version[] versions = xwikiAttachment.getVersions();
            List<Version> versionList = new ArrayList<Version>();
            for (Version version : versions) {
                versionList.add(version);
            }

            RangeIterable<Version> ri = new RangeIterable<Version>(versionList, start, number);

            for (Version version : ri) {
                com.xpn.xwiki.api.Attachment xwikiAttachmentAtVersion =
                        xwikiAttachment.getAttachmentRevision(version.toString());

                URL url = Utils.getXWikiContext(componentManager).getURLFactory()
                    .createAttachmentRevisionURL(attachmentName, spaceName, doc.getDocumentReference().getName(),
                        version.toString(), null, wikiName, Utils.getXWikiContext(componentManager));
                String attachmentXWikiAbsoluteUrl = url.toString();
                String attachmentXWikiRelativeUrl =
                    Utils.getXWikiContext(componentManager).getURLFactory().getURL(url,
                        Utils.getXWikiContext(componentManager));

                attachments.getAttachments().add(
                    DomainObjectFactory.createAttachmentAtVersion(objectFactory, uriInfo.getBaseUri(),
                        xwikiAttachmentAtVersion, attachmentXWikiRelativeUrl, attachmentXWikiAbsoluteUrl,
                        Utils.getXWikiApi(componentManager), false));
            }

            return attachments;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}

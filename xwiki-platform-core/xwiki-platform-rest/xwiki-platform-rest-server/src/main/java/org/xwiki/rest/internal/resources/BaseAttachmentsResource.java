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
package org.xwiki.rest.internal.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.RangeIterable;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.rest.resources.pages.PageResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public class BaseAttachmentsResource extends XWikiResource
{
    /**
     * Helper class that contains newly created attachment information to be returned to the client. It contains the
     * JAXB attachment object and a boolean variable that states if the attachment existed before. This class is used by
     * the storeAttachment utility method.
     */
    protected static class AttachmentInfo
    {
        protected Attachment attachment;

        protected boolean alreadyExisting;

        public AttachmentInfo(Attachment attachment, boolean alreadyExisting)
        {
            this.attachment = attachment;
            this.alreadyExisting = alreadyExisting;
        }

        public Attachment getAttachment()
        {
            return attachment;
        }

        public boolean isAlreadyExisting()
        {
            return alreadyExisting;
        }
    }

    /**
     * Retrieves the attachments by filtering them.
     *
     * @param wikiName The virtual wiki.
     * @param name Name filter (include only attachments that matches this name)
     * @param page Page filter (include only attachments are attached to a page matches this string)
     * @param space Space filter (include only attachments are attached to a page in a space matching this string)
     * @param author Author filter (include only attachments from an author who matches this string)
     * @param types A comma separated list of string that will be matched against the actual mime type of the
     *            attachments.
     * @return The list of the retrieved attachments.
     */
    public Attachments getAttachments(String wikiName, String name, String page, String space, String author,
        String types, Integer start, Integer number, Boolean withPrettyNames) throws XWikiRestException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        Attachments attachments = objectFactory.createAttachments();

        /* This try is just needed for executing the finally clause. */
        try {
            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

            Map<String, String> filters = new HashMap<String, String>();
            if (!name.equals("")) {
                filters.put("name", name);
            }
            if (!page.equals("")) {
                filters.put("page", name);
            }
            if (!space.equals("")) {
                filters.put("space", Utils.getLocalSpaceId(parseSpaceSegments(space)));
            }
            if (!author.equals("")) {
                filters.put("author", author);
            }

            /* Build the query */
            Formatter f = new Formatter();
            f.format("select doc.space, doc.name, doc.version, attachment from XWikiDocument as doc,"
                + " XWikiAttachment as attachment where (attachment.docId=doc.id ");

            if (filters.keySet().size() > 0) {
                for (String param : filters.keySet()) {
                    if (param.equals("name")) {
                        f.format(" and upper(attachment.filename) like :name ");
                    }
                    if (param.equals("page")) {
                        f.format(" and upper(doc.fullName) like :page ");
                    }
                    if (param.equals("space")) {
                        f.format(" and upper(doc.space) like :space ");
                    }

                    if (param.equals("author")) {
                        f.format(" and upper(attachment.author) like :author ");
                    }
                }
            }

            f.format(")");

            String queryString = f.toString();

            /* Execute the query by filling the parameters */
            List<Object> queryResult = null;
            try {
                Query query = queryManager.createQuery(queryString, Query.XWQL).setLimit(number).setOffset(start);
                for (String param : filters.keySet()) {
                    query.bindValue(param, String.format("%%%s%%", filters.get(param).toUpperCase()));
                }

                queryResult = query.execute();
            } catch (QueryException e) {
                throw new XWikiRestException(e);
            }

            Set<String> acceptedMimeTypes = new HashSet<String>();
            if (!types.equals("")) {
                String[] acceptedMimetypesArray = types.split(",");
                for (String type : acceptedMimetypesArray) {
                    acceptedMimeTypes.add(type);
                }
            }

            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;
                String pageSpaceId = (String) fields[0];
                List<String> pageSpaces = Utils.getSpacesFromSpaceId(pageSpaceId);
                String pageName = (String) fields[1];
                String pageId = Utils.getPageId(wikiName, pageSpaces, pageName);
                String pageVersion = (String) fields[2];
                XWikiAttachment xwikiAttachment = (XWikiAttachment) fields[3];

                String mimeType = xwikiAttachment.getMimeType(Utils.getXWikiContext(componentManager));

                boolean add = true;

                /* Check the mime type filter */
                if (acceptedMimeTypes.size() > 0) {
                    add = false;

                    for (String type : acceptedMimeTypes) {
                        if (mimeType.toUpperCase().contains(type.toUpperCase())) {
                            add = true;
                            break;
                        }
                    }
                }

                if (add) {
                    /*
                     * We manufacture attachments in place because we don't have all the data for calling the
                     * DomainObjectFactory method (doing so would require to retrieve an actual Document)
                     */
                    Attachment attachment = objectFactory.createAttachment();
                    attachment.setId(String.format("%s@%s", pageId, xwikiAttachment.getFilename()));
                    attachment.setName(xwikiAttachment.getFilename());
                    attachment.setLongSize(xwikiAttachment.getLongSize());
                    // Retro compatibility
                    attachment.setSize((int) xwikiAttachment.getLongSize());
                    attachment.setMimeType(mimeType);
                    attachment.setAuthor(xwikiAttachment.getAuthor());
                    if (withPrettyNames) {
                        attachment
                            .setAuthorName(Utils.getAuthorName(xwikiAttachment.getAuthorReference(), componentManager));
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(xwikiAttachment.getDate());
                    attachment.setDate(calendar);

                    attachment.setPageId(pageId);
                    attachment.setPageVersion(pageVersion);
                    attachment.setVersion(xwikiAttachment.getVersion());

                    URL absoluteUrl = Utils.getXWikiContext(componentManager).getURLFactory().createAttachmentURL(
                        xwikiAttachment.getFilename(), pageSpaceId, pageName, "download", null, wikiName,
                        Utils.getXWikiContext(componentManager));
                    attachment.setXwikiAbsoluteUrl(absoluteUrl.toString());
                    attachment.setXwikiRelativeUrl(Utils.getXWikiContext(componentManager).getURLFactory()
                        .getURL(absoluteUrl, Utils.getXWikiContext(componentManager)));

                    URI pageUri =
                        Utils.createURI(uriInfo.getBaseUri(), PageResource.class, wikiName, pageSpaces, pageName);
                    Link pageLink = objectFactory.createLink();
                    pageLink.setHref(pageUri.toString());
                    pageLink.setRel(Relations.PAGE);
                    attachment.getLinks().add(pageLink);

                    URI attachmentUri = Utils.createURI(uriInfo.getBaseUri(), AttachmentResource.class, wikiName,
                        pageSpaces, pageName, xwikiAttachment.getFilename());
                    Link attachmentLink = objectFactory.createLink();
                    attachmentLink.setHref(attachmentUri.toString());
                    attachmentLink.setRel(Relations.ATTACHMENT_DATA);
                    attachment.getLinks().add(attachmentLink);

                    attachments.getAttachments().add(attachment);
                }
            }
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }

        return attachments;
    }

    protected Attachments getAttachmentsForDocument(Document doc, int start, int number, Boolean withPrettyNames)
    {
        Attachments attachments = objectFactory.createAttachments();

        List<com.xpn.xwiki.api.Attachment> xwikiAttachments = doc.getAttachmentList();

        RangeIterable<com.xpn.xwiki.api.Attachment> ri =
            new RangeIterable<com.xpn.xwiki.api.Attachment>(xwikiAttachments, start, number);

        for (com.xpn.xwiki.api.Attachment xwikiAttachment : ri) {
            URL url = Utils.getXWikiContext(componentManager).getURLFactory().createAttachmentURL(
                xwikiAttachment.getFilename(), doc.getSpace(), doc.getDocumentReference().getName(), "download", null,
                doc.getWiki(), Utils.getXWikiContext(componentManager));
            String attachmentXWikiAbsoluteUrl = url.toString();
            String attachmentXWikiRelativeUrl = Utils.getXWikiContext(componentManager).getURLFactory().getURL(url,
                Utils.getXWikiContext(componentManager));

            attachments.getAttachments()
                .add(DomainObjectFactory.createAttachment(objectFactory, uriInfo.getBaseUri(), xwikiAttachment,
                    attachmentXWikiRelativeUrl, attachmentXWikiAbsoluteUrl, Utils.getXWikiApi(componentManager),
                    withPrettyNames));
        }

        return attachments;
    }

    protected AttachmentInfo storeAttachment(Document doc, String attachmentName, byte[] content) throws XWikiException
    {
        XWikiContext xcontext = Utils.getXWikiContext(componentManager);

        XWikiDocument xwikiDocument =
            Utils.getXWiki(componentManager).getDocument(doc.getDocumentReference(), xcontext);

        boolean alreadyExisting = xwikiDocument.getAttachment(attachmentName) != null;

        XWikiAttachment xwikiAttachment;
        try {
            xwikiAttachment = xwikiDocument.setAttachment(attachmentName,
                new ByteArrayInputStream(content != null ? content : new byte[0]), xcontext);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MISC,
                String.format("Failed to store the content of attachment [%s] in document [%s].", attachmentName,
                    doc.getPrefixedFullName()),
                e);
        }

        Utils.getXWiki(componentManager).saveDocument(xwikiDocument, xcontext);

        URL url = Utils.getXWikiContext(componentManager).getURLFactory().createAttachmentURL(attachmentName,
            doc.getSpace(), doc.getDocumentReference().getName(), "download", null, doc.getWiki(), xcontext);
        String attachmentXWikiAbsoluteUrl = url.toString();
        String attachmentXWikiRelativeUrl = xcontext.getURLFactory().getURL(url, xcontext);

        Attachment attachment = DomainObjectFactory.createAttachment(objectFactory, uriInfo.getBaseUri(),
            new com.xpn.xwiki.api.Attachment(doc, xwikiAttachment, xcontext), attachmentXWikiRelativeUrl,
            attachmentXWikiAbsoluteUrl, Utils.getXWikiApi(componentManager), false);

        return new AttachmentInfo(attachment, alreadyExisting);
    }
}

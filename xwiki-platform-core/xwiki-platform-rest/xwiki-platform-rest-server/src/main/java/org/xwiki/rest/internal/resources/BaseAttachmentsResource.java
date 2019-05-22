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
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.internal.RangeIterable;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;

import com.xpn.xwiki.XWiki;
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

    @Inject
    private ModelFactory modelFactory;

    @Inject
    @Named("hidden/document")
    private QueryFilter hiddenDocumentFilter;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

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
        XWikiContext xcontext = this.xcontextProvider.get();
        String database = xcontext.getWikiId();

        Attachments attachments = objectFactory.createAttachments();

        /* This try is just needed for executing the finally clause. */
        try {
            xcontext.setWikiId(wikiName);

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
            StringBuilder statement = new StringBuilder().append("select doc.space, doc.name, doc.version, attachment")
                .append(" from XWikiDocument as doc, XWikiAttachment as attachment")
                .append(" where (attachment.docId = doc.id");

            if (filters.keySet().size() > 0) {
                for (String param : filters.keySet()) {
                    if (param.equals("name")) {
                        statement.append(" and upper(attachment.filename) like :name ");
                    }
                    if (param.equals("page")) {
                        statement.append(" and upper(doc.fullName) like :page ");
                    }
                    if (param.equals("space")) {
                        statement.append(" and upper(doc.space) like :space ");
                    }

                    if (param.equals("author")) {
                        statement.append(" and upper(attachment.author) like :author ");
                    }
                }
            }

            statement.append(")");

            String queryString = statement.toString();

            /* Execute the query by filling the parameters */
            List<Object> queryResult = null;
            try {
                Query query = queryManager.createQuery(queryString, Query.XWQL).setLimit(number).setOffset(start);
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    query.bindValue(entry.getKey()).literal(entry.getValue().toUpperCase()).anyChars();
                }
                query.addFilter(this.hiddenDocumentFilter);
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
                List<String> pageSpaces = Utils.getSpacesFromSpaceId((String) fields[0]);
                String pageName = (String) fields[1];
                String pageVersion = (String) fields[2];
                XWikiAttachment xwikiAttachment = (XWikiAttachment) fields[3];

                String mimeType = xwikiAttachment.getMimeType(xcontext);

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
                    DocumentReference documentReference = new DocumentReference(wikiName, pageSpaces, pageName);
                    XWikiDocument document = new XWikiDocument(documentReference);
                    document.setVersion(pageVersion);
                    xwikiAttachment.setDoc(document, false);
                    com.xpn.xwiki.api.Attachment apiAttachment =
                        new com.xpn.xwiki.api.Attachment(new Document(document, xcontext), xwikiAttachment, xcontext);
                    attachments.getAttachments().add(this.modelFactory.toRestAttachment(this.uriInfo.getBaseUri(),
                        apiAttachment, withPrettyNames, false));
                }
            }
        } finally {
            xcontext.setWikiId(database);
        }

        return attachments;
    }

    protected Attachments getAttachmentsForDocument(Document doc, int start, int number, Boolean withPrettyNames)
    {
        Attachments attachments = this.objectFactory.createAttachments();

        RangeIterable<com.xpn.xwiki.api.Attachment> attachmentsRange =
            new RangeIterable<com.xpn.xwiki.api.Attachment>(doc.getAttachmentList(), start, number);
        for (com.xpn.xwiki.api.Attachment xwikiAttachment : attachmentsRange) {
            attachments.getAttachments().add(
                this.modelFactory.toRestAttachment(this.uriInfo.getBaseUri(), xwikiAttachment, withPrettyNames, false));
        }

        return attachments;
    }

    protected AttachmentInfo storeAttachment(Document doc, String attachmentName, byte[] content) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getDocumentReference(), xcontext);

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

        xwiki.saveDocument(xwikiDocument, xcontext);

        URL url = xcontext.getURLFactory().createAttachmentURL(attachmentName, doc.getSpace(),
            doc.getDocumentReference().getName(), "download", null, doc.getWiki(), xcontext);
        String attachmentXWikiAbsoluteUrl = url.toString();
        String attachmentXWikiRelativeUrl = xcontext.getURLFactory().getURL(url, xcontext);

        Attachment attachment = this.modelFactory.toRestAttachment(uriInfo.getBaseUri(),
            new com.xpn.xwiki.api.Attachment(doc, xwikiAttachment, xcontext), attachmentXWikiRelativeUrl,
            attachmentXWikiAbsoluteUrl, false, false);

        return new AttachmentInfo(attachment, alreadyExisting);
    }
}

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
package org.xwiki.rest.resources;

import java.net.URL;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;

import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.Relations;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.rest.resources.pages.PageResource;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/attachments")
public class BaseAttachmentsResource extends XWikiResource
{
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
     * @param start
     * @param number
     * @return The list of the retrieved attachments.
     * @throws QueryException
     */
    public Attachments getAttachments(String wikiName, String name, String page, String space, String author,
        String types, Integer start, Integer number) throws QueryException
    {
        String database = xwikiContext.getDatabase();

        Attachments attachments = objectFactory.createAttachments();

        /* This try is just needed for executing the finally clause. */
        try {
            xwikiContext.setDatabase(wikiName);

            Map<String, String> filters = new HashMap<String, String>();
            if (!name.equals("")) {
                filters.put("name", name);
            }
            if (!page.equals("")) {
                filters.put("page", name);
            }
            if (!space.equals("")) {
                filters.put("space", space);
            }
            if (!author.equals("")) {
                filters.put("author", author);
            }

            /* Build the query */
            Formatter f = new Formatter();
            f
                .format("select doc.space, doc.name, doc.version, attachment from XWikiDocument as doc, XWikiAttachment as attachment where (attachment.docId=doc.id ");

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
            QueryManager queryManager = (QueryManager) com.xpn.xwiki.web.Utils.getComponent(QueryManager.ROLE);
            Query query = queryManager.createQuery(queryString, Query.XWQL).setLimit(number).setOffset(start);
            for (String param : filters.keySet()) {
                query.bindValue(param, String.format("%%%s%%", filters.get(param).toUpperCase()));
            }

            List<Object> queryResult = null;
            queryResult = query.execute();

            Set<String> acceptedMimeTypes = new HashSet<String>();
            if (!types.equals("")) {
                String[] acceptedMimetypesArray = types.split(",");
                for (String type : acceptedMimetypesArray) {
                    acceptedMimeTypes.add(type);
                }
            }

            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;
                String pageSpace = (String) fields[0];
                String pageName = (String) fields[1];
                String pageId = Utils.getPageId(wikiName, pageSpace, pageName);
                String pageVersion = (String) fields[2];
                XWikiAttachment xwikiAttachment = (XWikiAttachment) fields[3];

                String mimeType = xwikiAttachment.getMimeType(xwikiContext);

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
                    attachment.setSize(xwikiAttachment.getFilesize());
                    attachment.setMimeType(mimeType);
                    attachment.setAuthor(xwikiAttachment.getAuthor());

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(xwikiAttachment.getDate());
                    attachment.setDate(calendar);

                    attachment.setPageId(pageId);
                    attachment.setPageVersion(pageVersion);
                    attachment.setVersion(xwikiAttachment.getVersion());

                    URL absoluteUrl =
                        xwikiContext.getURLFactory().createAttachmentURL(xwikiAttachment.getFilename(), pageSpace,
                            pageName, "download", null, wikiName, xwikiContext);
                    attachment.setXwikiAbsoluteUrl(absoluteUrl.toString());
                    attachment.setXwikiRelativeUrl(xwikiContext.getURLFactory().getURL(absoluteUrl, xwikiContext));

                    String baseUri = uriInfo.getBaseUri().toString();

                    String pageUri =
                        UriBuilder.fromUri(baseUri).path(PageResource.class).build(wikiName, pageSpace, pageName)
                            .toString();
                    Link pageLink = objectFactory.createLink();
                    pageLink.setHref(pageUri);
                    pageLink.setRel(Relations.PAGE);
                    attachment.getLinks().add(pageLink);

                    String attachmentUri =
                        UriBuilder.fromUri(baseUri).path(AttachmentResource.class).build(wikiName, pageSpace, pageName,
                            xwikiAttachment.getFilename()).toString();
                    Link attachmentLink = objectFactory.createLink();
                    attachmentLink.setHref(attachmentUri);
                    attachmentLink.setRel(Relations.ATTACHMENT_DATA);
                    attachment.getLinks().add(attachmentLink);

                    attachments.getAttachments().add(attachment);
                }
            }
        } finally {
            xwikiContext.setDatabase(database);
        }

        return attachments;
    }
}

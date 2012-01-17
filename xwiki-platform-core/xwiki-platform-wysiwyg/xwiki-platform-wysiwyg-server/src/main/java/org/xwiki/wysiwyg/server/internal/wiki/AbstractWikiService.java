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
package org.xwiki.wysiwyg.server.internal.wiki;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPage;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiService;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wysiwyg.server.wiki.EntityReferenceConverter;
import org.xwiki.wysiwyg.server.wiki.LinkService;

/**
 * Put here only the methods that can be implemented without depending on the old XWiki core.
 * 
 * @version $Id$
 * @since 3.2
 */
public abstract class AbstractWikiService implements WikiService
{
    /**
     * The object used to convert between client and server entity reference.
     */
    protected final EntityReferenceConverter entityReferenceConverter = new EntityReferenceConverter();

    /**
     * Logger.
     */
    @Inject
    protected Logger logger;

    /**
     * The component used to create queries.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * The service used to create links.
     */
    @Inject
    private LinkService linkService;

    /**
     * The component used to access documents. This is temporary till XWiki model is moved into components.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component that protects us against cross site request forgery by using a secret token validation mechanism.
     * The secret token is added to the query string of the upload URL and then checked in the upload action when files
     * are uploaded.
     */
    @Inject
    private CSRFToken csrf;

    @Override
    public List<String> getPageNames(String wikiName, String spaceName)
    {
        String statement =
            "select distinct doc.space, doc.name from XWikiDocument as doc where doc.space = :space "
                + "order by doc.space, doc.name";
        Query query = createHQLQuery(statement);
        query.setWiki(wikiName).bindValue("space", spaceName);
        List<String> pagesNames = new ArrayList<String>();
        for (DocumentReference documentReference : searchDocumentReferences(query)) {
            pagesNames.add(documentReference.getName());
        }
        return pagesNames;
    }

    @Override
    public List<WikiPage> getRecentlyModifiedPages(String wikiName, int offset, int limit)
    {
        String statement =
            "select distinct doc.space, doc.name, doc.date from XWikiDocument as doc where doc.author = :author "
                + "order by doc.date desc, doc.space, doc.name";
        Query query = createHQLQuery(statement);
        query.setWiki(wikiName).setOffset(offset).setLimit(limit);
        query.bindValue("author", getCurrentUserRelativeTo(wikiName));
        return getWikiPages(searchDocumentReferences(query));
    }

    /**
     * @param wikiName the name of a wiki
     * @return the name of the current user, relative to the specified wiki
     */
    protected abstract String getCurrentUserRelativeTo(String wikiName);

    @Override
    public List<WikiPage> getMatchingPages(String wikiName, String keyword, int offset, int limit)
    {
        StringBuilder statement = new StringBuilder();
        statement.append("select distinct doc.space, doc.name from XWikiDocument as doc where ");
        List<String> blackListedSpaces = getBlackListedSpaces();
        if (!blackListedSpaces.isEmpty()) {
            // Would have been nice to use a list parameter but the underlying implementation of the query module
            // doesn't support it so we have to compute the in(..) filter manually.
            for (int i = 0; i < blackListedSpaces.size(); i++) {
                statement.append(i == 0 ? "doc.space not in (" : ",");
                statement.append(":bSpace").append(i);
            }
            statement.append(") and ");
        }
        statement.append("(lower(doc.title) like '%'||:keyword||'%' or lower(doc.fullName) like '%'||:keyword||'%')");
        statement.append(" order by doc.space, doc.name");

        Query query = createHQLQuery(statement.toString());
        query.setWiki(wikiName).setOffset(offset).setLimit(limit);
        query.bindValue("keyword", keyword.toLowerCase());
        for (int i = 0; i < blackListedSpaces.size(); i++) {
            query.bindValue("bSpace" + i, blackListedSpaces.get(i));
        }

        return getWikiPages(searchDocumentReferences(query));
    }

    /**
     * Creates a new HQL query. Converts {@link QueryException} to a {@link RuntimeException}.
     * 
     * @param statement the query statement
     * @return the created query
     */
    private Query createHQLQuery(String statement)
    {
        try {
            return queryManager.createQuery(statement, Query.HQL);
        } catch (QueryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper function to retrieve the blacklisted spaces in this session, as they've been set in xwikivars.vm, when the
     * page edited with this WYSIWYG was loaded.
     * <p>
     * TODO: remove this when the public API will exclude them by default, or they'll be set in the configuration.
     * 
     * @return the list of blacklisted spaces from the session
     */
    protected abstract List<String> getBlackListedSpaces();

    /**
     * Helper function to create a list of {@link WikiPage}s from a list of document references.
     * 
     * @param documentReferences a list of document references
     * @return the list of {@link WikiPage}s corresponding to the given document references
     */
    protected abstract List<WikiPage> getWikiPages(List<DocumentReference> documentReferences);

    /**
     * Executes the given query and converts the result to a list of document references. The first two columns in each
     * result row must be the document space and name respectively.
     * 
     * @param query the query to be executed
     * @return the list of document references matching the result of executing the given query
     */
    private List<DocumentReference> searchDocumentReferences(Query query)
    {
        try {
            List<DocumentReference> documentReferences = new ArrayList<DocumentReference>();
            List<Object[]> results = query.execute();
            for (Object[] result : results) {
                documentReferences.add(new DocumentReference(query.getWiki(), (String) result[0], (String) result[1]));
            }
            return documentReferences;
        } catch (QueryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EntityConfig getEntityConfig(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference origin,
        ResourceReference destination)
    {
        return linkService.getEntityConfig(origin, destination);
    }

    @Override
    public ResourceReference parseLinkReference(String linkReference,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference baseReference)
    {
        return linkService.parseLinkReference(linkReference, baseReference);
    }

    @Override
    public Attachment getAttachment(org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference clientAttachmentReference)
    {
        AttachmentReference attachmentReference = entityReferenceConverter.convert(clientAttachmentReference);
        try {
            if (StringUtils.isBlank(documentAccessBridge.getAttachmentVersion(attachmentReference))) {
                logger.warn("Failed to get attachment: [{}] not found.", attachmentReference.getName());
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to get attachment: there was a problem with getting the document on the server.", e);
            return null;
        }

        Attachment attach = new Attachment();
        attach.setReference(clientAttachmentReference.getEntityReference());
        attach.setUrl(documentAccessBridge.getAttachmentURL(attachmentReference, false));
        return attach;
    }

    @Override
    public List<Attachment> getImageAttachments(WikiPageReference reference)
    {
        List<Attachment> imageAttachments = new ArrayList<Attachment>();
        List<Attachment> allAttachments = getAttachments(reference);
        for (Attachment attachment : allAttachments) {
            if (attachment.getMimeType().startsWith("image/")) {
                imageAttachments.add(attachment);
            }
        }
        return imageAttachments;
    }

    @Override
    public String getUploadURL(WikiPageReference reference)
    {
        String queryString = "form_token=" + csrf.getToken();
        return documentAccessBridge.getDocumentURL(entityReferenceConverter.convert(reference), "upload", queryString,
            null);
    }
}
